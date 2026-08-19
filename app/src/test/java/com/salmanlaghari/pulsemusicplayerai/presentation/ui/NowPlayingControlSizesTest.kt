package com.salmanlaghari.pulsemusicplayerai.presentation.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps the Now Playing player controls honest about sizing. Exact spec (Task A):
 *  - Previous/Next: 56dp container, 28dp icon.
 *  - Play/Pause:    80dp container, 40dp icon, filled circular primary control.
 *  - Play/Pause container must be larger than Prev/Next container (primary control).
 *
 * These are plain Int constants (no Compose dependency) so they are unit-testable
 * on the JVM. (True on-device touch-target / screenshot verification still needs a
 * device; see PR description — this guards the exact chosen sizes in CI.)
 */
class NowPlayingControlSizesTest {
    @Test
    fun prevNextExactSpec() {
        assertEquals("Prev/Next container must be 56dp", 56, NowPlayingControlSizes.PREV_NEXT_SIZE_DP)
        assertEquals("Prev/Next icon must be 28dp", 28, NowPlayingControlSizes.PREV_NEXT_ICON_DP)
        assertTrue(
            "Prev/Next container must meet 48dp a11y min",
            NowPlayingControlSizes.PREV_NEXT_SIZE_DP >= NowPlayingControlSizes.ACCESSIBILITY_MIN_DP
        )
    }

    @Test
    fun playPauseExactSpec() {
        assertEquals("Play/Pause container must be 80dp", 80, NowPlayingControlSizes.PLAY_PAUSE_SIZE_DP)
        assertEquals("Play/Pause icon must be 40dp", 40, NowPlayingControlSizes.PLAY_PAUSE_ICON_DP)
        assertTrue(
            "Play/Pause container must be larger than Prev/Next",
            NowPlayingControlSizes.PLAY_PAUSE_SIZE_DP > NowPlayingControlSizes.PREV_NEXT_SIZE_DP
        )
    }
}
