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

    @Test
    fun animatedWatermarkVisibleMidWindow() {
        // 5s into segment 0 -> inside the 10s visible window -> should show.
        val a = WatermarkLayout.computeAnimated(5_000_000L, 1920, 1080)
        assertTrue("alpha > 0 mid visible window", a.alpha > 0f)
        assertTrue("alpha <= peak opacity", a.alpha <= WatermarkLayout.OPACITY + 1e-5f)
    }

    @Test
    fun animatedWatermarkHiddenDuringGap() {
        // 11.5s into segment 0 -> hidden gap (visible 10s + 3s gap) -> alpha 0.
        val a = WatermarkLayout.computeAnimated(11_500_000L, 1920, 1080)
        assertTrue("alpha 0 during hidden gap", a.alpha <= 0f)
    }

    @Test
    fun animatedWatermarkFadesInAtStart() {
        // 100ms into the visible window -> still fading in, alpha below peak.
        val a = WatermarkLayout.computeAnimated(100_000L, 1920, 1080)
        assertTrue("alpha > 0 during fade-in", a.alpha > 0f)
        assertTrue("alpha below peak during fade-in", a.alpha < WatermarkLayout.OPACITY)
    }

    @Test
    fun animatedWatermarkCyclesCornersDeterministically() {
        // Each 13s segment maps to a corner in order; verify the loop and that
        // the same timestamp always yields the same placement.
        val seg0 = WatermarkLayout.computeAnimated(1_000_000L, 1920, 1080)
        val seg1 = WatermarkLayout.computeAnimated((13_000L + 1_000L) * 1000L, 1920, 1080)
        val seg2 = WatermarkLayout.computeAnimated((26_000L + 1_000L) * 1000L, 1920, 1080)
        val seg3 = WatermarkLayout.computeAnimated((39_000L + 1_000L) * 1000L, 1920, 1080)
        val seg4 = WatermarkLayout.computeAnimated((52_000L + 1_000L) * 1000L, 1920, 1080)

        // 4 distinct corner placements across the first 4 segments.
        val distinct = setOf(
            "${seg0.left},${seg0.top}",
            "${seg1.left},${seg1.top}",
            "${seg2.left},${seg2.top}",
            "${seg3.left},${seg3.top}"
        )
        assertTrue("four corners used across segments", distinct.size == 4)

        // Segment 4 (= segment 0 mod 4) returns to the same corner.
        assertTrue("loops back to first corner after full cycle",
            seg4.left == seg0.left && seg4.top == seg0.top)

        // Determinism: identical timestamp -> identical placement.
        val again = WatermarkLayout.computeAnimated(1_000_000L, 1920, 1080)
        assertTrue("deterministic for same timestamp",
            again.left == seg0.left && again.top == seg0.top && again.alpha == seg0.alpha)
    }

    @Test
    fun animatedWatermarkTransformIsSubtle() {
        val a = WatermarkLayout.computeAnimated(5_000_000L, 1920, 1080)
        assertTrue("scale stays subtle", a.scale in 0.9f..1.1f)
        assertTrue("rotation stays subtle", kotlin.math.abs(a.rotation) <= 8f)
        // The box keeps the same overall footprint regardless of transform.
        val h = a.bottom - a.top
        assertTrue("height ~16% of frame", kotlin.math.abs(h - 1080f * WatermarkLayout.SIZE_FRACTION) < 2f)
    }
}
