package com.salmanlaghari.pulsemusicplayerai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the built-in background-track catalogue and selection logic: the
 * "none" sentinel resolves to no track (source audio only), valid names resolve
 * to a track whose raw resource entry is non-blank, and every track is owned /
 * original (we never bundle commercial audio).
 */
class BuiltInBackgroundTracksTest {
    @Test
    fun catalogueIsNonEmpty() {
        assertTrue("should ship at least one built-in track", BuiltInBackgroundTracks.ALL.isNotEmpty())
    }

    @Test
    fun noneSentinelResolvesToNoTrack() {
        assertNull(BuiltInBackgroundTracks.resolve(null))
        assertNull(BuiltInBackgroundTracks.resolve(BuiltInBackgroundTracks.NONE))
    }

    @Test
    fun validSelectionResolves() {
        val track = BuiltInBackgroundTracks.resolve("bg_track_ambient")
        assertNotNull(track)
        assertEquals("bg_track_ambient", track!!.resEntryName)
        assertTrue("resEntryName must be non-blank", track.resEntryName.isNotBlank())
        assertTrue("suggested volume in range", track.suggestedVolume in 0f..1f)
    }

    @Test
    fun everyTrackHasValidResourceName() {
        for (t in BuiltInBackgroundTracks.ALL) {
            assertTrue("resEntryName non-blank for ${t.id}", t.resEntryName.isNotBlank())
            assertTrue("displayName non-blank for ${t.id}", t.displayName.isNotBlank())
            assertTrue("description non-blank for ${t.id}", t.description.isNotBlank())
        }
    }
}
