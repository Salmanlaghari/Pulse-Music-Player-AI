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
                    youTubeRepository.getAudioStream(song.id)
                } else {
                    song
                }

                // If audio URL resolution failed, show error and don't play
                if (resolvedSong == null || resolvedSong.audioUrl.isEmpty()) {
                    Toast.makeText(
                        getApplication(),
                        "Could not load audio for this song. Try another one.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.w(TAG, "Audio URL empty for: ${song.title} (${song.id})")
                    return@launch
                }

                _currentlyPlaying.value = resolvedSong

                // Resolve audio URLs for the queue (first 10 songs for faster start)
                val resolvedQueue = mutableListOf<YouTubeSong>()
                resolvedQueue.add(resolvedSong) // First song already resolved

                for (index in 1 until queue.size) {
                    if (index < 10) {
                        val queueSong = queue[index]
                        val resolved = if (queueSong.audioUrl.isEmpty()) {
                            try {
                                youTubeRepository.getAudioStream(queueSong.id)
                            } catch (e: Exception) {
                                Log.w(TAG, "Queue song resolve failed: ${queueSong.title}", e)
                                null
                            }
                        } else {
                            queueSong
                        }
                        // Only add songs that have valid audio URLs
                        if (resolved != null && resolved.audioUrl.isNotEmpty()) {
                            resolvedQueue.add(resolved)
                        }
                    } else {
                        resolvedQueue.add(queue[index]) // Rest will be resolved when needed
                    }
                }

                // Ensure we have at least the currently playing song in queue
                if (resolvedQueue.isEmpty()) {
                    resolvedQueue.add(resolvedSong)
                }

                // Play through existing PlaybackConnectionManager
                // Convert to Song model for compatibility
                val songAsLocal = resolvedSong.toSong()
                val queueAsLocal = resolvedQueue.map { it.toSong() }
                playbackConnectionManager.playSong(songAsLocal, queueAsLocal)

                Log.d(TAG, "Playing: ${resolvedSong.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play song", e)
                try {
                    Toast.makeText(
                        getApplication(),
                        "Playback error. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (t: Exception) { /* ignore toast errors */ }
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
