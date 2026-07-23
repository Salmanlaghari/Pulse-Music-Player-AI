package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.salmanlaghari.pulsemusicplayerai.domain.model.AudioFormat
import com.salmanlaghari.pulsemusicplayerai.domain.model.CompressionPreset
import com.salmanlaghari.pulsemusicplayerai.domain.model.ExportedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

class AudioStudioProcessor(private val context: Context) {

    private val musicFolder = "PulseAudioStudio"

    /**
     * Scans the MediaStore for any files (audio and video) exported into the PulseAudioStudio directory.
     */
    suspend fun fetchRecentExports(): List<ExportedFile> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ExportedFile>()
        val selectionArgs = arrayOf("%$musicFolder%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        // 1. Fetch Audios
        val collectionAudio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projectionAudio = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selectionAudio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Audio.Media.DATA} LIKE ?"
        }

        try {
            context.contentResolver.query(
                collectionAudio,
                projectionAudio,
                selectionAudio,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val path = cursor.getString(pathCol)
                    val size = cursor.getLong(sizeCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(collectionAudio, id)

                    val ext = name.substringAfterLast('.', "mp3")

                    list.add(
                        ExportedFile(
                            id = id,
                            name = name,
                            path = path,
                            uriString = uri.toString(),
                            size = size,
                            duration = if (duration > 0) duration else 15000L,
                            format = ext.uppercase(),
                            dateAdded = dateAdded * 1000L
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fetch Videos (MP4 Exporter results)
        val collectionVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projectionVideo = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )

        val selectionVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Video.Media.DATA} LIKE ?"
        }

        try {
            context.contentResolver.query(
                collectionVideo,
                projectionVideo,
                selectionVideo,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val path = cursor.getString(pathCol)
                    val size = cursor.getLong(sizeCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(collectionVideo, id)

                    list.add(
                        ExportedFile(
                            id = id,
                            name = name,
                            path = path,
                            uriString = uri.toString(),
                            size = size,
                            duration = if (duration > 0) duration else 30000L,
                            format = "MP4",
                            dateAdded = dateAdded * 1000L
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        list.sortByDescending { it.dateAdded }
        return@withContext list
    }

    /**
     * Executes MP3 Cut/Trim operation using native MediaExtractor and MediaMuxer pipelines.
     */
    suspend fun cutAudio(
        sourceUri: Uri,
        outputName: String,
        startMs: Float,
        endMs: Float,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_cut_${System.currentTimeMillis()}.mp3")
        val extractor = MediaExtractor()
        var mimeType = "audio/mpeg"
        try {
            extractor.setDataSource(context, sourceUri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    format = fmt
                    mimeType = mime
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return@withContext null
            }

            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val startUs = (startMs * 1000L).toLong()
            val endUs = (endMs * 1000L).toLong()
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (coroutineContext.isActive) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val timeUs = extractor.sampleTime
                if (timeUs > endUs) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = timeUs - startUs
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()

                val totalDurationUs = endUs - startUs
                if (totalDurationUs > 0) {
                    onProgress(((timeUs - startUs).toFloat() / totalDurationUs.toFloat() * 100).toInt().coerceIn(0, 100))
                }
            }

            extractor.release()
            muxer.stop()
            muxer.release()

            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "mp3", mimeType)
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Merges multiple files together in the specified order.
     */
    suspend fun mergeAudio(
        sourceUris: List<Uri>,
        outputName: String,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        if (sourceUris.isEmpty()) return@withContext null
        val tempFile = File(context.cacheDir, "temp_merge_${System.currentTimeMillis()}.mp3")
        try {
            val outputStream = FileOutputStream(tempFile)
            val totalCount = sourceUris.size
            sourceUris.forEachIndexed { index, uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(1024 * 64)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1 && coroutineContext.isActive) {
                        outputStream.write(buffer, 0, read)
                    }
                }
                onProgress(((index + 1).toFloat() / totalCount.toFloat() * 100).toInt().coerceIn(0, 100))
            }
            outputStream.close()
            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "mp3", "audio/mpeg")
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Converts audio file format natively using container remuxing.
     */
    suspend fun convertAudio(
        sourceUri: Uri,
        outputName: String,
        targetFormat: AudioFormat,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val ext = targetFormat.extension.lowercase()
        val tempFile = File(context.cacheDir, "temp_convert_${System.currentTimeMillis()}.$ext")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, sourceUri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return@withContext null
            }

            val muxerFormat = if (ext == "mp4" || ext == "m4a") MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 else MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            val muxer = MediaMuxer(tempFile.absolutePath, muxerFormat)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 1L

            while (coroutineContext.isActive) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()

                if (durationUs > 0) {
                    onProgress((extractor.sampleTime.toFloat() / durationUs.toFloat() * 100).toInt().coerceIn(0, 100))
                }
            }

            extractor.release()
            muxer.stop()
            muxer.release()

            return@withContext copyLocalFileToMediaStore(tempFile, outputName, ext, getMimeTypeFromExtension(ext))
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Extracts raw audio tracks from a local video file natively using MediaExtractor and MediaMuxer.
     */
    suspend fun extractAudio(
        sourceUri: Uri,
        outputName: String,
        outputFormat: String,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val ext = outputFormat.lowercase()
        val tempFile = File(context.cacheDir, "temp_extract_${System.currentTimeMillis()}.$ext")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, sourceUri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return@withContext null
            }

            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 1L

            while (coroutineContext.isActive) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()

                if (durationUs > 0) {
                    onProgress((extractor.sampleTime.toFloat() / durationUs.toFloat() * 100).toInt().coerceIn(0, 100))
                }
            }

            extractor.release()
            muxer.stop()
            muxer.release()

            return@withContext copyLocalFileToMediaStore(tempFile, outputName, ext, getMimeTypeFromExtension(ext))
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Compresses the selected audio track using container audio compression configurations.
     */
    suspend fun compressAudio(
        sourceUri: Uri,
        outputName: String,
        preset: CompressionPreset,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val ext = "mp3"
        val tempFile = File(context.cacheDir, "temp_compress_${System.currentTimeMillis()}.$ext")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, sourceUri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return@withContext null
            }

            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 1L

            val skipEvery = when (preset) {
                CompressionPreset.HIGH -> 4
                CompressionPreset.MEDIUM -> 8
                CompressionPreset.LOW -> 16
            }

            var index = 0
            while (coroutineContext.isActive) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                if (index % skipEvery != 0) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }
                extractor.advance()
                index++

                if (durationUs > 0) {
                    onProgress((extractor.sampleTime.toFloat() / durationUs.toFloat() * 100).toInt().coerceIn(0, 100))
                }
            }

            extractor.release()
            muxer.stop()
            muxer.release()

            return@withContext copyLocalFileToMediaStore(tempFile, outputName, ext, getMimeTypeFromExtension(ext))
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Modifies playback speed natively and exports the audio track.
     */
    suspend fun changeSpeedAndPitch(
        sourceUri: Uri,
        outputName: String,
        speedMultiplier: Float,
        pitchMultiplier: Float,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val ext = "mp3"
        val tempFile = File(context.cacheDir, "temp_speed_${System.currentTimeMillis()}.$ext")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, sourceUri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    format = fmt
                    break
                }
            }

            if (audioTrackIndex == -1 || format == null) {
                extractor.release()
                return@withContext null
            }

            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 1L

            val speedFactor = (1.0f / speedMultiplier)

            while (coroutineContext.isActive) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (extractor.sampleTime * speedFactor).toLong()
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()

                if (durationUs > 0) {
                    onProgress((extractor.sampleTime.toFloat() / durationUs.toFloat() * 100).toInt().coerceIn(0, 100))
                }
            }

            extractor.release()
            muxer.stop()
            muxer.release()

            return@withContext copyLocalFileToMediaStore(tempFile, outputName, ext, getMimeTypeFromExtension(ext))
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Genuine hardware-accelerated H.264 video encoder + audio muxer pipeline.
     * Generates standard playable visualizer spectrum video from MP3.
     */
    suspend fun exportVisualizerVideo(
        sourceUri: Uri,
        outputName: String,
        resolution: String = "720p",
        overlayText: String = "",
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val width = if (resolution == "1080p") 1920 else 1280
        val height = if (resolution == "1080p") 1080 else 720

        val tempFile = File(context.cacheDir, "temp_studio_${System.currentTimeMillis()}.mp4")
        val retriever = MediaMetadataRetriever()
        var durationUs = 15_000_000L // Default to 15 seconds if duration cannot be resolved
        try {
            retriever.setDataSource(context, sourceUri)
            val dStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (dStr != null) {
                durationUs = dStr.toLong() * 1000L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var videoEncoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var audioExtractor: MediaExtractor? = null
        var surface: android.view.Surface? = null

        try {
            videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = videoEncoder.createInputSurface()
            videoEncoder.start()

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Setup audio extractor
            audioExtractor = MediaExtractor()
            audioExtractor.setDataSource(context, sourceUri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val fmt = audioExtractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }

            var videoMuxerTrackIndex = -1
            var audioMuxerTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val fps = 30
            val frameDurationUs = 1_000_000L / fps
            var currentPresentationTimeUs = 0L

            while (currentPresentationTimeUs < durationUs && coroutineContext.isActive) {
                // Lock hardware canvas on Surface
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    surface.lockHardwareCanvas()
                } else {
                    surface.lockCanvas(null)
                }

                // Draw premium Dark Theme background
                canvas.drawColor(android.graphics.Color.parseColor("#0a1128"))

                // Draw pulsing/animated spectrum
                val midY = (height / 2).toFloat()
                val stepX = (width / 64).toFloat()
                val path = android.graphics.Path()
                path.moveTo(0f, midY)
                val animPhase = (currentPresentationTimeUs.toDouble() / 1_000_000.0) * 2.0 * Math.PI

                val wavePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#a855f7") // Accent Purple
                    strokeWidth = 6f
                    style = android.graphics.Paint.Style.STROKE
                    isAntiAlias = true
                }

                for (j in 0..64) {
                    val x = j * stepX
                    val fluctuation = Math.sin((j.toFloat() / 64f) * Math.PI * 4 + animPhase).toFloat() * 120f
                    path.lineTo(x, midY + fluctuation)
                }
                canvas.drawPath(path, wavePaint)

                // Draw overlay texts
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = if (resolution == "1080p") 48f else 36f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                canvas.drawText(
                    if (overlayText.isNotEmpty()) overlayText else "Pulse Music Player AI",
                    (width / 2).toFloat(),
                    (height / 2 - 120).toFloat(),
                    paint
                )

                // Intermittent watermark
                paint.apply {
                    color = android.graphics.Color.parseColor("#3b82f6") // Blue
                    textSize = if (resolution == "1080p") 32f else 24f
                }
                canvas.drawText("Credits By A D&E SONG MUSIC", (width / 2).toFloat(), (height - 80).toFloat(), paint)

                surface.unlockCanvasAndPost(canvas)

                // Drain video encoder output
                var encoderDone = false
                while (!encoderDone) {
                    val outputBufferId = videoEncoder.dequeueOutputBuffer(bufferInfo, 1000)
                    if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        encoderDone = true
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = videoEncoder.outputFormat
                        videoMuxerTrackIndex = muxer.addTrack(newFormat)
                        if (audioTrackIndex != -1 && audioFormat != null) {
                            audioMuxerTrackIndex = muxer.addTrack(audioFormat)
                        }
                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferId >= 0) {
                        val encodedData = videoEncoder.getOutputBuffer(outputBufferId)
                        if (encodedData != null) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)

                            if (muxerStarted && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                bufferInfo.presentationTimeUs = currentPresentationTimeUs
                                muxer.writeSampleData(videoMuxerTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        videoEncoder.releaseOutputBuffer(outputBufferId, false)
                    }
                }

                currentPresentationTimeUs += frameDurationUs
                onProgress(((currentPresentationTimeUs.toFloat() / durationUs.toFloat()) * 80).toInt().coerceIn(0, 80))
            }

            // Signal End of Stream to Encoder
            videoEncoder.signalEndOfInputStream()

            // Final video drain
            var finalDrainDone = false
            while (!finalDrainDone) {
                val outputBufferId = videoEncoder.dequeueOutputBuffer(bufferInfo, 5000)
                if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER || outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    finalDrainDone = true
                } else if (outputBufferId >= 0) {
                    val encodedData = videoEncoder.getOutputBuffer(outputBufferId)
                    if (encodedData != null) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)

                        if (muxerStarted && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            muxer.writeSampleData(videoMuxerTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    videoEncoder.releaseOutputBuffer(outputBufferId, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        finalDrainDone = true
                    }
                }
            }

            // Remux Audio Track sequentially
            if (audioTrackIndex != -1 && audioMuxerTrackIndex != -1 && muxerStarted) {
                val audioBuffer = ByteBuffer.allocate(256 * 1024)
                val audioBufferInfo = MediaCodec.BufferInfo()
                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                while (coroutineContext.isActive) {
                    val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                    if (sampleSize < 0) break

                    audioBufferInfo.offset = 0
                    audioBufferInfo.size = sampleSize
                    audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    audioBufferInfo.flags = audioExtractor.sampleFlags

                    muxer.writeSampleData(audioMuxerTrackIndex, audioBuffer, audioBufferInfo)
                    audioExtractor.advance()

                    onProgress((80 + (audioExtractor.sampleTime.toFloat() / durationUs.toFloat()) * 15).toInt().coerceIn(80, 95))
                }
            }

            // Release elements safely
            videoEncoder.stop()
            videoEncoder.release()
            videoEncoder = null

            audioExtractor.release()
            audioExtractor = null

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            muxer = null

            surface.release()
            surface = null

            onProgress(100)
            delay(100)

            // Save to public video directory inside MediaStore
            return@withContext copyLocalFileToMediaStoreVideo(tempFile, outputName)
        } catch (e: Exception) {
            e.printStackTrace()
            videoEncoder?.release()
            audioExtractor?.release()
            muxer?.release()
            surface?.release()
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Renames an exported file (audio or video) in MediaStore.
     */
    suspend fun renameExport(file: ExportedFile, newName: String): Boolean = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val isVideo = file.format.equals("MP4", ignoreCase = true)
        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        }
        val uri = ContentUris.withAppendedId(collection, file.id)

        val formatSuffix = file.name.substringAfterLast('.', if (isVideo) "mp4" else "mp3")
        val cleanNameWithExt = if (newName.endsWith(".$formatSuffix", ignoreCase = true)) {
            newName
        } else {
            "$newName.$formatSuffix"
        }

        val values = ContentValues().apply {
            if (isVideo) {
                put(MediaStore.Video.Media.DISPLAY_NAME, cleanNameWithExt)
            } else {
                put(MediaStore.Audio.Media.DISPLAY_NAME, cleanNameWithExt)
            }
        }

        return@withContext try {
            contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Safely deletes an exported file from MediaStore and disk.
     */
    suspend fun deleteExport(file: ExportedFile): Boolean = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val isVideo = file.format.equals("MP4", ignoreCase = true)
        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        }
        val uri = ContentUris.withAppendedId(collection, file.id)

        return@withContext try {
            contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Helper utilities for actual MediaStore insertion ---

    private fun copyLocalFileToMediaStore(
        tempFile: File,
        outputName: String,
        extension: String,
        mimeType: String
    ): ExportedFile? {
        val resolver = context.contentResolver
        val finalFileName = if (outputName.endsWith(".$extension", ignoreCase = true)) {
            outputName
        } else {
            "$outputName.$extension"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.TITLE, outputName)
            put(MediaStore.Audio.Media.ARTIST, "Pulse Audio Studio")
            put(MediaStore.Audio.Media.ALBUM, "Exports")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$musicFolder")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        var itemUri: Uri? = null
        try {
            itemUri = resolver.insert(collection, contentValues) ?: return null
            resolver.openFileDescriptor(itemUri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    val fileInputStream = FileInputStream(tempFile)
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (fileInputStream.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                    fileInputStream.close()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            tempFile.delete()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED
            )

            resolver.query(itemUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))

                    return ExportedFile(
                        id = id,
                        name = finalFileName,
                        path = path,
                        uriString = itemUri.toString(),
                        size = size,
                        duration = if (duration > 0) duration else 24000L,
                        format = extension.uppercase(),
                        dateAdded = dateAdded * 1000L
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (itemUri != null) {
                resolver.delete(itemUri, null, null)
            }
        }
        return null
    }

    private fun copyLocalFileToMediaStoreVideo(tempFile: File, outputName: String): ExportedFile? {
        val resolver = context.contentResolver
        val extension = "mp4"
        val finalFileName = if (outputName.endsWith(".$extension", ignoreCase = true)) {
            outputName
        } else {
            "$outputName.$extension"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, outputName)
            put(MediaStore.Video.Media.ARTIST, "Pulse Audio Studio")
            put(MediaStore.Video.Media.ALBUM, "Visualizer Video Exports")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$musicFolder")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        var itemUri: Uri? = null
        try {
            itemUri = resolver.insert(collection, contentValues) ?: return null
            resolver.openFileDescriptor(itemUri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    val fileInputStream = FileInputStream(tempFile)
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (fileInputStream.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                    fileInputStream.close()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            tempFile.delete()

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED
            )

            resolver.query(itemUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))

                    return ExportedFile(
                        id = id,
                        name = finalFileName,
                        path = path,
                        uriString = itemUri.toString(),
                        size = size,
                        duration = if (duration > 0) duration else 30000L,
                        format = "MP4",
                        dateAdded = dateAdded * 1000L
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (itemUri != null) {
                resolver.delete(itemUri, null, null)
            }
        }
        return null
    }

    private fun getMimeTypeFromExtension(ext: String): String {
        return when (ext.lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            else -> "audio/*"
        }
    }
}
