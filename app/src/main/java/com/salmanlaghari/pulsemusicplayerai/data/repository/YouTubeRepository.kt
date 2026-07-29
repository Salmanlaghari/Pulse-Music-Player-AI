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
 */
class YouTubeRepository {

    companion object {
        private const val TAG = "YouTubeRepo"

        private val API_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://api.piped.projectsegfault.com",
            "https://pipedapi.in.projectsegfault.com"
        )

        private var currentInstanceIndex = 0

        private fun getBaseUrl(): String = API_INSTANCES[currentInstanceIndex % API_INSTANCES.size]

        private fun switchInstance() {
            currentInstanceIndex = (currentInstanceIndex + 1) % API_INSTANCES.size
            Log.d(TAG, "Switched to: ${getBaseUrl()}")
        }
    }

    /**
     * Search YouTube for music/videos.
     * Uses "videos" filter which is widely supported across all Piped instances.
     */
    suspend fun search(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // Try "videos" filter first (most reliable), then fallback to no filter
        val filters = listOf("videos", "music_songs", "")
        var lastError: Exception? = null

        for (attempt in 0 until API_INSTANCES.size) {
            for (filter in filters) {
                try {
                    val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                    val url = if (filter.isNotEmpty()) {
                        "${getBaseUrl()}/search?q=$encodedQuery&filter=$filter"
                    } else {
                        "${getBaseUrl()}/search?q=$encodedQuery"
                    }

                    Log.d(TAG, "Searching: $url")
                    val response = httpGet(url)
                    val json = JSONObject(response)
                    val items = json.optJSONArray("items")

                    if (items != null && items.length() > 0) {
                        val results = parseSearchResults(items)
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Found ${results.size} results with filter='$filter'")
                            return@withContext results
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Search failed with filter='$filter': ${e.message}")
                    lastError = e
                }
            }
            switchInstance()
        }

        Log.e(TAG, "All search attempts failed", lastError)
        emptyList()
    }

    /**
     * Get trending videos from YouTube.
     */
    suspend fun getTrending(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        for (attempt in 0 until API_INSTANCES.size) {
            try {
                val url = "${getBaseUrl()}/trending?region=US"
                Log.d(TAG, "Fetching trending: $url")
                val response = httpGet(url)
                val json = JSONArray(response)

                val songs = mutableListOf<YouTubeSong>()
                for (i in 0 until json.length()) {
                    try {
                        val item = json.getJSONObject(i)
                        val song = parseTrendingItem(item)
                        if (song != null) songs.add(song)
                    } catch (e: Exception) { }
                }
                if (songs.isNotEmpty()) {
                    Log.d(TAG, "Found ${songs.size} trending songs")
                    return@withContext songs.take(50)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Trending failed: ${e.message}")
                switchInstance()
            }
        }
        emptyList()
    }

    /**
     * Get audio stream URL for a YouTube video.
     */
    suspend fun getAudioStream(videoId: String): YouTubeSong? = withContext(Dispatchers.IO) {
        for (attempt in 0 until API_INSTANCES.size) {
            try {
                val url = "${getBaseUrl()}/streams/$videoId"
                val response = httpGet(url)
                val json = JSONObject(response)

                val title = json.optString("title", "Unknown")
                val uploader = json.optString("uploader", json.optString("uploaderName", "Unknown Artist"))
                val duration = json.optLong("duration", 0)
                val thumbnail = json.optString("thumbnailUrl", json.optString("thumbnail", ""))

                val audioStreams = json.optJSONArray("audioStreams")
                if (audioStreams != null && audioStreams.length() > 0) {
                    val bestAudio = findBestAudioStream(audioStreams)
                    if (bestAudio != null) {
                        return@withContext YouTubeSong(
                            id = videoId,
                            title = title,
                            artist = uploader,
                            duration = duration,
                            thumbnailUrl = thumbnail,
                            audioUrl = bestAudio
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Stream failed for $videoId: ${e.message}")
                switchInstance()
            }
        }
        null
    }

    // --- Private Helpers ---

    private fun parseSearchResults(items: JSONArray): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        for (i in 0 until items.length()) {
            try {
                val item = items.getJSONObject(i)
                val type = item.optString("type", "")

                // Accept stream, video, or any item with a url
                if (type == "stream" || type == "video" || item.has("url")) {
                    var videoId = item.optString("url", "")
                    // Clean up video ID
                    if (videoId.startsWith("/watch?v=")) {
                        videoId = videoId.removePrefix("/watch?v=")
                    }
                    if (videoId.startsWith("/")) {
                        videoId = videoId.removePrefix("/")
                    }
                    if (videoId.isEmpty()) continue

                    val title = item.optString("title", "").ifEmpty { item.optString("name", "Unknown") }
                    val uploader = item.optString("uploaderName", item.optString("uploader", item.optString("author", "Unknown Artist")))
                    val duration = item.optLong("duration", 0)
                    val thumbnail = item.optString("thumbnail", item.optString("thumbnailUrl", ""))
                    val isLive = item.optBoolean("isLive", false)

                    if (!isLive && title.isNotEmpty()) {
                        songs.add(
                            YouTubeSong(
                                id = videoId,
                                title = title,
                                artist = uploader,
                                duration = duration,
                                thumbnailUrl = thumbnail,
                                audioUrl = ""
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
            var url = item.optString("url", "")
            var videoId = url.removePrefix("/watch?v=").removePrefix("/")
            if (videoId.isEmpty()) return null

            val title = item.optString("title", "").ifEmpty { item.optString("name", "Unknown") }
            val uploader = item.optString("uploaderName", item.optString("uploader", item.optString("author", "Unknown")))
            val duration = item.optLong("duration", 0)
            val thumbnail = item.optString("thumbnail", item.optString("thumbnailUrl", ""))
            val isLive = item.optBoolean("isLive", false)

            if (isLive || title.isEmpty()) return null

            YouTubeSong(
                id = videoId,
                title = title,
                artist = uploader,
                duration = duration,
                thumbnailUrl = thumbnail,
                audioUrl = ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun findBestAudioStream(audioStreams: JSONArray): String? {
        var bestUrl: String? = null
        var bestBitrate = 0

        for (i in 0 until audioStreams.length()) {
            try {
                val stream = audioStreams.getJSONObject(i)
                val bitrate = stream.optInt("bitrate", 0)
                val streamUrl = stream.optString("url", "")
                if (streamUrl.isEmpty()) continue
                if (bitrate > bestBitrate) {
                    bestUrl = streamUrl
                    bestBitrate = bitrate
                }
            } catch (e: Exception) { }
        }
        return bestUrl
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "PulseMusicPlayer/1.0")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

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
