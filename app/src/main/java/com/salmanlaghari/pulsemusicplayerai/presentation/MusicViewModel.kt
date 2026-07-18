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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    private val musicRepository: MusicRepository,
    private val playbackConnectionManager: PlaybackConnectionManager,
    private val lyricsRepository: com.salmanlaghari.pulsemusicplayerai.data.repository.LyricsRepository,
    private val premiumManager: com.salmanlaghari.pulsemusicplayerai.utils.PremiumManager
) : ViewModel() {

    // Synced Lyrics state
    private val _currentLyrics = MutableStateFlow<List<com.salmanlaghari.pulsemusicplayerai.data.repository.LyricLine>>(emptyList())
    val currentLyrics: StateFlow<List<com.salmanlaghari.pulsemusicplayerai.data.repository.LyricLine>> = _currentLyrics.asStateFlow()

    // Premium tier state
    private val _activeTier = MutableStateFlow(com.salmanlaghari.pulsemusicplayerai.utils.UserTier.GUEST)
    val activeTier: StateFlow<com.salmanlaghari.pulsemusicplayerai.utils.UserTier> = _activeTier.asStateFlow()

    init {
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    try {
                        _currentLyrics.value = lyricsRepository.getLyrics(song.title, song.artist, song.duration)
                    } catch (e: Exception) {
                        _currentLyrics.value = emptyList()
                    }
                } else {
                    _currentLyrics.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            combine(premiumManager.persistedTierFlow, premiumManager.sessionTier) { persisted, session ->
                session ?: persisted
            }.collect {
                _activeTier.value = it
            }
        }
    }

    fun setPersistedTier(tier: com.salmanlaghari.pulsemusicplayerai.utils.UserTier) {
        viewModelScope.launch {
            premiumManager.setPersistedTier(tier)
        }
    }

    fun setTemporarySessionTier(tier: com.salmanlaghari.pulsemusicplayerai.utils.UserTier?) {
        premiumManager.setTemporarySessionTier(tier)
    }

    // 1. Permission Granted State
    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

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
        }
    }

    fun loadMusicData() {
        viewModelScope.launch {
            // Fetch list from repository
            val songsList = musicRepository.getAllSongs(forceRefresh = true)
            _allSongs.value = songsList
            playbackConnectionManager.setAllSongsReference(songsList)

            _albums.value = musicRepository.getAlbums()
            _artists.value = musicRepository.getArtists()
            _folders.value = musicRepository.getFolders()
            _recentlyAdded.value = musicRepository.getRecentlyAdded()

            // Listen to dynamic favorites updates
            musicRepository.favoriteIdsFlow.collect { favIds ->
                val updatedSongs = songsList.map { song ->
                    song.copy(isFavorite = favIds.contains(song.id.toString()))
                }
                _allSongs.value = updatedSongs
                playbackConnectionManager.setAllSongsReference(updatedSongs)
                _recentlyAdded.value = updatedSongs.sortedByDescending { it.dateAdded }
                _favoriteSongs.value = updatedSongs.filter { favIds.contains(it.id.toString()) }
            }
        }
    }

    // 6. Playback Control Handlers
    fun playSong(song: Song, customQueue: List<Song> = _allSongs.value) {
        playbackConnectionManager.playSong(song, customQueue)
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            playbackConnectionManager.pause()
        } else {
            playbackConnectionManager.play()
        }
    }

    fun skipToNext() {
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
    private val playbackConnectionManager: PlaybackConnectionManager,
    private val lyricsRepository: com.salmanlaghari.pulsemusicplayerai.data.repository.LyricsRepository,
    private val premiumManager: com.salmanlaghari.pulsemusicplayerai.utils.PremiumManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(musicRepository, playbackConnectionManager, lyricsRepository, premiumManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
