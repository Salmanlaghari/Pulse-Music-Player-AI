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

        // Invidious API instances
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.nadeko.net",
            "https://invidious.fdn.fr",
            "https://vid.puffyan.us",
            "https://yewtu.be",
            "https://inv.tux.pizza",
            "https://invidious.privacyredirect.com",
            "https://invidious.nerdvpn.de"
        )

        // Piped API instances
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

        // Try Invidious search first (music-specific)
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                // Use type=music for music-specific results
                val url = "$instance/api/v1/search?q=$encodedQuery&type=music"
                Log.d(TAG, "Invidious music search: $url")
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    Log.d(TAG, "Found ${songs.size} music results from Invidious")
                    return@withContext songs
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious music search failed: ${e.message}")
            }
        }

        // Try Invidious video search
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video"
                Log.d(TAG, "Invidious video search: $url")
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    Log.d(TAG, "Found ${songs.size} video results from Invidious")
                    return@withContext songs
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious video search failed: ${e.message}")
            }
        }

        // Try Piped search
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/search?q=$encodedQuery&filter=music_songs"
                Log.d(TAG, "Piped music search: $url")
                val response = httpGet(url)
                val json = JSONObject(response)
                val items = json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val songs = parsePipedItems(items)
                    if (songs.isNotEmpty()) {
                        currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                        Log.d(TAG, "Found ${songs.size} results from Piped")
                        return@withContext songs
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped search failed: ${e.message}")
            }
        }

        // Try Piped with videos filter
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/search?q=$encodedQuery&filter=videos"
                Log.d(TAG, "Piped videos search: $url")
                val response = httpGet(url)
                val json = JSONObject(response)
                val items = json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val songs = parsePipedItems(items)
                    if (songs.isNotEmpty()) {
                        currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                        return@withContext songs
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped videos search failed: ${e.message}")
            }
        }

        // Last resort: return empty list (no fake fallback)
        Log.w(TAG, "All search APIs failed for: $query")
        emptyList()
    }

    suspend fun getTrending(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        // Try Invidious music-specific trending
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                // Search for "trending music 2026" to get music results
                val url = "$instance/api/v1/search?q=trending+music+2026&type=music"
                Log.d(TAG, "Invidious music trending: $url")
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    Log.d(TAG, "Found ${songs.size} trending music from Invidious")
                    return@withContext songs.take(50)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious music trending failed: ${e.message}")
            }
        }

        // Try Invidious popular
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val url = "$instance/api/v1/popular"
                Log.d(TAG, "Invidious popular: $url")
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    return@withContext songs.take(50)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious popular failed: ${e.message}")
            }
        }

        // Try Piped trending
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val url = "$instance/trending?region=US"
                Log.d(TAG, "Piped trending: $url")
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
                    return@withContext songs.take(50)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped trending failed: ${e.message}")
            }
        }

        // Fallback trending with diverse music
        getFallbackTrending()
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
                val thumbnails = json.optJSONArray("videoThumbnails")
                var thumbnail = ""
                if (thumbnails != null && thumbnails.length() > 0) {
                    thumbnail = thumbnails.getJSONObject(0).optString("url", "")
                }

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

    private fun parseInvidiousItems(items: JSONArray): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        for (i in 0 until items.length()) {
            try {
                val item = items.getJSONObject(i)
                val type = item.optString("type", "")
                if (type == "video" || type == "stream" || type == "music" || item.has("videoId")) {
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

    private fun getFallbackTrending(): List<YouTubeSong> {
        return listOf(
            // English Hits
            YouTubeSong("kJQP7kiw5Fk", "Luis Fonsi - Despacito ft. Daddy Yankee", "Luis Fonsi", 282, "https://i.ytimg.com/vi/kJQP7kiw5Fk/default.jpg", ""),
            YouTubeSong("JGwWNGJdvx8", "Ed Sheeran - Shape of You", "Ed Sheeran", 263, "https://i.ytimg.com/vi/JGwWNGJdvx8/default.jpg", ""),
            YouTubeSong("60ItHLz5WEA", "Alan Walker - Faded", "Alan Walker", 212, "https://i.ytimg.com/vi/60ItHLz5WEA/default.jpg", ""),
            YouTubeSong("fHI8X4OXluQ", "The Weeknd - Blinding Lights", "The Weeknd", 200, "https://i.ytimg.com/vi/fHI8X4OXluQ/default.jpg", ""),
            YouTubeSong("gBRi6aZnhGw", "Dua Lipa - Levitating", "Dua Lipa", 203, "https://i.ytimg.com/vi/gBRi6aZnhGw/default.jpg", ""),
            // Bollywood
            YouTubeSong("VYMQ_LI_OWM", "Kesariya - Brahmastra | Arijit Singh", "Arijit Singh", 262, "https://i.ytimg.com/vi/VYMQ_LI_OWM/default.jpg", ""),
            YouTubeSong("nfRTA9fAN_s", "Apna Bana Le - Bhediya | Arijit Singh", "Arijit Singh", 234, "https://i.ytimg.com/vi/nfRTA9fAN_s/default.jpg", ""),
            YouTubeSong("2MuPEG3FcwE", "Raatan Lambiyan - Shershaah", "Jubin Nautiyal", 210, "https://i.ytimg.com/vi/2MuPEG3FcwE/default.jpg", ""),
            YouTubeSong("lBVBbRqNCQQ", "Tum Hi Ho - Aashiqui 2 | Arijit Singh", "Arijit Singh", 262, "https://i.ytimg.com/vi/lBVBbRqNCQQ/default.jpg", ""),
            YouTubeSong("IJq0yyWug1k", "Chaiyya Chaiyya - Dil Se", "Sukhwinder Singh", 370, "https://i.ytimg.com/vi/IJq0yyWug1k/default.jpg", ""),
            // Punjabi
            YouTubeSong("nvB5G4jPBxQ", "Sidhu Moose Wala - 295", "Sidhu Moose Wala", 234, "https://i.ytimg.com/vi/nvB5G4jPBxQ/default.jpg", ""),
            YouTubeSong("8wzD9b_fXmM", "AP Dhillon - Excuses", "AP Dhillon", 180, "https://i.ytimg.com/vi/8wzD9b_fXmM/default.jpg", ""),
            // K-Pop
            YouTubeSong("4TWR90KJl8k", "BTS - Dynamite", "BTS", 199, "https://i.ytimg.com/vi/4TWR90KJl8k/default.jpg", ""),
            YouTubeSong("gdZLi9oWNZg", "BLACKPINK - How You Like That", "BLACKPINK", 180, "https://i.ytimg.com/vi/gdZLi9oWNZg/default.jpg", ""),
            // Latin
            YouTubeSong("6CgG8M6HjWU", "Bad Bunny - Titi Me Pregunto", "Bad Bunny", 260, "https://i.ytimg.com/vi/6CgG8M6HjWU/default.jpg", "")
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
