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
        private const val MAX_FORCE_RETRIES = 5 // Try up to 5 songs before giving up
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
     * FORCE PLAY — tries to play the selected song.
     * If audio URL resolution fails, automatically tries next songs in queue.
     * Guarantees music plays or shows clear error.
     */
    fun playSong(song: YouTubeSong, queue: List<YouTubeSong>) {
        viewModelScope.launch {
            _isPlayLoading.value = true
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

                    val audioUrl = if (candidate.audioUrl.isNotEmpty()) {
                        candidate.audioUrl
                    } else {
                        // Resolve audio stream from all APIs
                        try {
                            youTubeRepository.getAudioStream(candidate.id)?.audioUrl
                        } catch (e: Exception) {
                            Log.w(TAG, "Stream resolve failed for ${candidate.title}: ${e.message}")
                            null
                        }
                    }

                    if (!audioUrl.isNullOrEmpty()) {
                        resolvedSong = candidate.copy(audioUrl = audioUrl)
                        break
                    } else {
                        Log.w(TAG, "No audio for: ${candidate.title} (${candidate.id}), trying next...")
                    }
                }

                // If still no resolved song, show error
                if (resolvedSong == null || resolvedSong.audioUrl.isEmpty()) {
                    Toast.makeText(
                        getApplication(),
                        "Could not load any audio. Check your internet and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "All $attempts attempts failed — no playable song found")
                    _isPlayLoading.value = false
                    return@launch
                }

                _currentlyPlaying.value = resolvedSong

                // Resolve audio URLs for rest of queue (parallel first 10)
                val resolvedQueue = mutableListOf<YouTubeSong>()
                resolvedQueue.add(resolvedSong)

                for (index in 1 until queue.size) {
                    if (index < 10) {
                        val queueSong = queue[index]
                        val resolved = if (queueSong.audioUrl.isEmpty()) {
                            try {
                                youTubeRepository.getAudioStream(queueSong.id)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            queueSong
                        }
                        if (resolved != null && resolved.audioUrl.isNotEmpty()) {
                            resolvedQueue.add(resolved)
                        }
                    } else {
                        resolvedQueue.add(queue[index])
                    }
                }

                if (resolvedQueue.isEmpty()) {
                    resolvedQueue.add(resolvedSong)
                }

                // Play!
                val songAsLocal = resolvedSong.toSong()
                val queueAsLocal = resolvedQueue.map { it.toSong() }
                playbackConnectionManager.playSong(songAsLocal, queueAsLocal)

                if (attempts > 1) {
                    Toast.makeText(
                        getApplication(),
                        "Playing: ${resolvedSong.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Log.d(TAG, "✓ Playing: ${resolvedSong.title} (after $attempts attempts)")

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
