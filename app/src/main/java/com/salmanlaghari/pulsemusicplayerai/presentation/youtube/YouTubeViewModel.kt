package com.salmanlaghari.pulsemusicplayerai.presentation.youtube

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salmanlaghari.pulsemusicplayerai.core.service.PlaybackConnectionManager
import com.salmanlaghari.pulsemusicplayerai.data.repository.YouTubeRepository
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager
import com.salmanlaghari.pulsemusicplayerai.domain.model.YouTubeSong
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class YouTubeViewModel(
    private val application: Application,
    private val youTubeRepository: YouTubeRepository,
    private val playbackConnectionManager: PlaybackConnectionManager
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "YouTubeVM"
        private const val MAX_FORCE_RETRIES = 5
    }

    // Search state
    private val _searchResults = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val searchResults: StateFlow<List<YouTubeSong>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Trending state
    private val _trendingSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val trendingSongs: StateFlow<List<YouTubeSong>> = _trendingSongs.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    // Currently playing
    private val _currentlyPlaying = MutableStateFlow<YouTubeSong?>(null)
    val currentlyPlaying: StateFlow<YouTubeSong?> = _currentlyPlaying.asStateFlow()

    // Loading state for play action
    private val _isPlayLoading = MutableStateFlow(false)
    val isPlayLoading: StateFlow<Boolean> = _isPlayLoading.asStateFlow()

    // Whether playback was successful (used to trigger navigation)
    private val _playbackReady = MutableStateFlow(false)
    val playbackReady: StateFlow<Boolean> = _playbackReady.asStateFlow()

    // Debounce search
    private var searchJob: Job? = null

    init {
        loadTrending()
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isTrendingLoading.value = true
            try {
                val trending = youTubeRepository.getTrending()
                _trendingSongs.value = trending
                Log.d(TAG, "Loaded ${trending.size} trending songs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load trending", e)
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = youTubeRepository.search(query)
                _searchResults.value = results
                Log.d(TAG, "Found ${results.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    /**
     * Reset playback ready state (call after navigation)
     */
    fun resetPlaybackReady() {
        _playbackReady.value = false
    }

    /**
     * FORCE PLAY — tries to play the selected song.
     * If audio URL resolution fails, automatically tries next songs in queue.
     * Sets playbackReady = true ONLY when playback actually starts.
     */
    fun playSong(song: YouTubeSong, queue: List<YouTubeSong>) {
        viewModelScope.launch {
            _isPlayLoading.value = true
            _playbackReady.value = false
            try {
                AdManager.incrementSongChangeCount()

                // Build a list of songs to try (selected song first, then rest of queue)
                val songsToTry = mutableListOf(song)
                songsToTry.addAll(queue.filter { it.id != song.id })

                // Try up to MAX_FORCE_RETRIES songs
                var resolvedSong: YouTubeSong? = null
                var attempts = 0

                for (candidate in songsToTry) {
                    if (attempts >= MAX_FORCE_RETRIES) break
                    attempts++

                    try {
                        val audioUrl = if (candidate.hasValidAudio()) {
                            candidate.audioUrl
                        } else {
                            // Resolve audio stream from all APIs
                            youTubeRepository.getAudioStream(candidate.id)?.audioUrl
                        }

                        if (!audioUrl.isNullOrEmpty() && audioUrl.startsWith("http")) {
                            resolvedSong = candidate.copy(audioUrl = audioUrl.trim())
                            break
                        } else {
                            Log.w(TAG, "No audio for: ${candidate.title} (${candidate.id}), trying next...")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Stream resolve failed for ${candidate.title}: ${e.message}")
                    }
                }

                // If still no resolved song, show error and DO NOT navigate
                if (resolvedSong == null || !resolvedSong.hasValidAudio()) {
                    try {
                        Toast.makeText(
                            getApplication(),
                            "Could not load any audio. Check your internet and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (t: Exception) { }
                    Log.e(TAG, "All $attempts attempts failed — no playable song found")
                    _isPlayLoading.value = false
                    return@launch
                }

                _currentlyPlaying.value = resolvedSong

                // Convert to local Song — MUST succeed
                val songAsLocal = resolvedSong.toSong()
                if (songAsLocal == null) {
                    try {
                        Toast.makeText(
                            getApplication(),
                            "Invalid audio source. Try another song.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (t: Exception) { }
                    Log.e(TAG, "toSong() returned null for: ${resolvedSong.title}")
                    _isPlayLoading.value = false
                    return@launch
                }

                // Resolve audio URLs for rest of queue (parallel first 10)
                val resolvedQueue = mutableListOf<YouTubeSong>()
                resolvedQueue.add(resolvedSong)

                for (index in 1 until queue.size) {
                    if (index < 10) {
                        val queueSong = queue[index]
                        try {
                            val resolved = if (queueSong.hasValidAudio()) {
                                queueSong
                            } else {
                                youTubeRepository.getAudioStream(queueSong.id)
                            }
                            if (resolved != null && resolved.hasValidAudio()) {
                                resolvedQueue.add(resolved)
                            }
                        } catch (e: Exception) {
                            // Skip failed queue items
                        }
                    } else {
                        resolvedQueue.add(queue[index])
                    }
                }

                // Convert queue — filter out invalid songs
                val queueAsLocal = resolvedQueue.mapNotNull { it.toSong() }.toMutableList()
                if (queueAsLocal.isEmpty()) {
                    queueAsLocal.add(songAsLocal)
                }

                // Play!
                try {
                    playbackConnectionManager.playSong(songAsLocal, queueAsLocal)
                    _playbackReady.value = true
                    Log.d(TAG, "✓ Playing: ${resolvedSong.title} (after $attempts attempts)")
                } catch (e: Exception) {
                    Log.e(TAG, "PlaybackConnection.playSong failed", e)
                    try {
                        Toast.makeText(
                            getApplication(),
                            "Playback error. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (t: Exception) { }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to play song", e)
                try {
                    Toast.makeText(
                        getApplication(),
                        "Playback error. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (t: Exception) { }
            } finally {
                _isPlayLoading.value = false
            }
        }
    }

    fun refresh() {
        loadTrending()
    }
}

class YouTubeViewModelFactory(
    private val application: Application,
    private val youTubeRepository: YouTubeRepository,
    private val playbackConnectionManager: PlaybackConnectionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YouTubeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YouTubeViewModel(application, youTubeRepository, playbackConnectionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
