package com.salmanlaghari.pulsemusicplayerai.presentation.youtube

import android.util.Log
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
    private val youTubeRepository: YouTubeRepository,
    private val playbackConnectionManager: PlaybackConnectionManager
) : ViewModel() {

    companion object {
        private const val TAG = "YouTubeVM"
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

    // Debounce search
    private var searchJob: Job? = null

    init {
        loadTrending()
    }

    /**
     * Load trending music from YouTube.
     */
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

    /**
     * Search YouTube for music with debouncing.
     * @param query Search query
     */
    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
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

    /**
     * Clear search results.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    /**
     * Play a YouTube song.
     * First resolves the audio stream URL, then plays through the existing player.
     * @param song The YouTubeSong to play
     * @param queue The queue of songs to play
     */
    fun playSong(song: YouTubeSong, queue: List<YouTubeSong>) {
        viewModelScope.launch {
            try {
                // Track song change for interstitial ad
                AdManager.incrementSongChangeCount()

                // If audio URL not resolved yet, resolve it
                val resolvedSong = if (song.audioUrl.isEmpty()) {
                    youTubeRepository.getAudioStream(song.id) ?: song
                } else {
                    song
                }

                _currentlyPlaying.value = resolvedSong

                // Resolve audio URLs for the queue (first 10 songs for faster start)
                val resolvedQueue = mutableListOf<YouTubeSong>()
                for ((index, queueSong) in queue.withIndex()) {
                    if (index == 0) {
                        resolvedQueue.add(resolvedSong) // First song already resolved
                    } else if (index < 10) {
                        // Resolve first 10 songs in parallel
                        val resolved = if (queueSong.audioUrl.isEmpty()) {
                            try {
                                youTubeRepository.getAudioStream(queueSong.id) ?: queueSong
                            } catch (e: Exception) {
                                queueSong
                            }
                        } else {
                            queueSong
                        }
                        resolvedQueue.add(resolved)
                    } else {
                        resolvedQueue.add(queueSong) // Rest will be resolved when needed
                    }
                }

                // Play through existing PlaybackConnectionManager
                // Convert to Song model for compatibility
                val songAsLocal = resolvedSong.toSong()
                val queueAsLocal = resolvedQueue.map { it.toSong() }
                playbackConnectionManager.playSong(songAsLocal, queueAsLocal)

                Log.d(TAG, "Playing: ${resolvedSong.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play song", e)
            }
        }
    }

    /**
     * Refresh trending content.
     */
    fun refresh() {
        loadTrending()
    }
}

class YouTubeViewModelFactory(
    private val youTubeRepository: YouTubeRepository,
    private val playbackConnectionManager: PlaybackConnectionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YouTubeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YouTubeViewModel(youTubeRepository, playbackConnectionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
