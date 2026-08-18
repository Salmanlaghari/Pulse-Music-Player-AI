package com.salmanlaghari.pulsemusicplayerai.domain.model

/**
 * Curated, built-in background music tracks that the user can optionally layer
 * under their own audio during MP4 export.
 *
 * IMPORTANT: every track here is an ORIGINAL, procedurally generated ambient
 * loop (see app/src/main/res/raw/bg_track_*.wav). They are NOT commercial songs
 * and are safe to bundle & redistribute. The audio files are intentionally
 * short, low-sample-rate, mono loops so they keep the APK small.
 *
 * `resEntryName` is the resource name (without extension) under res/raw and is
 * what gets stored on [VisualizerVideoConfig.backgroundTrackResName].
 */
data class BuiltInBackgroundTrack(
    val id: String,
    val displayName: String,
    val resEntryName: String,
    val description: String,
    val durationSec: Int,
    /** Suggested default mix volume for this particular track (0f..1f). */
    val suggestedVolume: Float = 0.35f
)

object BuiltInBackgroundTracks {
    /** "None" sentinel — source audio only, the original behaviour. */
    const val NONE = "none"

    val ALL: List<BuiltInBackgroundTrack> = listOf(
        BuiltInBackgroundTrack(
            id = "ambient",
            displayName = "Ambient Drift",
            resEntryName = "bg_track_ambient",
            description = "Soft sustained pad, good for relaxing tracks.",
            durationSec = 12,
            suggestedVolume = 0.30f
        ),
        BuiltInBackgroundTrack(
            id = "lofi",
            displayName = "Lo-Fi Pulse",
            resEntryName = "bg_track_lofi",
            description = "Mellow low chord loop for a calm vibe.",
            durationSec = 12,
            suggestedVolume = 0.35f
        ),
        BuiltInBackgroundTrack(
            id = "cinematic",
            displayName = "Cinematic Rise",
            resEntryName = "bg_track_cinematic",
            description = "Wider chord swell for dramatic moments.",
            durationSec = 12,
            suggestedVolume = 0.28f
        )
    )

    /** Resolves a selection string to a track, or null for "source only". */
    fun resolve(selection: String?): BuiltInBackgroundTrack? {
        if (selection == null || selection == NONE) return null
        return ALL.firstOrNull { it.resEntryName == selection || it.id == selection }
    }
}
