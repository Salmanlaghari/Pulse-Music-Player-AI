package com.salmanlaghari.pulsemusicplayerai.domain.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Represents a song from YouTube, Deezer, or other music sources.
 * Compatible with the existing playback system.
 */
data class YouTubeSong(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long, // in seconds
    val thumbnailUrl: String,
    val audioUrl: String, // direct audio stream URL
    val isLive: Boolean = false
) {
    /**
     * Get the source type based on ID prefix.
     */
    val sourceType: String
        get() = when {
            id.startsWith("dz_") -> "Deezer"
            id.startsWith("ia_") -> "Internet Archive"
            id.startsWith("jm_") -> "Jamendo"
            else -> "YouTube"
        }

    /**
     * Convert to a MediaItem for ExoPlayer playback.
     */
    fun toMediaItem(): MediaItem {
        val safeUrl = audioUrl.takeIf { it.isNotBlank() && it.startsWith("http") } ?: ""
        val safeThumb = thumbnailUrl.takeIf { it.isNotBlank() } ?: getDefaultThumbnail()

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(safeThumb))
            .build()

        return MediaItem.Builder()
            .setMediaId("yt_$id")
            .setUri(safeUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Convert to a local Song model for compatibility with existing UI.
     * Returns null if audioUrl is invalid.
     */
    fun toSong(): Song? {
        if (!hasValidAudio()) return null
        val safeAudioUrl = audioUrl.trim()
        val safeThumbnail = thumbnailUrl.takeIf { it.isNotBlank() } ?: getDefaultThumbnail()
        return Song(
            id = id.hashCode().toLong(),
            title = title,
            artist = artist,
            album = "$sourceType Music",
            duration = duration * 1000, // convert to ms
            path = safeAudioUrl,
            uri = Uri.parse(safeAudioUrl),
            dateAdded = System.currentTimeMillis(),
            artUri = Uri.parse(safeThumbnail)
        )
    }

    /**
     * Check if this song has a valid playable audio URL.
     */
    fun hasValidAudio(): Boolean {
        return audioUrl.isNotBlank() &&
                audioUrl.trim().length > 10 &&
                (audioUrl.startsWith("http://") || audioUrl.startsWith("https://"))
    }

    private fun getDefaultThumbnail(): String {
        return when {
            id.startsWith("dz_") -> "https://e-cdns-images.dzcdn.net/images/cover/000000000/56x56.jpg"
            else -> "https://i.ytimg.com/vi/${id.removePrefix("yt_").removePrefix("dz_").removePrefix("ia_").removePrefix("jm_")}/default.jpg"
        }
    }
}
