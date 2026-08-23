package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import com.salmanlaghari.pulsemusicplayerai.domain.model.YouTubeSong
import com.salmanlaghari.pulsemusicplayerai.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackConnectionManager(private val context: Context) {

    companion object {
        private val LAST_SONG_ID_KEY = stringPreferencesKey("last_song_id")
        private val LAST_POSITION_KEY = longPreferencesKey("last_position")
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // State flows representing actual playback states
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    // Sleep Timer States
    private val _sleepTimerRemainingMs = MutableStateFlow<Long>(0L)
    val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs.asStateFlow()

    // Playback Speed & Pitch States
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    // Full song list references to resolve Song entities
    private var allSongsReference: List<Song> = emptyList()
    private var youTubeSongsReference: List<YouTubeSong> = emptyList()
    private var positionUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Track last persisted song id to avoid hammering DataStore every tick (fixes UI jank / hang)
    private var lastSavedSongId: Long = -1L

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            mediaController?.addListener(PlayerListener())
            updateStateFromController()
            restoreLastPlayedState()
        }, MoreExecutors.directExecutor())
    }

    fun setAllSongsReference(songs: List<Song>) {
        allSongsReference = songs
        restoreLastPlayedState()
    }

    fun setYouTubeSongsReference(songs: List<YouTubeSong>) {
        youTubeSongsReference = songs
    }

    /**
     * Reconcile every UI-facing StateFlow from the live MediaController.
     *
     * This is the single source of truth for what the Now Playing screen shows.
     * It is invoked both from the [PlayerListener] callbacks and on a tight loop
     * while audio is playing (see [startPositionUpdates]) so the UI can NEVER get
     * stuck on a stale "Not Playing / 00:00" state: even if the one-shot refresh
     * inside [playSong] runs before the controller has resolved the current media
     * item / playback state, the next tick here corrects it.
     *
     * Note: this method intentionally does NOT start/stop the position-update loop
     * itself — that is driven by [updateStateFromController] and [playSong] so we
     * don't recursively re-launch the loop.
     */
    private fun syncStateFromController() {
        val controller = mediaController ?: return
        _isPlaying.value = controller.isPlaying
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        _duration.value = controller.duration.coerceAtLeast(0L)
        _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
        _playbackSpeed.value = controller.playbackParameters.speed
        _playbackPitch.value = controller.playbackParameters.pitch

        val activeMediaId = controller.currentMediaItem?.mediaId
        if (activeMediaId != null) {
            // Try to find song in local songs first, then YouTube songs.
            // YouTube songs converted via toSong() have Song.id = id.hashCode().toLong(),
            // so their mediaId is hashCode.toString(). Match by that, or by "yt_$id" prefix.
            val foundSong = allSongsReference.find { it.id.toString() == activeMediaId }
                ?: youTubeSongsReference.find { "yt_${it.id}" == activeMediaId }?.toSong()
                ?: youTubeSongsReference.find { it.id.hashCode().toLong().toString() == activeMediaId }?.toSong()
            _currentSong.value = foundSong
            if (foundSong != null && foundSong.id != lastSavedSongId) {
                lastSavedSongId = foundSong.id
                saveLastPlayedState(foundSong.id, controller.currentPosition.coerceAtLeast(0L))
            }
        } else {
            _currentSong.value = null
        }

        // Rebuild current queue list from MediaController items
        val queueItems = mutableListOf<Song>()
        for (i in 0 until controller.mediaItemCount) {
            val mId = controller.getMediaItemAt(i).mediaId
            val foundSong = allSongsReference.find { it.id.toString() == mId }
                ?: youTubeSongsReference.find { "yt_${it.id}" == mId }?.toSong()
                ?: youTubeSongsReference.find { it.id.hashCode().toLong().toString() == mId }?.toSong()
            foundSong?.let { queueItems.add(it) }
        }
        _currentQueue.value = queueItems
    }

    /**
     * Push the current controller state to the UI flows, and (re)start the
     * continuous position/state reconciliation loop whenever audio is playing.
     */
    private fun updateStateFromController() {
        syncStateFromController()
        val controller = mediaController
        if (controller != null) {
            if (controller.isPlaying) startPositionUpdates() else stopPositionUpdates()
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                // Continuously reconcile the full playback state from the controller.
                // This guarantees the Now Playing screen always mirrors what is actually
                // playing (current track, play/pause, position, duration) — even if the
                // one-shot refresh inside playSong() ran before the controller had
                // resolved the current media item or playback state.
                syncStateFromController()

                // 500ms tick gives a smoother progress bar without flooding the UI thread
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // Playback control wrappers
    fun playSong(song: Song, playQueue: List<Song>) {
        try {
            val controller = mediaController ?: return

            // Validate: song must have a playable URI.
            // Both http(s):// (streaming) and content:// (local/device music) URIs are valid.
            // ExoPlayer handles both via its default data source factories.
            val songUri = song.uri?.toString() ?: ""
            if (songUri.isEmpty() || songUri == "null") {
                android.util.Log.w("PlaybackConn", "Cannot play song with invalid URI: ${song.title} uri=$songUri")
                return
            }

            controller.clearMediaItems()

            // Ensure queue is not empty and filter out songs with empty/invalid URIs.
            // Allow both http(s):// (streaming) and content:// (local/device) URIs.
            val safeQueue = playQueue.filter { s ->
                val uri = s.uri?.toString() ?: ""
                uri.isNotEmpty() && uri != "null" &&
                (uri.startsWith("http") || uri.startsWith("content"))
            }.let { filtered ->
                if (filtered.isEmpty()) listOf(song) else filtered
            }

            if (safeQueue.isEmpty()) {
                android.util.Log.w("PlaybackConn", "No valid songs in queue")
                return
            }

            // Set references and load items
            _currentQueue.value = safeQueue
            val mediaItems = safeQueue.map { it.toMediaItem() }
            controller.setMediaItems(mediaItems, true)

            val targetIndex = safeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            controller.seekTo(targetIndex, 0L)
            controller.prepare()
            controller.play()

            // Restore speed/pitch to player
            controller.setPlaybackParameters(androidx.media3.common.PlaybackParameters(_playbackSpeed.value, _playbackPitch.value))

            updateStateFromController()
            // Force the continuous reconciliation loop to start immediately so the
            // Now Playing screen reflects the new track without waiting for an
            // asynchronous player callback.
            startPositionUpdates()
        } catch (e: Exception) {
            android.util.Log.e("PlaybackConn", "playSong failed", e)
        }
    }

    fun updateQueue(newQueue: List<Song>) {
        try {
            val controller = mediaController ?: return
            val safeQueue = newQueue.filter { s ->
                val uri = s.uri?.toString() ?: ""
                uri.isNotEmpty() && uri != "null" &&
                        (uri.startsWith("http") || uri.startsWith("content"))
            }
            if (safeQueue.isEmpty()) return

            _currentQueue.value = safeQueue
            val mediaItems = safeQueue.map { it.toMediaItem() }
            controller.setMediaItems(mediaItems, true)
            updateStateFromController()
        } catch (e: Exception) {
            android.util.Log.e("PlaybackConn", "updateQueue failed", e)
        }
    }

    fun addMediaItemToQueue(song: Song) {
        try {
            val controller = mediaController ?: return
            val mediaItem = song.toMediaItem()
            controller.addMediaItem(mediaItem)
            updateStateFromController()
        } catch (e: Exception) {
            android.util.Log.e("PlaybackConn", "addMediaItemToQueue failed", e)
        }
    }

    var onNextRequested: (() -> Unit)? = null
    var onPreviousRequested: (() -> Unit)? = null

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun next() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount > controller.currentMediaItemIndex + 1) {
            controller.seekToNext()
        } else {
            onNextRequested?.invoke()
        }
    }

    fun previous() {
        val controller = mediaController ?: return
        if (controller.currentMediaItemIndex > 0) {
            controller.seekToPrevious()
        } else {
            onPreviousRequested?.invoke()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val nextMode = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = nextMode
        _shuffleEnabled.value = nextMode
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    // --- Sleep Timer ---
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingMs.value = 0L
            return
        }
        _sleepTimerRemainingMs.value = minutes * 60 * 1000L
        sleepTimerJob = scope.launch {
            while (_sleepTimerRemainingMs.value > 0L) {
                delay(1000)
                _sleepTimerRemainingMs.value = (_sleepTimerRemainingMs.value - 1000L).coerceAtLeast(0L)
            }
            // Timer expired: pause music
            pause()
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = 0L
    }

    // --- Speed & Pitch Controls ---
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val controller = mediaController ?: return
        controller.setPlaybackParameters(androidx.media3.common.PlaybackParameters(speed, _playbackPitch.value))
    }

    fun setPlaybackPitch(pitch: Float) {
        _playbackPitch.value = pitch
        val controller = mediaController ?: return
        controller.setPlaybackParameters(androidx.media3.common.PlaybackParameters(_playbackSpeed.value, pitch))
    }

    // --- State Persistence ---
    private fun saveLastPlayedState(songId: Long, positionMs: Long) {
        scope.launch {
            try {
                context.dataStore.edit { preferences ->
                    preferences[LAST_SONG_ID_KEY] = songId.toString()
                    preferences[LAST_POSITION_KEY] = positionMs
                }
            } catch (e: Exception) {
                // Ignore any write issues during quick position ticks
            }
        }
    }

    private fun restoreLastPlayedState() {
        if (allSongsReference.isEmpty()) return
        scope.launch {
            try {
                val preferences = context.dataStore.data.first()
                val lastSongIdStr = preferences[LAST_SONG_ID_KEY] ?: return@launch
                val lastPosition = preferences[LAST_POSITION_KEY] ?: 0L
                val lastSong = allSongsReference.find { it.id.toString() == lastSongIdStr }

                val controller = mediaController
                if (lastSong != null && controller != null && controller.currentMediaItem == null) {
                    _currentSong.value = lastSong
                    _currentPosition.value = lastPosition
                    _duration.value = lastSong.duration
                    lastSavedSongId = lastSong.id

                    // Pre-load the song silently into queue (do not auto-play on restore)
                    controller.setMediaItem(lastSong.toMediaItem())
                    controller.seekTo(lastPosition)
                    controller.prepare()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun removeFromQueue(songId: Long) {
        val controller = mediaController ?: return
        for (i in 0 until controller.mediaItemCount) {
            if (controller.getMediaItemAt(i).mediaId == songId.toString()) {
                controller.removeMediaItem(i)
                break
            }
        }
        updateStateFromController()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
            controller.moveMediaItem(fromIndex, toIndex)
            updateStateFromController()
        }
    }

    fun clearQueue() {
        val controller = mediaController ?: return
        controller.clearMediaItems()
        updateStateFromController()
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val controller = mediaController ?: run {
                    updateStateFromController()
                    return
                }
                val repeatMode = controller.repeatMode
                val itemCount = controller.mediaItemCount

                if (repeatMode == Player.REPEAT_MODE_ONE) {
                    controller.seekTo(controller.currentMediaItemIndex, 0L)
                    controller.prepare()
                    controller.play()
                } else if (itemCount > 1 && repeatMode != Player.REPEAT_MODE_ONE) {
                    controller.seekToNext()
                    controller.prepare()
                    controller.play()
                } else if (repeatMode == Player.REPEAT_MODE_ALL && itemCount > 0) {
                    controller.seekTo(0, 0L)
                    controller.prepare()
                    controller.play()
                } else {
                    controller.stop()
                }
            }
            updateStateFromController()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateStateFromController()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateStateFromController()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.w("PlaybackConn", "Player error: ${error.errorCodeName} — auto-skipping")
            // Auto-skip to next track so a bad/expired URL never leaves the app "hung"
            try {
                val controller = mediaController
                if (controller != null && controller.mediaItemCount > 1) {
                    controller.seekToNext()
                    controller.prepare()
                    controller.play()
                } else {
                    controller?.stop()
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackConn", "Auto-skip failed", e)
            }
            updateStateFromController()
        }
    }
}
