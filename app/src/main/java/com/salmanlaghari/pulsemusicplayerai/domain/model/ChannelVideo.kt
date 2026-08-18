package com.salmanlaghari.pulsemusicplayerai.domain.model

/**
 * A single video from the owner's YouTube channel ("My Channel" source).
 * Playback for these goes through the official, ToS-compliant embedded
 * YouTube player (not raw audio extraction), so we only need the video id,
 * metadata for the list/now-playing UI.
 */
data class ChannelVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val publishedAt: String // ISO-8601 timestamp from the channel RSS feed
)
