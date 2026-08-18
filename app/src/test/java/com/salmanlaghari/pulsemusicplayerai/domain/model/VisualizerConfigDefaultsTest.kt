package com.salmanlaghari.pulsemusicplayerai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the default export config: the Pulse watermark is ON by default and
 * no background track is selected (source audio only) — the original behaviour.
 */
class VisualizerConfigDefaultsTest {
    @Test
    fun watermarkIsEnabledByDefault() {
        assertTrue("Watermark must default ON", VisualizerVideoConfig().watermarkEnabled)
    }

    @Test
    fun noBackgroundTrackByDefault() {
        assertNull("Default export uses the source audio only", VisualizerVideoConfig().backgroundTrackResName)
    }

    @Test
    fun backgroundTrackVolumeInRange() {
        val v = VisualizerVideoConfig().backgroundTrackVolume
        assertTrue("backgroundTrackVolume must be within 0..1", v > 0f && v <= 1f)
    }

    @Test
    fun configCopiesNewFields() {
        val cfg = VisualizerVideoConfig().copy(
            backgroundTrackResName = "bg_track_ambient",
            backgroundTrackVolume = 0.4f,
            watermarkEnabled = false
        )
        assertEquals("bg_track_ambient", cfg.backgroundTrackResName)
        assertEquals(0.4f, cfg.backgroundTrackVolume, 0.0001f)
        assertFalse(cfg.watermarkEnabled)
    }
}
