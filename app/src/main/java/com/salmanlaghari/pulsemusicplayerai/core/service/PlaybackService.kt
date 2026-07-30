package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.youtube.YouTubeMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlaybackService : MediaSessionService() {

    companion object {
        val audioEffectManager = AudioEffectManager()
    }

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize ExoPlayer with YouTube support
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Create renderers factory with YouTube extension support
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        // Create media source factory that supports YouTube
        val mediaSourceFactory = DefaultMediaSourceFactory(renderersFactory)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // 2. Initialize Audio effects
        exoPlayer?.let { player ->
            audioEffectManager.initEffects(player.audioSessionId)
        }

        // 3. Initialize Media3 MediaSession
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
