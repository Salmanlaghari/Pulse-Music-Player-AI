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

    private val _searchResults = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val searchResults: StateFlow<List<YouTubeSong>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val trendingSongs: StateFlow<List<YouTubeSong>> = _trendingSongs.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    private val _currentlyPlaying = MutableStateFlow<YouTubeSong?>(null)
    val currentlyPlaying: StateFlow<YouTubeSong?> = _currentlyPlaying.asStateFlow()

    private val _isPlayLoading = MutableStateFlow(false)
    val isPlayLoading: StateFlow<Boolean> = _isPlayLoading.asStateFlow()

    private var searchJob: Job? = null
    private var trendingJob: Job? = null

    init {
        // Load trending songs asynchronously with proper error handling
        loadTrending()
    }

    fun loadTrending() {
        // Cancel any existing loading job to prevent duplicate requests
        trendingJob?.cancel()
        trendingJob = viewModelScope.launch {
            _isTrendingLoading.value = true
            try {
                // Add a small delay to prevent rapid consecutive requests
                kotlinx.coroutines.delay(100)
                val trending = youTubeRepository.getTrending()
                _trendingSongs.value = trending
                Log.d(TAG, "Loaded ${trending.size} trending songs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load trending", e)
                // Keep the existing songs if load fails
                if (_trendingSongs.value.isEmpty()) {
                    // Load fallback songs from local database
                    loadFallbackSongs()
                }
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }

    private fun loadFallbackSongs() {
        // This will be called from the repository's getTrending method
        // which already includes fallback songs from FREE_MUSIC_DATABASE
        Log.d(TAG, "Attempting to load fallback songs")
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
     * Play a YouTube song. Resolves audio URL, then plays via PlaybackConnectionManager.
     * Returns true if playback started successfully.
     */
    suspend fun playSong(song: YouTubeSong, queue: List<YouTubeSong>): Boolean {
        _isPlayLoading.value = true
        try {
            AdManager.incrementSongChangeCount()
            
            Log.d(TAG, "Attempting to play: ${song.title}")

            // Try to resolve audio for selected song first
            var resolvedSong = resolveAudio(song)
            Log.d(TAG, "resolveAudio result: ${if (resolvedSong != null) "success" else "failed"}")

            // If selected song fails, try next songs in queue
            if (resolvedSong == null) {
                val fallbackQueue = queue.filter { it.id != song.id }.take(10)
                Log.d(TAG, "Trying fallback queue: ${fallbackQueue.size} songs")
                for ((index, fallback) in fallbackQueue.withIndex()) {
                    Log.d(TAG, "Trying fallback $index: ${fallback.title}")
                    resolvedSong = resolveAudio(fallback)
                    if (resolvedSong != null) {
                        Log.d(TAG, "Fallback $index worked!")
                        break
                    }
                }
            }

            if (resolvedSong == null) {
                try {
                    Toast.makeText(
                        getApplication(),
                        "This video may be unavailable. Try another song.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (t: Exception) { }
                Log.e(TAG, "All audio resolution attempts failed")
                return false
            }

            _currentlyPlaying.value = resolvedSong

            // Store YouTube songs reference for playback state tracking
            playbackConnectionManager.setYouTubeSongsReference(queue)

            // Convert to local Song
            val songAsLocal = resolvedSong.toSong() ?: run {
                Log.e(TAG, "toSong() returned null for ${resolvedSong.title}")
                try {
                    Toast.makeText(
                        getApplication(),
                        "Error playing this track. Try another.",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (t: Exception) { }
                return false
            }

            // Resolve queue (best effort, first 5)
            val resolvedQueue = mutableListOf(songAsLocal)
            for (i in 1 until minOf(queue.size, 6)) {
                try {
                    val qSong = queue[i]
                    val resolved = if (qSong.hasValidAudio()) qSong else resolveAudio(qSong)
                    if (resolved != null) {
                        resolved.toSong()?.let { resolvedQueue.add(it) }
                    }
                } catch (e: Exception) { /* skip */ }
            }

            // Play via connection manager
            playbackConnectionManager.playSong(songAsLocal, resolvedQueue)
            Log.d(TAG, "✓ Playing: ${resolvedSong.title}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "playSong failed", e)
            try {
                Toast.makeText(
                    getApplication(),
                    "Playback error. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (t: Exception) { }
            return false
        } finally {
            _isPlayLoading.value = false
        }
    }

    private suspend fun resolveAudio(song: YouTubeSong): YouTubeSong? {
        // If already has valid audio URL, return as-is
        if (song.hasValidAudio()) return song

        // Try to resolve from API
        return try {
            youTubeRepository.getAudioStream(song.id)
        } catch (e: Exception) {
            Log.w(TAG, "Audio resolve failed for ${song.title}: ${e.message}")
            null
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
