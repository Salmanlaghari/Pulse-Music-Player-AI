package com.salmanlaghari.pulsemusicplayerai.core.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackConnectionManager(private val context: Context) {

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

    // Full song list references to resolve Song entities
    private var allSongsReference: List<Song> = emptyList()
    private var positionUpdateJob: Job? = null

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
        }, MoreExecutors.directExecutor())
    }

    fun setAllSongsReference(songs: List<Song>) {
        allSongsReference = songs
    }

    private fun updateStateFromController() {
        val controller = mediaController ?: return
        _isPlaying.value = controller.isPlaying
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        _duration.value = controller.duration.coerceAtLeast(0L)
        _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)

        val activeMediaId = controller.currentMediaItem?.mediaId
        if (activeMediaId != null) {
            _currentSong.value = allSongsReference.find { it.id.toString() == activeMediaId }
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
                    _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
                    _duration.value = controller.duration.coerceAtLeast(0L)
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
