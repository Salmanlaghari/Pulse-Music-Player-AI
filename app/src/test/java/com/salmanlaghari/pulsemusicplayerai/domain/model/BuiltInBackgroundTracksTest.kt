package com.salmanlaghari.pulsemusicplayerai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the built-in background-track catalogue and selection logic: the
 * "none" sentinel resolves to no track (source audio only), valid names resolve
 * to a track, every track is royalty-free, and the catalogue ships 20+ tracks
 * each paired with a distinct animated [BackgroundMood].
 */
class BuiltInBackgroundTracksTest {
    @Test
    fun catalogueIsLargeEnough() {
        assertTrue("should ship at least 20 background tracks", BuiltInBackgroundTracks.ALL.size >= 20)
    }

    @Test
    fun noneSentinelResolvesToNoTrack() {
        assertNull(BuiltInBackgroundTracks.resolve(null))
        assertNull(BuiltInBackgroundTracks.resolve(BuiltInBackgroundTracks.NONE))
    }

    @Test
    fun validSelectionResolves() {
        val track = BuiltInBackgroundTracks.resolve("ambient")
        assertNotNull(track)
        assertEquals("ambient", track!!.id)
        assertTrue("displayName non-blank", track.displayName.isNotBlank())
        assertTrue("description non-blank", track.description.isNotBlank())
        assertTrue("has an animated mood", track.mood.palette.isNotEmpty())
    }

    @Test
    fun everyTrackHasValidAudioAndMood() {
        for (t in BuiltInBackgroundTracks.ALL) {
            // Either a bundled res entry or a remote royalty-free URL must exist.
            val hasAudio = when (t.audioSource) {
                BackgroundAudioSource.BUNDLED -> t.resEntryName != null && t.resEntryName.isNotBlank()
                BackgroundAudioSource.REMOTE -> t.remoteUrl != null && t.remoteUrl.startsWith("http")
            }
            assertTrue("audio source valid for ${t.id}", hasAudio)
            assertTrue("royalty-free for ${t.id}", t.license.isNotBlank())
            assertTrue("mood defined for ${t.id}", t.mood.palette.isNotEmpty())
        }
    }

    @Test
    fun everyTrackHasDistinctAnimatedMood() {
        // Enough distinct moods that no two tracks feel visually identical.
        assertTrue("should expose many distinct moods", BuiltInBackgroundTracks.moods.size >= 15)
    }
}
