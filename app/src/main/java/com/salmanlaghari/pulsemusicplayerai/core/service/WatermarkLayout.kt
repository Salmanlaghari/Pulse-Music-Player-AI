package com.salmanlaghari.pulsemusicplayerai.core.service

/**
 * Pure (JVM-testable) layout maths for the exported-video watermark.
 *
 * The watermark is placed in the bottom-right corner with a small margin and
 * scaled to a fraction of the frame height so it stays legible on every
 * resolution (720p / 1080p) without ever obstructing the visualizer. Kept free
 * of any android.* types so it can be unit-tested on the JVM.
 */
object WatermarkLayout {
    /** Fraction of frame height the watermark box occupies. */
    const val SIZE_FRACTION = 0.16f
    /** Margin from the right/bottom edges, as a fraction of frame width/height. */
    const val MARGIN_FRACTION = 0.025f
    /** Opacity applied when compositing the watermark (0f..1f). */
    const val OPACITY = 0.72f

    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun computeRect(frameWidth: Int, frameHeight: Int): Rect {
        val w = frameWidth.coerceAtLeast(2)
        val h = frameHeight.coerceAtLeast(2)
        val boxH = h * SIZE_FRACTION
        // Keep the watermark's aspect ratio (wide wordmark) ~ 3.2 : 1.
        val boxW = boxH * 3.2f
        val marginX = w * MARGIN_FRACTION
        val marginY = h * MARGIN_FRACTION
        val right = (w - marginX).coerceAtLeast(boxW)
        val bottom = (h - marginY).coerceAtLeast(boxH)
        val left = (right - boxW).coerceAtLeast(0f)
        val top = (bottom - boxH).coerceAtLeast(0f)
        return Rect(left, top, right, bottom)
    }
}
