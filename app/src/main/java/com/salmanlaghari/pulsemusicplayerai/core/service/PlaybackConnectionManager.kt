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
    private var positionUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        try {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

            mediaControllerFuture?.addListener({
                try {
                    mediaController = mediaControllerFuture?.get()
                    mediaController?.addListener(PlayerListener())
                    updateStateFromController()
                    restoreLastPlayedState()
                } catch (e: Exception) {
                    android.util.Log.e("PlaybackConnection", "Failed to resolve MediaController: ${e.message}")
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            android.util.Log.e("PlaybackConnection", "Failed to build SessionToken or MediaController: ${e.message}")
        }
    }

    fun setAllSongsReference(songs: List<Song>) {
        allSongsReference = songs
        restoreLastPlayedState()
    }

    private fun updateStateFromController() {
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
            val foundSong = allSongsReference.find { it.id.toString() == activeMediaId }
            _currentSong.value = foundSong
            if (foundSong != null) {
                saveLastPlayedState(foundSong.id, controller.currentPosition.coerceAtLeast(0L))
            }
        } else {
            _currentSong.value = null
        }

        // Rebuild current queue list from MediaController items
        val queueItems = mutableListOf<Song>()
        for (i in 0 until controller.mediaItemCount) {
            val mId = controller.getMediaItemAt(i).mediaId
            allSongsReference.find { it.id.toString() == mId }?.let { queueItems.add(it) }
        }
        _currentQueue.value = queueItems

        if (controller.isPlaying) {
            startPositionUpdates()
        } else {
            stopPositionUpdates()
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                mediaController?.let { controller ->
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    _currentPosition.value = pos
                    _duration.value = controller.duration.coerceAtLeast(0L)
                    _currentSong.value?.let { saveLastPlayedState(it.id, pos) }
                }
                delay(1000)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // Playback control wrappers
    fun playSong(song: Song, playQueue: List<Song>) {
        val controller = mediaController ?: return

        controller.stop()
        controller.clearMediaItems()

        // Set references and load items
        _currentQueue.value = playQueue
        val mediaItems = playQueue.map { it.toMediaItem() }
        controller.addMediaItems(mediaItems)

        val targetIndex = playQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        controller.seekTo(targetIndex, 0L)
        controller.prepare()
        controller.play()

        // Restore speed/pitch to player
        controller.setPlaybackParameters(androidx.media3.common.PlaybackParameters(_playbackSpeed.value, _playbackPitch.value))

        updateStateFromController()
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun next() {
        mediaController?.seekToNext()
    }

    fun previous() {
        mediaController?.seekToPrevious()
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

                    // Pre-load the song silently into queue
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
    }
}
