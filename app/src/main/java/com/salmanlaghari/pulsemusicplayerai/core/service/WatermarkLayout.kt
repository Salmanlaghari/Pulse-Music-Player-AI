package com.salmanlaghari.pulsemusicplayerai.core.service

import kotlin.math.sin

/**
 * Pure (JVM-testable) layout + animation maths for the exported-video watermark.
 *
 * The watermark is placed in one of the four safe on-screen corners with a small
 * margin and scaled to a fraction of the frame height so it stays legible on
 * every resolution (720p / 1080p) without ever obstructing the visualizer.
 *
 * It is no longer static: [computeAnimated] loops the logo through the corners,
 * shows it for a fixed window with a smooth fade in/out, hides it for a gap, then
 * reappears at the next corner. While visible it gets a subtle breathing scale +
 * slow rotation so it reads as polished branding. Everything is derived
 * deterministically from the frame's presentation timestamp, so the exact same
 * motion is baked into every exported frame. Kept free of any android.* types so
 * it can be unit-tested on the JVM.
 */
object WatermarkLayout {
    /** Fraction of frame height the watermark box occupies. */
    const val SIZE_FRACTION = 0.16f
    /** Margin from the edges, as a fraction of frame width/height. */
    const val MARGIN_FRACTION = 0.025f
    /** Peak opacity applied when compositing the watermark (0f..1f). */
    const val OPACITY = 0.72f

    /** How long the watermark stays visible per appearance (ms). */
    const val VISIBLE_MS = 10_000L
    /** How long it is hidden between appearances (ms). */
    const val HIDDEN_MS = 3_000L
    /** Fade in/out duration at the edges of a visible window (ms). */
    const val FADE_MS = 450L
    /** Full loop length for one corner = visible + hidden. */
    private const val SEGMENT_MS = VISIBLE_MS + HIDDEN_MS

    /** Subtle breathing pulse frequency (cycles per second). */
    private const val PULSE_HZ = 0.25f
    /** Subtle rotation amplitude (degrees). */
    private const val ROTATE_AMP_DEG = 6f
    /** Subtle rotation frequency (cycles per second). */
    private const val ROTATE_HZ = 0.1f

    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /**
     * Animated watermark state for a given presentation timestamp.
     *
     * [alpha] is 0f when the watermark should be hidden entirely (callers skip
     * drawing). [scale] and [rotation] are subtle, deterministic transforms.
     */
    data class AnimatedWatermark(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val alpha: Float,
        val scale: Float,
        val rotation: Float
    )

    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

    /** Static bottom-right placement (kept for tests / legacy callers). */
    fun computeRect(frameWidth: Int, frameHeight: Int): Rect {
        return cornerRect(Corner.BOTTOM_RIGHT, frameWidth, frameHeight)
    }

    /** Box placement for a specific corner. */
    fun cornerRect(corner: Corner, frameWidth: Int, frameHeight: Int): Rect {
        val w = frameWidth.coerceAtLeast(2)
        val h = frameHeight.coerceAtLeast(2)
        val boxH = h * SIZE_FRACTION
        // Keep the watermark's aspect ratio (wide wordmark) ~ 3.2 : 1.
        val boxW = boxH * 3.2f
        val marginX = w * MARGIN_FRACTION
        val marginY = h * MARGIN_FRACTION

        val left: Float
        val top: Float
        when (corner) {
            Corner.TOP_LEFT -> { left = marginX; top = marginY }
            Corner.TOP_RIGHT -> { left = (w - marginX - boxW).coerceAtLeast(0f); top = marginY }
            Corner.BOTTOM_LEFT -> { left = marginX; top = (h - marginY - boxH).coerceAtLeast(0f) }
            Corner.BOTTOM_RIGHT -> {
                left = (w - marginX - boxW).coerceAtLeast(0f)
                top = (h - marginY - boxH).coerceAtLeast(0f)
            }
        }
        return Rect(left, top, left + boxW, top + boxH)
    }

    /**
     * Computes the watermark's position/opacity/scale/rotation for a frame at
     * [frameTimeUs]. The motion is fully deterministic from the timestamp so the
     * exported video is identical run-to-run and matches the live preview.
     */
    fun computeAnimated(frameTimeUs: Long, frameWidth: Int, frameHeight: Int): AnimatedWatermark {
        val timeMs = frameTimeUs / 1000L
        val segment = (timeMs / SEGMENT_MS).let { if (it < 0) 0L else it }
        val localMs = timeMs - segment * SEGMENT_MS // 0 .. SEGMENT_MS-1

        // Alpha: 0 during the hidden gap; otherwise fade in/out at the edges of
        // the visible window with a flat peak in between.
        val alpha = if (localMs >= VISIBLE_MS) {
            0f
        } else {
            val fadeIn = if (localMs < FADE_MS) localMs.toFloat() / FADE_MS.toFloat() else 1f
            val fadeOut = if (localMs > VISIBLE_MS - FADE_MS)
                (VISIBLE_MS - localMs).toFloat() / FADE_MS.toFloat() else 1f
            (fadeIn.coerceAtMost(fadeOut)).coerceIn(0f, 1f) * OPACITY
        }

        val corner = Corner.values()[(segment % Corner.values().size).toInt()]
        val r = cornerRect(corner, frameWidth, frameHeight)

        val tSec = localMs / 1000f
        // Subtle "breathing" scale around 1.0.
        val scale = 1f + 0.05f * sin(2f * Math.PI.toFloat() * PULSE_HZ * tSec)
        // Very slow, gentle rotation.
        val rotation = ROTATE_AMP_DEG * sin(2f * Math.PI.toFloat() * ROTATE_HZ * tSec)

        return AnimatedWatermark(r.left, r.top, r.right, r.bottom, alpha, scale, rotation)
    }
}
