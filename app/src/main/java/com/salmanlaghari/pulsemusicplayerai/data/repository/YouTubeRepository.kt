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
import java.util.Locale

class YouTubeRepository {

    companion object {
        private const val TAG = "YouTubeRepo"

        // ═══ FAST TIMEOUTS (reduced for faster loading) ═══
        private const val FAST_TIMEOUT = 5000 // 5 seconds
        private const val NORMAL_TIMEOUT = 8000 // 8 seconds

        // ═══ REGION MAPPING — maps locale to YouTube region codes ═══
        private val REGION_MAP = mapOf(
            "IN" to "IN",  // India
            "US" to "US",  // USA
            "GB" to "GB",  // UK
            "PK" to "PK",  // Pakistan
            "BD" to "BD",  // Bangladesh
            "AE" to "AE",  // UAE
            "SA" to "SA",  // Saudi Arabia
            "DE" to "DE",  // Germany
            "FR" to "FR",  // France
            "ES" to "ES",  // Spain
            "IT" to "IT",  // Italy
            "BR" to "BR",  // Brazil
            "MX" to "MX",  // Mexico
            "JP" to "JP",  // Japan
            "KR" to "KR",  // Korea
            "CN" to "CN",  // China
            "RU" to "RU",  // Russia
            "ID" to "ID",  // Indonesia
            "TH" to "TH",  // Thailand
            "VN" to "VN",  // Vietnam
            "PH" to "PH",  // Philippines
            "MY" to "MY",  // Malaysia
            "TR" to "TR",  // Turkey
            "EG" to "EG",  // Egypt
            "NG" to "NG",  // Nigeria
            "ZA" to "ZA",  // South Africa
            "AR" to "AR",  // Argentina
            "CO" to "CO",  // Colombia
            "CL" to "CL",  // Chile
            "PE" to "PE",  // Peru
            "CA" to "CA",  // Canada
            "AU" to "AU",  // Australia
            "NZ" to "NZ"   // New Zealand
        )

        // ═══ COUNTRY-SPECIFIC SEARCH KEYWORDS ═══
        private val REGIONAL_MUSIC_KEYWORDS = mapOf(
            "IN" to listOf("bollywood songs", "hindi songs", "punjabi songs", "indian music"),
            "US" to listOf("top hits", "pop music", "hip hop", "american music"),
            "GB" to listOf("uk hits", "british music", "uk charts"),
            "PK" to listOf("pakistani songs", "urdu songs", "pakistani music"),
            "BD" to listOf("bangla songs", "bangladeshi music"),
            "AE" to listOf("arabic music", "gulf music", "emirati songs"),
            "SA" to listOf("arabic songs", "saudi music"),
            "DE" to listOf("german music", "deutsch music"),
            "FR" to listOf("french music", "chanson francaise"),
            "ES" to listOf("spanish music", "latin music", "reggaeton"),
            "IT" to listOf("italian music", "italo disco"),
            "BR" to listOf("brazilian music", "samba", "bossa nova"),
            "MX" to listOf("reggaeton", "latin pop", "musica mexicana"),
            "JP" to listOf("jpop", "japanese music", "anime songs"),
            "KR" to listOf("kpop", "korean music", "kdrama ost"),
            "CN" to listOf("cpop", "chinese music"),
            "ID" to listOf("indonesian music", "dangdut"),
            "TH" to listOf("thai music", "luk thung"),
            "VN" to listOf("vietnamese music", "vpop"),
            "PH" to listOf("opm", "filipino music"),
            "MY" to listOf("malay music", "malaysian songs"),
            "TR" to listOf("turkish music", "arabesque"),
            "EG" to listOf("arabic music", "egyptian songs"),
            "NG" to listOf("afrobeats", "nigerian music"),
            "ZA" to listOf("amapiano", "south african music"),
            "AR" to listOf("reggaeton", "latin music"),
            "CO" to listOf("salsa", "colombian music"),
            "CA" to listOf("canadian music", "canadian hits"),
            "AU" to listOf("australian music", "auspop")
        )

        // ═══ Invidious instances (ordered by reliability) ═══
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.zoomerville.com",
            "https://yewtu.be",
            "https://inv.tux.pizza",
            "https://iv.ggtyler.dev",
            "https://invidious.fdn.fr",
            "https://invidious.futo.org",
            "https://invidious.perennialte.ch",
            "https://invidious.protokolla.fi",
            "https://invidious.privacyredirect.com",
            "https://invidious.nerdvpn.de",
            "https://inv.nadeko.net",
            "https://invidious.lunar.icu",
            "https://yt.artemislena.eu",
            "https://invidious.drgns.space"
        )

        // ═══ Piped instances (ordered by reliability) ═══
        private val PIPED_INSTANCES = listOf(
            "https://api.piped.private.coffee",
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.hostux.net",
            "https://pipedapi.darkness.services",
            "https://pipedapi.r4fo.com",
            "https://piped-api.lunar.icu",
            "https://pipedapi.in.projectsegfault.com",
            "https://api.piped.projectsegfault.com",
            "https://watchapi.whatever.social"
        )

        // ═══ cobalt.tools instances ═══
        private val COBALT_INSTANCES = listOf(
            "https://cobalt-api.hyper.lol",
            "https://co.eepy.today",
            "https://cobalt.canine.tools",
            "https://cobalt.api.timelessnesses.me"
        )

        private var currentInvidiousIndex = 0
        private var currentPipedIndex = 0
        private var currentCobaltIndex = 0

        // ═══ CACHE for faster subsequent loads ═══
        private var cachedTrendingSongs: List<YouTubeSong>? = null
        private var cachedTrendingRegion: String? = null
        private var cachedTrendingTime: Long = 0
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000 // 5 minutes

        // ═══ FAST MUSIC FILTER KEYWORDS ═══
        private val MUSIC_KEYWORDS = listOf(
            "music", "song", "official video", "official audio", "official music",
            "lyric video", "lyrics", "ft.", "feat.", "remix", "live",
            "vevo", "topic", "recordings", "musica", "musique", "musik"
        )

        private val MUSIC_CHANNEL_IDS = listOf(
            "UC", // YouTube Music
            "Vevo",
            "Spinnin'",
            "Trap Nation",
            "CloudKid",
            "MrSuicideSheep",
            "ChilledCow",
            "Radio Swiss",
            "Ultra Music",
            "SonyMusicIndia",
            "T-Series",
            "Gaana",
            "Hungama"
        )

        // ═══ FREE LEGAL MUSIC — Internet Archive Direct Links ═══
        // These are public domain / Creative Commons music files
        private val FREE_MUSIC_DATABASE = listOf(
            // ── Bollywood / Indian ──
            FreeSong("ia_bollywood_1", "Bollywood Dance Mix", "Free Music Archive", 180,
                "https://i.ytimg.com/vi/VYMQ_LI_OWM/default.jpg",
                "https://archive.org/download/free-music-bollywood/bollywood-dance-mix.mp3"),
            FreeSong("ia_bollywood_2", "Indian Classical Raga", "Ravi Shankar Tribute", 240,
                "https://i.ytimg.com/vi/nfRTA9fAN_s/default.jpg",
                "https://archive.org/download/free-indian-music/indian-classical-raga.mp3"),
            FreeSong("ia_bollywood_3", "Punjabi Bhangra Beats", "DJ Free Mix", 200,
                "https://i.ytimg.com/vi/nvB5G4jPBxQ/default.jpg",
                "https://archive.org/download/free-punjabi-music/punjabi-bhangra.mp3"),

            // ── English Pop / Rock ──
            FreeSong("ia_pop_1", "Acoustic Sunrise", "FMA Artists", 195,
                "https://i.ytimg.com/vi/JGwWNGJdvx8/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Doctor_Dream/Pluck/Doctor_Dream_-_01_-_Acoustic_Sunrise.mp3"),
            FreeSong("ia_pop_2", "Indie Pop Vibes", "Jamendo Artists", 210,
                "https://i.ytimg.com/vi/60ItHLz5WEA/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Chad_Crouch/Arps/Chad_Crouch_-_Shipping_Lanes.mp3"),
            FreeSong("ia_pop_3", "Electronic Dreams", "Synthwave Free", 225,
                "https://i.ytimg.com/vi/fHI8X4OXluQ/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Ketsa/Rainbow/Ketsa_-_11_-_Rainbow.mp3"),
            FreeSong("ia_pop_4", "Summer Chill", "Chillhop Free", 180,
                "https://i.ytimg.com/vi/gBRi6aZnhGw/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Rolemusic/Rolemusic_-_The_Pirates_Anthem/Rolemusic_-_01_-_The_Pirates_Anthem.mp3"),
            FreeSong("ia_pop_5", "Rock Anthem", "Free Rock Music", 240,
                "https://i.ytimg.com/vi/RgKAFK5djSk/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Scott_Holmes/Happy_Rock/Scott_Holmes_-_Happy_Rock.mp3"),

            // ── Lo-fi / Chill ──
            FreeSong("ia_lofi_1", "Late Night Lo-fi", "Chill Beats Free", 300,
                "https://i.ytimg.com/vi/4TWR90KJl8k/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Ketsa/Rainbow/Ketsa_-_01_-_We_Are_One.mp3"),
            FreeSong("ia_lofi_2", "Rainy Day Jazz", "Jazz Free Collection", 260,
                "https://i.ytimg.com/vi/gdZLi9oWNZg/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Blue_Skies/Blue_Skies_-_01_-_A_Song_for_You.mp3"),

            // ── Hip Hop / Rap ──
            FreeSong("ia_hiphop_1", "Street Beats", "Hip Hop Free", 190,
                "https://i.ytimg.com/vi/pRpeEdMmmQ0/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Jahzzar/Travellers/Jahzzar_-_01_-_Sierra_Nevada.mp3"),

            // ── Classical / Piano ──
            FreeSong("ia_classical_1", "Moonlight Sonata Remix", "Classical Free", 280,
                "https://i.ytimg.com/vi/lBVBbRqNCQQ/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Rolemusic/Rolemusic_-_01_-_The_Pirates_Anthem.mp3"),
            FreeSong("ia_classical_2", "Piano Meditation", "Relaxation Music", 320,
                "https://i.ytimg.com/vi/nCg0Cpxo3pU/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Pictures_of_the_Floating_World/Pictures_of_the_Floating_World_-_01_-_Mountain_Path.mp3"),

            // ── EDM / Dance ──
            FreeSong("ia_edm_1", "Festival Drop", "EDM Free", 210,
                "https://i.ytimg.com/vi/09R8_2nJtjg/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Doctor_Dream/Pluck/Doctor_Dream_-_02_-_Pluck.mp3"),
            FreeSong("ia_edm_2", "Club Night", "Dance Music Free", 195,
                "https://i.ytimg.com/vi/kJQP7kiw5Fk/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Ketsa/Rainbow/Ketsa_-_03_-_Rise.mp3"),

            // ── Arabic / Middle Eastern ──
            FreeSong("ia_arabic_1", "Arabic Nights", "Middle Eastern Free", 230,
                "https://i.ytimg.com/vi/IJq0yyWug1k/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Jahzzar/Travellers/Jahzzar_-_02_-_Shadows.mp3"),

            // ── African ──
            FreeSong("ia_african_1", "African Drums", "World Music Free", 200,
                "https://i.ytimg.com/vi/D0JfSSjLOnQ/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Rolemusic/Rolemusic_-_02_-_Lakeside_Nature_Lodge.mp3"),

            // ── Spanish / Latin ──
            FreeSong("ia_latin_1", "Latin Fire", "Latin Music Free", 185,
                "https://i.ytimg.com/vi/f3xUhAMCbig/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Jahzzar/Travellers/Jahzzar_-_03_-_Valley_of_the_Shadows.mp3"),

            // ── Chinese / Asian ──
            FreeSong("ia_asian_1", "Zen Garden", "Asian Meditation", 250,
                "https://i.ytimg.com/vi/x3pMiriOiZk/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Pictures_of_the_Floating_World/Pictures_of_the_Floating_World_-_02_-_Ancient_City.mp3"),

            // ── Turkish ──
            FreeSong("ia_turkish_1", "Istanbul Nights", "Turkish Free", 215,
                "https://i.ytimg.com/vi/hoNb6HuNmQ0/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Jahzzar/Travellers/Jahzzar_-_04_-_All_Through_the_Night.mp3"),

            // ── K-Pop ──
            FreeSong("ia_kpop_1", "K-Pop Energy", "Korean Pop Free", 195,
                "https://i.ytimg.com/vi/8wzD9b_fXmM/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Ketsa/Rainbow/Ketsa_-_05_-_Feel_the_Sun.mp3"),

            // ── Soundtrack / Cinematic ──
            FreeSong("ia_cinematic_1", "Epic Cinematic", "Film Score Free", 270,
                "https://i.ytimg.com/vi/Umqb9KENgmk/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Scott_Holmes/Happy_Rock/Scott_Holmes_-_Inspiring.mp3"),
            FreeSong("ia_cinematic_2", "Dark Ambient", "Atmospheric Free", 310,
                "https://i.ytimg.com/vi/sH3C6G_PpQQ/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Doctor_Dream/Pluck/Doctor_Dream_-_03_-_Dreaming.mp3"),

            // ── Reggae ──
            FreeSong("ia_reggae_1", "Island Vibes", "Reggae Free", 220,
                "https://i.ytimg.com/vi/YxWlaLCeA5Q/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Jahzzar/Travellers/Jahzzar_-_05_-_Island_Life.mp3"),

            // ── Country ──
            FreeSong("ia_country_1", "Country Road", "Country Free", 200,
                "https://i.ytimg.com/vi/2MuPEG3FcwE/default.jpg",
                "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Scott_Holmes/Happy_Rock/Scott_Holmes_-_Country_Morning.mp3")
        )

        // ═══ Internet Archive Search API (FREE, No API Key!) ═══
        private const val IA_SEARCH_URL = "https://archive.org/advancedsearch.php"
        private const val IA_METADATA_URL = "https://archive.org/metadata"

        // ═══ Deezer API (FREE, No API Key for basic search!) ═══
        private const val DEEZER_API_BASE = "https://api.deezer.com"
        
        // ═══ Jamendo API (FREE music, needs client_id) ═══
        private const val JAMENDO_API_BASE = "https://api.jamendo.com/v3.0"
        
        // ═══ JioSaavn API - FULL SONGS! (FREE) ═══
        private const val JIOSAAVN_API = "https://saavn.sumit.co/api"
    }

    data class FreeSong(
        val id: String,
        val title: String,
        val artist: String,
        val duration: Long,
        val thumbnailUrl: String,
        val audioUrl: String
    )

    // ═══════════════════════════════════════════════
    // SEARCH — Deezer + Internet Archive + YouTube
    // ═══════════════════════════════════════════════
    suspend fun search(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val searchQuery = query.trim()
        Log.d(TAG, "Searching for: $searchQuery")

        // 1. DEEZER FIRST - Most reliable for music search
        try {
            val deezerResults = searchDeezer(searchQuery)
            if (deezerResults.isNotEmpty()) {
                Log.d(TAG, "Deezer search: ${deezerResults.size} results")
                return@withContext deezerResults
            }
        } catch (e: Exception) { Log.w(TAG, "Deezer search fail: ${e.message}") }

        // 2. Try local database
        val localResults = searchLocalDatabase(query)
        if (localResults.isNotEmpty()) {
            Log.d(TAG, "Local DB search: ${localResults.size} results")
            return@withContext localResults
        }

        // 3. INTERNET ARCHIVE - Free full songs
        try {
            val iaResults = searchInternetArchive(searchQuery)
            if (iaResults.isNotEmpty()) {
                Log.d(TAG, "Internet Archive search: ${iaResults.size} results")
                return@withContext iaResults.take(30)
            }
        } catch (e: Exception) { Log.w(TAG, "Internet Archive search fail: ${e.message}") }

        // 4. Try YouTube/Piped as fallback
        for (i in 0 until minOf(3, PIPED_INSTANCES.size)) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val url = "$instance/search?q=$encodedQuery&filter=videos"
                val response = httpGet(url, timeout = NORMAL_TIMEOUT)
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
                    val items = json.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        val songs = parsePipedItems(items)
                        if (songs.isNotEmpty()) {
                            currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                            Log.d(TAG, "Piped search: ${songs.size} results")
                            return@withContext songs.take(30)
                        }
                    }
                }
            } catch (e: Exception) { Log.w(TAG, "Piped search fail: ${e.message}") }
        }

        // 5. Try Invidious as fallback
        for (i in 0 until minOf(3, INVIDIOUS_INSTANCES.size)) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val region = getUserRegion()
                val url = "$instance/api/v1/search?q=$encodedQuery&type=videos&region=$region"
                val response = httpGet(url, timeout = NORMAL_TIMEOUT)
                if (response.isNotBlank()) {
                    val jsonArray = JSONArray(response)
                    val songs = parseInvidiousItems(jsonArray)
                    if (songs.isNotEmpty()) {
                        currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                        Log.d(TAG, "Invidious search: ${songs.size} results")
                        return@withContext songs.take(30)
                    }
                }
            } catch (e: Exception) { Log.w(TAG, "Invidious search fail: ${e.message}") }
        }

        Log.w(TAG, "All search APIs failed for: $query")
        emptyList()
    }

    // ═══════════════════════════════════════════════
    // JIOSAAVN SEARCH - FULL SONGS! Bollywood, Hindi, Punjabi
    // ═══════════════════════════════════════════════
    suspend fun searchJioSaavn(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "$JIOSAAVN_API/search/songs?query=$encodedQuery"
            val searchResponse = httpGet(searchUrl, timeout = NORMAL_TIMEOUT)

            if (searchResponse.isNotBlank()) {
                val searchJson = JSONObject(searchResponse)
                // New saavn.sumit.co API format: { "success": true, "data": { "results": [...] } }
                val dataObj = searchJson.optJSONObject("data")
                val results = dataObj?.optJSONArray("results")
                    ?: searchJson.optJSONArray("results") // fallback to old format
                    ?: return@withContext emptyList()

                for (i in 0 until minOf(results.length(), 20)) {
                    try {
                        val result = results.getJSONObject(i)
                        val id = result.optString("id", "")
                        val title = result.optString("name", result.optString("title", "Unknown"))

                        // Artists: array of { name, role } - collect primary artists
                        val artistsArr = result.optJSONArray("artists")
                        val primaryArr = result.optJSONObject("artists")?.optJSONArray("primary")
                        val artistName = StringBuilder()
                        val artistSource = primaryArr ?: artistsArr
                        if (artistSource != null) {
                            for (a in 0 until artistSource.length()) {
                                try {
                                    val aObj = artistSource.getJSONObject(a)
                                    val name = aObj.optString("name", "")
                                    if (name.isNotBlank()) {
                                        if (artistName.isNotEmpty()) artistName.append(", ")
                                        artistName.append(name)
                                    }
                                } catch (e: Exception) { }
                            }
                        }
                        // Fallback to old more_info.singers format
                        if (artistName.isBlank()) {
                            val moreInfo = result.optJSONObject("more_info")
                            val singers = moreInfo?.optString("singers", "") ?: ""
                            if (singers.isNotBlank()) artistName.append(singers)
                        }

                        // Image: array of { quality, url } - pick the largest
                        var thumbnail = ""
                        val imageArr = result.optJSONArray("image")
                        if (imageArr != null && imageArr.length() > 0) {
                            thumbnail = imageArr.getJSONObject(imageArr.length() - 1).optString("url", "")
                        }
                        if (thumbnail.isBlank()) {
                            thumbnail = result.optString("image", "")
                        }

                        // Download URL: array of { quality, url } - pick highest quality
                        val downloadArr = result.optJSONArray("downloadUrl")
                        val mediaUrl = pickJioSaavnMediaUrl(downloadArr)

                        // Duration: integer seconds in new API
                        val durationSec = result.optLong("duration", 0)
                        val duration = if (durationSec > 0) durationSec else {
                            // Fallback to old format "M:SS"
                            val durationStr = result.optString("duration", "0")
                            parseDurationString(durationStr)
                        }

                        if (id.isNotBlank()) {
                            songs.add(YouTubeSong(
                                id = "js_$id",
                                title = title,
                                artist = artistName.toString().ifBlank { "Unknown Artist" },
                                duration = duration,
                                thumbnailUrl = thumbnail,
                                audioUrl = mediaUrl ?: ""
                            ))
                        }
                    } catch (e: Exception) { /* skip invalid */ }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "JioSaavn search error: ${e.message}")
        }
        songs
    }

    // Pick the highest quality media URL from JioSaavn downloadUrl array
    private fun pickJioSaavnMediaUrl(downloadArr: JSONArray?): String? {
        if (downloadArr == null || downloadArr.length() == 0) return null
        // The array is ordered from lowest to highest quality typically
        // Pick the last entry (highest quality, usually 320kbps)
        for (i in downloadArr.length() - 1 downTo 0) {
            try {
                val item = downloadArr.getJSONObject(i)
                val url = item.optString("url", "")
                if (url.isNotBlank() && url.startsWith("http")) {
                    return url
                }
            } catch (e: Exception) { }
        }
        return null
    }

    // Get full song URL from JioSaavn (320kbps) - new saavn.sumit.co API
    private suspend fun getJioSaavnSongDetails(songId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = "$JIOSAAVN_API/songs/$songId"
            val response = httpGet(url, timeout = NORMAL_TIMEOUT)
            if (response.isNotBlank()) {
                val json = JSONObject(response)
                val data = json.optJSONObject("data")
                if (data != null) {
                    return@withContext data
                }
                // Fallback to old format
                if (json.optBoolean("status", false)) {
                    return@withContext json
                }
            }
        } catch (e: Exception) { Log.w(TAG, "JioSaavn song details error: ${e.message}") }
        null
    }

    // ═══════════════════════════════════════════════
    // DEEZER SEARCH - Free API with working previews
    // ═══════════════════════════════════════════════
    private suspend fun searchDeezer(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$DEEZER_API_BASE/search?q=$encodedQuery&limit=30"
            val response = httpGet(url, timeout = NORMAL_TIMEOUT)
            
            if (response.isNotBlank()) {
                val json = JSONObject(response)
                val tracks = json.optJSONArray("data") ?: return@withContext emptyList()
                
                for (i in 0 until tracks.length()) {
                    try {
                        val track = tracks.getJSONObject(i)
                        val title = track.optString("title_short", track.optString("title", "Unknown"))
                        val artist = track.optString("artist_name", track.optString("artist", "")?.let { 
                            val a = JSONObject(it); a.optString("name", "Unknown Artist") 
                        } ?: "Unknown Artist")
                        val duration = track.optLong("duration", 0)
                        val deezerId = track.optString("id", "")
                        val previewUrl = track.optString("preview", "")
                        
                        // Get album cover
                        val albumObj = track.optJSONObject("album")
                        val coverMedium = albumObj?.optString("cover_medium", "") 
                            ?: track.optString("md5_image", "").let { "https://e-cdns-images.dzcdn.net/images/cover/$it/250x250-000000-80-0-0.jpg" }
                        
                        // Only add if we have a valid preview URL (30 second preview)
                        if (previewUrl.isNotBlank() && previewUrl.startsWith("http")) {
                            songs.add(YouTubeSong(
                                id = "dz_$deezerId",
                                title = title,
                                artist = artist,
                                duration = duration,
                                thumbnailUrl = coverMedium.ifBlank { "https://e-cdns-images.dzcdn.net/images/cover/${track.optString("md5_image","")}/250x250-000000-80-0-0.jpg" },
                                audioUrl = previewUrl
                            ))
                        }
                    } catch (e: Exception) { /* skip invalid track */ }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Deezer search error: ${e.message}")
        }
        songs
    }

    // ═══════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════════════
    // APPLE MUSIC SEARCH — iTunes Search API (FREE, no auth, 30s preview m4a)
    // ═══════════════════════════════════════════════════════════════════════════════
    suspend fun searchAppleMusic(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            // iTunes Search API — returns tracks with 30-second previewUrl (m4a, playable in ExoPlayer)
            val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=30"
            val response = httpGet(url, timeout = NORMAL_TIMEOUT)

            if (response.isNotBlank()) {
                val json = JSONObject(response)
                val results = json.optJSONArray("results") ?: return@withContext emptyList()

                for (i in 0 until results.length()) {
                    try {
                        val track = results.getJSONObject(i)
                        val trackName = track.optString("trackName", "Unknown")
                        val artistName = track.optString("artistName", "Unknown Artist")
                        val trackId = track.optString("trackId", "")
                        val previewUrl = track.optString("previewUrl", "")
                        val artworkUrl = track.optString("artworkUrl100", "")
                            .replace("100x100", "300x300")
                        val durationMs = track.optLong("trackTimeMillis", 0)
                        val durationSec = if (durationMs > 0) durationMs / 1000 else 0

                        // Only add tracks that have a playable preview URL
                        if (previewUrl.isNotBlank() && previewUrl.startsWith("http")) {
                            songs.add(
                                YouTubeSong(
                                    id = "am_$trackId",
                                    title = trackName,
                                    artist = artistName,
                                    duration = durationSec,
                                    thumbnailUrl = artworkUrl.ifBlank { "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/00/00/00/0000000000/300x300.jpg" },
                                    audioUrl = previewUrl
                                )
                            )
                        }
                    } catch (e: Exception) { /* skip invalid track */ }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Apple Music search error: ${e.message}")
        }
        songs
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SPOTIFY SEARCH — via Spotify public access token (metadata + 30s preview)
    // ═══════════════════════════════════════════════════════════════════════════════
    suspend fun searchSpotify(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")

            // 1. Obtain a public access token (Spotify Web Player anonymous token endpoint)
            val tokenUrl = "https://open.spotify.com/get_access_token?reason=transport&productType=web_player"
            val tokenResponse = httpGet(tokenUrl, timeout = FAST_TIMEOUT)
            var accessToken: String? = null
            if (tokenResponse.isNotBlank()) {
                try {
                    val tokenJson = JSONObject(tokenResponse)
                    accessToken = tokenJson.optString("accessToken", "")
                } catch (e: Exception) { /* fall through */ }
            }

            if (accessToken.isNullOrBlank()) {
                Log.w(TAG, "Spotify token fetch failed; skipping Spotify search")
                return@withContext emptyList()
            }

            // 2. Search tracks using the public access token
            val searchUrl = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=30"
            val searchResponse = httpGetWithAuth(searchUrl, "Bearer $accessToken", timeout = NORMAL_TIMEOUT)

            if (searchResponse.isNotBlank()) {
                val json = JSONObject(searchResponse)
                val tracks = json.optJSONObject("tracks")?.optJSONArray("items")
                    ?: return@withContext emptyList()

                for (i in 0 until tracks.length()) {
                    try {
                        val track = tracks.getJSONObject(i)
                        val trackName = track.optString("name", "Unknown")
                        val trackId = track.optString("id", "")
                        val artists = track.optJSONArray("artists")
                        val artistName = if (artists != null && artists.length() > 0) {
                            artists.getJSONObject(0).optString("name", "Unknown Artist")
                        } else "Unknown Artist"
                        val durationMs = track.optLong("duration_ms", 0)
                        val durationSec = if (durationMs > 0) durationMs / 1000 else 0
                        val previewUrl = track.optString("preview_url", "")
                        val albumObj = track.optJSONObject("album")
                        val images = albumObj?.optJSONArray("images")
                        val thumbnail = if (images != null && images.length() > 0) {
                            images.getJSONObject(0).optString("url", "")
                        } else ""

                        // Spotify preview_url can be null for some tracks; still include metadata-only
                        // entries so they appear in sync results, but mark audio as empty so fallback resolves them.
                        val audio = previewUrl.takeIf { it.isNotBlank() && it.startsWith("http") } ?: ""
                        songs.add(
                            YouTubeSong(
                                id = "sp_$trackId",
                                title = trackName,
                                artist = artistName,
                                duration = durationSec,
                                thumbnailUrl = thumbnail.ifBlank { "https://i.scdn.co/image/ab67616d0000b273000000000000000000000000" },
                                audioUrl = audio
                            )
                        )
                    } catch (e: Exception) { /* skip invalid track */ }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Spotify search error: ${e.message}")
        }
        songs
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // YOUTUBE MUSIC SEARCH — dedicated Piped/Invidious video search (full streams)
    // ═══════════════════════════════════════════════════════════════════════════════
    suspend fun searchYouTubeMusic(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val searchQuery = query.trim()
        val songs = mutableListOf<YouTubeSong>()

        // 1. YouTube WEB innerTube search (PRIMARY - works directly, no third-party needed)
        try {
            val innerTubeResults = searchYouTubeInnerTube(searchQuery)
            if (innerTubeResults.isNotEmpty()) {
                Log.d(TAG, "YouTube innerTube search: ${innerTubeResults.size} results")
                return@withContext innerTubeResults.take(30)
            }
        } catch (e: Exception) { Log.w(TAG, "YouTube innerTube search fail: ${e.message}") }

        // 2. Piped fallback
        for (i in 0 until minOf(3, PIPED_INSTANCES.size)) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val url = "$instance/search?q=$encodedQuery&filter=music_songs"
                val response = httpGet(url, timeout = NORMAL_TIMEOUT)
                if (response.isNotBlank()) {
                    val json = JSONObject(response)
                    val items = json.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        val parsed = parsePipedItems(items)
                        if (parsed.isNotEmpty()) {
                            currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                            songs.addAll(parsed)
                            return@withContext songs.take(30)
                        }
                    }
                }
            } catch (e: Exception) { Log.w(TAG, "Piped YTM search fail: ${e.message}") }
        }

        // 3. Invidious fallback
        for (i in 0 until minOf(3, INVIDIOUS_INSTANCES.size)) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                val region = getUserRegion()
                val url = "$instance/api/v1/search?q=$encodedQuery&type=video&region=$region"
                val response = httpGet(url, timeout = NORMAL_TIMEOUT)
                if (response.isNotBlank()) {
                    val jsonArray = JSONArray(response)
                    val parsed = parseInvidiousItems(jsonArray)
                    if (parsed.isNotEmpty()) {
                        currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                        songs.addAll(parsed)
                        return@withContext songs.take(30)
                    }
                }
            } catch (e: Exception) { Log.w(TAG, "Invidious YTM search fail: ${e.message}") }
        }

        songs
    }

    // YouTube WEB innerTube search API - works directly from YouTube, no third-party instance needed
    private fun searchYouTubeInnerTube(query: String): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
            // WEB client context
            val jsonBody = """{"context":{"client":{"clientName":"WEB","clientVersion":"2.20240726.01.00"}},"query":"$query"}"""
            val response = httpPostJson(url, jsonBody, timeout = 15000)
            if (response.isNotBlank()) {
                val json = JSONObject(response)
                // Parse: contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents[].itemSectionRenderer.contents[].videoRenderer
                val twoCol = json.optJSONObject("contents")?.optJSONObject("twoColumnSearchResultsRenderer")
                val primaryContents = twoCol?.optJSONObject("primaryContents")
                val sectionList = primaryContents?.optJSONObject("sectionListRenderer")
                val sections = sectionList?.optJSONArray("contents")
                if (sections != null) {
                    for (s in 0 until sections.length()) {
                        try {
                            val section = sections.getJSONObject(s)
                            val itemSection = section.optJSONObject("itemSectionRenderer")
                            val contents = itemSection?.optJSONArray("contents")
                            if (contents != null) {
                                for (c in 0 until contents.length()) {
                                    try {
                                        val item = contents.getJSONObject(c)
                                        val videoRenderer = item.optJSONObject("videoRenderer")
                                        if (videoRenderer != null) {
                                            val videoId = videoRenderer.optString("videoId", "")
                                            if (videoId.isBlank()) continue

                                            // Title from title.runs
                                            val titleRuns = videoRenderer.optJSONObject("title")?.optJSONArray("runs")
                                            val title = StringBuilder()
                                            if (titleRuns != null) {
                                                for (r in 0 until titleRuns.length()) {
                                                    title.append(titleRuns.getJSONObject(r).optString("text", ""))
                                                }
                                            }
                                            if (title.isBlank()) continue

                                            // Channel from ownerText.runs or shortBylineText.runs
                                            val ownerRuns = videoRenderer.optJSONObject("ownerText")?.optJSONArray("runs")
                                                ?: videoRenderer.optJSONObject("shortBylineText")?.optJSONArray("runs")
                                            val channel = StringBuilder()
                                            if (ownerRuns != null && ownerRuns.length() > 0) {
                                                channel.append(ownerRuns.getJSONObject(0).optString("text", ""))
                                            }
                                            if (channel.isBlank()) channel.append("Unknown Artist")

                                            // Duration from lengthText.simpleText
                                            val lengthText = videoRenderer.optJSONObject("lengthText")?.optString("simpleText", "")
                                            val duration = if (lengthText != null && lengthText.isNotBlank()) parseDurationString(lengthText) else 0L

                                            // Thumbnail from thumbnail.thumbnails (largest)
                                            var thumbnail = ""
                                            val thumbsArr = videoRenderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                            if (thumbsArr != null && thumbsArr.length() > 0) {
                                                thumbnail = thumbsArr.getJSONObject(thumbsArr.length() - 1).optString("url", "")
                                            }
                                            if (thumbnail.isBlank()) {
                                                thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                            }

                                            songs.add(YouTubeSong(
                                                id = "yt_$videoId",
                                                title = title.toString().trim(),
                                                artist = channel.toString().trim(),
                                                duration = duration,
                                                thumbnailUrl = thumbnail,
                                                audioUrl = "" // resolved on playback
                                            ))
                                        }
                                    } catch (e: Exception) { /* skip */ }
                                }
                            }
                        } catch (e: Exception) { /* skip section */ }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube innerTube search error: ${e.message}")
        }
        return songs
    }

    // Parse duration string like "3:45" or "1:23:45" into seconds
    private fun parseDurationString(duration: String): Long {
        if (duration.isBlank()) return 0L
        val parts = duration.split(":")
        return when (parts.size) {
            2 -> (parts[0].toLongOrNull() ?: 0) * 60 + (parts[1].toLongOrNull() ?: 0)
            3 -> (parts[0].toLongOrNull() ?: 0) * 3600 + (parts[1].toLongOrNull() ?: 0) * 60 + (parts[2].toLongOrNull() ?: 0)
            1 -> parts[0].toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    // TRENDING — Deezer + Internet Archive + local
    // ═══════════════════════════════════════════════
    suspend fun getTrending(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val region = getUserRegion()
        val currentTime = System.currentTimeMillis()

        // Check cache first
        if (cachedTrendingSongs != null && 
            cachedTrendingRegion == region && 
            currentTime - cachedTrendingTime < CACHE_VALIDITY_MS) {
            Log.d(TAG, "Returning cached trending for region: $region")
            return@withContext cachedTrendingSongs!!
        }

        val allSongs = mutableListOf<YouTubeSong>()
        Log.d(TAG, "Loading trending for region: $region")

        // 1. DEEZER FIRST - Best for trending music (30-second previews)
        try {
            val regionQuery = when(region) {
                "IN" -> "indian songs"
                "US" -> "top hits"
                "GB" -> "uk hits"
                "PK" -> "pakistani songs"
                else -> "popular music"
            }
            val deezerResults = searchDeezer(regionQuery)
            if (deezerResults.isNotEmpty()) {
                allSongs.addAll(deezerResults)
                Log.d(TAG, "Deezer trending: ${deezerResults.size} songs")
            }
        } catch (e: Exception) { Log.w(TAG, "Deezer trending fail: ${e.message}") }

        // 2. INTERNET ARCHIVE - Free full songs (no preview limit!)
        if (allSongs.size < 20) {
            try {
                val iaResults = searchInternetArchive("popular")
                if (iaResults.isNotEmpty()) {
                    allSongs.addAll(iaResults.take(20))
                    Log.d(TAG, "Internet Archive trending: ${iaResults.size} songs")
                }
            } catch (e: Exception) { Log.w(TAG, "Internet Archive trending fail: ${e.message}") }
        }

        // 3. Try YouTube/Piped trending as fallback
        if (allSongs.size < 20) {
            try {
                for (i in 0 until minOf(3, PIPED_INSTANCES.size)) {
                    try {
                        val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                        val url = "$instance/trending?region=$region&category=music"
                        val response = httpGet(url, timeout = NORMAL_TIMEOUT)
                        if (response.isNotBlank()) {
                            val json = JSONArray(response)
                            for (j in 0 until minOf(json.length(), 30)) {
                                try {
                                    val item = json.getJSONObject(j)
                                    val song = parsePipedTrendingItem(item)
                                    if (song != null) allSongs.add(song)
                                } catch (e: Exception) { }
                            }
                            if (allSongs.size >= 20) {
                                currentPipedIndex = (currentPipedIndex + i) % PIPED_INSTANCES.size
                                break
                            }
                        }
                    } catch (e: Exception) { Log.w(TAG, "Piped trending fail: ${e.message}") }
                }
            } catch (e: Exception) { Log.e(TAG, "Trending error: ${e.message}") }
        }

        // 4. Add local free music
        val freeSongs = FREE_MUSIC_DATABASE.map { free ->
            YouTubeSong(
                id = free.id,
                title = free.title,
                artist = free.artist,
                duration = free.duration,
                thumbnailUrl = free.thumbnailUrl,
                audioUrl = free.audioUrl
            )
        }
        allSongs.addAll(freeSongs.take(10))

        if (allSongs.isEmpty()) {
            Log.w(TAG, "No trending songs available, returning free music only")
            return@withContext freeSongs
        }

        val unique = allSongs.distinctBy { it.id }
        
        // Update cache
        cachedTrendingSongs = unique.take(80)
        cachedTrendingRegion = region
        cachedTrendingTime = currentTime
        
        Log.d(TAG, "Trending total: ${unique.size} songs for region $region")
        return@withContext unique.take(80)
    }

    // Helper function to detect if content is likely music
    private fun isLikelyMusic(title: String, artist: String): Boolean {
        val combined = "$title $artist".lowercase()
        // Check for music-related keywords
        return MUSIC_KEYWORDS.any { combined.contains(it.lowercase()) } ||
               // Or from known music channels (checked elsewhere in parsing)
               title.isNotBlank() && artist.isNotBlank() &&
               // Exclude videos that are clearly not music
               !combined.contains("minecraft") &&
               !combined.contains("gaming") &&
               !combined.contains("tutorial") &&
               !combined.contains("react") &&
               !combined.contains("compilation") ||
               combined.contains("song") ||
               combined.contains("music")
    }

    // Get user's region from device locale
    private fun getUserRegion(): String {
        return try {
            val locale = Locale.getDefault()
            val country = locale.country
            
            // Map to supported regions
            REGION_MAP[country] ?: 
            // Try to extract country from locale (e.g., "en_IN" -> "IN")
            if (country.length == 2) country else 
            // Default to US if unknown
            "US"
        } catch (e: Exception) {
            Log.w(TAG, "Could not detect region, defaulting to US")
            "US"
        }
    }

    // ═══════════════════════════════════════════════
    // AUDIO STREAM — tries APIs + direct URLs
    // ═══════════════════════════════════════════════
    suspend fun getAudioStream(videoId: String): YouTubeSong? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Resolving audio for: $videoId")

        if (videoId.isBlank()) {
            Log.w(TAG, "Empty videoId provided")
            return@withContext null
        }

        // 1. Check if it's a local free music ID
        val localSong = FREE_MUSIC_DATABASE.find { it.id == videoId }
        if (localSong != null) {
            Log.d(TAG, "✓ Local free music: ${localSong.title}")
            return@withContext YouTubeSong(
                id = localSong.id,
                title = localSong.title,
                artist = localSong.artist,
                duration = localSong.duration,
                thumbnailUrl = localSong.thumbnailUrl,
                audioUrl = localSong.audioUrl
            )
        }

        // 2. Try ALL Invidious instances
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
                if (thumbnail.isEmpty()) thumbnail = "https://i.ytimg.com/vi/$videoId/default.jpg"

                // Try adaptiveFormats (audio-only streams)
                val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                if (adaptiveFormats != null) {
                    var bestAudioUrl = ""
                    var bestBitrate = 0
                    for (j in 0 until adaptiveFormats.length()) {
                        try {
                            val fmt = adaptiveFormats.getJSONObject(j)
                            val type = fmt.optString("type", "")
                            if (type.startsWith("audio/")) {
                                val bitrate = fmt.optInt("bitrate", 0)
                                val audioUrl = fmt.optString("url", "")
                                if (audioUrl.isNotEmpty() && audioUrl.startsWith("http") && bitrate > bestBitrate) {
                                    bestAudioUrl = audioUrl
                                    bestBitrate = bitrate
                                }
                            }
                        } catch (e: Exception) { /* skip */ }
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

                // Try formatStreams
                val formatStreams = json.optJSONArray("formatStreams")
                if (formatStreams != null) {
                    for (j in 0 until formatStreams.length()) {
                        try {
                            val fmt = formatStreams.getJSONObject(j)
                            val streamUrl = fmt.optString("url", "")
                            if (streamUrl.isNotEmpty() && streamUrl.startsWith("http")) {
                                currentInvidiousIndex = (currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size
                                Log.d(TAG, "✓ Invidious formatStream OK: $title")
                                return@withContext YouTubeSong(
                                    id = videoId, title = title, artist = author,
                                    duration = lengthSeconds, thumbnailUrl = thumbnail,
                                    audioUrl = streamUrl
                                )
                            }
                        } catch (e: Exception) { /* skip */ }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious stream fail [$i]: ${e.message}")
            }
        }

        // 3. Try ALL Piped instances
        for (i in PIPED_INSTANCES.indices) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val url = "$instance/streams/$videoId"
                val response = httpGet(url, timeout = 15000)
                val json = JSONObject(response)

                val title = json.optString("title", "Unknown")
                val uploader = json.optString("uploader", json.optString("uploaderName", "Unknown Artist"))
                val duration = json.optLong("duration", 0)
                var thumbnail = json.optString("thumbnailUrl", json.optString("thumbnail", ""))
                if (thumbnail.isEmpty()) thumbnail = "https://i.ytimg.com/vi/$videoId/default.jpg"

                val audioStreams = json.optJSONArray("audioStreams")
                if (audioStreams != null && audioStreams.length() > 0) {
                    var bestUrl = ""
                    var bestBitrate = 0
                    for (j in 0 until audioStreams.length()) {
                        try {
                            val stream = audioStreams.getJSONObject(j)
                            val bitrate = stream.optInt("bitrate", 0)
                            val streamUrl = stream.optString("url", "")
                            if (streamUrl.isNotEmpty() && streamUrl.startsWith("http") && bitrate > bestBitrate) {
                                bestUrl = streamUrl
                                bestBitrate = bitrate
                            }
                        } catch (e: Exception) { /* skip */ }
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
            } catch (e: Exception) {
                Log.w(TAG, "Piped stream fail [$i]: ${e.message}")
            }
        }

        // 4. Try cobalt.tools
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

        // 5. JioSaavn fallback: use YouTube video metadata to find the same song on JioSaavn
        try {
            val meta = getYouTubeVideoMeta(videoId)
            if (meta != null) {
                val (title, channel) = meta
                val cleanTitle = cleanYouTubeTitle(title)
                Log.d(TAG, "JioSaavn fallback: searching '$cleanTitle' (from YT: '$title')")
                val resolved = resolveFullSong(cleanTitle, channel, "")
                if (resolved != null && resolved.hasValidAudio()) {
                    // Keep the original YouTube title/channel but use the resolved audio URL
                    Log.d(TAG, "✓ JioSaavn fallback OK: $title -> ${resolved.title}")
                    return@withContext YouTubeSong(
                        id = videoId,
                        title = title, // keep YouTube title
                        artist = channel, // keep YouTube channel
                        duration = resolved.duration,
                        thumbnailUrl = resolved.thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                        audioUrl = resolved.audioUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "JioSaavn fallback fail: ${e.message}")
        }

        Log.e(TAG, "✗ ALL APIs failed for: $videoId")
        null
    }

    // Get YouTube video metadata (title + channel) via oembed API
    private fun getYouTubeVideoMeta(videoId: String): Pair<String, String>? {
        return try {
            val url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val response = httpGet(url, timeout = 10000)
            if (response.isNotBlank()) {
                val json = JSONObject(response)
                val title = json.optString("title", "")
                val author = json.optString("author_name", "")
                if (title.isNotBlank()) {
                    return Pair(title, author.ifBlank { "Unknown Artist" })
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "YouTube oembed fail for $videoId: ${e.message}")
            null
        }
    }

    // Clean YouTube video title for better JioSaavn matching
    // Removes common YouTube suffixes like " - Official Video", " | Lyric Video", etc.
    private fun cleanYouTubeTitle(title: String): String {
        var cleaned = title.trim()
        // Remove content in brackets/parentheses at the end
        cleaned = cleaned.replace(Regex("\\s*\\(.*?\\)\\s*$"), "")
        // Remove " - Official Video", " | Official Music Video", etc.
        cleaned = cleaned.replace(Regex("\\s*[-|]\\s*(Official\\s*(Music\\s*)?Video|Lyric\\s*Video|Audio|Visualizer|MV|M/V|Full\\s*Song|4K|HD)\\s*$", RegexOption.IGNORE_CASE), "")
        // Remove " | something" at the end
        cleaned = cleaned.replace(Regex("\\s*\\|\\s*[^|]+$"), "")
        // Remove "feat. ..."
        cleaned = cleaned.replace(Regex("\\s*feat\\..*", RegexOption.IGNORE_CASE), "")
        // Remove " - something" if what follows looks like a video descriptor
        cleaned = cleaned.replace(Regex("\\s*-\\s*(video|lyrics?|audio|full song|official)\\s*$", RegexOption.IGNORE_CASE), "")
        return cleaned.trim()
    }

    /**
     * Resolve a FULL (non-preview) audio stream for a song that only has a 30-second
     * preview URL (Apple Music, Spotify, Deezer). Searches JioSaavn and YouTube Music
     * for a matching track by title + artist and returns the full stream URL.
     *
     * @param title  The song title
     * @param artist The song artist
     * @param originalThumbnail The original thumbnail (kept if no better one found)
     * @return A YouTubeSong with a full audio stream URL, or null if not found
     */
    suspend fun resolveFullSong(
        title: String,
        artist: String,
        originalThumbnail: String
    ): YouTubeSong? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        // Build a clean search query: "title artist"
        val cleanTitle = title.trim().replace(Regex("\\(.*?\\)"), "").trim() // remove (feat. ...) etc.
        val cleanArtist = artist.trim().replace(Regex("\\s+feat\\..*"), "").trim()
        val searchQuery = if (cleanArtist.isNotBlank() && cleanArtist != "Unknown Artist") {
            "$cleanTitle $cleanArtist"
        } else {
            cleanTitle
        }

        Log.d(TAG, "resolveFullSong: searching '$searchQuery' for full stream")

        // 1. Try JioSaavn first (full songs, 320kbps)
        try {
            val jioResults = searchJioSaavn(searchQuery)
            if (jioResults.isNotEmpty()) {
                // Find best match by title similarity
                val match = findBestMatch(jioResults, cleanTitle, cleanArtist)
                if (match != null && match.hasValidAudio()) {
                    Log.d(TAG, "✓ resolveFullSong: JioSaavn match '${match.title}'")
                    return@withContext match
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveFullSong: JioSaavn fail: ${e.message}")
        }

        // 2. Try YouTube Music (Piped/Invidious - full streams)
        try {
            val ytmResults = searchYouTubeMusic(searchQuery)
            if (ytmResults.isNotEmpty()) {
                val match = findBestMatch(ytmResults, cleanTitle, cleanArtist)
                if (match != null) {
                    // YouTube Music results may need stream resolution
                    if (match.hasValidAudio()) {
                        Log.d(TAG, "✓ resolveFullSong: YouTube Music match '${match.title}'")
                        return@withContext match
                    }
                    // Try to resolve the stream
                    val resolved = getAudioStream(match.id.removePrefix("yt_"))
                    if (resolved != null && resolved.hasValidAudio()) {
                        Log.d(TAG, "✓ resolveFullSong: YouTube Music resolved '${match.title}'")
                        return@withContext resolved
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveFullSong: YouTube Music fail: ${e.message}")
        }

        // 3. Try Internet Archive as last resort
        try {
            val iaResults = searchInternetArchive(searchQuery)
            if (iaResults.isNotEmpty()) {
                val match = findBestMatch(iaResults, cleanTitle, cleanArtist)
                if (match != null && match.hasValidAudio()) {
                    Log.d(TAG, "✓ resolveFullSong: Internet Archive match '${match.title}'")
                    return@withContext match
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveFullSong: Internet Archive fail: ${e.message}")
        }

        Log.w(TAG, "✗ resolveFullSong: no full stream found for '$title' by '$artist'")
        null
    }

    /**
     * Find the best matching song from a list by comparing title similarity.
     * Uses simple string containment + Levenshtein-like heuristic.
     */
    private fun findBestMatch(
        songs: List<YouTubeSong>,
        expectedTitle: String,
        expectedArtist: String
    ): YouTubeSong? {
        val targetTitle = expectedTitle.lowercase().trim()
        val targetArtist = expectedArtist.lowercase().trim()

        var bestSong: YouTubeSong? = null
        var bestScore = -1

        for (song in songs) {
            val songTitle = song.title.lowercase().trim()
            val songArtist = song.artist.lowercase().trim()

            var score = 0

            // Title matching
            if (songTitle == targetTitle) {
                score += 100
            } else if (songTitle.contains(targetTitle) || targetTitle.contains(songTitle)) {
                score += 60
            } else {
                // Check word overlap
                val targetWords = targetTitle.split(" ").filter { it.length > 2 }
                val songWords = songTitle.split(" ").filter { it.length > 2 }
                val commonWords = targetWords.intersect(songWords.toSet())
                score += commonWords.size * 10
            }

            // Artist matching (bonus, not required)
            if (targetArtist.isNotBlank() && songArtist.isNotBlank()) {
                if (songArtist.contains(targetArtist) || targetArtist.contains(songArtist)) {
                    score += 30
                }
            }

            // Prefer songs with valid audio
            if (song.hasValidAudio()) score += 5

            if (score > bestScore) {
                bestScore = score
                bestSong = song
            }
        }

        // Only return if we have a reasonable match (score >= 20)
        return if (bestScore >= 20) bestSong else null
    }

    // ═══════════════════════════════════════════════
    // INTERNET ARCHIVE SEARCH (FREE, No API Key!)
    // ═══════════════════════════════════════════════
    private suspend fun searchInternetArchive(query: String): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$IA_SEARCH_URL?q=($encodedQuery)+mediatype:audio&fl[]=identifier,title,creator,runtime&rows=15&output=json"
            val response = httpGet(url, timeout = 15000)
            val json = JSONObject(response)
            val responseObj = json.optJSONObject("response")
            val docs = responseObj?.optJSONArray("docs")

            if (docs != null) {
                for (i in 0 until docs.length()) {
                    try {
                        val doc = docs.getJSONObject(i)
                        val identifier = doc.optString("identifier", "")
                        val title = doc.optString("title", "Unknown")
                        val creator = doc.optString("creator", "Internet Archive")
                        val runtime = doc.optString("runtime", "0").toLongOrNull() ?: 0

                        if (identifier.isNotEmpty()) {
                            // Construct direct audio URL
                            val audioUrl = "https://archive.org/download/$identifier/${identifier}.mp3"
                            val thumbUrl = "https://archive.org/services/img/$identifier"

                            songs.add(YouTubeSong(
                                id = "ia_$identifier",
                                title = title,
                                artist = creator,
                                duration = runtime,
                                thumbnailUrl = thumbUrl,
                                audioUrl = audioUrl
                            ))
                        }
                    } catch (e: Exception) { /* skip */ }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Internet Archive search error: ${e.message}")
        }
        return songs
    }

    // ═══════════════════════════════════════════════
    // LOCAL DATABASE SEARCH
    // ═══════════════════════════════════════════════
    private fun searchLocalDatabase(query: String): List<YouTubeSong> {
        val lowerQuery = query.lowercase()
        return FREE_MUSIC_DATABASE.filter { song ->
            song.title.lowercase().contains(lowerQuery) ||
                    song.artist.lowercase().contains(lowerQuery)
        }.map { free ->
            YouTubeSong(
                id = free.id,
                title = free.title,
                artist = free.artist,
                duration = free.duration,
                thumbnailUrl = free.thumbnailUrl,
                audioUrl = free.audioUrl
            )
        }
    }

    // ═══════════════════════════════════════════════
    // cobalt.tools API
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
    // HTTP helper with robust error handling
    // ═══════════════════════════════════════════════
    private fun httpGet(urlString: String, timeout: Int = 10000): String {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) PulseMusicPlayer/1.0")
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.instanceFollowRedirects = true

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "HTTP error: $responseCode for URL: $urlString")
                throw Exception("HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP GET failed for $urlString: ${e.message}")
            throw e
        }
    }

    // HTTP GET with Authorization header (used for Spotify public token API)
    private fun httpGetWithAuth(urlString: String, authHeader: String, timeout: Int = 10000): String {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) PulseMusicPlayer/1.0")
            conn.setRequestProperty("Authorization", authHeader)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.instanceFollowRedirects = true

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "HTTP(auth) error: $responseCode for URL: $urlString")
                throw Exception("HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP GET(auth) failed for $urlString: ${e.message}")
            throw e
        }
    }

    // HTTP POST with JSON body (used for YouTube innerTube API)
    private fun httpPostJson(urlString: String, jsonBody: String, timeout: Int = 10000): String {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) PulseMusicPlayer/1.0")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.instanceFollowRedirects = true
            conn.doOutput = true

            val outputStream = conn.outputStream
            outputStream.write(jsonBody.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.w(TAG, "HTTP POST error: $responseCode for URL: $urlString")
                throw Exception("HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP POST failed for $urlString: ${e.message}")
            throw e
        }
    }
}
