package com.salmanlaghari.pulsemusicplayerai.data.repository

import android.util.Log
import com.salmanlaghari.pulsemusicplayerai.domain.model.YouTubeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Repository for fetching YouTube music via Piped API (free, no API key).
 * Provides trending music and search functionality.
 */
class YouTubeRepository {

    companion object {
        private const val TAG = "YouTubeRepo"

        // Piped API instances (free, open-source YouTube frontend)
        private val API_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://api.piped.projectsegfault.com"
        )

        private var currentInstanceIndex = 0

        private fun getBaseUrl(): String {
            return API_INSTANCES[currentInstanceIndex % API_INSTANCES.size]
        }

        private fun switchInstance() {
            currentInstanceIndex = (currentInstanceIndex + 1) % API_INSTANCES.size
            Log.d(TAG, "Switched to instance: ${getBaseUrl()}")
        }
    }

    /**
     * Search YouTube for music.
     * @param query Search query
     * @param filter Filter type: "music_songs", "music_videos", "music_albums", "all"
     * @return List of YouTubeSong results
     */
    suspend fun search(
        query: String,
        filter: String = "music_songs"
    ): List<YouTubeSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "${getBaseUrl()}/search?q=$encodedQuery&filter=$filter"

            val response = httpGet(url)
            val json = JSONObject(response)

            val items = json.optJSONArray("items") ?: return@withContext emptyList()
            parseSearchResults(items)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed, trying another instance", e)
            try {
                switchInstance()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "${getBaseUrl()}/search?q=$encodedQuery&filter=$filter"
                val response = httpGet(url)
                val json = JSONObject(response)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()
                parseSearchResults(items)
            } catch (e2: Exception) {
                Log.e(TAG, "All instances failed", e2)
                emptyList()
            }
        }
    }

    /**
     * Get trending music from YouTube Music.
     * @return List of trending YouTubeSong
     */
    suspend fun getTrending(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/trending?region=US"
            val response = httpGet(url)
            val json = JSONArray(response)

            val songs = mutableListOf<YouTubeSong>()
            for (i in 0 until json.length()) {
                try {
                    val item = json.getJSONObject(i)
                    if (item.optString("uploaderName", "").isNotEmpty()) {
                        val song = parseTrendingItem(item)
                        if (song != null) songs.add(song)
                    }
                } catch (e: Exception) {
                    // Skip malformed items
                }
            }
            songs.take(50) // Limit to 50 results
        } catch (e: Exception) {
            Log.e(TAG, "Trending failed, trying another instance", e)
            try {
                switchInstance()
                val url = "${getBaseUrl()}/trending?region=US"
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = mutableListOf<YouTubeSong>()
                for (i in 0 until json.length()) {
                    try {
                        val item = json.getJSONObject(i)
                        if (item.optString("uploaderName", "").isNotEmpty()) {
                            val song = parseTrendingItem(item)
                            if (song != null) songs.add(song)
                        }
                    } catch (e2: Exception) { }
                }
                songs.take(50)
            } catch (e2: Exception) {
                Log.e(TAG, "All instances failed for trending", e2)
                emptyList()
            }
        }
    }

    /**
     * Get audio stream URL for a YouTube video.
     * This is the main method to get playable audio.
     * @param videoId YouTube video ID
     * @return YouTubeSong with audio stream URL, or null if failed
     */
    suspend fun getAudioStream(videoId: String): YouTubeSong? = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/streams/$videoId"
            val response = httpGet(url)
            val json = JSONObject(response)

            val title = json.optString("title", "Unknown")
            val uploader = json.optString("uploader", "Unknown Artist")
            val duration = json.optLong("duration", 0)
            val thumbnail = json.optString("thumbnailUrl", "")

            // Find the best audio stream
            val audioStreams = json.optJSONArray("audioStreams") ?: return@withContext null
            val bestAudio = findBestAudioStream(audioStreams)

            if (bestAudio != null) {
                YouTubeSong(
                    id = videoId,
                    title = title,
                    artist = uploader,
                    duration = duration,
                    thumbnailUrl = thumbnail,
                    audioUrl = bestAudio
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio stream for $videoId", e)
            try {
                switchInstance()
                val url = "${getBaseUrl()}/streams/$videoId"
                val response = httpGet(url)
                val json = JSONObject(response)
                val title = json.optString("title", "Unknown")
                val uploader = json.optString("uploader", "Unknown Artist")
                val duration = json.optLong("duration", 0)
                val thumbnail = json.optString("thumbnailUrl", "")
                val audioStreams = json.optJSONArray("audioStreams") ?: return@withContext null
                val bestAudio = findBestAudioStream(audioStreams)
                if (bestAudio != null) {
                    YouTubeSong(
                        id = videoId,
                        title = title,
                        artist = uploader,
                        duration = duration,
                        thumbnailUrl = thumbnail,
                        audioUrl = bestAudio
                    )
                } else null
            } catch (e2: Exception) {
                Log.e(TAG, "All instances failed for streams", e2)
                null
            }
        }
    }

    /**
     * Search and get songs with audio URLs pre-resolved.
     * @param query Search query
     * @return List of YouTubeSong with playable audio URLs
     */
    suspend fun searchWithAudio(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val searchResults = search(query)
        searchResults.mapNotNull { song ->
            try {
                getAudioStream(song.id) ?: song
            } catch (e: Exception) {
                song // Return without audio URL if resolution fails
            }
        }
    }

    // --- Private Helper Methods ---

    private fun parseSearchResults(items: JSONArray): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        for (i in 0 until items.length()) {
            try {
                val item = items.getJSONObject(i)
                val type = item.optString("type", "")

                // Only process video/streams (skip channels, playlists)
                if (type == "stream") {
                    val videoId = item.optString("url", "")
                        .replace("/watch?v=", "")
                        .replace("/", "")

                    if (videoId.isNotEmpty()) {
                        songs.add(
                            YouTubeSong(
                                id = videoId,
                                title = item.optString("title", "Unknown"),
                                artist = item.optString("uploaderName", "Unknown Artist"),
                                duration = item.optLong("duration", 0),
                                thumbnailUrl = item.optString("thumbnail", ""),
                                audioUrl = "", // Will be resolved when playing
                                isLive = item.optBoolean("isLive", false)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Skip malformed items
            }
        }
        return songs
    }

    private fun parseTrendingItem(item: JSONObject): YouTubeSong? {
        return try {
            val url = item.optString("url", "")
            val videoId = url.replace("/watch?v=", "").replace("/", "")
            if (videoId.isEmpty()) return null

            YouTubeSong(
                id = videoId,
                title = item.optString("title", "Unknown"),
                artist = item.optString("uploaderName", "Unknown Artist"),
                duration = item.optLong("duration", 0),
                thumbnailUrl = item.optString("thumbnail", ""),
                audioUrl = "",
                isLive = item.optBoolean("isLive", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find the best audio stream from Piped API response.
     * Prefers: opus > mp4a > webm, highest bitrate
     */
    private fun findBestAudioStream(audioStreams: JSONArray): String? {
        var bestUrl: String? = null
        var bestBitrate = 0
        var bestCodec = ""

        for (i in 0 until audioStreams.length()) {
            try {
                val stream = audioStreams.getJSONObject(i)
                val bitrate = stream.optInt("bitrate", 0)
                val codec = stream.optString("codec", "").lowercase()
                val streamUrl = stream.optString("url", "")

                if (streamUrl.isEmpty()) continue

                // Prefer opus, then mp4a, then anything
                val codecPriority = when {
                    codec.contains("opus") -> 3
                    codec.contains("mp4a") -> 2
                    codec.contains("webm") -> 1
                    else -> 0
                }

                val currentBestPriority = when {
                    bestCodec.contains("opus") -> 3
                    bestCodec.contains("mp4a") -> 2
                    bestCodec.contains("webm") -> 1
                    else -> 0
                }

                if (codecPriority > currentBestPriority ||
                    (codecPriority == currentBestPriority && bitrate > bestBitrate)
                ) {
                    bestUrl = streamUrl
                    bestBitrate = bitrate
                    bestCodec = codec
                }
            } catch (e: Exception) {
                // Skip malformed streams
            }
        }
        return bestUrl
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "PulseMusicPlayer/1.0")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        return try {
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }
}
