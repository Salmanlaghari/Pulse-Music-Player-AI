package com.salmanlaghari.pulsemusicplayerai.domain.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Represents a song from YouTube (Piped/Invidious API).
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
     * Convert to a MediaItem for ExoPlayer playback.
     */
    fun toMediaItem(): MediaItem {
        val safeUrl = audioUrl.takeIf { it.isNotBlank() && it.startsWith("http") } ?: ""
        val safeThumb = thumbnailUrl.takeIf { it.isNotBlank() } ?: "https://i.ytimg.com/vi/$id/default.jpg"

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
        val safeThumbnail = thumbnailUrl.takeIf { it.isNotBlank() } ?: "https://i.ytimg.com/vi/$id/default.jpg"
        return Song(
            id = id.hashCode().toLong(),
            title = title,
            artist = artist,
            album = "YouTube Music",
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
}
