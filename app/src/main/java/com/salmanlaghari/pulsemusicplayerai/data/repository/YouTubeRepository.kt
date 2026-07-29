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

class YouTubeRepository {

    companion object {
        private const val TAG = "YouTubeRepo"

        // Invidious API instances (more reliable than Piped)
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.nadeko.net",
            "https://invidious.fdn.fr",
            "https://vid.puffyan.us",
            "https://invidious.snopyta.org",
            "https://yewtu.be",
            "https://inv.tux.pizza",
            "https://invidious.privacyredirect.com"
        )

        // Piped API instances as fallback
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://api.piped.projectsegfault.com"
        )

        private var currentInvidiousIndex = 0
        private var currentPipedIndex = 0
    }

    suspend fun search(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // Try Invidious first (more reliable)
        try {
            val results = searchInvidious(query)
            if (results.isNotEmpty()) return@withContext results
        } catch (e: Exception) {
            Log.w(TAG, "Invidious search failed: ${e.message}")
        }

        // Fallback to Piped
        try {
            val results = searchPiped(query)
            if (results.isNotEmpty()) return@withContext results
        } catch (e: Exception) {
            Log.w(TAG, "Piped search failed: ${e.message}")
        }

        // Fallback: return hardcoded popular songs if APIs fail
        Log.w(TAG, "All APIs failed, returning fallback results")
        return@withContext getFallbackResults(query)
    }

    suspend fun getTrending(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        // Try Invidious popular/trending
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val url = "$instance/api/v1/popular"
                Log.d(TAG, "Trying trending: $url")
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    Log.d(TAG, "Found ${songs.size} trending songs from Invidious")
                    return@withContext songs.take(50)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious trending failed: ${e.message}")
            }
        }

        // Try Piped trending
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val url = "$instance/trending?region=US"
                Log.d(TAG, "Trying Piped trending: $url")
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = mutableListOf<YouTubeSong>()
                for (j in 0 until json.length()) {
                    try {
                        val item = json.getJSONObject(j)
                        val song = parsePipedTrendingItem(item)
                        if (song != null) songs.add(song)
                    } catch (e: Exception) { }
                }
                if (songs.isNotEmpty()) {
                    currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                    Log.d(TAG, "Found ${songs.size} trending songs from Piped")
                    return@withContext songs.take(50)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped trending failed: ${e.message}")
            }
        }

        // Return fallback trending
        Log.w(TAG, "All trending APIs failed, returning fallback")
        return@withContext getFallbackTrending()
    }

    suspend fun getAudioStream(videoId: String): YouTubeSong? = withContext(Dispatchers.IO) {
        // Try Invidious first
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val url = "$instance/api/v1/videos/$videoId"
                val response = httpGet(url)
                val json = JSONObject(response)

                val title = json.optString("title", "Unknown")
                val author = json.optString("author", "Unknown Artist")
                val lengthSeconds = json.optLong("lengthSeconds", 0)
                val thumbnail = json.optString("videoThumbnails", "")

                // Get audio URL from adaptive formats
                val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                if (adaptiveFormats != null) {
                    var bestAudioUrl = ""
                    var bestBitrate = 0
                    for (j in 0 until adaptiveFormats.length()) {
                        val fmt = adaptiveFormats.getJSONObject(j)
                        val type = fmt.optString("type", "")
                        if (type.startsWith("audio/")) {
                            val bitrate = fmt.optInt("bitrate", 0)
                            val audioUrl = fmt.optString("url", "")
                            if (audioUrl.isNotEmpty() && bitrate > bestBitrate) {
                                bestAudioUrl = audioUrl
                                bestBitrate = bitrate
                            }
                        }
                    }
                    if (bestAudioUrl.isNotEmpty()) {
                        return@withContext YouTubeSong(
                            id = videoId, title = title, artist = author,
                            duration = lengthSeconds, thumbnailUrl = thumbnail,
                            audioUrl = bestAudioUrl
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious stream failed: ${e.message}")
            }
        }

        // Try Piped
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val url = "$instance/streams/$videoId"
                val response = httpGet(url)
                val json = JSONObject(response)

                val title = json.optString("title", "Unknown")
                val uploader = json.optString("uploader", json.optString("uploaderName", "Unknown Artist"))
                val duration = json.optLong("duration", 0)
                val thumbnail = json.optString("thumbnailUrl", json.optString("thumbnail", ""))

                val audioStreams = json.optJSONArray("audioStreams")
                if (audioStreams != null && audioStreams.length() > 0) {
                    var bestUrl = ""
                    var bestBitrate = 0
                    for (j in 0 until audioStreams.length()) {
                        val stream = audioStreams.getJSONObject(j)
                        val bitrate = stream.optInt("bitrate", 0)
                        val streamUrl = stream.optString("url", "")
                        if (streamUrl.isNotEmpty() && bitrate > bestBitrate) {
                            bestUrl = streamUrl
                            bestBitrate = bitrate
                        }
                    }
                    if (bestUrl.isNotEmpty()) {
                        return@withContext YouTubeSong(
                            id = videoId, title = title, artist = uploader,
                            duration = duration, thumbnailUrl = thumbnail,
                            audioUrl = bestUrl
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped stream failed: ${e.message}")
            }
        }
        null
    }

    // --- Invidious Search ---
    private fun searchInvidious(query: String): List<YouTubeSong> {
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video"
                Log.d(TAG, "Invidious search: $url")
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    return songs
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious instance failed: ${e.message}")
            }
        }
        return emptyList()
    }

    // --- Piped Search ---
    private fun searchPiped(query: String): List<YouTubeSong> {
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/search?q=$encodedQuery&filter=videos"
                Log.d(TAG, "Piped search: $url")
                val response = httpGet(url)
                val json = JSONObject(response)
                val items = json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val songs = parsePipedItems(items)
                    if (songs.isNotEmpty()) {
                        currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                        return songs
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped instance failed: ${e.message}")
            }
        }
        return emptyList()
    }

    private fun parseInvidiousItems(items: JSONArray): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        for (i in 0 until items.length()) {
            try {
                val item = items.getJSONObject(i)
                val type = item.optString("type", "")
                if (type == "video" || type == "stream" || item.has("videoId")) {
                    val videoId = item.optString("videoId", "")
                    if (videoId.isEmpty()) continue
                    val title = item.optString("title", "Unknown")
                    val author = item.optString("author", "Unknown Artist")
                    val lengthSeconds = item.optLong("lengthSeconds", 0)
                    val thumbnails = item.optJSONArray("videoThumbnails")
                    var thumbnail = ""
                    if (thumbnails != null && thumbnails.length() > 0) {
                        thumbnail = thumbnails.getJSONObject(0).optString("url", "")
                    }
                    if (title.isNotEmpty()) {
                        songs.add(YouTubeSong(id = videoId, title = title, artist = author, duration = lengthSeconds, thumbnailUrl = thumbnail, audioUrl = ""))
                    }
                }
            } catch (e: Exception) { }
        }
        return songs
    }

    private fun parsePipedItems(items: JSONArray): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        for (i in 0 until items.length()) {
            try {
                val item = items.getJSONObject(i)
                val type = item.optString("type", "")
                if (type == "stream" || type == "video" || item.has("url")) {
                    var videoId = item.optString("url", "").removePrefix("/watch?v=").removePrefix("/")
                    if (videoId.isEmpty()) continue
                    val title = item.optString("title", "").ifEmpty { "Unknown" }
                    val uploader = item.optString("uploaderName", item.optString("uploader", "Unknown Artist"))
                    val duration = item.optLong("duration", 0)
                    val thumbnail = item.optString("thumbnail", item.optString("thumbnailUrl", ""))
                    val isLive = item.optBoolean("isLive", false)
                    if (!isLive && title.isNotEmpty()) {
                        songs.add(YouTubeSong(id = videoId, title = title, artist = uploader, duration = duration, thumbnailUrl = thumbnail, audioUrl = ""))
                    }
                }
            } catch (e: Exception) { }
        }
        return songs
    }

    private fun parsePipedTrendingItem(item: JSONObject): YouTubeSong? {
        return try {
            var videoId = item.optString("url", "").removePrefix("/watch?v=").removePrefix("/")
            if (videoId.isEmpty()) return null
            val title = item.optString("title", "").ifEmpty { "Unknown" }
            val uploader = item.optString("uploaderName", item.optString("uploader", "Unknown"))
            val duration = item.optLong("duration", 0)
            val thumbnail = item.optString("thumbnail", item.optString("thumbnailUrl", ""))
            if (item.optBoolean("isLive", false) || title.isEmpty()) return null
            YouTubeSong(id = videoId, title = title, artist = uploader, duration = duration, thumbnailUrl = thumbnail, audioUrl = "")
        } catch (e: Exception) { null }
    }

    // Fallback results when all APIs fail
    private fun getFallbackResults(query: String): List<YouTubeSong> {
        val popularSongs = listOf(
            YouTubeSong("kJQP7kiw5Fk", "Luis Fonsi - Despacito ft. Daddy Yankee", "Luis Fonsi", 282, "https://i.ytimg.com/vi/kJQP7kiw5Fk/default.jpg", ""),
            YouTubeSong("JGwWNGJdvx8", "Ed Sheeran - Shape of You", "Ed Sheeran", 263, "https://i.ytimg.com/vi/JGwWNGJdvx8/default.jpg", ""),
            YouTubeSong("RgKAFKWXdjI", "Wiz Khalifa - See You Again ft. Charlie Puth", "Wiz Khalifa", 237, "https://i.ytimg.com/vi/RgKAFKWXdjI/default.jpg", ""),
            YouTubeSong("fJ9rUzIMcZQ", "Queen - Bohemian Rhapsody", "Queen", 354, "https://i.ytimg.com/vi/fJ9rUzIMcZQ/default.jpg", ""),
            YouTubeSong("60ItHLz5WEA", "Alan Walker - Faded", "Alan Walker", 212, "https://i.ytimg.com/vi/60ItHLz5WEA/default.jpg", ""),
            YouTubeSong("YqeW9_5kURI", "Alan Walker - Darkside", "Alan Walker", 210, "https://i.ytimg.com/vi/YqeW9_5kURI/default.jpg", ""),
            YouTubeSong("pRpeEdMmmQ0", "Shakira - Waka Waka", "Shakira", 212, "https://i.ytimg.com/vi/pRpeEdMmmQ0/default.jpg", ""),
            YouTubeSong("OPf0YbXqDm0", "Mark Ronson - Uptown Funk ft. Bruno Mars", "Mark Ronson", 269, "https://i.ytimg.com/vi/OPf0YbXqDm0/default.jpg", ""),
            YouTubeSong("09R8_2nJtjg", "Maroon 5 - Sugar", "Maroon 5", 235, "https://i.ytimg.com/vi/09R8_2nJtjg/default.jpg", ""),
            YouTubeSong("hT_nvWreIhg", "The Chainsmokers - Closer ft. Halsey", "The Chainsmokers", 244, "https://i.ytimg.com/vi/hT_nvWreIhg/default.jpg", "")
        )
        // Filter by query if possible
        val lowerQuery = query.lowercase()
        val filtered = popularSongs.filter {
            it.title.lowercase().contains(lowerQuery) || it.artist.lowercase().contains(lowerQuery)
        }
        return if (filtered.isNotEmpty()) filtered else popularSongs
    }

    private fun getFallbackTrending(): List<YouTubeSong> {
        return listOf(
            YouTubeSong("kJQP7kiw5Fk", "Luis Fonsi - Despacito ft. Daddy Yankee", "Luis Fonsi", 282, "https://i.ytimg.com/vi/kJQP7kiw5Fk/default.jpg", ""),
            YouTubeSong("JGwWNGJdvx8", "Ed Sheeran - Shape of You", "Ed Sheeran", 263, "https://i.ytimg.com/vi/JGwWNGJdvx8/default.jpg", ""),
            YouTubeSong("RgKAFKWXdjI", "Wiz Khalifa - See You Again ft. Charlie Puth", "Wiz Khalifa", 237, "https://i.ytimg.com/vi/RgKAFKWXdjI/default.jpg", ""),
            YouTubeSong("fJ9rUzIMcZQ", "Queen - Bohemian Rhapsody", "Queen", 354, "https://i.ytimg.com/vi/fJ9rUzIMcZQ/default.jpg", ""),
            YouTubeSong("60ItHLz5WEA", "Alan Walker - Faded", "Alan Walker", 212, "https://i.ytimg.com/vi/60ItHLz5WEA/default.jpg", ""),
            YouTubeSong("YqeW9_5kURI", "Alan Walker - Darkside", "Alan Walker", 210, "https://i.ytimg.com/vi/YqeW9_5kURI/default.jpg", ""),
            YouTubeSong("pRpeEdMmmQ0", "Shakira - Waka Waka", "Shakira", 212, "https://i.ytimg.com/vi/pRpeEdMmmQ0/default.jpg", ""),
            YouTubeSong("OPf0YbXqDm0", "Mark Ronson - Uptown Funk ft. Bruno Mars", "Mark Ronson", 269, "https://i.ytimg.com/vi/OPf0YbXqDm0/default.jpg", ""),
            YouTubeSong("09R8_2nJtjg", "Maroon 5 - Sugar", "Maroon 5", 235, "https://i.ytimg.com/vi/09R8_2nJtjg/default.jpg", ""),
            YouTubeSong("hT_nvWreIhg", "The Chainsmokers - Closer ft. Halsey", "The Chainsmokers", 244, "https://i.ytimg.com/vi/hT_nvWreIhg/default.jpg", ""),
            YouTubeSong("fHI8X4OXluQ", "The Weeknd - Blinding Lights", "The Weeknd", 200, "https://i.ytimg.com/vi/fHI8X4OXluQ/default.jpg", ""),
            YouTubeSong("gBRi6aZnhGw", "Dua Lipa - Levitating", "Dua Lipa", 203, "https://i.ytimg.com/vi/gBRi6aZnhGw/default.jpg", ""),
            YouTubeSong("TmKh7lAwnBI", "Harry Styles - As It Was", "Harry Styles", 167, "https://i.ytimg.com/vi/TmKh7lAwnBI/default.jpg", ""),
            YouTubeSong("QYh6mYIJG2Y", "Miley Cyrus - Flowers", "Miley Cyrus", 200, "https://i.ytimg.com/vi/QYh6mYIJG2Y/default.jpg", ""),
            YouTubeSong("kYfE6KhtSOE", "SZA - Kill Bill", "SZA", 153, "https://i.ytimg.com/vi/kYfE6KhtSOE/default.jpg", "")
        )
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) PulseMusicPlayer/1.0")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.instanceFollowRedirects = true

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
