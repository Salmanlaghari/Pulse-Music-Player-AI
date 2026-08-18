package com.salmanlaghari.pulsemusicplayerai.presentation.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps the Now Playing player controls honest about touch-target sizing:
 *  - Previous/Next >= 56dp (comfortably above the 48dp accessibility minimum)
 *  - Play/Pause >= 72dp and clearly the largest, primary control.
 *
 * These are plain Int constants (no Compose dependency) so they are unit-testable
 * on the JVM. (True on-device touch-target / screenshot verification still needs a
 * device; see PR description — this guards the chosen sizes in CI.)
 */
class NowPlayingControlSizesTest {
    @Test
    fun prevNextMeetsAccessibilityMinimum() {
        assertTrue(
            "Prev/Next must be >= 56dp (48dp a11y min)",
            NowPlayingControlSizes.PREV_NEXT_SIZE_DP >= 56
        )
        assertTrue(
            "Prev/Next icon must be >= 48dp a11y min",
            NowPlayingControlSizes.PREV_NEXT_ICON_DP >= NowPlayingControlSizes.ACCESSIBILITY_MIN_DP
        )
    }

    @Test
    fun playPauseIsPrimaryAndLargeEnough() {
        assertTrue(
            "Play/Pause container must be >= 72dp",
            NowPlayingControlSizes.PLAY_PAUSE_SIZE_DP >= 72
        )
        assertTrue(
            "Play/Pause icon must be >= 72dp (reads as primary)",
            NowPlayingControlSizes.PLAY_PAUSE_ICON_DP >= 72
        )
        assertTrue(
            "Play/Pause must be larger than Prev/Next",
            NowPlayingControlSizes.PLAY_PAUSE_SIZE_DP > NowPlayingControlSizes.PREV_NEXT_SIZE_DP
        )
    }
}
