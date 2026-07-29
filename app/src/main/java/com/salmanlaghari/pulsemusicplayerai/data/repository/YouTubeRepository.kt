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

        // ═══ MASSIVE Invidious instances list ═══
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.nadeko.net",
            "https://invidious.fdn.fr",
            "https://vid.puffyan.us",
            "https://yewtu.be",
            "https://inv.tux.pizza",
            "https://invidious.privacyredirect.com",
            "https://invidious.nerdvpn.de",
            "https://inv.in.projectsegfault.com",
            "https://invidious.lunar.icu",
            "https://iv.ggtyler.dev",
            "https://invidious.protokolla.fi",
            "https://inv.us.projectsegfault.com",
            "https://invidious.io.lol",
            "https://yt.artemislena.eu",
            "https://invidious.futo.org",
            "https://invidious.perennialte.ch",
            "https://invidious.drgns.space"
        )

        // ═══ MASSIVE Piped instances list ═══
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://api.piped.projectsegfault.com",
            "https://pipedapi.darkness.services",
            "https://pipedapi.r4fo.com",
            "https://pipedapi.hostux.net",
            "https://piped-api.lunar.icu",
            "https://pipedapi.in.projectsegfault.com",
            "https://watchapi.whatever.social",
            "https://api.piped.yt"
        )

        // ═══ cobalt.tools API instances (open-source YouTube audio extractor) ═══
        private val COBALT_INSTANCES = listOf(
            "https://cobalt-api.hyper.lol",
            "https://co.eepy.today",
            "https://cobalt.canine.tools",
            "https://cobalt.api.timelessnesses.me"
        )

        private var currentInvidiousIndex = 0
        private var currentPipedIndex = 0
        private var currentCobaltIndex = 0
    }

    // ═══════════════════════════════════════════════
    // SEARCH — tries all APIs aggressively
    // ═══════════════════════════════════════════════
    suspend fun search(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // 1. Try Invidious music search
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/api/v1/search?q=$encodedQuery&type=music"
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    Log.d(TAG, "Invidious music: ${songs.size} results")
                    return@withContext songs
                }
            } catch (e: Exception) { Log.w(TAG, "Invidious music search fail: ${e.message}") }
        }

        // 2. Try Invidious video search
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video"
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    Log.d(TAG, "Invidious video: ${songs.size} results")
                    return@withContext songs
                }
            } catch (e: Exception) { Log.w(TAG, "Invidious video search fail: ${e.message}") }
        }

        // 3. Try Piped music search
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/search?q=$encodedQuery&filter=music_songs"
                val response = httpGet(url)
                val json = JSONObject(response)
                val items = json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val songs = parsePipedItems(items)
                    if (songs.isNotEmpty()) {
                        currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                        Log.d(TAG, "Piped music: ${songs.size} results")
                        return@withContext songs
                    }
                }
            } catch (e: Exception) { Log.w(TAG, "Piped music search fail: ${e.message}") }
        }

        // 4. Try Piped videos search
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "$instance/search?q=$encodedQuery&filter=videos"
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
            } catch (e: Exception) { Log.w(TAG, "Piped video search fail: ${e.message}") }
        }

        Log.w(TAG, "All search APIs failed for: $query")
        emptyList()
    }

    // ═══════════════════════════════════════════════
    // TRENDING — Bollywood + Hollywood + Global
    // ═══════════════════════════════════════════════
    suspend fun getTrending(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val allSongs = mutableListOf<YouTubeSong>()

        // 1. Try Invidious popular
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val url = "$instance/api/v1/popular"
                val response = httpGet(url)
                val json = JSONArray(response)
                val songs = parseInvidiousItems(json)
                if (songs.isNotEmpty()) {
                    currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                    allSongs.addAll(songs)
                    break
                }
            } catch (e: Exception) { Log.w(TAG, "Invidious popular fail: ${e.message}") }
        }

        // 2. Try Piped trending
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val url = "$instance/trending?region=US"
                val response = httpGet(url)
                val json = JSONArray(response)
                for (j in 0 until json.length()) {
                    try {
                        val item = json.getJSONObject(j)
                        val song = parsePipedTrendingItem(item)
                        if (song != null) allSongs.add(song)
                    } catch (e: Exception) { }
                }
                if (allSongs.isNotEmpty()) {
                    currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                    break
                }
            } catch (e: Exception) { Log.w(TAG, "Piped trending fail: ${e.message}") }
        }

        // 3. Search Bollywood trending
        try {
            val bollywood = search("Bollywood latest songs 2025 2026 Arijit Singh")
            allSongs.addAll(bollywood)
        } catch (e: Exception) { Log.w(TAG, "Bollywood search fail: ${e.message}") }

        // 4. Search Hollywood/English trending
        try {
            val hollywood = search("top english pop songs 2025 2026 hits")
            allSongs.addAll(hollywood)
        } catch (e: Exception) { Log.w(TAG, "Hollywood search fail: ${e.message}") }

        // 5. Search Punjabi hits
        try {
            val punjabi = search("Punjabi hit songs 2025 AP Dhillon Sidhu Moosewala")
            allSongs.addAll(punjabi)
        } catch (e: Exception) { Log.w(TAG, "Punjabi search fail: ${e.message}") }

        if (allSongs.isNotEmpty()) {
            // Deduplicate by video ID
            val unique = allSongs.distinctBy { it.id }
            Log.d(TAG, "Trending total: ${unique.size} songs (Bollywood+Hollywood+Punjabi)")
            return@withContext unique.take(80)
        }

        // 6. Last resort: hardcoded fallback
        Log.d(TAG, "Using hardcoded fallback trending")
        getFallbackTrending()
    }

    // ═══════════════════════════════════════════════
    // AUDIO STREAM — tries Invidious → Piped → cobalt → InnerTube
    // ═══════════════════════════════════════════════
    suspend fun getAudioStream(videoId: String): YouTubeSong? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Resolving audio for: $videoId")

        // 1. Try ALL Invidious instances
        for (i in INVIDIOUS_INSTANCES.indices) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val url = "$instance/api/v1/videos/$videoId"
                val response = httpGet(url, timeout = 15000)
                val json = JSONObject(response)

                val title = json.optString("title", "Unknown")
                val author = json.optString("author", "Unknown Artist")
                val lengthSeconds = json.optLong("lengthSeconds", 0)
                val thumbnails = json.optJSONArray("videoThumbnails")
                var thumbnail = ""
                if (thumbnails != null && thumbnails.length() > 0) {
                    thumbnail = thumbnails.getJSONObject(0).optString("url", "")
                }

                // Try adaptiveFormats (audio-only streams)
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
                        currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                        Log.d(TAG, "✓ Invidious stream OK: $title (${bestBitrate}bps)")
                        return@withContext YouTubeSong(
                            id = videoId, title = title, artist = author,
                            duration = lengthSeconds, thumbnailUrl = thumbnail,
                            audioUrl = bestAudioUrl
                        )
                    }
                }

                // Try formatStreams (combined audio+video as fallback)
                val formatStreams = json.optJSONArray("formatStreams")
                if (formatStreams != null) {
                    for (j in 0 until formatStreams.length()) {
                        val fmt = formatStreams.getJSONObject(j)
                        val streamUrl = fmt.optString("url", "")
                        if (streamUrl.isNotEmpty()) {
                            currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                            Log.d(TAG, "✓ Invidious formatStream OK: $title")
                            return@withContext YouTubeSong(
                                id = videoId, title = title, artist = author,
                                duration = lengthSeconds, thumbnailUrl = thumbnail,
                                audioUrl = streamUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious stream fail [$i]: ${e.message}")
            }
        }

        // 2. Try ALL Piped instances
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val url = "$instance/streams/$videoId"
                val response = httpGet(url, timeout = 15000)
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
                        currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                        Log.d(TAG, "✓ Piped stream OK: $title (${bestBitrate}bps)")
                        return@withContext YouTubeSong(
                            id = videoId, title = title, artist = uploader,
                            duration = duration, thumbnailUrl = thumbnail,
                            audioUrl = bestUrl
                        )
                    }
                }

                // Try videoStreams as fallback (has audio too)
                val videoStreams = json.optJSONArray("videoStreams")
                if (videoStreams != null && videoStreams.length() > 0) {
                    for (j in 0 until videoStreams.length()) {
                        val stream = videoStreams.getJSONObject(j)
                        val mimeType = stream.optString("mimeType", "")
                        if (mimeType.contains("audio")) {
                            val streamUrl = stream.optString("url", "")
                            if (streamUrl.isNotEmpty()) {
                                currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                                Log.d(TAG, "✓ Piped videoStream(audio) OK: $title")
                                return@withContext YouTubeSong(
                                    id = videoId, title = title, artist = uploader,
                                    duration = duration, thumbnailUrl = thumbnail,
                                    audioUrl = streamUrl
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped stream fail [$i]: ${e.message}")
            }
        }

        // 3. Try cobalt.tools API (open-source YouTube audio extractor)
        for (i in COBALT_INSTANCES.indices) {
            try {
                val instance = COBALT_INSTANCES[(currentCobaltIndex + i) % COBALT_INSTANCES.size]
                val result = tryCobaltAudio(instance, videoId)
                if (result != null) {
                    currentCobaltIndex = (currentCobaltIndex + i) % COBALT_INSTANCES.size
                    Log.d(TAG, "✓ cobalt stream OK: ${result.title}")
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.w(TAG, "cobalt fail [$i]: ${e.message}")
            }
        }

        // 4. Try direct InnerTube API (YouTube's internal API)
        try {
            val result = tryInnerTubeAudio(videoId)
            if (result != null) {
                Log.d(TAG, "✓ InnerTube stream OK: ${result.title}")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube fail: ${e.message}")
        }

        Log.e(TAG, "✗ ALL APIs failed for: $videoId")
        null
    }

    // ═══════════════════════════════════════════════
    // cobalt.tools API — open-source audio extractor
    // ═══════════════════════════════════════════════
    private fun tryCobaltAudio(instance: String, videoId: String): YouTubeSong? {
        try {
            val urlObj = URL("$instance/")
            val conn = urlObj.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "PulseMusicPlayer/1.0")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            val body = JSONObject().apply {
                put("url", "https://www.youtube.com/watch?v=$videoId")
                put("isAudioOnly", true)
                put("aFormat", "mp3")
            }.toString()

            conn.outputStream.bufferedWriter().use { it.write(body) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val status = json.optString("status", "")
                val audioUrl = json.optString("url", json.optString("audio", ""))

                if ((status == "stream" || status == "tunnel" || status == "redirect" || audioUrl.isNotEmpty()) && audioUrl.isNotEmpty()) {
                    return YouTubeSong(
                        id = videoId,
                        title = json.optString("title", "Unknown"),
                        artist = json.optString("author", "Unknown Artist"),
                        duration = json.optLong("duration", 0) / 1000,
                        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/default.jpg",
                        audioUrl = audioUrl
                    )
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "cobalt error: ${e.message}")
        }
        return null
    }

    // ═══════════════════════════════════════════════
    // InnerTube API — YouTube's internal API (no proxy needed)
    // ═══════════════════════════════════════════════
    private fun tryInnerTubeAudio(videoId: String): YouTubeSong? {
        try {
            val url = URL("https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "com.google.android.youtube/19.02.39 (Linux; U; Android 14) gzip")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true

            val body = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID")
                        put("clientVersion", "19.02.39")
                        put("hl", "en")
                        put("gl", "US")
                        put("androidSdkVersion", 34)
                    })
                })
            }.toString()

            conn.outputStream.bufferedWriter().use { it.write(body) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val title = json.optJSONObject("videoDetails")?.optString("title", "Unknown") ?: "Unknown"
                val author = json.optJSONObject("videoDetails")?.optString("author", "Unknown Artist") ?: "Unknown Artist"
                val lengthSeconds = json.optJSONObject("videoDetails")?.optLong("lengthSeconds", 0) ?: 0
                val thumbnail = "https://i.ytimg.com/vi/$videoId/default.jpg"

                // Extract audio URL from streamingData
                val streamingData = json.optJSONObject("streamingData")
                if (streamingData != null) {
                    // Try adaptiveFormats first
                    val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                    if (adaptiveFormats != null) {
                        var bestUrl = ""
                        var bestBitrate = 0
                        for (i in 0 until adaptiveFormats.length()) {
                            val fmt = adaptiveFormats.getJSONObject(i)
                            val mimeType = fmt.optString("mimeType", "")
                            if (mimeType.startsWith("audio/")) {
                                val bitrate = fmt.optInt("bitrate", 0)
                                val streamUrl = fmt.optString("url", "")
                                if (streamUrl.isNotEmpty() && bitrate > bestBitrate) {
                                    bestUrl = streamUrl
                                    bestBitrate = bitrate
                                }
                            }
                        }
                        if (bestUrl.isNotEmpty()) {
                            Log.d(TAG, "InnerTube adaptiveFormat: ${bestBitrate}bps")
                            return YouTubeSong(videoId, title, author, lengthSeconds.toLong(), thumbnail, bestUrl)
                        }
                    }

                    // Try formats (combined)
                    val formats = streamingData.optJSONArray("formats")
                    if (formats != null && formats.length() > 0) {
                        val fmt = formats.getJSONObject(0)
                        val streamUrl = fmt.optString("url", "")
                        if (streamUrl.isNotEmpty()) {
                            return YouTubeSong(videoId, title, author, lengthSeconds.toLong(), thumbnail, streamUrl)
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube error: ${e.message}")
        }
        return null
    }

    // ═══════════════════════════════════════════════
    // PARSERS
    // ═══════════════════════════════════════════════
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
                    if (thumbnail.isEmpty()) thumbnail = "https://i.ytimg.com/vi/$videoId/default.jpg"
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
                    var thumbnail = item.optString("thumbnail", item.optString("thumbnailUrl", ""))
                    if (thumbnail.isEmpty()) thumbnail = "https://i.ytimg.com/vi/$videoId/default.jpg"
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
            var thumbnail = item.optString("thumbnail", item.optString("thumbnailUrl", ""))
            if (thumbnail.isEmpty()) thumbnail = "https://i.ytimg.com/vi/$videoId/default.jpg"
            if (item.optBoolean("isLive", false) || title.isEmpty()) return null
            YouTubeSong(id = videoId, title = title, artist = uploader, duration = duration, thumbnailUrl = thumbnail, audioUrl = "")
        } catch (e: Exception) { null }
    }

    // ═══════════════════════════════════════════════
    // FALLBACK — hardcoded popular songs (Bollywood + Hollywood + Punjabi)
    // ═══════════════════════════════════════════════
    private fun getFallbackTrending(): List<YouTubeSong> {
        return listOf(
            // ── Bollywood ──
            YouTubeSong("VYMQ_LI_OWM", "Kesariya - Brahmastra | Arijit Singh", "Arijit Singh", 262, "https://i.ytimg.com/vi/VYMQ_LI_OWM/default.jpg", ""),
            YouTubeSong("nfRTA9fAN_s", "Apna Bana Le - Bhediya | Arijit Singh", "Arijit Singh", 234, "https://i.ytimg.com/vi/nfRTA9fAN_s/default.jpg", ""),
            YouTubeSong("2MuPEG3FcwE", "Raatan Lambiyan - Shershaah", "Jubin Nautiyal", 210, "https://i.ytimg.com/vi/2MuPEG3FcwE/default.jpg", ""),
            YouTubeSong("lBVBbRqNCQQ", "Tum Hi Ho - Aashiqui 2 | Arijit Singh", "Arijit Singh", 262, "https://i.ytimg.com/vi/lBVBbRqNCQQ/default.jpg", ""),
            YouTubeSong("IJq0yyWug1k", "Chaiyya Chaiyya - Dil Se", "Sukhwinder Singh", 370, "https://i.ytimg.com/vi/IJq0yyWug1k/default.jpg", ""),
            YouTubeSong("sH3C6G_PpQQ", "Shayad - Love Aaj Kal | Arijit Singh", "Arijit Singh", 238, "https://i.ytimg.com/vi/sH3C6G_PpQQ/default.jpg", ""),
            YouTubeSong("nCg0Cpxo3pU", "Agar Tum Saath Ho - Tamasha", "Arijit Singh", 331, "https://i.ytimg.com/vi/nCg0Cpxo3pU/default.jpg", ""),
            YouTubeSong("hoNb6HuNmQ0", "Kabira - Yeh Jawaani Hai Deewani", "Arijit Singh", 243, "https://i.ytimg.com/vi/hoNb6HuNmQ0/default.jpg", ""),
            YouTubeSong("Umqb9KENgmk", "Apna Time Aayega - Gully Boy", "Ranveer Singh", 195, "https://i.ytimg.com/vi/Umqb9KENgmk/default.jpg", ""),
            YouTubeSong("x3pMiriOiZk", "Malang - Title Track", "Ved Sharma", 237, "https://i.ytimg.com/vi/x3pMiriOiZk/default.jpg", ""),
            // ── Hollywood / English ──
            YouTubeSong("kJQP7kiw5Fk", "Despacito ft. Daddy Yankee", "Luis Fonsi", 282, "https://i.ytimg.com/vi/kJQP7kiw5Fk/default.jpg", ""),
            YouTubeSong("JGwWNGJdvx8", "Shape of You", "Ed Sheeran", 263, "https://i.ytimg.com/vi/JGwWNGJdvx8/default.jpg", ""),
            YouTubeSong("60ItHLz5WEA", "Faded", "Alan Walker", 212, "https://i.ytimg.com/vi/60ItHLz5WEA/default.jpg", ""),
            YouTubeSong("fHI8X4OXluQ", "Blinding Lights", "The Weeknd", 200, "https://i.ytimg.com/vi/fHI8X4OXluQ/default.jpg", ""),
            YouTubeSong("gBRi6aZnhGw", "Levitating", "Dua Lipa", 203, "https://i.ytimg.com/vi/gBRi6aZnhGw/default.jpg", ""),
            YouTubeSong("4TWR90KJl8k", "Dynamite", "BTS", 199, "https://i.ytimg.com/vi/4TWR90KJl8k/default.jpg", ""),
            YouTubeSong("gdZLi9oWNZg", "How You Like That", "BLACKPINK", 180, "https://i.ytimg.com/vi/gdZLi9oWNZg/default.jpg", ""),
            YouTubeSong("RgKAFK5djSk", "See You Again ft. Charlie Puth", "Wiz Khalifa", 237, "https://i.ytimg.com/vi/RgKAFK5djSk/default.jpg", ""),
            YouTubeSong("pRpeEdMmmQ0", "Someone Like You", "Adele", 285, "https://i.ytimg.com/vi/pRpeEdMmmQ0/default.jpg", ""),
            YouTubeSong("09R8_2nJtjg", "Sugar", "Maroon 5", 235, "https://i.ytimg.com/vi/09R8_2nJtjg/default.jpg", ""),
            // ── Punjabi ──
            YouTubeSong("nvB5G4jPBxQ", "295", "Sidhu Moose Wala", 234, "https://i.ytimg.com/vi/nvB5G4jPBxQ/default.jpg", ""),
            YouTubeSong("8wzD9b_fXmM", "Excuses", "AP Dhillon", 180, "https://i.ytimg.com/vi/8wzD9b_fXmM/default.jpg", ""),
            YouTubeSong("D0JfSSjLOnQ", "Brown Munde", "AP Dhillon", 198, "https://i.ytimg.com/vi/D0JfSSjLOnQ/default.jpg", ""),
            YouTubeSong("f3xUhAMCbig", "Lahore", "Guru Randhawa", 215, "https://i.ytimg.com/vi/f3xUhAMCbig/default.jpg", ""),
            YouTubeSong("YxWlaLCeA5Q", "Titliaan", "Harrdy Sandhu", 210, "https://i.ytimg.com/vi/YxWlaLCeA5Q/default.jpg", "")
        )
    }

    // ═══════════════════════════════════════════════
    // HTTP helper with configurable timeout
    // ═══════════════════════════════════════════════
    private fun httpGet(urlString: String, timeout: Int = 10000): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) PulseMusicPlayer/1.0")
        conn.connectTimeout = timeout
        conn.readTimeout = timeout
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
