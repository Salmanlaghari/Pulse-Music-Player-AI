package com.salmanlaghari.pulsemusicplayerai.core.service

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM verification of the watermark placement maths used when burning the
 * logo into exported frames. (No android.* types involved, so this runs on the
 * unit-test JVM, not an emulator.)
 */
class WatermarkLayoutTest {
    @Test
    fun bottomRightPlacementIsValidFor720p() {
        val r = WatermarkLayout.computeRect(1280, 720)
        assertTrue("left < right", r.left < r.right)
        assertTrue("top < bottom", r.top < r.bottom)
        assertTrue("inside frame width", r.right <= 1280f)
        assertTrue("inside frame height", r.bottom <= 720f)
        assertTrue("non-negative left", r.left >= 0f)
        assertTrue("non-negative top", r.top >= 0f)
    }

    @Test
    fun marginKeepsWatermarkOffTheEdges() {
        val r = WatermarkLayout.computeRect(1920, 1080)
        val marginX = 1920f - r.right
        val marginY = 1080f - r.bottom
        assertTrue("right margin positive", marginX > 0f)
        assertTrue("bottom margin positive", marginY > 0f)
        // Margins scale with frame size and stay small (corner placement).
        assertTrue("right margin reasonable", marginX < 1920f * 0.1f)
    }

    @Test
    fun sizeScalesWithFrameHeight() {
        val small = WatermarkLayout.computeRect(640, 360)
        val large = WatermarkLayout.computeRect(1920, 1080)
        val smallH = small.bottom - small.top
        val largeH = large.bottom - large.top
        assertTrue("taller frame -> taller watermark", largeH > smallH)
        // ~16% of frame height.
        assertTrue("height ~16% of frame", Math.abs(largeH - 1080f * WatermarkLayout.SIZE_FRACTION) < 2f)
    }

    @Test
    fun opacityWithinRange() {
        assertTrue("opacity in (0,1]", WatermarkLayout.OPACITY > 0f && WatermarkLayout.OPACITY <= 1f)
    }
}
