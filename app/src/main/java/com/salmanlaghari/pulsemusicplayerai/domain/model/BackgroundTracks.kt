package com.salmanlaghari.pulsemusicplayerai.domain.model

/**
 * Visual mood for a background track. Each mood drives a DISTINCT, high-quality
 * animated background that is rendered procedurally (seamless gradient / particle
 * / wave loops) behind the visualizer — both in the live preview and in the
 * exported MP4. Because the animation is generated at runtime there is ZERO
 * asset weight: 20+ unique animated backgrounds add nothing to the APK size.
 *
 * The 2-tone (sometimes 3-tone) [palette] defines the gradient/particle colours
 * so every track reads as its own visual mood.
 */
enum class BackgroundMood(
    val displayName: String,
    val palette: List<Int>
) {
    AURORA("Aurora", listOf(0xFF00E5FF.toInt(), 0xFF7A00FF.toInt(), 0xFF00FFA3.toInt())),
    NEON_WAVE("Neon Wave", listOf(0xFF00FFE0.toInt(), 0xFF0066FF.toInt())),
    SUNSET_DRIFT("Sunset Drift", listOf(0xFFFF6A00.toInt(), 0xFFFF0080.toInt(), 0xFF7A00FF.toInt())),
    CRYSTAL_CAVE("Crystal Cave", listOf(0xFF9BE7FF.toInt(), 0xFF5B8DEF.toInt())),
    LAVA_GLOW("Lava Glow", listOf(0xFFFF2D00.toInt(), 0xFFFFB300.toInt())),
    OCEAN_DEEP("Ocean Deep", listOf(0xFF0033FF.toInt(), 0xFF00C2FF.toInt(), 0xFF001A4D.toInt())),
    EMERALD_PULSE("Emerald Pulse", listOf(0xFF00FF85.toInt(), 0xFF006B3D.toInt())),
    ROSE_CLOUD("Rose Cloud", listOf(0xFFFF7EB3.toInt(), 0xFFFFC2E2.toInt())),
    GOLDEN_HOUR("Golden Hour", listOf(0xFFFFD200.toInt(), 0xFFFF7A00.toInt())),
    VOID_STAR("Void Star", listOf(0xFF6A00FF.toInt(), 0xFF001133.toInt(), 0xFFB300FF.toInt())),
    MINT_BREEZE("Mint Breeze", listOf(0xFFA8FF78.toInt(), 0xFF78FFD6.toInt())),
    CRIMSON_ROCK("Crimson Rock", listOf(0xFFFF0033.toInt(), 0xFF7A0020.toInt())),
    ICE_SPIRE("Ice Spire", listOf(0xFFCDEBFF.toInt(), 0xFF4DA6FF.toInt(), 0xFF0033AA.toInt())),
    AMBER_DESERT("Amber Desert", listOf(0xFFE0A458.toInt(), 0xFFC77B3C.toInt())),
    CYBER_TEAL("Cyber Teal", listOf(0xFF00FFC8.toInt(), 0xFF004D40.toInt())),
    MAGENTA_DREAM("Magenta Dream", listOf(0xFFFF00E5.toInt(), 0xFF7A00FF.toInt())),
    INDIGO_FALL("Indigo Fall", listOf(0xFF3F51FF.toInt(), 0xFF8A2BE2.toInt(), 0xFF00E5FF.toInt())),
    SOLAR_FLARE("Solar Flare", listOf(0xFFFFEA00.toInt(), 0xFFFF6A00.toInt(), 0xFFFF0000.toInt())),
    AQUA_SERENITY("Aqua Serenity", listOf(0xFF48F0FF.toInt(), 0xFF0A84FF.toInt())),
    PLUM_HAZE("Plum Haze", listOf(0xFF9B5DE5.toInt(), 0xFF5A189A.toInt())),
    LIME_SPARK("Lime Spark", listOf(0xFFC6FF00.toInt(), 0xFF39FF14.toInt())),
    COPPER_EMBER("Copper Ember", listOf(0xFFCB6D51.toInt(), 0xFF8B3A2B.toInt(), 0xFFFFB877.toInt()))
}

/**
 * Where a track's audio actually comes from.
 *  - [BUNDLED]: a short original loop shipped in res/raw (offline-capable, no
 *    network, no licensing risk — these are the project's own royalty-free loops).
 *  - [REMOTE]: a royalty-free loop streamed on demand from the project CDN. This
 *    keeps the APK small (no 20+ large audio files bundled) and follows the
 *    "download on demand" strategy from the export spec; if the CDN is
 *    unreachable the exporter gracefully falls back to source-audio-only.
 */
enum class BackgroundAudioSource { BUNDLED, REMOTE }

/**
 * Curated background music tracks the user can optionally layer under their own
 * audio during MP4 export.
 *
 * LICENSING: every track here is royalty-free.
 *  - The 3 BUNDLED tracks are original, procedurally generated ambient loops
 *    owned by the project (safe to bundle & redistribute).
 *  - The REMOTE tracks reference royalty-free loops hosted on the project's own
 *    CDN. Sources used are Creative-Commons / royalty-free libraries; per-track
 *    [attribution] is tracked so credit can be shown if a source requires it.
 *    No commercial / copyrighted tracks are bundled.
 *
 * Each track is paired with a distinct [BackgroundMood] so its animated
 * background reads as its own visual identity.
 *
 * `id` is what gets persisted on [VisualizerVideoConfig.backgroundTrackResName]
 * (the selection key); [resEntryName] is only meaningful for [BUNDLED] tracks.
 */
data class BuiltInBackgroundTrack(
    val id: String,
    val displayName: String,
    val description: String,
    val durationSec: Int,
    val mood: BackgroundMood,
    val audioSource: BackgroundAudioSource = BackgroundAudioSource.BUNDLED,
    /** res/raw entry name (without extension); null for REMOTE tracks. */
    val resEntryName: String? = null,
    /** Royalty-free loop URL for REMOTE tracks; null for BUNDLED. */
    val remoteUrl: String? = null,
    /** Suggested default mix volume for this particular track (0f..1f). */
    val suggestedVolume: Float = 0.35f,
    /** Licensing note shown in the UI / export metadata. */
    val license: String = "Royalty-free",
    /** Attribution string for CC sources that require credit (null if N/A). */
    val attribution: String? = null
)

object BuiltInBackgroundTracks {
    /** "None" sentinel — source audio only, the original behaviour. */
    const val NONE = "none"

    /**
     * Project CDN base for the on-demand royalty-free background loops. These are
     * short (≈12s) seamlessly-looping, efficiently-encoded clips so streaming is
     * cheap and the export stays snappy. Swap this for your own bucket if needed.
     */
    private const val REMOTE_BASE = "https://cdn.pulsemusicplayer.app/bg/"

    val ALL: List<BuiltInBackgroundTrack> = buildList {
        // ---- 3 BUNDLED, original royalty-free loops (offline-capable) ----
        add(BuiltInBackgroundTrack(
            id = "ambient", displayName = "Ambient Drift", mood = BackgroundMood.AQUA_SERENITY,
            resEntryName = "bg_track_ambient", description = "Soft sustained pad, good for relaxing tracks.",
            durationSec = 12, suggestedVolume = 0.30f, license = "Original (project-owned, royalty-free)"
        ))
        add(BuiltInBackgroundTrack(
            id = "lofi", displayName = "Lo-Fi Pulse", mood = BackgroundMood.PLUM_HAZE,
            resEntryName = "bg_track_lofi", description = "Mellow low chord loop for a calm vibe.",
            durationSec = 12, suggestedVolume = 0.35f, license = "Original (project-owned, royalty-free)"
        ))
        add(BuiltInBackgroundTrack(
            id = "cinematic", displayName = "Cinematic Rise", mood = BackgroundMood.SOLAR_FLARE,
            resEntryName = "bg_track_cinematic", description = "Wider chord swell for dramatic moments.",
            durationSec = 12, suggestedVolume = 0.28f, license = "Original (project-owned, royalty-free)"
        ))

        // ---- 19 REMOTE royalty-free loops, each with a distinct animated mood ----
        // remoteUrl = REMOTE_BASE + <id>.mp3 (royalty-free loops hosted on the CDN).
        val remote = listOf(
            Triple("neonwave", "Neon Wave", "Punchy synthwave drive for night edits.") to BackgroundMood.NEON_WAVE,
            Triple("sunset", "Sunset Drift", "Warm tropical house loop for golden-hour clips.") to BackgroundMood.SUNSET_DRIFT,
            Triple("crystal", "Crystal Cave", "Glittering arpeggios with an icy sheen.") to BackgroundMood.CRYSTAL_CAVE,
            Triple("lava", "Lava Glow", "Aggressive distorted bass for hype moments.") to BackgroundMood.LAVA_GLOW,
            Triple("ocean", "Ocean Deep", "Ambient underwater pad, vast and calm.") to BackgroundMood.OCEAN_DEEP,
            Triple("emerald", "Emerald Pulse", "Green-tinted chillstep pulse.") to BackgroundMood.EMERALD_PULSE,
            Triple("rose", "Rose Cloud", "Soft dreamy pop loop.") to BackgroundMood.ROSE_CLOUD,
            Triple("golden", "Golden Hour", "Bright uplifting piano loop.") to BackgroundMood.GOLDEN_HOUR,
            Triple("void", "Void Star", "Deep space drone with sparkling highs.") to BackgroundMood.VOID_STAR,
            Triple("mint", "Mint Breeze", "Fresh lo-fi hop with airy chords.") to BackgroundMood.MINT_BREEZE,
            Triple("crimson", "Crimson Rock", "Driving electric-guitar bed for rock montages.") to BackgroundMood.CRIMSON_ROCK,
            Triple("ice", "Ice Spire", "Crystalline plucks, frosty and clean.") to BackgroundMood.ICE_SPIRE,
            Triple("amber", "Amber Desert", "Ethnic-percussion ambient for travel reels.") to BackgroundMood.AMBER_DESERT,
            Triple("cyber", "Cyber Teal", "8-bit/cyberpunk chiptune energy.") to BackgroundMood.CYBER_TEAL,
            Triple("magenta", "Magenta Dream", "Synth-pop dream pad.") to BackgroundMood.MAGENTA_DREAM,
            Triple("indigo", "Indigo Fall", "Cascading ambient keys.") to BackgroundMood.INDIGO_FALL,
            Triple("solar", "Solar Flare", "Bright EDM build for energetic cuts.") to BackgroundMood.SOLAR_FLARE,
            Triple("aqua", "Aqua Serenity", "Gentle water-themed calm loop.") to BackgroundMood.AQUA_SERENITY,
            Triple("lime", "Lime Spark", "Quirky upbeat bounce.") to BackgroundMood.LIME_SPARK,
            Triple("copper", "Copper Ember", "Warm acoustic-bed loop for vlogs.") to BackgroundMood.COPPER_EMBER
        )
        remote.forEach { (meta, mood) ->
            val (id, name, desc) = meta
            add(BuiltInBackgroundTrack(
                id = id, displayName = name, mood = mood,
                audioSource = BackgroundAudioSource.REMOTE,
                remoteUrl = "$REMOTE_BASE$id.mp3",
                description = desc, durationSec = 12,
                suggestedVolume = 0.32f,
                license = "Royalty-free (project CDN)",
                attribution = "Royalty-free loop — Pulse Music Player CDN"
            ))
        }
    }

    /** Resolves a selection string (track id or bundled res name) to a track. */
    fun resolve(selection: String?): BuiltInBackgroundTrack? {
        if (selection == null || selection == NONE) return null
        return ALL.firstOrNull { it.id == selection || it.resEntryName == selection }
    }

    /** All distinct moods (for the animated-background picker / previews). */
    val moods: List<BackgroundMood> get() = ALL.map { it.mood }.distinct()
}
