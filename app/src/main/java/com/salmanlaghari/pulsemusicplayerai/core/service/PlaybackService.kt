package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    companion object {
        val audioEffectManager = AudioEffectManager()

        // Browser-like User-Agent for streaming bypass — many CDNs and frontends
        // reject requests with non-browser User-Agents.
        private const val STREAM_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
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

        // Custom HTTP DataSource with bypass headers for streaming.
        // Many audio CDNs (JioSaavn, Internet Archive, Invidious, Piped) and some
        // Cobalt relay endpoints reject or throttle requests that don't look like
        // a real browser. We set a browser User-Agent and standard browser headers
        // so ExoPlayer can stream from all these sources without being blocked.
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(STREAM_USER_AGENT)
            .setAllowCrossProtocolRedirects(true) // http -> https redirects
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "audio/*, video/*, application/octet-stream, */*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Accept-Encoding" to "identity", // no gzip for audio streams
                    "Connection" to "keep-alive",
                    "Range" to "bytes=0-" // request streaming from byte 0
                )
            )

        // Wrap HTTP factory in DefaultDataSource so both http(s):// and content://
        // (device music) URIs are handled transparently.
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // Build ExoPlayer with custom media source factory that uses our bypass DataSource
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
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
