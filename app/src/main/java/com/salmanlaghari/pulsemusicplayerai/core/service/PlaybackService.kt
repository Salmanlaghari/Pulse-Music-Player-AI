package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    companion object {
        val audioEffectManager = AudioEffectManager()
    }

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Smooth playback LoadControl: larger buffer + min buffer before playing
        // prevents the "hung/stutter" behaviour during network streaming.
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs       = */ 30_000, // buffer up to 30s before playing
                /* maxBufferMs       = */ 90_000, // keep up to 90s buffered
                /* playbackBufferMs  = */ 1_500,  // start playback once 1.5s buffered
                /* rebufferBufferMs  = */ 3_000   // resume once 3s buffered after stall
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build()

        exoPlayer?.let { player ->
            audioEffectManager.initEffects(player.audioSessionId)
        }

        exoPlayer?.let { player ->
            mediaSession = MediaSession.Builder(this, player)
                .setCallback(CustomSessionCallback())
                .build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        audioEffectManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }

    private inner class CustomSessionCallback : MediaSession.Callback
}
