package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.IntDef
import com.salmanlaghari.pulsemusicplayerai.domain.model.AudioFormat
import com.salmanlaghari.pulsemusicplayerai.domain.model.BackgroundFit
import com.salmanlaghari.pulsemusicplayerai.domain.model.BackgroundAudioSource
import com.salmanlaghari.pulsemusicplayerai.domain.model.BuiltInBackgroundTracks
import com.salmanlaghari.pulsemusicplayerai.domain.model.CompressionPreset
import com.salmanlaghari.pulsemusicplayerai.domain.model.ExportedFile
import com.salmanlaghari.pulsemusicplayerai.domain.model.VideoResolution
import com.salmanlaghari.pulsemusicplayerai.domain.model.VisualizerVideoConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

class AudioStudioProcessor(private val context: Context) {

    private val musicFolder = "PulseAudioStudio"

    /**
     * Captures the most recent, human-readable failure reason from an export so the
     * UI can surface real diagnostics instead of a generic "Process Failed" message.
     */
    var lastExportError: String? = null
        private set

    @IntDef(
        MediaCodec.BUFFER_FLAG_SYNC_FRAME,
        MediaCodec.BUFFER_FLAG_KEY_FRAME,
        MediaCodec.BUFFER_FLAG_CODEC_CONFIG,
        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
        flag = true
    )
    @Retention(AnnotationRetention.SOURCE)
    private annotation class BufferFlags

    /**
     * Sanitizes extractor flags to only include valid MediaCodec buffer flags.
     */
    @BufferFlags
    private fun sanitizeFlags(extractorFlags: Int): Int {
        return extractorFlags and (
            MediaCodec.BUFFER_FLAG_SYNC_FRAME or
            MediaCodec.BUFFER_FLAG_KEY_FRAME or
            MediaCodec.BUFFER_FLAG_CODEC_CONFIG or
            MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
    }

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
            Log.e("AudioStudioProcessor", "Audio Studio operation failed: "+e.message, e)
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
            Log.e("AudioStudioProcessor", "Audio Studio operation failed: "+e.message, e)
        }

        list.sortByDescending { it.dateAdded }
        return@withContext list
    }

    /**
     * Trims an audio file to the requested range.
     *
     * If the source audio track is AAC it is remuxed losslessly into an MP4/M4A
     * container. For any other codec (MP3, FLAC, Vorbis/Opus ...) MediaMuxer
     * cannot carry the track, so the range is decoded to PCM and re-encoded to
     * AAC. Either way the produced file is a valid .m4a with a matching MIME
     * type — the previous implementation wrote an MP4 container but named it
     * ".mp3" with an "audio/mpeg" MIME type, which is why outputs failed to open.
     */
    suspend fun cutAudio(
        sourceUri: Uri,
        outputName: String,
        startMs: Float,
        endMs: Float,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val startUs = (startMs * 1000L).toLong().coerceAtLeast(0L)
        val endUs = (endMs * 1000L).toLong()
        if (endUs <= startUs) return@withContext null

        val tempFile = File(context.cacheDir, "temp_cut_${System.currentTimeMillis()}.m4a")

        // Determine the source codec first so we can pick remux vs re-encode.
        var sourceMime = ""
        val probe = MediaExtractor()
        try {
            probe.setDataSource(context, sourceUri, null)
            for (i in 0 until probe.trackCount) {
                val m = probe.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                if (m.startsWith("audio/")) { sourceMime = m; break }
            }
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Cut probe failed: " + e.message, e)
        } finally {
            try { probe.release() } catch (_: Exception) { }
        }

        if (sourceMime.isEmpty()) { if (tempFile.exists()) tempFile.delete(); return@withContext null }

        try {
            if (isMp4MuxableAudio(sourceMime)) {
                if (!remuxAudioRangeToM4a(sourceUri, tempFile, startUs, endUs, onProgress)) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext null
                }
            } else {
                val pcm = decodeToPcm(sourceUri, startUs, endUs) { p ->
                    onProgress((p * 0.5f).toInt().coerceIn(0, 50))
                } ?: run { if (tempFile.exists()) tempFile.delete(); return@withContext null }
                if (!encodePcmToM4a(pcm, tempFile, bitRate = 192_000, progStart = 50, progEnd = 100, onProgress = onProgress)) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext null
                }
            }
            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "m4a", "audio/mp4")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Cut failed: " + e.message, e)
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Lossless remux of an AAC audio range into an MP4/M4A container.
     */
    private suspend fun remuxAudioRangeToM4a(
        sourceUri: Uri,
        tempFile: File,
        startUs: Long,
        endUs: Long,
        onProgress: (Int) -> Unit
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor.setDataSource(context, sourceUri, null)
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if ((fmt.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
                    extractor.selectTrack(i)
                    format = fmt
                    break
                }
            }
            if (format == null) return false

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val track = muxer.addTrack(format)
            muxer.start()
            muxerStarted = true

            if (startUs > 0L) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(256 * 1024)
            val info = MediaCodec.BufferInfo()
            val rangeUs = if (endUs != Long.MAX_VALUE) (endUs - startUs).coerceAtLeast(1L) else (format?.let { if (it.containsKey(MediaFormat.KEY_DURATION)) it.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L) else 1L } ?: 1L)
            var wroteAny = false

            while (coroutineContext.isActive) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val timeUs = extractor.sampleTime
                if (endUs != Long.MAX_VALUE && timeUs > endUs) break

                info.offset = 0
                info.size = sampleSize
                info.presentationTimeUs = (timeUs - startUs).coerceAtLeast(0L)
                info.flags = sanitizeFlags(extractor.sampleFlags)
                muxer.writeSampleData(track, buffer, info)
                wroteAny = true
                extractor.advance()

                onProgress((((timeUs - startUs).toFloat() / rangeUs.toFloat()) * 100f).toInt().coerceIn(0, 100))
            }

            onProgress(100)
            return wroteAny
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Remux failed: " + e.message, e)
            return false
        } finally {
            try { extractor.release() } catch (_: Exception) { }
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) { }
            try { muxer?.release() } catch (_: Exception) { }
        }
    }

    /**
     * Merges multiple audio files into a single valid file.
     *
     * This performs a technically correct merge: every input is decoded to raw
     * PCM, resampled/re-channeled to a common target format, concatenated, and
     * then re-encoded to a single valid AAC/MP4 (.m4a) output. It does NOT
     * byte-concatenate compressed streams.
     */
    suspend fun mergeAudio(
        sourceUris: List<Uri>,
        outputName: String,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        if (sourceUris.isEmpty()) return@withContext null
        val tempFile = File(context.cacheDir, "temp_merge_${System.currentTimeMillis()}.m4a")
        try {
            // Decode the first file to establish the common target format.
            val first = decodeToPcm(sourceUris.first()) ?: run {
                if (tempFile.exists()) tempFile.delete()
                return@withContext null
            }
            val targetRate = first.sampleRate
            val targetChannels = first.channelCount

            val merged = ByteArrayOutputStream()
            merged.write(first.data)
            onProgress((1f / sourceUris.size * 40).toInt())

            for (i in 1 until sourceUris.size) {
                if (!coroutineContext.isActive) return@withContext null
                val pcm = decodeToPcm(sourceUris[i]) ?: continue
                merged.write(resamplePcm(pcm, targetRate, targetChannels))
                onProgress(((i + 1).toFloat() / sourceUris.size * 40).toInt())
            }

            val combined = PcmAudio(merged.toByteArray(), targetRate, targetChannels)
            merged.close()

            val ok = encodePcmToM4a(combined, tempFile, bitRate = 192_000, progStart = 40, progEnd = 100, onProgress = onProgress)
            if (!ok) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext null
            }
            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "m4a", "audio/mp4")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Merge failed: " + e.message, e)
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Converts an audio file to another format using a real decode + re-encode
     * pipeline.
     *
     * - WAV: full PCM decode written to a valid RIFF/WAVE (uncompressed) file.
     * - AAC / M4A: PCM re-encoded to AAC inside an MP4 container (.m4a).
     * - MP3 / FLAC / OGG: the Android platform does not ship reliable encoders
     *   for these formats, so we fall back to a genuinely valid AAC/M4A file
     *   with the correct extension and MIME type instead of writing a file with
     *   a misleading extension. The returned ExportedFile reports the actual
     *   produced format.
     */
    suspend fun convertAudio(
        sourceUri: Uri,
        outputName: String,
        targetFormat: AudioFormat,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val pcm = decodeToPcm(sourceUri) { p ->
            onProgress((p * 0.5f).toInt().coerceIn(0, 50))
        } ?: return@withContext null

        try {
            if (targetFormat == AudioFormat.WAV) {
                val tempFile = File(context.cacheDir, "temp_convert_${System.currentTimeMillis()}.wav")
                onProgress(75)
                if (!writeWav(pcm, tempFile)) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext null
                }
                onProgress(100)
                return@withContext copyLocalFileToMediaStore(tempFile, outputName, "wav", "audio/wav")
            }

            // Everything else is produced as a valid AAC/MP4 (.m4a) file.
            val tempFile = File(context.cacheDir, "temp_convert_${System.currentTimeMillis()}.m4a")
            val ok = encodePcmToM4a(pcm, tempFile, bitRate = 192_000, progStart = 50, progEnd = 100, onProgress = onProgress)
            if (!ok) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext null
            }
            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "m4a", "audio/mp4")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Convert failed: " + e.message, e)
        }
        return@withContext null
    }

    /**
     * Extracts the audio track from a video file.
     *
     * Requesting WAV yields a real uncompressed WAV; anything else yields a
     * valid AAC/M4A file (with correct extension + MIME). AAC tracks are remuxed
     * losslessly; other codecs are decoded and re-encoded.
     */
    suspend fun extractAudio(
        sourceUri: Uri,
        outputName: String,
        outputFormat: String,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val wantWav = outputFormat.equals("wav", ignoreCase = true)
        try {
            if (wantWav) {
                val pcm = decodeToPcm(sourceUri) { p -> onProgress((p * 0.7f).toInt().coerceIn(0, 70)) }
                    ?: return@withContext null
                val tempFile = File(context.cacheDir, "temp_extract_${System.currentTimeMillis()}.wav")
                onProgress(85)
                if (!writeWav(pcm, tempFile)) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext null
                }
                onProgress(100)
                return@withContext copyLocalFileToMediaStore(tempFile, outputName, "wav", "audio/wav")
            }

            // Probe the source audio codec.
            var sourceMime = ""
            val probe = MediaExtractor()
            try {
                probe.setDataSource(context, sourceUri, null)
                for (i in 0 until probe.trackCount) {
                    val m = probe.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                    if (m.startsWith("audio/")) { sourceMime = m; break }
                }
            } finally {
                try { probe.release() } catch (_: Exception) { }
            }
            if (sourceMime.isEmpty()) return@withContext null

            val tempFile = File(context.cacheDir, "temp_extract_${System.currentTimeMillis()}.m4a")
            if (isMp4MuxableAudio(sourceMime)) {
                if (!remuxAudioRangeToM4a(sourceUri, tempFile, 0L, Long.MAX_VALUE, onProgress)) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext null
                }
            } else {
                val pcm = decodeToPcm(sourceUri) { p -> onProgress((p * 0.5f).toInt().coerceIn(0, 50)) }
                    ?: run { if (tempFile.exists()) tempFile.delete(); return@withContext null }
                if (!encodePcmToM4a(pcm, tempFile, bitRate = 192_000, progStart = 50, progEnd = 100, onProgress = onProgress)) {
                    if (tempFile.exists()) tempFile.delete()
                    return@withContext null
                }
            }
            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "m4a", "audio/mp4")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Extract failed: " + e.message, e)
        }
        return@withContext null
    }

    /**
     * Genuinely compresses the audio by decoding it to PCM and re-encoding it to
     * AAC at a lower target bitrate. (The previous implementation dropped raw
     * compressed samples, which produced a corrupted/glitching file rather than
     * a smaller valid one.)
     */
    suspend fun compressAudio(
        sourceUri: Uri,
        outputName: String,
        preset: CompressionPreset,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_compress_${System.currentTimeMillis()}.m4a")
        try {
            val pcm = decodeToPcm(sourceUri) { p ->
                onProgress((p * 0.5f).toInt().coerceIn(0, 50))
            } ?: run { if (tempFile.exists()) tempFile.delete(); return@withContext null }

            // HIGH compression -> smallest file / lowest bitrate.
            val bitRate = when (preset) {
                CompressionPreset.HIGH -> 64_000
                CompressionPreset.MEDIUM -> 96_000
                CompressionPreset.LOW -> 160_000
            }

            val ok = encodePcmToM4a(pcm, tempFile, bitRate = bitRate, progStart = 50, progEnd = 100, onProgress = onProgress)
            if (!ok) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext null
            }
            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "m4a", "audio/mp4")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Compress failed: " + e.message, e)
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Changes playback speed (and correspondingly pitch) by genuinely resampling
     * the decoded PCM stream, then re-encoding it.
     *
     * NOTE: This is a resampling-based speed change, so pitch moves together
     * with speed (the classic "tape speed" behaviour). Independent pitch
     * shifting without a speed change requires a time-stretch/phase-vocoder DSP
     * stage, which the Android platform does not provide for offline export; the
     * `pitchMultiplier` is therefore combined into the resample ratio rather
     * than being faked.
     */
    suspend fun changeSpeedAndPitch(
        sourceUri: Uri,
        outputName: String,
        speedMultiplier: Float,
        pitchMultiplier: Float,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_speed_${System.currentTimeMillis()}.m4a")
        try {
            val pcm = decodeToPcm(sourceUri) { p ->
                onProgress((p * 0.5f).toInt().coerceIn(0, 50))
            } ?: run { if (tempFile.exists()) tempFile.delete(); return@withContext null }

            val ratio = (speedMultiplier.coerceIn(0.25f, 4.0f)) * (pitchMultiplier.coerceIn(0.25f, 4.0f))

            // Resampling to a lower rate while keeping the declared output rate
            // constant makes the audio play back faster (and higher pitched).
            val processed = if (kotlin.math.abs(ratio - 1.0f) < 0.001f) {
                pcm
            } else {
                val srcRateForRatio = (pcm.sampleRate / ratio).toInt().coerceAtLeast(4000)
                PcmAudio(
                    resamplePcm(pcm, srcRateForRatio, pcm.channelCount),
                    pcm.sampleRate,
                    pcm.channelCount
                )
            }

            val ok = encodePcmToM4a(processed, tempFile, bitRate = 192_000, progStart = 50, progEnd = 100, onProgress = onProgress)
            if (!ok) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext null
            }
            return@withContext copyLocalFileToMediaStore(tempFile, outputName, "m4a", "audio/mp4")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Speed/pitch change failed: " + e.message, e)
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext null
    }

    /**
     * Legacy entry point kept for source compatibility. Delegates to the real
     * config-driven exporter.
     */
    suspend fun exportVisualizerVideo(
        sourceUri: Uri,
        outputName: String,
        resolution: String = "720p",
        overlayText: String = "",
        onProgress: (Int) -> Unit
    ): ExportedFile? {
        val res = when (resolution) {
            "1080p" -> VideoResolution.FHD_1080
            "480p" -> VideoResolution.SD_480
            else -> VideoResolution.HD_720
        }
        return exportVisualizerVideo(
            sourceUri,
            VisualizerVideoConfig(
                resolution = res,
                title = overlayText,
                outputName = outputName
            ),
            onProgress
        )
    }

    /**
     * REAL MP3 -> MP4 export.
     *
     * Pipeline (three passes, each using standard Android media APIs):
     *  1. The audio is decoded to raw PCM (honouring the trim range). This PCM is
     *     used both as the audio track source and as the data the visualizer
     *     reacts to, so the video genuinely follows the music.
     *  2. Video frames are rendered with [VisualizerFrameRenderer] onto an
     *     H.264 encoder input surface -> a video-only MP4.
     *  3. The PCM is encoded to AAC (-> M4A), then the video and audio elementary
     *     streams are muxed into the final MP4 containing BOTH tracks.
     *
     * Pass 3 is required rather than muxing the source audio track directly:
     * MediaMuxer cannot carry an MP3 track inside MP4, which is exactly why the
     * previous implementation failed for MP3 input (the whole point of the
     * feature). Re-encoding to AAC produces a standards-compliant MP4.
     *
     * Every field of [config] (preset, aspect ratio, resolution, fps, trim,
     * background, text, scale/position, glow, colours) is consumed here, and the
     * same [config] + the same renderer drive the on-screen live preview.
     */
    suspend fun exportVisualizerVideo(
        sourceUri: Uri,
        config: VisualizerVideoConfig,
        onProgress: (Int) -> Unit
    ): ExportedFile? = withContext(Dispatchers.IO) {
        val stamp = System.currentTimeMillis()
        val videoOnly = File(context.cacheDir, "temp_vid_$stamp.mp4")
        val audioOnly = File(context.cacheDir, "temp_aud_$stamp.m4a")
        val finalFile = File(context.cacheDir, "temp_final_$stamp.mp4")
        lastExportError = null

        try {
            // ---------- Pass 1: decode real audio to PCM (trim aware) ----------
            val startUs = config.startMs.coerceAtLeast(0L) * 1000L
            val endUs = if (config.endMs > config.startMs) config.endMs * 1000L else Long.MAX_VALUE

            val pcm = decodeToPcm(sourceUri, startUs, endUs) { p ->
                onProgress((p * 0.20f).toInt().coerceIn(0, 20))
            } ?: run {
                lastExportError = "Audio decode failed: the selected file could not be decoded to PCM (unsupported/empty audio track)."
                return@withContext null
            }

            val bytesPerFrame = 2 * pcm.channelCount
            val totalAudioFrames = pcm.data.size / bytesPerFrame
            if (totalAudioFrames <= 0) {
                lastExportError = "Decoded audio is empty (0 frames)."
                return@withContext null
            }
            var durationUs = totalAudioFrames.toLong() * 1_000_000L / pcm.sampleRate
            // Bound the render so a very long track cannot produce an endless job.
            if (durationUs > MAX_EXPORT_MS * 1000L) durationUs = MAX_EXPORT_MS * 1000L

            // If a built-in background track is selected, layer it UNDER the source
            // audio at a reduced gain. The visualizer keeps reacting to the source
            // audio (so the on-screen preview stays truthful); only the output audio
            // track is the mix.
            val audioPcm = mixBackgroundTrack(pcm, config) ?: pcm

            // ---------- Pass 2: render + encode the video track ----------
            if (!renderVideoTrack(videoOnly, config, pcm, durationUs) { p ->
                    onProgress((20 + p * 0.60f).toInt().coerceIn(20, 80))
                }) {
                lastExportError = lastExportError ?: "Video rendering/encoding failed (H.264 encoder or surface error — see logcat)."
                return@withContext null
            }

            // ---------- Pass 3: encode audio + mux both tracks ----------
            if (!encodePcmToM4a(audioPcm, audioOnly, bitRate = 192_000, progStart = 80, progEnd = 90, onProgress = onProgress)) {
                lastExportError = "AAC audio encoding failed."
                return@withContext null
            }

            if (!muxVideoAndAudio(videoOnly, audioOnly, finalFile)) {
                Log.e("AudioStudioProcessor", "MP4 mux step failed")
                lastExportError = "Final MP4 muxing failed (could not combine video + audio tracks)."
                return@withContext null
            }
            onProgress(95)

            // Validate the container before reporting success. A broken MP4 must
            // never be presented to the user as a successful export.
            if (!validateMp4(finalFile)) {
                Log.e("AudioStudioProcessor", "MP4 final validation failed — not reporting success")
                lastExportError = "Exported MP4 failed validation (missing video/audio track or invalid duration)."
                return@withContext null
            }
            onProgress(97)

            return@withContext copyLocalFileToMediaStoreVideo(finalFile, config.outputName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Video export failed: ${e.message}", e)
            return@withContext null
        } finally {
            // Always clean up intermediates, including on cancellation.
            listOf(videoOnly, audioOnly, finalFile).forEach {
                try { if (it.exists()) it.delete() } catch (_: Exception) { }
            }
        }
    }

    /**
     * Renders every frame with the shared renderer and encodes it to H.264 in a
     * video-only MP4. Cancellation-safe; releases codec/surface/muxer in finally.
     */
    private suspend fun renderVideoTrack(
        outFile: File,
        config: VisualizerVideoConfig,
        pcm: PcmAudio,
        durationUs: Long,
        onProgress: (Int) -> Unit
    ): Boolean {
        // Pick an encoder configuration that the device actually supports,
        // preserving aspect ratio and falling back to safe values.
        val plan = buildVideoEncoderFormat(config.videoWidth, config.videoHeight, config.fps, config.videoBitRate)
        val width = plan.width
        val height = plan.height
        val fps = plan.fps

        var encoder: MediaCodec? = null
        var surface: android.view.Surface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var videoTrack = -1
        var lastVideoPts = -1L

        val background = loadBackgroundBitmap(config, width, height)
        // High-res Pulse logo burned into the frame (recycled in finally).
        val watermark = if (config.watermarkEnabled) WatermarkAssets.loadPulseWatermark(context) else null
        val renderer = VisualizerFrameRenderer(config, watermark)
        val bgPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

        // Offscreen software buffer: the ENTIRE frame (visualizer + optional
        // background image + watermark) is composited here first, then blitted
        // to the encoder's input surface with a SINGLE drawBitmap. Doing all the
        // heavy/complex Canvas work (gradient/shadow-layer shaders, the per-frame
        // watermark bitmap blit, thousands of glowing shapes from the visualizer
        // presets) on an offscreen Canvas — instead of issuing it straight onto
        // the encoder surface — keeps the encoder surface in a clean, consistent
        // state every frame. This eliminates the device-specific "H.264 encoder /
        // surface error" that re-appeared once the watermark + Task-F polish made
        // the per-frame draw much heavier on real-device timing. PR #34's rule is
        // preserved below: we still use lockCanvas(null), never lockHardwareCanvas.
        val frameBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val frameCanvas = android.graphics.Canvas(frameBitmap)
        val blitPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

        try {
            Log.d("AudioStudioProcessor", "Video encoder plan: ${width}x${height} @ ${fps}fps, bitrate=${plan.bitrate}, encoder=${plan.encoderName}")

            encoder = if (plan.encoderName != null) {
                try {
                    MediaCodec.createByCodecName(plan.encoderName)
                } catch (e: Exception) {
                    Log.w("AudioStudioProcessor", "createByCodecName failed, falling back to createEncoderByType: " + e.message)
                    MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                }
            } else {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            }
            encoder.configure(plan.format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = encoder.createInputSurface()
            Log.d("AudioStudioProcessor", "Encoder input surface created=${surface != null}")
            encoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val info = MediaCodec.BufferInfo()
            val frameDurationUs = 1_000_000L / fps
            val totalFrames = (durationUs / frameDurationUs).toInt().coerceAtLeast(1)

            for (frameIndex in 0 until totalFrames) {
                if (!coroutineContext.isActive) return false

                val ptsUs = frameIndex.toLong() * frameDurationUs

                // --- real audio analysis for THIS frame (identical maths to the
                // live preview's analysis table) ---
                val (mags, wave) = frameData(pcm, ptsUs)
                val beatEnergy = AudioSpectrum.beat(mags)
                // Cyclic phase driven by time, modulated by real energy.
                val beatPhase = ((ptsUs / 1_000_000f) * (0.5f + beatEnergy)) % 1f

                // Compose the whole frame onto the offscreen software Canvas
                // (visualizer + optional background image + animated watermark),
                // then blit it to the encoder input surface with a single
                // surface-safe drawBitmap. Every complex/scene op stays OFF the
                // encoder surface so its state never becomes inconsistent frame
                // to frame. PR #34's rule is preserved: we still use
                // lockCanvas(null), never lockHardwareCanvas().
                var drewBackground = false
                if (background != null) {
                    frameCanvas.drawColor(android.graphics.Color.BLACK)
                    frameCanvas.drawBitmap(background, null, backgroundDestRect(background, width, height, config), bgPaint)
                    drewBackground = true
                }
                renderer.draw(frameCanvas, width, height, mags, wave, beatPhase, drewBackground, ptsUs)

                // Single surface-safe blit of the finished frame.
                val canvas = lockEncoderCanvas(surface)
                if (canvas == null) {
                    Log.e("AudioStudioProcessor", "Failed to lock encoder surface canvas")
                    return false
                }
                try {
                    canvas.drawBitmap(frameBitmap, null, android.graphics.Rect(0, 0, width, height), blitPaint)
                } finally {
                    surface.unlockCanvasAndPost(canvas)
                }

                // Drain whatever the encoder has produced so far.
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(info, 0)
                    if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrack = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        continue
                    }
                    if (outIndex >= 0) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        val buf = encoder.getOutputBuffer(outIndex)
                        if (buf != null) {
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            // Surface input timestamps come from the wall clock but the
                            // producer; override with a deterministic, strictly increasing
                            // clock so MediaMuxer never rejects duplicate/!monotonic pts.
                            val pts = maxOf(ptsUs, lastVideoPts + 1)
                            lastVideoPts = pts
                            info.presentationTimeUs = pts
                            muxer.writeSampleData(videoTrack, buf, info)
                        }
                    }
                        encoder.releaseOutputBuffer(outIndex, false)
                    }
                }

                onProgress(((frameIndex + 1).toFloat() / totalFrames * 100).toInt().coerceIn(0, 99))
            }

            // Flush the encoder.
            encoder.signalEndOfInputStream()
            val finalFrameStep = frameDurationUs.coerceAtLeast(1L)
            while (true) {
                if (!coroutineContext.isActive) return false
                val outIndex = encoder.dequeueOutputBuffer(info, 10_000L)
                if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        videoTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    continue
                }
                if (outIndex >= 0) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        val buf = encoder.getOutputBuffer(outIndex)
                        if (buf != null) {
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            lastVideoPts += finalFrameStep
                            info.presentationTimeUs = lastVideoPts
                            muxer.writeSampleData(videoTrack, buf, info)
                        }
                    }
                    val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    encoder.releaseOutputBuffer(outIndex, false)
                    if (eos) break
                }
            }

            onProgress(100)
            return muxerStarted
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Video render failed: " + e.message, e)
            return false
        } finally {
            try { encoder?.stop() } catch (_: Exception) { }
            try { encoder?.release() } catch (_: Exception) { }
            try { surface?.release() } catch (_: Exception) { }
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) { }
            try { muxer?.release() } catch (_: Exception) { }
            try { background?.recycle() } catch (_: Exception) { }
            try { watermark?.recycle() } catch (_: Exception) { }
            try { frameBitmap.recycle() } catch (_: Exception) { }
        }
    }

    /**
     * Inspects the device's H.264 encoders and returns a [MediaFormat] whose
     * width/height/frame-rate/bitrate/profile/level are guaranteed to be within
     * the hardware's capabilities, falling back to safe values when the requested
     * resolution or FPS is unsupported. The aspect ratio of the requested frame
     * is preserved by scaling both dimensions by the same factor.
     */
    private fun buildVideoEncoderFormat(
        requestedW: Int,
        requestedH: Int,
        requestedFps: Int,
        requestedBitrate: Int
    ): EncoderPlan {
        var w = requestedW.coerceAtLeast(2)
        var h = requestedH.coerceAtLeast(2)
        var fps = requestedFps.coerceIn(15, 60)
        var bitrate = requestedBitrate.coerceAtLeast(500_000)
        var encoderName: String? = null
        var prof: Int? = null
        var lvl: Int? = null

        try {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in list.codecInfos) {
                if (!info.isEncoder) continue
                if (!info.supportedTypes.contains(MediaFormat.MIMETYPE_VIDEO_AVC)) continue
                val caps = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                encoderName = info.name

                val vc = caps.videoCapabilities
                val maxW = vc.supportedWidths.upper
                val maxH = vc.supportedHeights.upper
                val minW = vc.supportedWidths.lower.coerceAtLeast(2)
                val minH = vc.supportedHeights.lower.coerceAtLeast(2)
                val alignW = vc.widthAlignment.coerceAtLeast(1)
                val alignH = vc.heightAlignment.coerceAtLeast(1)

                // Preserve aspect ratio by scaling both dimensions equally.
                val scale = minOf(1f, maxW.toFloat() / requestedW, maxH.toFloat() / requestedH)
                w = ((requestedW * scale) / alignW).toInt() * alignW
                h = ((requestedH * scale) / alignH).toInt() * alignH
                w = w.coerceIn(minW, maxW)
                h = h.coerceIn(minH, maxH)

                val fpsRange = vc.supportedFrameRates
                if (fpsRange.upper < fps) fps = fpsRange.upper.toInt().coerceAtLeast(15)
                val brRange = vc.bitrateRange
                bitrate = bitrate.coerceIn(brRange.lower, brRange.upper)

                // Select a widely supported profile/level only if this exact encoder
                // advertises it, so configure() can never reject our request.
                val candidates = listOf(
                    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh to android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel31,
                    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain to android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel31,
                    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline to android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel31
                )
                for ((p, l) in candidates) {
                    if (caps.profileLevels.any { it.profile == p && it.level >= l }) {
                        prof = p
                        lvl = l
                        break
                    }
                }
                break
            }
        } catch (e: Exception) {
            Log.w("AudioStudioProcessor", "Encoder capability probe failed, using requested values: " + e.message)
        }

        if (w <= 0 || h <= 0) { w = 320; h = 240 }
        w = (w / 2) * 2
        h = (h / 2) * 2

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            if (prof != null && lvl != null) {
                setInteger(MediaFormat.KEY_PROFILE, prof)
                setInteger(MediaFormat.KEY_LEVEL, lvl)
            }
        }

        return EncoderPlan(format, w, h, fps, bitrate, encoderName)
    }

    private data class EncoderPlan(
        val format: MediaFormat,
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrate: Int,
        val encoderName: String?
    )

    /**
     * Locks the encoder input surface for drawing.
     *
     * We deliberately use [android.view.Surface.lockCanvas] and NEVER
     * [android.view.Surface.lockHardwareCanvas]: the latter throws
     * UnsupportedOperationException on real MediaCodec encoder surfaces and was the
     * root cause of the "Process Failed" MP4 export crash.
     */
    private fun lockEncoderCanvas(surface: android.view.Surface): android.graphics.Canvas? {
        return try {
            surface.lockCanvas(null)
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "lockCanvas on encoder surface failed: " + e.message, e)
            null
        }
    }

    /**
     * Validates the finished MP4: the file must exist, be non-empty, and contain
     * both a video track and an audio track with a positive duration. Returns
     * false (with diagnostics) if the export produced an invalid container so we
     * never report success for a broken file.
     */
    private fun validateMp4(file: File): Boolean {
        if (!file.exists()) {
            Log.e("AudioStudioProcessor", "MP4 validation FAILED: file does not exist at ${file.absolutePath}")
            return false
        }
        if (file.length() == 0L) {
            Log.e("AudioStudioProcessor", "MP4 validation FAILED: file is empty (0 bytes)")
            return false
        }
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var hasVideo = false
            var hasAudio = false
            var durationUs = 0L
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) hasVideo = true
                else if (mime.startsWith("audio/")) hasAudio = true
            }
            if (extractor.trackCount > 0) {
                val f = extractor.getTrackFormat(0)
                if (f.containsKey(MediaFormat.KEY_DURATION)) durationUs = f.getLong(MediaFormat.KEY_DURATION)
            }
            Log.d("AudioStudioProcessor", "MP4 validation: size=${file.length()}, tracks=${extractor.trackCount}, video=$hasVideo, audio=$hasAudio, durationUs=$durationUs")
            if (!hasVideo || !hasAudio) {
                Log.e("AudioStudioProcessor", "MP4 validation FAILED: missing track (video=$hasVideo, audio=$hasAudio)")
                false
            } else if (durationUs <= 0L) {
                Log.e("AudioStudioProcessor", "MP4 validation FAILED: non-positive duration ($durationUs)")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "MP4 validation FAILED with exception: " + e.message, e)
            false
        } finally {
            try { extractor.release() } catch (_: Exception) { }
        }
    }

    /** Computes the destination rect for the background image honouring crop/fit. */
    private fun backgroundDestRect(
        bmp: android.graphics.Bitmap,
        width: Int,
        height: Int,
        config: VisualizerVideoConfig
    ): android.graphics.RectF = VisualizerBackground.destRect(
        bmp.width, bmp.height, width, height, config.backgroundFit
    )

    /** Loads and down-samples the optional background image. */
    private fun loadBackgroundBitmap(
        config: VisualizerVideoConfig,
        width: Int,
        height: Int
    ): android.graphics.Bitmap? {
        val uriStr = config.backgroundImageUri ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val opts = android.graphics.BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                android.graphics.BitmapFactory.decodeStream(input, null, opts)
            }
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Background image load failed: " + e.message, e)
            null
        }
    }

    /**
     * Muxes a video-only MP4 and an audio-only M4A into one MP4 containing both
     * an H.264 video track and an AAC audio track.
     */
    private suspend fun muxVideoAndAudio(videoFile: File, audioFile: File, outFile: File): Boolean {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)

            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val fmt = videoExtractor.getTrackFormat(i)
                if ((fmt.getString(MediaFormat.KEY_MIME) ?: "").startsWith("video/")) {
                    videoExtractor.selectTrack(i); videoFormat = fmt; break
                }
            }
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val fmt = audioExtractor.getTrackFormat(i)
                if ((fmt.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
                    audioExtractor.selectTrack(i); audioFormat = fmt; break
                }
            }
            if (videoFormat == null) return false

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val vTrack = muxer.addTrack(videoFormat)
            val aTrack = if (audioFormat != null) muxer.addTrack(audioFormat) else -1
            muxer.start()
            started = true

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()

            // Copy video samples.
            while (true) {
                if (!coroutineContext.isActive) return false
                val size = videoExtractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = videoExtractor.sampleTime
                info.flags = sanitizeFlags(videoExtractor.sampleFlags)
                muxer.writeSampleData(vTrack, buffer, info)
                videoExtractor.advance()
            }

            // Copy audio samples.
            if (aTrack >= 0) {
                while (true) {
                    if (!coroutineContext.isActive) return false
                    val size = audioExtractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = audioExtractor.sampleTime
                    info.flags = sanitizeFlags(audioExtractor.sampleFlags)
                    muxer.writeSampleData(aTrack, buffer, info)
                    audioExtractor.advance()
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Final mux failed: " + e.message, e)
            return false
        } finally {
            try { videoExtractor.release() } catch (_: Exception) { }
            try { audioExtractor.release() } catch (_: Exception) { }
            try { if (started) muxer?.stop() } catch (_: Exception) { }
            try { muxer?.release() } catch (_: Exception) { }
        }
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
            Log.e("AudioStudioProcessor", "Audio Studio operation failed: "+e.message, e)
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
            Log.e("AudioStudioProcessor", "Audio Studio operation failed: "+e.message, e)
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
            Log.e("AudioStudioProcessor", "Audio Studio operation failed: "+e.message, e)
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
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/PulseAudioStudio")
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
                        duration = if (duration > 0) duration else 24000L,
                        format = extension.uppercase(),
                        dateAdded = dateAdded * 1000L
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Audio Studio operation failed: "+e.message, e)
            if (itemUri != null) {
                resolver.delete(itemUri, null, null)
            }
        }
        return null
    }

    // ---------------------------------------------------------------------
    // Real PCM decode / encode engine
    //
    // All lossy-format operations funnel through here so that we never claim a
    // conversion/merge/compression that is really just container remuxing or a
    // byte-level concatenation.
    // ---------------------------------------------------------------------

    /** Raw signed 16-bit little-endian interleaved PCM. */
    private class PcmAudio(
        val data: ByteArray,
        val sampleRate: Int,
        val channelCount: Int
    )

    /** True when a compressed audio track can legally be muxed into MP4/M4A as-is. */
    private fun isMp4MuxableAudio(mime: String): Boolean =
        mime.equals("audio/mp4a-latm", ignoreCase = true)

    /**
     * Fully decodes an audio stream (optionally only a time range) to raw PCM
     * using MediaExtractor + MediaCodec. Cancellation-aware; all native
     * resources are released in a finally block.
     */
    private suspend fun decodeToPcm(
        uri: Uri,
        startUs: Long = 0L,
        endUs: Long = Long.MAX_VALUE,
        onProgress: (Int) -> Unit = {}
    ): PcmAudio? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)

            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if ((fmt.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
                    trackIndex = i
                    format = fmt
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2
            val trackDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            val rangeEndUs = if (endUs == Long.MAX_VALUE) Long.MAX_VALUE else endUs
            val rangeUs = when {
                rangeEndUs != Long.MAX_VALUE -> (rangeEndUs - startUs).coerceAtLeast(1L)
                trackDurationUs > 0L -> trackDurationUs
                else -> 0L
            }

            if (startUs > 0L) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val out = ByteArrayOutputStream()
            var sawInputEos = false
            var sawOutputEos = false

            while (!sawOutputEos) {
                if (!coroutineContext.isActive) return null

                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000L)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        val sampleTime = extractor.sampleTime
                        if (sampleSize < 0 || (rangeEndUs != Long.MAX_VALUE && sampleTime > rangeEndUs)) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                            if (rangeUs > 0L) {
                                val done = (sampleTime - startUs).toFloat() / rangeUs.toFloat()
                                onProgress((done * 100f).toInt().coerceIn(0, 99))
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000L)
                if (outIndex >= 0) {
                    if (info.size > 0) {
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null) {
                            val chunk = ByteArray(info.size)
                            outBuf.position(info.offset)
                            outBuf.get(chunk, 0, info.size)
                            out.write(chunk)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEos = true
                }
            }

            onProgress(100)
            if (out.size() == 0) return null
            return PcmAudio(out.toByteArray(), sampleRate, channelCount)
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "PCM decode failed: " + e.message, e)
            return null
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            try { codec?.release() } catch (_: Exception) { }
            try { extractor.release() } catch (_: Exception) { }
        }
    }

    /**
     * Decodes a bundled raw resource (e.g. a built-in background music WAV under
     * res/raw) to PCM using the same MediaCodec pipeline as [decodeToPcm]. Used
     * to layer a curated background track under the user's audio during export.
     */
    private suspend fun decodeRawResourceToPcm(resId: Int): PcmAudio? {
        val afd = try {
            context.resources.openRawResourceFd(resId)
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Raw resource open failed: " + e.message, e)
            return null
        } ?: return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if ((fmt.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
                    trackIndex = i
                    format = fmt
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null
            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val out = ByteArrayOutputStream()
            var sawInputEos = false
            var sawOutputEos = false

            while (!sawOutputEos) {
                if (!coroutineContext.isActive) return null
                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000L)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10_000L)
                if (outIndex >= 0) {
                    if (info.size > 0) {
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null) {
                            val chunk = ByteArray(info.size)
                            outBuf.position(info.offset)
                            outBuf.get(chunk, 0, info.size)
                            out.write(chunk)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEos = true
                }
            }
            if (out.size() == 0) return null
            return PcmAudio(out.toByteArray(), sampleRate, channelCount)
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "Raw PCM decode failed: " + e.message, e)
            return null
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            try { codec?.release() } catch (_: Exception) { }
            try { extractor.release() } catch (_: Exception) { }
            try { afd.close() } catch (_: Exception) { }
        }
    }

    /**
     * Mixes a built-in background track under the source audio.
     *
     * The background PCM is resampled to the source's sample rate / channel count,
     * looped to exactly match the source length, and added at [config.backgroundTrackVolume]
     * gain (so the user's track remains clearly on top). Returns null when no
     * background track is selected or it cannot be decoded — in which case the
     * caller falls back to the source audio only (original behaviour).
     */
    private suspend fun mixBackgroundTrack(source: PcmAudio, config: VisualizerVideoConfig): PcmAudio? {
        val sel = config.backgroundTrackResName ?: return null
        if (sel == BuiltInBackgroundTracks.NONE) return null

        // Resolve the selection to a catalogue track so we know whether the audio
        // is a BUNDLED res loop or a REMOTE royalty-free loop streamed on demand.
        val track = BuiltInBackgroundTracks.resolve(sel)
        val bg = if (track?.audioSource == BackgroundAudioSource.REMOTE && track.remoteUrl != null) {
            // Best-effort on-demand download + decode. Any failure (CDN down,
            // unsupported format) returns null and we fall back to source-only.
            decodeRemoteUrlToPcm(track.remoteUrl)
        } else {
            val resName = track?.resEntryName ?: sel
            val resId = try {
                context.resources.getIdentifier(resName, "raw", context.packageName)
            } catch (e: Exception) { 0 }
            if (resId == 0) null else decodeRawResourceToPcm(resId)
        } ?: return null

        val bgMatched = if (bg.sampleRate != source.sampleRate || bg.channelCount != source.channelCount) {
            PcmAudio(resamplePcm(bg, source.sampleRate, source.channelCount), source.sampleRate, source.channelCount)
        } else bg

        val gain = config.backgroundTrackVolume.coerceIn(0f, 1f)
        val out = ByteArray(source.data.size)
        val srcShorts = ByteBuffer.wrap(source.data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val bgShorts = ByteBuffer.wrap(bgMatched.data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val outShorts = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val bgSamples = bgShorts.capacity()
        val n = minOf(outShorts.capacity(), srcShorts.capacity())
        for (i in 0 until n) {
            val s = srcShorts.get(i).toInt()
            val b = if (bgSamples > 0) bgShorts.get(i % bgSamples).toInt() else 0
            val mixed = (s + (b * gain)).toInt().coerceIn(-32768, 32767)
            outShorts.put(i, mixed.toShort())
        }
        return PcmAudio(out, source.sampleRate, source.channelCount)
    }

    /**
     * Downloads a royalty-free background loop from [url] to the cache and decodes
     * it to PCM. Returns null on any failure so callers can gracefully fall back
     * to source-audio-only (the export still succeeds, just without the bed).
     */
    private suspend fun decodeRemoteUrlToPcm(url: String): PcmAudio? = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "bg_remote_${System.currentTimeMillis()}.mp3")
        try {
            if (!downloadToFile(url, tmp)) return@withContext null
            val uri = Uri.fromFile(tmp)
            decodeToPcm(uri)
        } catch (e: Exception) {
            Log.w("AudioStudioProcessor", "Remote bg decode failed: ${e.message}")
            null
        } finally {
            try { if (tmp.exists()) tmp.delete() } catch (_: Exception) { }
        }
    }

    /** Streams [url] bytes into [outFile]. Returns true on success. */
    private fun downloadToFile(url: String, outFile: File): Boolean {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { fos ->
                    val buf = ByteArray(32 * 1024)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) fos.write(buf, 0, read)
                }
            }
            outFile.length() > 0L
        } catch (e: Exception) {
            Log.w("AudioStudioProcessor", "bg download failed: ${e.message}")
            false
        }
    }


    /**
     * Encodes raw PCM to AAC-LC inside an MP4 container (a valid .m4a file).
     * AAC is the one lossy audio encoder the Android platform guarantees.
     */
    private suspend fun encodePcmToM4a(
        pcm: PcmAudio,
        tempFile: File,
        bitRate: Int,
        progStart: Int = 0,
        progEnd: Int = 100,
        onProgress: (Int) -> Unit = {}
    ): Boolean {
        if (pcm.data.isEmpty()) return false

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var muxerTrack = -1

        try {
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                pcm.sampleRate,
                pcm.channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            }

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val info = MediaCodec.BufferInfo()
            val bytesPerFrame = 2 * pcm.channelCount
            val totalBytes = pcm.data.size
            var inputOffset = 0
            var presentationTimeUs = 0L
            var sawInputEos = false
            var sawOutputEos = false

            while (!sawOutputEos) {
                if (!coroutineContext.isActive) return false
                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000L)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)
                        if (inBuf == null) {
                            codec.queueInputBuffer(inIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            inBuf.clear()
                            val remaining = totalBytes - inputOffset
                            // Keep chunks frame-aligned so channel interleaving stays intact.
                            var chunk = minOf(inBuf.capacity(), remaining)
                            chunk -= (chunk % bytesPerFrame)
                            if (chunk <= 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEos = true
                            } else {
                                inBuf.put(pcm.data, inputOffset, chunk)
                                codec.queueInputBuffer(inIndex, 0, chunk, presentationTimeUs, 0)
                                inputOffset += chunk
                                val framesQueued = inputOffset.toLong() / bytesPerFrame
                                presentationTimeUs = framesQueued * 1_000_000L / pcm.sampleRate
                                val ratio = inputOffset.toFloat() / totalBytes.toFloat()
                                onProgress((progStart + ratio * (progEnd - progStart)).toInt().coerceIn(progStart, progEnd))
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000L)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        muxerTrack = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outIndex >= 0) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // Codec config is delivered to the muxer via addTrack().
                        info.size = 0
                    }
                    if (info.size > 0 && muxerStarted) {
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null) {
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            muxer.writeSampleData(muxerTrack, outBuf, info)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEos = true
                }
            }

            onProgress(progEnd)
            return muxerStarted
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "AAC encode failed: " + e.message, e)
            return false
        } finally {
            try { codec?.stop() } catch (_: Exception) { }
            try { codec?.release() } catch (_: Exception) { }
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) { }
            try { muxer?.release() } catch (_: Exception) { }
        }
    }

    /** Writes raw PCM out as a valid uncompressed RIFF/WAVE file. */
    private fun writeWav(pcm: PcmAudio, tempFile: File): Boolean {
        return try {
            val dataSize = pcm.data.size
            val byteRate = pcm.sampleRate * pcm.channelCount * 2
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1) // PCM
            header.putShort(pcm.channelCount.toShort())
            header.putInt(pcm.sampleRate)
            header.putInt(byteRate)
            header.putShort((pcm.channelCount * 2).toShort())
            header.putShort(16) // bits per sample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)

            FileOutputStream(tempFile).use { fos ->
                fos.write(header.array())
                fos.write(pcm.data)
                fos.flush()
            }
            true
        } catch (e: Exception) {
            Log.e("AudioStudioProcessor", "WAV write failed: " + e.message, e)
            false
        }
    }

    /**
     * Linear-interpolation resampler + channel adapter. Used to normalise
     * mismatched inputs to a single target format before merging/encoding.
     */
    private fun resamplePcm(pcm: PcmAudio, targetRate: Int, targetChannels: Int): ByteArray {
        if (pcm.sampleRate == targetRate && pcm.channelCount == targetChannels) return pcm.data
        if (pcm.data.isEmpty() || pcm.channelCount <= 0) return pcm.data

        val inShorts = ShortArray(pcm.data.size / 2)
        ByteBuffer.wrap(pcm.data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(inShorts)

        val inFrames = inShorts.size / pcm.channelCount
        if (inFrames <= 0) return pcm.data

        val outFrames = (inFrames.toLong() * targetRate / pcm.sampleRate).toInt().coerceAtLeast(1)
        val outShorts = ShortArray(outFrames * targetChannels)

        for (i in 0 until outFrames) {
            val srcPos = i.toDouble() * pcm.sampleRate / targetRate
            val i0 = srcPos.toInt().coerceIn(0, inFrames - 1)
            val i1 = (i0 + 1).coerceAtMost(inFrames - 1)
            val frac = (srcPos - i0).toFloat()

            for (ch in 0 until targetChannels) {
                val srcCh = if (ch < pcm.channelCount) ch else pcm.channelCount - 1
                val s0 = inShorts[i0 * pcm.channelCount + srcCh].toFloat()
                val s1 = inShorts[i1 * pcm.channelCount + srcCh].toFloat()
                val v = s0 + (s1 - s0) * frac
                outShorts[i * targetChannels + ch] = v.coerceIn(-32768f, 32767f).toInt().toShort()
            }
        }

        val outBytes = ByteArray(outShorts.size * 2)
        ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outShorts)
        return outBytes
    }

    // ---------------------------------------------------------------------
    // Shared audio analysis (live preview AND export use this identical code,
    // which is what makes the preview a truthful representation of the export).
    // ---------------------------------------------------------------------

    companion object {
        const val ANALYSIS_WINDOW = 1024
        const val ANALYSIS_BINS = 64
        const val WAVEFORM_POINTS = 96
        /** Hard cap so analysis/export memory stays bounded. */
        const val MAX_EXPORT_MS = 10 * 60 * 1000L
    }

    /**
     * A precomputed, frame-indexed spectrum/waveform table for a track.
     *
     * The live preview looks frames up by playback position, so the on-screen
     * visualizer is driven by the real decoded audio rather than a decorative
     * animation — and without needing the RECORD_AUDIO permission that
     * android.media.audiofx.Visualizer would demand.
     */
    class SpectrumTrack(
        val fps: Int,
        val durationMs: Long,
        val magnitudes: Array<FloatArray>,
        val waveforms: Array<FloatArray>
    ) {
        val frameCount: Int get() = magnitudes.size

        /** Returns the analysis frame for a playback position in ms. */
        fun frameAt(positionMs: Long): Int {
            if (frameCount == 0) return 0
            return ((positionMs * fps) / 1000L).toInt().coerceIn(0, frameCount - 1)
        }
    }

    /** Computes the magnitude + waveform pair for one instant of PCM. */
    private fun frameData(pcm: PcmAudio, ptsUs: Long): Pair<FloatArray, FloatArray> {
        val sampleIndex = (ptsUs * pcm.sampleRate / 1_000_000L).toInt()
        val byteOffset = sampleIndex * 2 * pcm.channelCount
        val mono = AudioSpectrum.pcmWindowToMono(pcm.data, byteOffset, ANALYSIS_WINDOW, pcm.channelCount)
        val mags = AudioSpectrum.magnitudes(mono, ANALYSIS_BINS)
        // Decimate the window down to the shared waveform resolution.
        val wave = FloatArray(WAVEFORM_POINTS)
        for (i in 0 until WAVEFORM_POINTS) {
            wave[i] = mono[(i * ANALYSIS_WINDOW / WAVEFORM_POINTS).coerceIn(0, mono.size - 1)]
        }
        return mags to wave
    }

    /**
     * Decodes the track and builds the frame-indexed analysis table used by the
     * live preview. Runs off the main thread and is cancellation aware.
     */
    suspend fun analyzeSpectrum(
        sourceUri: Uri,
        fps: Int = 30,
        startMs: Long = 0L,
        endMs: Long = 0L,
        onProgress: (Int) -> Unit = {}
    ): SpectrumTrack? = withContext(Dispatchers.IO) {
        val startUs = startMs.coerceAtLeast(0L) * 1000L
        val endUs = if (endMs > startMs) endMs * 1000L else Long.MAX_VALUE

        val pcm = decodeToPcm(sourceUri, startUs, endUs) { p ->
            onProgress((p * 0.8f).toInt().coerceIn(0, 80))
        } ?: return@withContext null

        val safeFps = fps.coerceIn(15, 60)
        val bytesPerFrame = 2 * pcm.channelCount
        val totalSamples = pcm.data.size / bytesPerFrame
        var durationMs = totalSamples.toLong() * 1000L / pcm.sampleRate
        if (durationMs > MAX_EXPORT_MS) durationMs = MAX_EXPORT_MS

        val frameDurationUs = 1_000_000L / safeFps
        val frameCount = ((durationMs * 1000L) / frameDurationUs).toInt().coerceIn(1, 60 * 60 * safeFps)

        val mags = Array(frameCount) { FloatArray(ANALYSIS_BINS) }
        val waves = Array(frameCount) { FloatArray(WAVEFORM_POINTS) }

        for (i in 0 until frameCount) {
            if (!coroutineContext.isActive) return@withContext null
            val (m, w) = frameData(pcm, i.toLong() * frameDurationUs)
            mags[i] = m
            waves[i] = w
            if (i % 32 == 0) {
                onProgress((80 + (i.toFloat() / frameCount) * 20f).toInt().coerceIn(80, 100))
            }
        }
        onProgress(100)
        return@withContext SpectrumTrack(safeFps, durationMs, mags, waves)
    }

    private fun getMimeTypeFromExtension(ext: String): String {
        return when (ext.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "mp4" -> "video/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            else -> "audio/mpeg"
        }
    }
}
