package com.salmanlaghari.pulsemusicplayerai.presentation.ui

/**
 * Touch-target / visual sizes (in dp) for the Now Playing player controls.
 *
 * Kept as plain Int constants (no Compose dependency) so they can be unit-tested
 * on the JVM. Exact spec (enforced by NowPlayingControlSizesTest):
 *  - Previous / Next:  56dp container, 28dp icon.
 *  - Play / Pause:     80dp container, 40dp icon, filled circular primary control.
 *
 * The container size is the touch target (>= 48dp a11y minimum); the icon size
 * is the glyph drawn inside it, which is intentionally smaller than the touch
 * target. The Composable multiplies these by `dp` at the call site.
 */
object NowPlayingControlSizes {
    /** Visual size of the Previous / Next icon buttons. */
    const val PREV_NEXT_SIZE_DP = 56
    /** Icon size drawn inside the Previous / Next buttons. */
    const val PREV_NEXT_ICON_DP = 28
    /** Visual (container) size of the central Play / Pause button. */
    const val PLAY_PAUSE_SIZE_DP = 80
    /** Icon size drawn inside the Play / Pause button (must read as primary). */
    const val PLAY_PAUSE_ICON_DP = 36

    /** Android accessibility minimum touch target. */
    const val ACCESSIBILITY_MIN_DP = 48
}
