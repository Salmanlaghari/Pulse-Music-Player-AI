package com.salmanlaghari.pulsemusicplayerai.domain.model

/**
 * Visualizer styles available for the MP3 -> MP4 video studio.
 *
 * Each entry maps to a genuinely different drawing routine in
 * [com.salmanlaghari.pulsemusicplayerai.core.service.VisualizerFrameRenderer]
 * and every one of them is driven by the real audio spectrum/waveform data.
 */
/**
 * Every preset maps to a genuinely different drawing routine in
 * [com.salmanlaghari.pulsemusicplayerai.core.service.VisualizerFrameRenderer]
 * and is driven by the real audio spectrum/waveform data. The names intentionally
 * mirror the "Playing Song" visualizers so the same looks are available for export.
 */
enum class VideoVisualizerPreset(
    val displayName: String,
    val description: String,
    val category: String
) {
    // ---------- BARS ----------
    SPECTRUM_BARS("Spectrum Bars", "Classic equalizer bars rising from a baseline.", "Bars"),
    MIRROR_BARS("Mirror Bars", "Spectrum bars mirrored around the horizontal centre line.", "Bars"),
    NEON_BARS("Neon Bars", "Vibrant neon bars pulsing from top and bottom baselines.", "Bars"),
    FLAME_SPECTRUM("Flame Spectrum", "Rising red-to-yellow bars resembling flames.", "Bars"),
    INFINITY_BARS("Infinity Bars", "Bars expanding from the centre horizon to both sides.", "Bars"),
    EXTREME_SPECTRUM_X("Extreme Spectrum X", "Dual-ended spectrum cross expanding on beats.", "Bars"),
    LINEAR_BARS("Linear Bars", "Clean equalizer bars standing on a bottom baseline.", "Bars"),
    DUAL_ENDED_BARS("Dual Ended Bars", "Bars growing outward from the centre line.", "Bars"),
    RAINBOW_BARS("Rainbow Bars", "Full-spectrum coloured bars on a baseline.", "Bars"),
    GLOW_BARS("Glow Bars", "Bottom bars with a heavy neon glow.", "Bars"),
    PEAK_BARS("Peak Bars", "Bottom bars topped with bright peak caps.", "Bars"),
    THICK_BARS("Thick Bars", "Bold thick equalizer bars.", "Bars"),

    // ---------- CIRCULAR ----------
    CIRCULAR_SPECTRUM("Circular Spectrum", "Spectrum bars radiating outward around a centre ring.", "Circular"),
    RAINBOW_RING("Rainbow Ring", "A full-spectrum ring of dots pulsating radially.", "Circular"),
    GALAXY_RING("Galaxy Ring", "A rotating ring of particles orbiting on bass drops.", "Circular"),
    SPIRAL_GALAXY("Spiral Galaxy", "A multi-arm spiral of glowing cosmic dust.", "Circular"),
    RADIAL_DOTS("Radial Dots", "Dots radiating outward from the centre.", "Circular"),
    FUTURE_PULSE("Future Pulse", "An advanced circular telemetry pulse indicator ring.", "Circular"),
    ORBITAL_SR("Orbital Ring", "Planetary rings revolving with custom tilt.", "Circular"),
    PULSE_RING("Pulse Ring", "A single ring scaling with the acoustic beat.", "Circular"),
    CONCENTRIC_DOTS("Concentric Dots", "Dots arranged in concentric rings.", "Circular"),
    WHEEL_SPECTRUM("Wheel Spectrum", "Spoked wheel of spectrum bars.", "Circular"),

    // ---------- WAVE ----------
    WAVEFORM("Waveform", "The raw audio waveform drawn as a continuous line.", "Wave"),
    DUAL_WAVE("Dual Wave", "Two mirrored waveform lines.", "Wave"),
    MULTI_WAVE("Multi Wave", "Several layered waveform lines.", "Wave"),
    MIRRORED_WAVE("Mirrored Wave", "Left-to-right perfectly mirrored wave.", "Wave"),
    FILLED_WAVE("Filled Wave", "A waveform filled with colour underneath.", "Wave"),
    RIBBON_WAVE("Sound Ribbon", "A smooth bezier ribbon weaving across the screen.", "Wave"),
    STEP_WAVE("Step Wave", "An 8-bit stepped digital waveform.", "Wave"),
    SMOOTH_WAVE("Smooth Wave", "A smooth filled waveform area.", "Wave"),
    CROSS_WAVE("Cross Wave", "Two crossing waveform lines.", "Wave"),
    ECHO_WAVE("Echo Wave", "Trailing echo waveform lines.", "Wave"),

    // ---------- PARTICLE ----------
    PARTICLE_BEAT("Particle Beat", "Particles pushed outward by detected beats.", "Particle"),
    PARTICLE_ORB("Particle Orb", "A central glowing orb surrounded by orbiting particles.", "Particle"),
    STARFIELD("Starfield", "Stars moving outward from the centre on audio.", "Particle"),
    QUANTUM_CLOUD("Quantum Cloud", "A cloud of high-velocity subatomic particles.", "Particle"),
    FIREWORKS("Fireworks", "Explosive firework bursts on beats.", "Particle"),
    METEOR_SHOWER("Meteor Shower", "Diagonal meteor streaks flashing on transients.", "Particle"),
    SNOWFALL("Snowfall", "Delicate snowflakes drifting on the beat.", "Particle"),
    COLOR_BURST("Color Burst", "Explosive colour particle bursts from centre.", "Particle"),
    ORBIT_PARTICLES("Orbit Particles", "Particles orbiting the centre on elliptical paths.", "Particle"),
    GALAXY_CLOUD("Galaxy Cloud", "A rotating cloud of glowing particles.", "Particle"),

    // ---------- GEOMETRIC ----------
    HEXAGON_MESH("Hexagon Mesh", "A structural grid of glowing hexagons.", "Geometric"),
    CRYSTAL_MESH("Crystal Mesh", "Interconnected glowing crystal nodes.", "Geometric"),
    ISOMETRIC_GRID("Isometric Grid", "Cubes in isometric projection scaling with frequency.", "Geometric"),
    DOUBLE_HELIX("Double Helix", "Two intertwined rotating waves forming a helix.", "Geometric"),
    KALEIDOSCOPE("Kaleidoscope", "Symmetric vectors mirrored around a rotating origin.", "Geometric"),
    PRISM("Prism", "Light refraction vectors splitting into rainbow colours.", "Geometric"),
    DIAMOND_GLOW("Diamond Glow", "Nested glowing diamonds scaling with beats.", "Geometric"),
    LASER_BEAMS("Laser Beams", "Sharp laser rays projecting from the centre.", "Geometric"),
    INFINITY_LOOP("Infinity Loop", "An overlaid rotating infinity loop.", "Geometric"),
    FREQUENCY_LINES("Frequency Lines", "Clean overlaid sine-wave indicator bands.", "Geometric"),
    TUNNEL_WARP("Tunnel Warp", "Concentric squares zooming outward into a tunnel.", "Geometric"),

    // ---------- MINIMAL ----------
    LINE_SPECTRUM("Line Spectrum", "Thin spectrum lines on a clean baseline.", "Minimal"),
    DOT_SPECTRUM("Dot Spectrum", "Dots rising on a baseline.", "Minimal"),
    PULSE_LINE("Pulse Line", "A single horizontal pulse line.", "Minimal"),
    MINIMAL_BARS("Minimal Bars", "Thin minimal equalizer bars.", "Minimal"),
    EQUALIZER_DOTS("Equalizer Dots", "Vertical columns of equializer dots.", "Minimal"),
    TICK_SPECTRUM("Tick Spectrum", "Spectrum drawn as vertical ticks.", "Minimal")
}

enum class VideoAspectRatio(val displayName: String, val widthRatio: Int, val heightRatio: Int) {
    RATIO_16_9("16:9", 16, 9),
    RATIO_9_16("9:16", 9, 16),
    RATIO_1_1("1:1", 1, 1)
}

enum class VideoResolution(val displayName: String, val shortSide: Int) {
    SD_480("480p", 480),
    HD_720("720p", 720),
    FHD_1080("1080p", 1080),
    RES_4K("4K", 2160),
    RES_8K("8K", 4320)
}

/** How the background is produced when no image is supplied. */
enum class VideoBackgroundStyle(val displayName: String) {
    DARK_GRADIENT("Dark Gradient"),
    SOLID_BLACK("Solid Black"),
    ACCENT_GLOW("Accent Glow")
}

/** How a supplied background image is fitted into the frame. */
enum class BackgroundFit(val displayName: String) {
    CROP("Crop (fill)"),
    FIT("Fit (letterbox)")
}

/**
 * Post-processing effects the user can layer on top of the rendered visualizer
 * during export (and the matching live preview).
 *
 * Each id mirrors an [EffectType] in the export UI so the selection the user
 * makes in the VIDEO EFFECTS section drives a genuinely different look. The
 * renderer reads [VisualizerVideoConfig.effect] and applies a distinct overlay.
 */
enum class VideoEffect(val id: String, val displayName: String) {
    NONE("none", "None"),
    GLOW("glow", "Glow"),
    BLOOM("bloom", "Bloom"),
    PARTICLES("particles", "Particles"),
    STARFIELD("starfield", "Starfield"),
    LIGHT_RAYS("light_rays", "Light Rays"),
    LENS_FLARE("lens_flare", "Lens Flare"),
    SMOKE("smoke", "Smoke"),
    SPARKS("sparks", "Sparks"),
    EQ_GLOW("eq_glow", "Equalizer Glow"),
    VIGNETTE("vignette", "Vignette"),
    FILM_GRAIN("film_grain", "Film Grain"),
    MOTION_BLUR("motion_blur", "Motion Blur"),
    RGB_SHIFT("rgb_shift", "RGB Shift"),
    NEON_EDGE("neon_edge", "Neon Edge");

    companion object {
        fun fromId(id: String?): VideoEffect =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

/**
 * The single source of truth for the MP3 -> MP4 export.
 *
 * The exact same instance drives the live preview and the encoder, which is what
 * guarantees "what you see is what you export".
 */
data class VisualizerVideoConfig(
    val preset: VideoVisualizerPreset = VideoVisualizerPreset.SPECTRUM_BARS,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.RATIO_16_9,
    val resolution: VideoResolution = VideoResolution.HD_720,
    val fps: Int = 30,
    /** Trim range in milliseconds. [endMs] <= 0 means "to the end of the track". */
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val title: String = "",
    val artist: String = "",
    /**
     * Burn the song title/artist text onto the frame. Default OFF: the generic
     * title overlay was an unwanted artifact on both the preview and the exported
     * MP4 (no preset is designed around showing the title as a core look). Users
     * can still opt in via the "Show title text" switch in the export UI.
     */
    val showText: Boolean = false,
    /** Optional background image content/file uri, as a string. */
    val backgroundImageUri: String? = null,
    val backgroundFit: BackgroundFit = BackgroundFit.CROP,
    val backgroundStyle: VideoBackgroundStyle = VideoBackgroundStyle.DARK_GRADIENT,
    /** Dim applied over the background image so overlays stay readable (0f..1f). */
    val backgroundDim: Float = 0.35f,
    /**
     * Procedural gradient background chosen from the ANIMATION BACKGROUNDS
     * section (a list of ARGB colour ints). When set it is drawn by the renderer
     * instead of the flat [backgroundStyle] fill, so a different background
     * genuinely changes the exported video. Null keeps the flat style.
     */
    val backgroundGradient: List<Int>? = null,
    val accentColor: Int = 0xFFA855F7.toInt(),
    val secondaryColor: Int = 0xFF3B82F6.toInt(),
    /**
     * Post-processing effect applied on top of the rendered frame (VIDEO
     * EFFECTS section). Defaults to [VideoEffect.NONE].
     */
    val effect: VideoEffect = VideoEffect.NONE,
    /**
     * Colour theme (COLOR THEME section) overriding the visualizer's accent /
     * secondary colours. Null keeps the default [accentColor]/[secondaryColor].
     */
    val themePrimary: Int? = null,
    val themeSecondary: Int? = null,
    val themeTertiary: Int? = null,
    /**
     * Scale multiplier applied to the visualizer geometry (0.4f..1.6f). The
     * visualizer crop editor lets the user nudge this independently of the
     * horizontal/vertical offsets below, so the crop box can be resized while
     * the position stays pinned.
     */
    val visualizerScale: Float = 1.0f,
    /**
     * Horizontal crop multiplier (0.4f..1.6f). Independent of the vertical one
     * so the crop box can be made wider without affecting its height.
     */
    val visualizerScaleX: Float = 1.0f,
    /**
     * Vertical crop multiplier (0.4f..1.6f). Independent of the horizontal one
     * so the crop box can be made taller without affecting its width.
     */
    val visualizerScaleY: Float = 1.0f,
    /**
     * Horizontal offset of the visualizer centre from the frame centre
     * (-1f = far left, 0f = centred, 1f = far right). Driven by the Move
     * Left / Right / Center controls in the crop editor.
     */
    val visualizerOffsetX: Float = 0.0f,
    /**
     * Vertical offset of the visualizer centre from the frame centre
     * (-1f = top, 0f = centred, 1f = bottom). Driven by the Move Up / Down /
     * Center controls in the crop editor.
     */
    val visualizerOffsetY: Float = 0.0f,
    /** Rotation of the visualizer in degrees (0f..360f). */
    val visualizerRotation: Float = 0.0f,
    /** Vertical anchor of the visualizer inside the frame (0f = top, 1f = bottom). */
    val visualizerPositionY: Float = 0.6f,
    val glow: Boolean = true,
    /**
     * Built-in background music track (a raw resource entry name under res/raw)
     * that is layered UNDER the source audio at [backgroundTrackVolume]. Null means
     * "source audio only" (the original behaviour) — the default.
     */
    val backgroundTrackResName: String? = null,
    /** Mix gain (0f..1f) applied to the built-in background track. */
    val backgroundTrackVolume: Float = 0.35f,
    /**
     * Animated visual mood drawn behind the visualizer during export. Each
     * background track carries its own [BackgroundMood]; when set, a distinct
     * procedural animated background (seamless gradient/particle loop) is rendered
     * instead of the flat [backgroundStyle] fill. Null keeps the flat style.
     */
    val backgroundMood: BackgroundMood? = null,
    /**
     * Burn the Pulse Music Player logo as a semi-transparent watermark into the
     * exported MP4. Default ON; users can disable it for a clean export.
     */
    val watermarkEnabled: Boolean = true,
    val outputName: String = "PulseVisualizer"
) {
    /**
     * Resolves the encoder frame size. Dimensions are forced to even numbers
     * because H.264 encoders require it.
     */
    val videoWidth: Int
        get() {
            val short = resolution.shortSide
            val w = if (aspectRatio.widthRatio >= aspectRatio.heightRatio) {
                short * aspectRatio.widthRatio / aspectRatio.heightRatio
            } else short
            return (w / 2) * 2
        }

    val videoHeight: Int
        get() {
            val short = resolution.shortSide
            val h = if (aspectRatio.heightRatio > aspectRatio.widthRatio) {
                short * aspectRatio.heightRatio / aspectRatio.widthRatio
            } else short
            return (h / 2) * 2
        }

    /** Bitrate scaled with the pixel count so quality tracks the resolution. */
    val videoBitRate: Int
        get() {
            val pixels = videoWidth.toLong() * videoHeight.toLong()
            val perPixel = (pixels * fps / 30L)
            return (perPixel / 4L).toInt().coerceIn(2_000_000, 16_000_000)
        }

    /** Effective accent colour after applying the selected colour theme. */
    val effectiveAccent: Int
        get() = themePrimary ?: accentColor

    /** Effective secondary colour after applying the selected colour theme. */
    val effectiveSecondary: Int
        get() = themeSecondary ?: secondaryColor
}
