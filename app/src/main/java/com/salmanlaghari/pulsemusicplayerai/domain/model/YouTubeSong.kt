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
    val audioUrl: String, // direct audio stream URL OR YouTube video URL
    val isLive: Boolean = false
) {
    /**
     * Get the source type based on ID prefix.
     */
    val sourceType: String
        get() = when {
            id.startsWith("dz_") -> "Deezer"
            id.startsWith("yt_") -> "YouTube"
            id.startsWith("ia_") -> "Archive"
            else -> "Music"
        }

    /**
     * Check if this is a YouTube video (not extracted audio).
     */
    val isYouTubeVideo: Boolean
        get() = !id.startsWith("dz_") && !id.startsWith("ia_") && !id.startsWith("jm_") && !audioUrl.startsWith("http")

    /**
     * Convert to a MediaItem for ExoPlayer playback.
     * For YouTube videos, use the video URL. For others, use audioUrl.
     */
    fun toMediaItem(): MediaItem {
        val mediaUri = when {
            // YouTube video - use the video URL directly
            isYouTubeVideo -> "https://www.youtube.com/watch?v=$id"
            // Has direct audio URL
            audioUrl.isNotBlank() && audioUrl.startsWith("http") -> audioUrl
            // Fallback
            else -> ""
        }
        
        val safeThumb = thumbnailUrl.takeIf { it.isNotBlank() } ?: getDefaultThumbnail()

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(safeThumb))
            .build()

        return MediaItem.Builder()
            .setMediaId("yt_$id")
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Convert to a local Song model for compatibility with existing UI.
     * Returns null if no valid URI available.
     */
    fun toSong(): Song? {
        val uri = when {
            isYouTubeVideo -> Uri.parse("https://www.youtube.com/watch?v=$id")
            audioUrl.isNotBlank() && audioUrl.startsWith("http") -> Uri.parse(audioUrl.trim())
            else -> return null
        }
        
        val safeThumbnail = thumbnailUrl.takeIf { it.isNotBlank() } ?: getDefaultThumbnail()
        
        return Song(
            id = id.hashCode().toLong(),
            title = title,
            artist = artist,
            album = "$sourceType Music",
            duration = duration * 1000,
            path = uri.toString(),
            uri = uri,
            dateAdded = System.currentTimeMillis(),
            artUri = Uri.parse(safeThumbnail)
        )
    }

    /**
     * Check if this song has a valid playable URL.
     */
    fun hasValidAudio(): Boolean {
        return when {
            isYouTubeVideo -> true // YouTube videos can be played directly
            audioUrl.isNotBlank() && audioUrl.trim().length > 10 && audioUrl.startsWith("http") -> true
            else -> false
        }
    }

    private fun getDefaultThumbnail(): String {
        return when {
            id.startsWith("dz_") -> "https://e-cdns-images.dzcdn.net/images/cover/000000000/56x56.jpg"
            else -> "https://i.ytimg.com/vi/$id/default.jpg"
        }
    }
}
