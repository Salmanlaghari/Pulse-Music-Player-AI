package com.salmanlaghari.pulsemusicplayerai.domain.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Represents a song from YouTube (Piped API).
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
     * Uses the audio stream URL directly.
     */
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(thumbnailUrl))
            .build()

        return MediaItem.Builder()
            .setMediaId("yt_$id")
            .setUri(audioUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Convert to a local Song model for compatibility with existing UI.
     */
    fun toSong(): Song {
        return Song(
            id = id.hashCode().toLong(),
            title = title,
            artist = artist,
            album = "YouTube Music",
            duration = duration * 1000, // convert to ms
            path = audioUrl,
            uri = Uri.parse(audioUrl),
            dateAdded = System.currentTimeMillis(),
            artUri = Uri.parse(thumbnailUrl)
        )
    }
}
