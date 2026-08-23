package com.salmanlaghari.pulsemusicplayerai.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.FileOutputStream

class SongArtworkFetcher(
    private val context: Context,
    private val song: Song,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val cacheDir = File(context.cacheDir, "artwork_cache").apply {
            if (!exists()) mkdirs()
        }
        val cacheFile = File(cacheDir, "song_${song.id}.jpg")

        // 1. Check disk cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return SourceResult(
                source = coil.decode.ImageSource(cacheFile.toOkioPath(), okio.FileSystem.SYSTEM),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK
            )
        }

        // 2. Try remote http(s) artwork (streaming / Desi Hits / Deezer / etc.)
        //    The custom fetcher only knows how to open local content:// + embedded
        //    artwork, so a remote URL must be downloaded to the cache first.
        if (song.artUri != null &&
            (song.artUri.scheme == "http" || song.artUri.scheme == "https")
        ) {
            try {
                val url = java.net.URL(song.artUri.toString())
                url.openStream().use { input ->
                    FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
                }
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    return SourceResult(
                        source = coil.decode.ImageSource(cacheFile.toOkioPath(), okio.FileSystem.SYSTEM),
                        mimeType = "image/jpeg",
                        dataSource = DataSource.NETWORK
                    )
                }
            } catch (e: Exception) {
                // Fall through to the placeholder (return null)
            }
        }

        // 3. Try MediaStore artwork URI (local songs)
        if (song.artUri != null) {
            try {
                context.contentResolver.openInputStream(song.artUri)?.use { inputStream ->
                    FileOutputStream(cacheFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    if (cacheFile.exists() && cacheFile.length() > 0) {
                        return SourceResult(
                            source = coil.decode.ImageSource(cacheFile.toOkioPath(), okio.FileSystem.SYSTEM),
                            mimeType = "image/jpeg",
                            dataSource = DataSource.DISK
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback to embedded picture
            }
        }

        // 4. Try Embedded Art using MediaMetadataRetriever
        if (song.path.isNotEmpty()) {
            val file = File(song.path)
            if (file.exists()) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(song.path)
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        FileOutputStream(cacheFile).use { outputStream ->
                            outputStream.write(picture)
                        }
                        if (cacheFile.exists() && cacheFile.length() > 0) {
                            return SourceResult(
                                source = coil.decode.ImageSource(cacheFile.toOkioPath(), okio.FileSystem.SYSTEM),
                                mimeType = "image/jpeg",
                                dataSource = DataSource.DISK
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Extraction failed
                } finally {
                    try {
                        retriever.release()
                    } catch (ignored: Exception) {}
                }
            }
        }

        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Song> {
        override fun create(data: Song, options: Options, imageLoader: ImageLoader): Fetcher {
            return SongArtworkFetcher(context, data, options)
        }
    }
}
