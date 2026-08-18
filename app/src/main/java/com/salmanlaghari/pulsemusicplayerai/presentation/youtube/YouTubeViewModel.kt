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
import com.salmanlaghari.pulsemusicplayerai.domain.model.ChannelVideo
import com.salmanlaghari.pulsemusicplayerai.domain.model.YouTubeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

    // South Asian catalog (Bollywood/Pakistani/South Indian/Northern — 500-1000 songs)
    private val _southAsianSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val southAsianSongs: StateFlow<List<YouTubeSong>> = _southAsianSongs.asStateFlow()

    private val _isSouthAsianLoading = MutableStateFlow(false)
    val isSouthAsianLoading: StateFlow<Boolean> = _isSouthAsianLoading.asStateFlow()

    private val _southAsianProgress = MutableStateFlow(0 to 0) // (completed, total)
    val southAsianProgress: StateFlow<Pair<Int, Int>> = _southAsianProgress.asStateFlow()

    private var southAsianJob: Job? = null
    private var southAsianLoaded = false

    private var youTubeTrendingJob: Job? = null
    private var youTubeTrendingLoaded = false

    // My Channel (owner's YouTube channel) — latest uploads from the RSS feed
    private val _channelVideos = MutableStateFlow<List<ChannelVideo>>(emptyList())
    val channelVideos: StateFlow<List<ChannelVideo>> = _channelVideos.asStateFlow()

    private val _isChannelLoading = MutableStateFlow(false)
    val isChannelLoading: StateFlow<Boolean> = _isChannelLoading.asStateFlow()

    private val _channelError = MutableStateFlow<String?>(null)
    val channelError: StateFlow<String?> = _channelError.asStateFlow()

    private var channelJob: Job? = null
    private var channelLoaded = false

    private val _currentlyPlaying = MutableStateFlow<YouTubeSong?>(null)
    val currentlyPlaying: StateFlow<YouTubeSong?> = _currentlyPlaying.asStateFlow()

    private val _isPlayLoading = MutableStateFlow(false)
    val isPlayLoading: StateFlow<Boolean> = _isPlayLoading.asStateFlow()

    // Descriptive loading message so the UI can show a calm, accurate state
    // instead of always saying "Finding full song from all sources".
    private val _playLoadingMessage = MutableStateFlow("Loading...")
    val playLoadingMessage: StateFlow<String> = _playLoadingMessage.asStateFlow()

    private var searchJob: Job? = null
    private var trendingJob: Job? = null

    init {
        // Load trending songs asynchronously with proper error handling
        loadTrending()
        // Auto-load Desi Hits catalog on startup (it's the default tab)
        loadSouthAsianCatalog()
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

    /**
     * Load the owner's YouTube channel videos from the public RSS feed.
     * Playback is handled by the official embedded YouTube player (see UI),
     * so this only provides the list metadata. Pass [force] = true to bypass
     * the cache and pull the freshest uploads (used by the refresh button).
     */
    fun loadChannelVideos(force: Boolean = false) {
        if (channelLoaded && !force && _channelVideos.value.isNotEmpty()) {
            Log.d(TAG, "My Channel already loaded (${_channelVideos.value.size} videos), skipping")
            return
        }
        channelJob?.cancel()
        channelJob = viewModelScope.launch {
            _isChannelLoading.value = true
            _channelError.value = null
            try {
                kotlinx.coroutines.delay(100)
                val videos = youTubeRepository.getChannelVideos()
                if (videos.isNotEmpty()) {
                    _channelVideos.value = videos
                    channelLoaded = true
                    Log.d(TAG, "Loaded ${videos.size} My Channel videos")
                } else {
                    _channelError.value = "No uploads found on this channel yet."
                    Log.w(TAG, "My Channel returned no videos")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load My Channel videos", e)
                _channelError.value = "Couldn't load the channel. Tap refresh to retry."
            } finally {
                _isChannelLoading.value = false
            }
        }
    }

    /**
     * Load 500-1000 Bollywood/Pakistani/South Asian/Northern songs from JioSaavn.
     * Uses curated search queries to build a large deduplicated catalog.
     * All songs come with full 320kbps stream URLs (confirmed working).
     */
    fun loadSouthAsianCatalog() {
        if (southAsianLoaded && _southAsianSongs.value.isNotEmpty()) {
            Log.d(TAG, "South Asian catalog already loaded (${_southAsianSongs.value.size} songs), skipping")
            return
        }
        southAsianJob?.cancel()
        southAsianJob = viewModelScope.launch {
            _isSouthAsianLoading.value = true
            try {
                kotlinx.coroutines.delay(100)
                val songs = youTubeRepository.loadSouthAsianCatalog { completed, total ->
                    _southAsianProgress.value = completed to total
                }
                _southAsianSongs.value = songs
                southAsianLoaded = true
                Log.d(TAG, "Loaded ${songs.size} South Asian songs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load South Asian catalog", e)
            } finally {
                _isSouthAsianLoading.value = false
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

    /**
     * Search the Desi Hits catalog (JioSaavn-backed, but remapped to dh_ prefix
     * so the source badge shows "Desi Hits" instead of "JioSaavn").
     * This keeps Desi Hits as its own distinct source while using working streams.
     */
    fun searchDesiHits(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = youTubeRepository.searchJioSaavn(query)
                // Remap js_ → dh_ so the badge shows "Desi Hits"
                val desiResults = results.map { song ->
                    val rawId = song.id.removePrefix("js_")
                    song.copy(id = "dh_$rawId")
                }
                _searchResults.value = desiResults
                Log.d(TAG, "Desi Hits found ${desiResults.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "Desi Hits search failed", e)
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

    fun searchSoundCloud(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            try {
                val results = youTubeRepository.searchSoundCloud(query)
                _searchResults.value = results
                Log.d(TAG, "SoundCloud found ${results.size} results for '$query'")
            } catch (e: Exception) {
                Log.e(TAG, "SoundCloud search failed", e)
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

                // Run all source searches concurrently for speed and sync them together.
                // Each source is bounded by an 8s timeout so a single dead/slow source
                // (e.g. Spotify 403, slow Piped/Invidious instances) never blocks the
                // whole aggregated search and makes the app appear hung.
                val appleDeferred = async { runCatching { withTimeoutOrNull(8000) { youTubeRepository.searchAppleMusic(query) } }.getOrDefault(null) ?: emptyList() }
                val saavnDeferred = async { runCatching { withTimeoutOrNull(8000) { youTubeRepository.searchJioSaavn(query) } }.getOrDefault(null) ?: emptyList() }
                val spotifyDeferred = async { runCatching { withTimeoutOrNull(8000) { youTubeRepository.searchSpotify(query) } }.getOrDefault(null) ?: emptyList() }
                val ytmDeferred = async { runCatching { withTimeoutOrNull(8000) { youTubeRepository.searchYouTubeMusic(query) } }.getOrDefault(null) ?: emptyList() }
                val soundcloudDeferred = async { runCatching { withTimeoutOrNull(8000) { youTubeRepository.searchSoundCloud(query) } }.getOrDefault(null) ?: emptyList() }
                val generalDeferred = async { runCatching { withTimeoutOrNull(8000) { youTubeRepository.search(query) } }.getOrDefault(null) ?: emptyList() }

                val apple = appleDeferred.await().take(10)
                val saavn = saavnDeferred.await().take(10)
                val spotify = spotifyDeferred.await().take(10)
                val ytm = ytmDeferred.await().take(10)
                val soundcloud = soundcloudDeferred.await().take(10)
                val general = generalDeferred.await().take(10)

                Log.d(TAG, "searchAllSources '$query' -> apple=${apple.size} saavn=${saavn.size} spotify=${spotify.size} ytm=${ytm.size} soundcloud=${soundcloud.size} general=${general.size}")

                // JioSaavn kept first so full-song Bollywood/Hindi results stay
                // synced at the top, then the other synced platforms follow.
                for (list in listOf(saavn, apple, spotify, ytm, soundcloud, general)) {
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
            // Safety timeout: if playback resolution takes more than 15 seconds, abort
            return withTimeoutOrNull(15_000) {
                playSongInternal(song, queue)
            } ?: run {
                Log.e(TAG, "playSong timed out for: ${song.title}")
                try {
                    Toast.makeText(getApplication(), "Song took too long. Try another.", Toast.LENGTH_SHORT).show()
                } catch (t: Exception) { }
                false
            }
        } finally {
            _isPlayLoading.value = false
        }
    }

    private suspend fun playSongInternal(song: YouTubeSong, queue: List<YouTubeSong>): Boolean {
        AdManager.incrementSongChangeCount()

        Log.d(TAG, "Attempting to play: ${song.title}")

        // FAST PATH: if the selected song already has a valid, playable audio URL
        if (song.hasValidAudio() && !isPreviewOnlySource(song.id)) {
            _playLoadingMessage.value = "Loading..."
            Log.d(TAG, "Fast path: song has valid audio, playing immediately")
            // JioSaavn/Desi Hits URLs expire — refresh before playing.
            // SoundCloud stores a transcoding endpoint that must be resolved
            // into a fresh, directly-playable CDN url at playback time.
            val refreshedSong = when {
                song.id.startsWith("js_") || song.id.startsWith("dh_") -> {
                    _playLoadingMessage.value = "Refreshing stream..."
                    val fresh = youTubeRepository.refreshSongAudio(song)
                    if (fresh.hasValidAudio()) fresh else song
                }
                song.id.startsWith("sc_") -> {
                    _playLoadingMessage.value = "Resolving SoundCloud stream..."
                    val streamUrl = youTubeRepository.refreshSoundCloudUrl(song.audioUrl)
                    if (!streamUrl.isNullOrBlank()) song.copy(audioUrl = streamUrl) else song
                }
                else -> song
            }
            _currentlyPlaying.value = refreshedSong
            playbackConnectionManager.setYouTubeSongsReference(queue)
            val songAsLocal = refreshedSong.toSong() ?: run {
                Log.e(TAG, "toSong() returned null for ${refreshedSong.title}")
                try {
                    Toast.makeText(getApplication(), "Error playing this track. Try another.", Toast.LENGTH_SHORT).show()
                } catch (t: Exception) { }
                return false
            }
            val initialQueue = mutableListOf(songAsLocal)
            playbackConnectionManager.playSong(songAsLocal, initialQueue)
            Log.d(TAG, "✓ Playing (fast path): ${refreshedSong.title}")
            resolveQueueInBackground(refreshedSong, queue, songAsLocal)
            return true
        }

        // SLOW PATH: preview-only source or no valid audio -> resolve full song
        _playLoadingMessage.value = "Finding full song..."
        Log.d(TAG, "Slow path: resolving full song for '${song.title}'")

        // JioSaavn/Desi Hits: fast refresh
        if (song.id.startsWith("js_") || song.id.startsWith("dh_")) {
            val refreshed = youTubeRepository.refreshSongAudio(song)
            if (refreshed.hasValidAudio()) {
                _currentlyPlaying.value = refreshed
                playbackConnectionManager.setYouTubeSongsReference(queue)
                val songAsLocal = refreshed.toSong() ?: return false
                val initialQueue = mutableListOf(songAsLocal)
                playbackConnectionManager.playSong(songAsLocal, initialQueue)
                Log.d(TAG, "✓ Playing (JioSaavn refreshed): ${refreshed.title}")
                resolveQueueInBackground(refreshed, queue, songAsLocal)
                return true
            }
        }

        // SoundCloud: resolve the transcoding endpoint into a playable stream url
        if (song.id.startsWith("sc_")) {
            val streamUrl = youTubeRepository.refreshSoundCloudUrl(song.audioUrl)
            if (!streamUrl.isNullOrBlank()) {
                val refreshed = song.copy(audioUrl = streamUrl)
                _currentlyPlaying.value = refreshed
                playbackConnectionManager.setYouTubeSongsReference(queue)
                val songAsLocal = refreshed.toSong() ?: return false
                val initialQueue = mutableListOf(songAsLocal)
                playbackConnectionManager.playSong(songAsLocal, initialQueue)
                Log.d(TAG, "✓ Playing (SoundCloud resolved): ${refreshed.title}")
                resolveQueueInBackground(refreshed, queue, songAsLocal)
                return true
            }
        }

        var resolvedSong = resolveAudio(song)
        Log.d(TAG, "resolveAudio result: ${if (resolvedSong != null) "success" else "failed"}")

        if (resolvedSong == null) {
            val fallbackQueue = queue.filter { it.id != song.id }.take(3)
            for ((index, fallback) in fallbackQueue.withIndex()) {
                resolvedSong = resolveAudio(fallback)
                if (resolvedSong != null) break
            }
        }

        if (resolvedSong == null) {
            try {
                Toast.makeText(getApplication(), "This video may be unavailable. Try another.", Toast.LENGTH_LONG).show()
            } catch (t: Exception) { }
            Log.e(TAG, "All audio resolution attempts failed")
            return false
        }

        _currentlyPlaying.value = resolvedSong
        playbackConnectionManager.setYouTubeSongsReference(queue)

        val songAsLocal = resolvedSong.toSong() ?: run {
            Log.e(TAG, "toSong() returned null for ${resolvedSong.title}")
            try {
                Toast.makeText(getApplication(), "Error playing this track. Try another.", Toast.LENGTH_SHORT).show()
            } catch (t: Exception) { }
            return false
        }

        val initialQueue = mutableListOf(songAsLocal)
        playbackConnectionManager.playSong(songAsLocal, initialQueue)
        Log.d(TAG, "✓ Playing (slow path): ${resolvedSong.title}")
        resolveQueueInBackground(resolvedSong, queue, songAsLocal)
        return true
    }

    /**
     * Resolve the rest of the playback queue in the background so that skipping
     * to the next song is smooth. This runs after playback has already started,
     * so it never blocks the user from hearing the selected song.
     */
    private fun resolveQueueInBackground(
        selectedSong: YouTubeSong,
        queue: List<YouTubeSong>,
        alreadyResolved: com.salmanlaghari.pulsemusicplayerai.domain.model.Song
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolvedQueue = mutableListOf(alreadyResolved)
                for (i in queue.indices) {
                    val qSong = queue[i]
                    if (qSong.id == selectedSong.id) continue
                    if (resolvedQueue.size >= 6) break
                    try {
                        val resolved = if (qSong.hasValidAudio() && !isPreviewOnlySource(qSong.id)) {
                            // Refresh JioSaavn/Desi Hits URLs in background queue
                            if (qSong.id.startsWith("js_") || qSong.id.startsWith("dh_")) {
                                youTubeRepository.refreshSongAudio(qSong)
                            } else qSong
                        } else {
                            resolveAudio(qSong)
                        }
                        if (resolved != null) {
                            resolved.toSong()?.let { resolvedQueue.add(it) }
                        }
                    } catch (e: Exception) { /* skip */ }
                }
                // Update the playback queue if we resolved more songs
                if (resolvedQueue.size > 1) {
                    playbackConnectionManager.playSong(alreadyResolved, resolvedQueue)
                    Log.d(TAG, "Background queue resolved: ${resolvedQueue.size} songs")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background queue resolution failed: ${e.message}")
            }
        }
    }

    private suspend fun resolveAudio(song: YouTubeSong): YouTubeSong? {
        // If the song is from a preview-only source (Apple Music, Spotify, Deezer),
        // it only has a 30-second preview URL. Resolve the FULL song from JioSaavn /
        // YouTube Music by matching title + artist.
        if (song.hasValidAudio() && !isPreviewOnlySource(song.id)) {
            return song
        }

        // JioSaavn/Desi Hits songs: refresh the URL directly (faster than full search)
        if (song.id.startsWith("js_") || song.id.startsWith("dh_")) {
            Log.d(TAG, "resolveAudio: refreshing JioSaavn URL for '${song.title}'")
            val refreshed = youTubeRepository.refreshSongAudio(song)
            if (refreshed.hasValidAudio()) {
                Log.d(TAG, "✓ resolveAudio: JioSaavn URL refreshed for '${song.title}'")
                return refreshed
            }
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
