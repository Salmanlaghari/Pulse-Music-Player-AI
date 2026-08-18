package com.salmanlaghari.pulsemusicplayerai.domain.model

/**
 * Visualizer styles available for the MP3 -> MP4 video studio.
 *
 * Each entry maps to a genuinely different drawing routine in
 * [com.salmanlaghari.pulsemusicplayerai.core.service.VisualizerFrameRenderer]
 * and every one of them is driven by the real audio spectrum/waveform data.
 */
enum class VideoVisualizerPreset(val displayName: String, val description: String) {
    SPECTRUM_BARS("Spectrum Bars", "Classic equalizer bars rising from a baseline."),
    CIRCULAR_SPECTRUM("Circular Spectrum", "Spectrum bars radiating outward around a centre ring."),
    WAVEFORM("Waveform", "The raw audio waveform drawn as a continuous line."),
    RADIAL_PULSE("Radial Pulse", "Concentric rings that expand with the low-frequency energy."),
    PARTICLE_BEAT("Particle Beat", "Particles pushed outward by detected beats."),
    MIRROR_BARS("Mirror Bars", "Spectrum bars mirrored around the horizontal centre line.")
}

enum class VideoAspectRatio(val displayName: String, val widthRatio: Int, val heightRatio: Int) {
    RATIO_16_9("16:9", 16, 9),
    RATIO_9_16("9:16", 9, 16),
    RATIO_1_1("1:1", 1, 1)
}

enum class VideoResolution(val displayName: String, val shortSide: Int) {
    SD_480("480p", 480),
    HD_720("720p", 720),
    FHD_1080("1080p", 1080)
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
    val showText: Boolean = true,
    /** Optional background image content/file uri, as a string. */
    val backgroundImageUri: String? = null,
    val backgroundFit: BackgroundFit = BackgroundFit.CROP,
    val backgroundStyle: VideoBackgroundStyle = VideoBackgroundStyle.DARK_GRADIENT,
    /** Dim applied over the background image so overlays stay readable (0f..1f). */
    val backgroundDim: Float = 0.35f,
    val accentColor: Int = 0xFFA855F7.toInt(),
    val secondaryColor: Int = 0xFF3B82F6.toInt(),
    /** Scale multiplier applied to the visualizer geometry (0.4f..1.6f). */
    val visualizerScale: Float = 1.0f,
    /** Vertical anchor of the visualizer inside the frame (0f = top, 1f = bottom). */
    val visualizerPositionY: Float = 0.6f,
    val glow: Boolean = true,
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
            return (perPixel / 8L).toInt().coerceIn(1_500_000, 12_000_000)
        }
}
