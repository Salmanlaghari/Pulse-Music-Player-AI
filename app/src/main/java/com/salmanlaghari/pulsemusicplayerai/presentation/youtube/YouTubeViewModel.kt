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
import kotlinx.coroutines.async
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

    // YouTube Music trending songs - auto-populated without search
    private val _youTubeTrending = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val youTubeTrending: StateFlow<List<YouTubeSong>> = _youTubeTrending.asStateFlow()

    private val _isYouTubeTrendingLoading = MutableStateFlow(false)
    val isYouTubeTrendingLoading: StateFlow<Boolean> = _isYouTubeTrendingLoading.asStateFlow()

    private var youTubeTrendingJob: Job? = null
    private var youTubeTrendingLoaded = false

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

    /**
     * Load YouTube Music trending songs - auto-populated without search.
     * Called when the YouTube Music tab is opened.
     */
    fun loadYouTubeTrending() {
        if (youTubeTrendingLoaded && _youTubeTrending.value.isNotEmpty()) {
            Log.d(TAG, "YouTube trending already loaded, skipping")
            return
        }
        youTubeTrendingJob?.cancel()
        youTubeTrendingJob = viewModelScope.launch {
            _isYouTubeTrendingLoading.value = true
            try {
                kotlinx.coroutines.delay(100)
                val trending = youTubeRepository.getYouTubeTrending()
                _youTubeTrending.value = trending
                youTubeTrendingLoaded = true
                Log.d(TAG, "Loaded ${trending.size} YouTube trending songs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load YouTube trending", e)
            } finally {
                _isYouTubeTrendingLoading.value = false
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

    fun searchJioSaavn(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = youTubeRepository.searchJioSaavn(query)
                _searchResults.value = results
                Log.d(TAG, "JioSaavn found ${results.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "JioSaavn search failed", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchAppleMusic(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = youTubeRepository.searchAppleMusic(query)
                _searchResults.value = results
                Log.d(TAG, "Apple Music found ${results.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "Apple Music search failed", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchSpotify(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = youTubeRepository.searchSpotify(query)
                _searchResults.value = results
                Log.d(TAG, "Spotify found ${results.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "Spotify search failed", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchYouTubeMusic(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = youTubeRepository.searchYouTubeMusic(query)
                _searchResults.value = results
                Log.d(TAG, "YouTube Music found ${results.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "YouTube Music search failed", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Unified synced search — aggregates results from ALL platforms (JioSaavn,
     * Apple Music, Spotify, YouTube Music, Deezer, Internet Archive) so the
     * player shows a single synced catalog across every source.
     */
    fun searchAllSources(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            try {
                val combined = mutableListOf<YouTubeSong>()
                val seen = mutableSetOf<String>()

                // Run all source searches concurrently for speed and sync them together
                val appleDeferred = async { runCatching { youTubeRepository.searchAppleMusic(query) }.getOrDefault(emptyList()) }
                val saavnDeferred = async { runCatching { youTubeRepository.searchJioSaavn(query) }.getOrDefault(emptyList()) }
                val spotifyDeferred = async { runCatching { youTubeRepository.searchSpotify(query) }.getOrDefault(emptyList()) }
                val ytmDeferred = async { runCatching { youTubeRepository.searchYouTubeMusic(query) }.getOrDefault(emptyList()) }
                val generalDeferred = async { runCatching { youTubeRepository.search(query) }.getOrDefault(emptyList()) }

                val apple = appleDeferred.await().take(10)
                val saavn = saavnDeferred.await().take(10)
                val spotify = spotifyDeferred.await().take(10)
                val ytm = ytmDeferred.await().take(10)
                val general = generalDeferred.await().take(10)

                for (list in listOf(apple, saavn, spotify, ytm, general)) {
                    for (s in list) {
                        if (seen.add(s.id)) combined.add(s)
                    }
                }

                _searchResults.value = combined
                Log.d(TAG, "Synced search aggregated ${combined.size} unique results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "Synced search failed", e)
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
        // If the song is from a preview-only source (Apple Music, Spotify, Deezer),
        // it only has a 30-second preview URL. Resolve the FULL song from JioSaavn /
        // YouTube Music by matching title + artist.
        if (song.hasValidAudio() && !isPreviewOnlySource(song.id)) {
            return song
        }

        // For preview-only sources (or empty audio), try to find the full song
        if (isPreviewOnlySource(song.id) || !song.hasValidAudio()) {
            Log.d(TAG, "resolveAudio: resolving full song for '${song.title}' (source: ${song.sourceType})")
            val fullSong = try {
                youTubeRepository.resolveFullSong(
                    title = song.title,
                    artist = song.artist,
                    originalThumbnail = song.thumbnailUrl
                )
            } catch (e: Exception) {
                Log.w(TAG, "resolveFullSong failed for ${song.title}: ${e.message}")
                null
            }

            if (fullSong != null && fullSong.hasValidAudio()) {
                // Keep the original song's display info but use the full stream URL
                return YouTubeSong(
                    id = song.id, // keep original ID for UI tracking
                    title = song.title,
                    artist = song.artist,
                    duration = if (fullSong.duration > 0) fullSong.duration else song.duration,
                    thumbnailUrl = song.thumbnailUrl.ifBlank { fullSong.thumbnailUrl },
                    audioUrl = fullSong.audioUrl,
                    isLive = false
                )
            }

            // If full song resolution failed, fall back to preview if available
            if (song.hasValidAudio()) {
                Log.w(TAG, "resolveAudio: falling back to 30s preview for '${song.title}'")
                return song
            }
        }

        // Try to resolve from API (for YouTube/Internet Archive IDs)
        return try {
            youTubeRepository.getAudioStream(song.id)
        } catch (e: Exception) {
            Log.w(TAG, "Audio resolve failed for ${song.title}: ${e.message}")
            null
        }
    }

    /**
     * Check if a song ID is from a source that only provides 30-second previews.
     * These sources need full-song resolution from JioSaavn/YouTube Music.
     */
    private fun isPreviewOnlySource(id: String): Boolean {
        return id.startsWith("am_") ||  // Apple Music (iTunes preview)
               id.startsWith("sp_") ||  // Spotify (30s preview)
               id.startsWith("dz_")     // Deezer (30s preview)
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
