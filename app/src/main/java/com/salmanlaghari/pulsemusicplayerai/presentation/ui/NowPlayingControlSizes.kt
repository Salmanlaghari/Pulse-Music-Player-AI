package com.salmanlaghari.pulsemusicplayerai.presentation.ui

/**
 * Touch-target / visual sizes (in dp) for the Now Playing player controls.
 *
 * Kept as plain Int constants (no Compose dependency) so they can be unit-tested
 * on the JVM. Requirements (Android accessibility minimum is 48dp):
 *  - Previous / Next:  >= 56dp visual, comfortably tappable.
 *  - Play / Pause:     >= 72dp and clearly the largest, primary control.
 *
 * The Composable multiplies these by `dp` at the call site.
 */
object NowPlayingControlSizes {
    /** Visual size of the Previous / Next icon buttons. */
    const val PREV_NEXT_SIZE_DP = 64
    /** Icon size drawn inside the Previous / Next buttons. */
    const val PREV_NEXT_ICON_DP = 48
    /** Visual (container) size of the central Play / Pause button. */
    const val PLAY_PAUSE_SIZE_DP = 96
    /** Icon size drawn inside the Play / Pause button (must read as primary). */
    const val PLAY_PAUSE_ICON_DP = 72

    /** Android accessibility minimum touch target. */
    const val ACCESSIBILITY_MIN_DP = 48
}
