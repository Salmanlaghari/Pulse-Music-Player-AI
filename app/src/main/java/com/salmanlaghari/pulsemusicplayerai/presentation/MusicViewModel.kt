package com.salmanlaghari.pulsemusicplayerai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackConnectionManager
import com.salmanlaghari.pulsemusicplayerai.data.repository.MusicRepository
import com.salmanlaghari.pulsemusicplayerai.domain.model.Album
import com.salmanlaghari.pulsemusicplayerai.domain.model.Artist
import com.salmanlaghari.pulsemusicplayerai.domain.model.Folder
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    private val musicRepository: MusicRepository,
    private val playbackConnectionManager: PlaybackConnectionManager
) : ViewModel() {

    // 1. Permission Granted State
    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    // Loading state — true during initial data load (MediaStore scan, albums, artists, etc.)
    // The splash screen navigates here quickly, but data loading takes several seconds.
    // We show a loading overlay until this becomes false so the user doesn't see a blank/blue screen.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 2. Local Lists States
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _recentlyAdded = MutableStateFlow<List<Song>>(emptyList())
    val recentlyAdded: StateFlow<List<Song>> = _recentlyAdded.asStateFlow()

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    // 3. Playback Controller States (forwarded from connection manager)
    val currentSong = playbackConnectionManager.currentSong
    val isPlaying = playbackConnectionManager.isPlaying
    val currentPosition = playbackConnectionManager.currentPosition
    val duration = playbackConnectionManager.duration
    val shuffleEnabled = playbackConnectionManager.shuffleEnabled
    val repeatMode = playbackConnectionManager.repeatMode
    val currentQueue = playbackConnectionManager.currentQueue

    // Sleep Timer, Speed, and Pitch States
    val sleepTimerRemainingMs = playbackConnectionManager.sleepTimerRemainingMs
    val playbackSpeed = playbackConnectionManager.playbackSpeed
    val playbackPitch = playbackConnectionManager.playbackPitch

    // Equalizer & Audio Effects States
    val isEqSupported get() = com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.isEqSupported
    val isBassSupported get() = com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.isBassSupported
    val isVirtualizerSupported get() = com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.isVirtualizerSupported
    val isLoudnessSupported get() = com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.isLoudnessSupported

    val bassStrength get() = com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.bassStrength
    val virtualizerStrength get() = com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.virtualizerStrength
    val loudnessGainDb get() = com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.loudnessGainDb

    // 4. Search States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Song>> = combine(_allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true) ||
                        song.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 5. Handle Permission Status & Load Music
    fun setPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
        if (granted) {
            loadMusicData()
        } else {
            // Permission NOT granted — don't load music, and immediately clear
            // the loading overlay so the user can see the app and grant permission.
            // Without this, the overlay would stay stuck forever (loadMusicData
            // is never called, so _isLoading would never become false).
            _isLoading.value = false
        }
    }

    fun loadMusicData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch list from repository
                val songsList = musicRepository.getAllSongs(forceRefresh = true)
                _allSongs.value = songsList
                playbackConnectionManager.setAllSongsReference(songsList)

                _albums.value = musicRepository.getAlbums()
                _artists.value = musicRepository.getArtists()
                _folders.value = musicRepository.getFolders()
                _recentlyAdded.value = musicRepository.getRecentlyAdded()

                // Apply favorites ONCE synchronously (use .first() — NOT .collect{}).
                // IMPORTANT: favoriteIdsFlow is an infinite cold Flow from DataStore.
                // Calling .collect{} here would suspend forever and the finally block
                // would never run, leaving the loading overlay stuck on screen.
                try {
                    val favIds = musicRepository.favoriteIdsFlow.first()
                    val updatedSongs = songsList.map { song ->
                        song.copy(isFavorite = favIds.contains(song.id.toString()))
                    }
                    _allSongs.value = updatedSongs
                    playbackConnectionManager.setAllSongsReference(updatedSongs)
                    _recentlyAdded.value = updatedSongs.sortedByDescending { it.dateAdded }
                    _favoriteSongs.value = updatedSongs.filter { favIds.contains(song.id.toString()) }
                } catch (fe: Exception) {
                    android.util.Log.w("MusicVM", "Initial favorites apply failed: " + fe.message)
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicVM", "loadMusicData failed", e)
            } finally {
                // CRITICAL: this MUST run. Set loading false BEFORE launching the
                // infinite favorites collector below, otherwise the overlay never hides.
                _isLoading.value = false
            }

            // Listen to dynamic favorites updates in a SEPARATE background coroutine.
            // This runs forever (as intended) but does NOT block the loading state.
            launch {
                try {
                    musicRepository.favoriteIdsFlow.collect { favIds ->
                        val currentSongs = _allSongs.value
                        if (currentSongs.isEmpty()) return@collect
                        val updatedSongs = currentSongs.map { song ->
                            song.copy(isFavorite = favIds.contains(song.id.toString()))
                        }
                        _allSongs.value = updatedSongs
                        playbackConnectionManager.setAllSongsReference(updatedSongs)
                        _recentlyAdded.value = updatedSongs.sortedByDescending { it.dateAdded }
                        _favoriteSongs.value = updatedSongs.filter { favIds.contains(song.id.toString()) }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicVM", "Favorites collector ended: " + e.message)
                }
            }
        }

        // SAFETY TIMEOUT: If data loading somehow hangs (e.g. MediaStore very slow
        // on a device with thousands of files, or a repository deadlock), force
        // the loading overlay off after 20 seconds so the user can always access
        // the app. This is a last-resort guard against a permanently stuck screen.
        viewModelScope.launch {
            kotlinx.coroutines.delay(20_000)
            if (_isLoading.value) {
                android.util.Log.w("MusicVM", "Loading safety timeout — forcing isLoading=false after 20s")
                _isLoading.value = false
            }
        }
    }

    // 6. Playback Control Handlers
    fun playSong(song: Song, customQueue: List<Song> = _allSongs.value) {
        try {
            val queue = if (customQueue.isEmpty()) listOf(song) else customQueue
            playbackConnectionManager.playSong(song, queue)
        } catch (e: Exception) {
            android.util.Log.e("MusicVM", "playSong failed", e)
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            playbackConnectionManager.pause()
        } else {
            playbackConnectionManager.play()
        }
    }

    fun skipToNext() {
        // Track song change for interstitial ad (every 3 songs)
        AdManager.incrementSongChangeCount()
        playbackConnectionManager.next()
    }

    fun skipToPrevious() {
        playbackConnectionManager.previous()
    }

    fun seekTo(positionMs: Long) {
        playbackConnectionManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playbackConnectionManager.toggleShuffle()
    }

    fun toggleRepeatMode() {
        playbackConnectionManager.toggleRepeatMode()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song.id)
        }
    }

    // 7. Queue Control Handlers
    fun removeFromQueue(song: Song) {
        playbackConnectionManager.removeFromQueue(song.id)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        playbackConnectionManager.moveQueueItem(fromIndex, toIndex)
    }

    fun clearQueue() {
        playbackConnectionManager.clearQueue()
    }

    // 8. Sleep Timer & Parameter Control
    fun startSleepTimer(minutes: Int) {
        playbackConnectionManager.startSleepTimer(minutes)
    }

    fun stopSleepTimer() {
        playbackConnectionManager.stopSleepTimer()
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackConnectionManager.setPlaybackSpeed(speed)
    }

    fun setPlaybackPitch(pitch: Float) {
        playbackConnectionManager.setPlaybackPitch(pitch)
    }

    // 9. Equalizer and Audio effects Wrapper
    fun getEqBandsCount(): Int {
        return com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.getBandsCount()
    }

    fun getEqBandFrequency(band: Short): Int {
        return com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.getBandFrequency(band)
    }

    fun getEqBandLevelRange(): Pair<Short, Short> {
        return com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.getBandLevelRange()
    }

    fun getEqBandLevel(band: Short): Short {
        return com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.getBandLevel(band)
    }

    fun setEqBandLevel(band: Short, level: Short) {
        com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.setBandLevel(band, level)
    }

    fun setBassBoostStrength(strength: Short) {
        com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.setBassStrength(strength)
    }

    fun setVirtualizerStrength(strength: Short) {
        com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.setVirtualizerStrength(strength)
    }

    fun setLoudnessGain(gainDb: Float) {
        com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.setLoudnessGain(gainDb)
    }

    fun applyEqPreset(presetName: String) {
        com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackService.audioEffectManager.applyPreset(presetName)
    }
}

class MusicViewModelFactory(
    private val musicRepository: MusicRepository,
    private val playbackConnectionManager: PlaybackConnectionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(musicRepository, playbackConnectionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
