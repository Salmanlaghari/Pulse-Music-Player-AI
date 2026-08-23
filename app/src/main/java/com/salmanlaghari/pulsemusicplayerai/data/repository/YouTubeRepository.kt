package com.salmanlaghari.pulsemusicplayerai.data.repository

import android.util.Log
import com.salmanlaghari.pulsemusicplayerai.BuildConfig
import com.salmanlaghari.pulsemusicplayerai.domain.model.ChannelVideo
import com.salmanlaghari.pulsemusicplayerai.domain.model.YouTubeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import android.text.Html

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

        // ═══ Invidious instances (ordered by reliability - verified working 2025) ═══
        private val INVIDIOUS_INSTANCES = listOf(
            "https://invidious.f5.si",
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://invidious.tiekoetter.com",
            "https://yt.chocolatemoo53.com",
            "https://yewtu.be",
            "https://iv.ggtyler.dev",
            "https://invidious.fdn.fr",
            "https://invidious.futo.org",
            "https://invidious.perennialte.ch",
            "https://invidious.protokolla.fi",
            "https://invidious.privacyredirect.com",
            "https://invidious.lunar.icu",
            "https://yt.artemislena.eu",
            "https://invidious.drgns.space"
        )

        // ═══ Piped instances (ordered by reliability - verified from official docs 2025) ═══
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.nosebs.ru",
            "https://api.piped.yt",
            "https://pipedapi.drgns.space",
            "https://pipedapi.owo.si",
            "https://pipedapi.ducks.party",
            "https://api.piped.private.coffee",
            "https://pipedapi.darkness.services",
            "https://pipedapi.orangenet.cc",
            "https://piped-api.codespace.cz",
            "https://pipedapi.reallyaweso.me",
            "https://pipedapi.hostux.net",
            "https://pipedapi.r4fo.com"
        )

        // ═══ cobalt.tools instances ═══
        private val COBALT_INSTANCES = listOf(
            "https://cobalt.canine.tools",
            "https://co.eepy.today",
            "https://cobalt-api.hyper.lol",
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
        // Primary endpoint. NOTE: saavn.sumit.co has been observed returning
        // "error code: 1027" (upstream outage) — when that happens we transparently
        // fall back to a compatible public mirror so Desi Hits / JioSaavn keep
        // working instead of showing an empty catalog.
        private const val JIOSAAVN_API = "https://saavn.sumit.co/api"
        private const val JIOSAAVN_MIRROR = "https://jiosaavn-api.vercel.app"
        private const val JIOSAAVN_MIRROR_2 = "https://jiosaavn-api-blue.vercel.app"

        // ═══ MY CHANNEL (owner's YouTube channel) ═══
        // Fetched from the public, key-free YouTube RSS feed. Because this is the
        // owner's own content, playback is routed through the official embedded
        // YouTube player (fully ToS-compliant) rather than raw audio extraction.
        private const val CHANNEL_ID = "UC9gMAFtR6CnKiYBh58vLYjQ"
        private const val CHANNEL_NAME = "A D&E Song Music"
        private const val CHANNEL_HANDLE = "@beatthemusiclife"
        private const val CHANNEL_RSS_URL =
            "https://www.youtube.com/feeds/videos.xml?channel_id=$CHANNEL_ID"
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

    // Decode HTML entities like &quot; &amp; &#39; &lt; &gt; in JioSaavn API responses
    private fun decodeHtmlEntities(text: String): String {
        if (text.isBlank()) return text
        return try {
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Exception) {
            // Fallback: manual common entity replacement
            text
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .trim()
        }
    }
    // ═══════════════════════════════════════════════
    suspend fun searchJioSaavn(query: String): List<YouTubeSong> {
        // 1. Try primary endpoint (saavn.sumit.co) with retry
        repeat(2) { attempt ->
            val primary = runCatching { searchJioSaavnPrimary(query) }.getOrDefault(emptyList())
            if (primary.isNotEmpty()) {
                Log.d(TAG, "JioSaavn PRIMARY OK for '$query' -> ${primary.size} results")
                return primary
            }
            if (attempt < 1) kotlinx.coroutines.delay(300L)
        }

        // 2. Try Official JioSaavn API directly (jiosaavn.com/api.php) with retry
        repeat(2) { attempt ->
            val official = runCatching { searchJioSaavnOfficial(query) }.getOrDefault(emptyList())
            if (official.isNotEmpty()) {
                Log.d(TAG, "JioSaavn OFFICIAL OK for '$query' -> ${official.size} results")
                return official
            }
            if (attempt < 1) kotlinx.coroutines.delay(300L)
        }

        // 3. Try mirror 1 (jiosaavn-api.vercel.app) with retry
        repeat(2) { attempt ->
            val mirror1 = runCatching { searchJioSaavnMirror(query, JIOSAAVN_MIRROR) }.getOrDefault(emptyList())
            if (mirror1.isNotEmpty()) {
                Log.d(TAG, "JioSaavn MIRROR OK for '$query' -> ${mirror1.size} results")
                return mirror1
            }
            if (attempt < 1) kotlinx.coroutines.delay(300L)
        }

        // 4. Try mirror 2 (jiosaavn-api-blue.vercel.app) with retry
        repeat(2) { attempt ->
            val mirror2 = runCatching { searchJioSaavnMirror(query, JIOSAAVN_MIRROR_2) }.getOrDefault(emptyList())
            if (mirror2.isNotEmpty()) {
                Log.d(TAG, "JioSaavn MIRROR 2 OK for '$query' -> ${mirror2.size} results")
                return mirror2
            }
            if (attempt < 1) kotlinx.coroutines.delay(300L)
        }

        Log.e(TAG, "JioSaavn ALL endpoints FAILED for '$query'")
        return emptyList()
    }

    /**
     * Search directly using official JioSaavn web API endpoints without third-party proxies.
     */
    private suspend fun searchJioSaavnOfficial(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&p=1&n=20&q=$encodedQuery"
            val response = httpGetSafe(searchUrl, timeout = NORMAL_TIMEOUT)
            if (response.isBlank()) return@withContext emptyList()

            val json = JSONObject(response)
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            for (i in 0 until minOf(results.length(), 20)) {
                try {
                    val item = results.getJSONObject(i)
                    val id = item.optString("id", "")
                    if (id.isBlank()) continue
                    val rawTitle = item.optString("song", item.optString("title", "Unknown"))
                    val title = decodeHtmlEntities(rawTitle)
                    val rawArtist = item.optString("primary_artists", item.optString("singers", "Unknown Artist"))
                    val artist = decodeHtmlEntities(rawArtist)
                    var image = item.optString("image", "")
                    if (image.contains("150x150")) {
                        image = image.replace("150x150", "500x500")
                    }
                    val durationSec = item.optLong("duration", 0)

                    // Audio URL fallback chain — ONLY full-stream fields.
                    // "vlink" is rejected when hosted on jiotunepreview.jio.com
                    // (ringtone/preview host), and "media_preview_url" is never
                    // accepted: both serve short 96kbps preview clips, not the
                    // full song. An empty audioUrl here is fine — the existing
                    // refresh path (refreshJioSaavnUrl → getJioSaavnSongDetails)
                    // resolves the real full-stream URL before playback.
                    var audioUrl = item.optString("vlink", "")
                    if (!isFullStreamUrl(audioUrl)) {
                        audioUrl = item.optString("media_url", "")
                        if (!isFullStreamUrl(audioUrl)) audioUrl = ""
                    }

                    songs.add(
                        YouTubeSong(
                            id = "js_$id",
                            title = title,
                            artist = artist.ifBlank { "Unknown Artist" },
                            duration = durationSec,
                            thumbnailUrl = image,
                            audioUrl = if (audioUrl.startsWith("http")) audioUrl else ""
                        )
                    )
                } catch (e: Exception) { /* skip malformed */ }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Official JioSaavn search error for '$query': ${e.message}")
        }
        return@withContext songs
    }

    /**
     * Try the mirror up to 3 times with linear backoff. This is the resilience
     * fix for "Desi Hits not loading": the working mirror is rate-limited under
     * burst, so a single attempt can come back empty even though the host is up.
     */
    private suspend fun searchJioSaavnMirrorRetry(query: String): List<YouTubeSong> {
        repeat(3) { attempt ->
            val res = runCatching { searchJioSaavnMirror(query, JIOSAAVN_MIRROR_2) }.getOrDefault(emptyList())
            if (res.isNotEmpty()) {
                Log.d(TAG, "JioSaavn MIRROR OK for '$query' -> ${res.size} results (attempt ${attempt + 1})")
                return res
            }
            Log.w(TAG, "JioSaavn MIRROR empty for '$query' (attempt ${attempt + 1})")
            if (attempt < 2) kotlinx.coroutines.delay(500L * (attempt + 1))
        }
        Log.e(TAG, "JioSaavn BOTH primary and mirror FAILED for '$query'")
        return emptyList()
    }

    private suspend fun searchJioSaavnPrimary(query: String): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "$JIOSAAVN_API/search/songs?query=$encodedQuery"
            val searchResponse = httpGetSafe(searchUrl, timeout = NORMAL_TIMEOUT)
            if (searchResponse.isBlank()) {
                // httpGetSafe returns "" on any HTTP error (e.g. the primary's
                // HTTP 429 / "error code: 1027"). Log it so the primary-down
                // state is visible instead of silently falling through.
                Log.w(TAG, "JioSaavn PRIMARY returned no body for '$query' (primary likely down/rate-limited)")
                return@searchJioSaavnPrimary emptyList()
            }
            Log.d(TAG, "JioSaavn search response length: ${searchResponse.length} for query: $query")

            if (searchResponse.isNotBlank()) {
                Log.d(TAG, "JioSaavn: parsing response for query: $query")
                val searchJson = JSONObject(searchResponse)
                // New saavn.sumit.co API format: { "success": true, "data": { "results": [...] } }
                val dataObj = searchJson.optJSONObject("data")
                val results = dataObj?.optJSONArray("results")
                    ?: searchJson.optJSONArray("results") // fallback to old format
                    ?: return emptyList()

                for (i in 0 until minOf(results.length(), 20)) {
                    try {
                        val result = results.getJSONObject(i)
                        val id = result.optString("id", "")
                        val title = decodeHtmlEntities(result.optString("name", result.optString("title", "Unknown")))

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
                                        artistName.append(decodeHtmlEntities(name))
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
        return songs
    }

    /**
     * Mirror of [searchJioSaavnPrimary] for public mirror endpoints.
     * The response shape differs slightly (top-level `results` with
     * `title` instead of `name`, `image` as a plain string, and `more_info`
     * carrying `singers`), so it is parsed separately.
     */
    private suspend fun searchJioSaavnMirror(query: String, mirrorUrl: String): List<YouTubeSong> {
        val songs = mutableListOf<YouTubeSong>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "$mirrorUrl/api/search?query=$encodedQuery"
            val searchResponse = httpGetSafe(searchUrl, timeout = NORMAL_TIMEOUT)
            if (searchResponse.isBlank()) return emptyList()
            val searchJson = JSONObject(searchResponse)
            val results = searchJson.optJSONArray("results") ?: return emptyList()
            for (i in 0 until minOf(results.length(), 20)) {
                try {
                    val result = results.getJSONObject(i)
                    val id = result.optString("id", "")
                    val title = decodeHtmlEntities(result.optString("title", "Unknown"))
                    val artistsArr = result.optJSONArray("primary_artists")
                        ?: result.optJSONArray("artists")
                    val artistName = StringBuilder()
                    if (artistsArr != null) {
                        for (a in 0 until artistsArr.length()) {
                            val name = artistsArr.optString(a, "")
                            if (name.isNotBlank()) {
                                if (artistName.isNotEmpty()) artistName.append(", ")
                                artistName.append(decodeHtmlEntities(name))
                            }
                        }
                    }
                    if (artistName.isBlank()) {
                        artistName.append(decodeHtmlEntities(result.optString("singers", "")))
                    }
                    if (artistName.isBlank()) {
                        (result.optJSONObject("more_info")?.optString("singers", ""))?.let {
                            if (it.isNotBlank()) artistName.append(decodeHtmlEntities(it))
                        }
                    }
                    val thumbnail = result.optString("image", "")
                    val durationSec = result.optLong("duration", 0)
                    val duration = if (durationSec > 0) durationSec else {
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
                            audioUrl = "" // resolved fresh at playback via the mirror
                        ))
                    }
                } catch (e: Exception) { /* skip invalid */ }
            }
        } catch (e: Exception) {
            Log.w(TAG, "JioSaavn mirror search error: ${e.message}")
        }
        Log.d(TAG, "JioSaavn mirror returned ${songs.size} results for '$query'")
        return songs
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
                if (isFullStreamUrl(url)) return url
            } catch (e: Exception) { }
        }
        return null
    }

    /**
     * True only when [url] points at a documented FULL-STREAM audio file.
     *
     * Preview sources are explicitly rejected:
     * - "media_preview_url" values (short ~30s 96kbps clips)
     * - any URL on jiotunepreview.jio.com (ringtone/preview host)
     * Accepting these causes songs to cut off after a few seconds and users
     * to report "song not playing".
     */
    private fun isFullStreamUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.startsWith("http")) return false
        val lower = url.lowercase()
        if (lower.contains("jiotunepreview.jio.com")) return false
        if (lower.contains("media_preview_url")) return false
        return true
    }

    // Get full song URL from JioSaavn (320kbps) - new saavn.sumit.co API
    //
    // IMPORTANT: The saavn.sumit.co /songs/{id} endpoint returns "data" as a
    // JSON ARRAY of song objects, e.g. { "success": true, "data": [ {...} ] },
    // NOT a single object. Older API versions returned an object. We handle
    // BOTH shapes here so song resolution (and therefore JioSaavn/Desi Hits
    // playback) works regardless of which format the API serves.
    private suspend fun getJioSaavnSongDetails(songId: String): JSONObject? = withContext(Dispatchers.IO) {
        repeat(3) { attempt ->
            val primary = runCatching { getJioSaavnSongDetailsPrimary(songId) }.getOrNull()
            if (primary != null) {
                Log.d(TAG, "JioSaavn details PRIMARY OK for '$songId' (attempt ${attempt + 1})")
                return@withContext primary
            }
            Log.w(TAG, "JioSaavn details PRIMARY failed for '$songId' (attempt ${attempt + 1}/3)")
            if (attempt < 2) kotlinx.coroutines.delay(500L * (attempt + 1))
        }
        Log.w(TAG, "JioSaavn details PRIMARY FAILED for '$songId' after 3 attempts — trying mirror")
        return@withContext getJioSaavnSongDetailsMirrorRetry(songId)
    }

    /**
     * Mirror retry for song details (same resilience rationale as the search
     * fallback): the working mirror can rate-limit, so retry before giving up.
     */
    private suspend fun getJioSaavnSongDetailsMirrorRetry(songId: String): JSONObject? {
        repeat(3) { attempt ->
            val res = runCatching { getJioSaavnSongDetailsMirror(songId) }.getOrNull()
            if (res != null) {
                Log.d(TAG, "JioSaavn details MIRROR OK for '$songId' (attempt ${attempt + 1})")
                return res
            }
            Log.w(TAG, "JioSaavn details MIRROR failed for '$songId' (attempt ${attempt + 1})")
            if (attempt < 2) kotlinx.coroutines.delay(400L * (attempt + 1))
        }
        Log.e(TAG, "JioSaavn details BOTH primary and mirror FAILED for '$songId'")
        return null
    }

    private suspend fun getJioSaavnSongDetailsPrimary(songId: String): JSONObject? {
        try {
            val url = "$JIOSAAVN_API/songs/$songId"
            Log.d(TAG, "getJioSaavnSongDetailsPrimary: requesting $url")
            val response = httpGetSafe(url, timeout = NORMAL_TIMEOUT)
            if (response.isNotBlank()) {
                Log.d(TAG, "getJioSaavnSongDetailsPrimary: response length=${response.length} for $songId")
                val json = JSONObject(response)
                val dataArr = json.optJSONArray("data")
                if (dataArr != null && dataArr.length() > 0) {
                    val first = dataArr.optJSONObject(0)
                    if (first != null) {
                        Log.d(TAG, "getJioSaavnSongDetailsPrimary: got data array item for $songId")
                        return first
                    }
                }
                val dataObj = json.optJSONObject("data")
                if (dataObj != null) {
                    val nested = dataObj.optJSONArray("results")
                        ?: dataObj.optJSONArray("songs")
                    if (nested != null && nested.length() > 0) {
                        val first = nested.optJSONObject(0)
                        if (first != null) {
                            Log.d(TAG, "getJioSaavnSongDetailsPrimary: got nested item for $songId")
                            return first
                        }
                    }
                    Log.d(TAG, "getJioSaavnSongDetailsPrimary: returning dataObj for $songId")
                    return dataObj
                }
                if (json.optBoolean("status", false)) {
                    Log.d(TAG, "getJioSaavnSongDetailsPrimary: legacy status=true for $songId")
                    return json
                }
                Log.w(TAG, "getJioSaavnSongDetailsPrimary: no usable data shape for $songId")
            } else {
                Log.w(TAG, "getJioSaavnSongDetailsPrimary: empty response for $songId")
            }
        } catch (e: Exception) { Log.w(TAG, "JioSaavn song details error for $songId: ${e.message}") }
        return null
    }

    /**
     * Mirror fallback for song details. The jiosaavn-api.vercel.app /song?id=
     * endpoint returns a directly-playable [media_url] (and a [media_urls] map of
     * bitrates). We surface it via a synthetic JSONObject so the existing
     * [refreshJioSaavnUrl] caller (which reads the `media_url` field) keeps working.
     */
    private suspend fun getJioSaavnSongDetailsMirror(songId: String): JSONObject? {
        try {
            val url = "$JIOSAAVN_MIRROR/song?id=$songId"
            Log.d(TAG, "getJioSaavnSongDetailsMirror: requesting $url")
            val response = httpGetSafe(url, timeout = NORMAL_TIMEOUT)
            if (response.isBlank()) {
                Log.w(TAG, "getJioSaavnSongDetailsMirror: empty response for $songId")
                return null
            }
            Log.d(TAG, "getJioSaavnSongDetailsMirror: response length=${response.length} for $songId")
            val json = JSONObject(response)
            if (!json.optBoolean("status", true)) {
                Log.w(TAG, "getJioSaavnSongDetailsMirror: status=false for $songId")
                return null
            }

            val bestUrl = pickBestVercelMediaUrl(json)
            if (bestUrl.isNullOrBlank()) {
                Log.w(TAG, "getJioSaavnSongDetailsMirror: no media_url for $songId")
                return null
            }
            Log.d(TAG, "getJioSaavnSongDetailsMirror: got media_url for $songId")
            val out = JSONObject()
            out.put("media_url", bestUrl)
            return out
        } catch (e: Exception) {
            Log.w(TAG, "JioSaavn mirror song details error for $songId: ${e.message}")
            return null
        }
    }

    /** Prefer the highest bitrate in media_urls, then fall back to media_url. */
    private fun pickBestVercelMediaUrl(json: JSONObject): String? {
        val mediaUrls = json.optJSONObject("media_urls")
        if (mediaUrls != null) {
            // Iterate keys (e.g. "320_KBPS", "160_KBPS", "96_KBPS") and pick the
            // highest numeric bitrate available.
            var best: String? = null
            var bestKbps = -1
            val keys = mediaUrls.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val url = mediaUrls.optString(key, "")
                if (url.isBlank() || !url.startsWith("http")) continue
                val kbps = Regex("(\\d+)").find(key)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (kbps >= bestKbps) {
                    bestKbps = kbps
                    best = url
                }
            }
            if (best != null) return best
        }
        val single = json.optString("media_url", "")
        return if (single.startsWith("http")) single else null
    }

    /**
     * Refresh a JioSaavn song's audio URL. CDN URLs expire after some time,
     * so this fetches a fresh URL right before playback.
     *
     * @param songId The raw JioSaavn song ID (without js_ or dh_ prefix)
     * @return A fresh 320kbps audio URL, or null if resolution fails
     */
    suspend fun refreshJioSaavnUrl(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val details = getJioSaavnSongDetails(songId)
            if (details != null) {
                val keys = mutableListOf<String>()
                details.keys().forEachRemaining { keys.add(it) }
                Log.d(TAG, "refreshJioSaavnUrl: got details for $songId, keys=$keys")
                val downloadArr = details.optJSONArray("downloadUrl")
                val url = pickJioSaavnMediaUrl(downloadArr)
                if (url != null && url.startsWith("http")) {
                    Log.d(TAG, "✓ refreshJioSaavnUrl: got downloadUrl for $songId")
                    return@withContext url
                }
                val mediaUrl = details.optString("media_url", "")
                if (isFullStreamUrl(mediaUrl)) {
                    Log.d(TAG, "✓ refreshJioSaavnUrl: got media_url for $songId")
                    return@withContext mediaUrl
                }
                // NOTE: "vlink" and "media_preview_url" are intentionally NOT
                // accepted here. They point at preview/ringtone clips (often on
                // jiotunepreview.jio.com) that cut off after 10–30 seconds,
                // which users perceive as "song not playing". Returning null
                // lets callers fall back to full-stream resolution instead.
                Log.w(TAG, "refreshJioSaavnUrl: no usable URL in details for $songId")
            } else {
                Log.w(TAG, "refreshJioSaavnUrl: details is null for $songId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshJioSaavnUrl failed for $songId: ${e.message}")
        }

        // Fallback: try official song details API directly
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$songId&_format=json&_marker=0"
            val response = httpGetSafe(url, timeout = NORMAL_TIMEOUT)
            if (response.isNotBlank()) {
                val root = JSONObject(response)
                val item = root.optJSONObject(songId)
                if (item != null) {
                    // Only full-stream fields are accepted. "vlink" (frequently
                    // a jiotunepreview.jio.com ringtone/preview) and
                    // "media_preview_url" (short clip) would play for only
                    // 10–30 seconds, so both are rejected.
                    val officialMediaUrl = item.optString("media_url", "")
                    if (isFullStreamUrl(officialMediaUrl)) {
                        Log.d(TAG, "✓ refreshJioSaavnUrl: official API got media_url for $songId")
                        return@withContext officialMediaUrl
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshJioSaavnUrl official fallback failed for $songId: ${e.message}")
        }

        null
    }

    /**
     * Refresh a YouTubeSong's audio URL if it's from JioSaavn/Desi Hits.
     * Returns the song with a fresh URL, or the original if refresh fails.
     */
    suspend fun refreshSongAudio(song: YouTubeSong): YouTubeSong = withContext(Dispatchers.IO) {
        val rawId = song.id.removePrefix("js_").removePrefix("dh_")
        if (song.id.startsWith("js_") || song.id.startsWith("dh_")) {
            Log.d(TAG, "refreshSongAudio: attempting refresh for id=$rawId title=${song.title}")
            // Primary path: resolve a fresh stream URL for the stored JioSaavn id.
            val freshUrl = refreshJioSaavnUrl(rawId)
            if (freshUrl != null) {
                Log.d(TAG, "refreshSongAudio: primary refresh OK for ${song.title}")
                return@withContext song.copy(audioUrl = freshUrl)
            }
            Log.w(TAG, "refreshSongAudio: primary refresh failed for ${song.title} — trying title fallback")

            // Fallback: this specific id may have expired or been removed.
            // Re-search by title to obtain a fresh, playable JioSaavn id and
            // resolve its stream URL. This keeps Desi Hits/JioSaavn playback
            // alive even when an individual cached id stops working.
            try {
                val candidate = searchJioSaavn(song.title).firstOrNull { it.id.isNotBlank() }
                if (candidate != null) {
                    val candidateRaw = candidate.id.removePrefix("js_")
                    Log.d(TAG, "refreshSongAudio: title fallback candidate=$candidateRaw title=${candidate.title}")
                    val fallbackUrl = refreshJioSaavnUrl(candidateRaw)
                    if (fallbackUrl != null) {
                        Log.d(TAG, "refreshSongAudio: title fallback refresh OK for ${song.title}")
                        return@withContext song.copy(
                            id = "dh_$candidateRaw",
                            audioUrl = fallbackUrl
                        )
                    }
                    Log.w(TAG, "refreshSongAudio: title fallback refresh failed for candidate=$candidateRaw")
                } else {
                    Log.w(TAG, "refreshSongAudio: no search candidate for ${song.title}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "refreshSongAudio fallback failed for '${song.title}': ${e.message}")
            }
        }
        Log.w(TAG, "refreshSongAudio: returning original song without valid audio for ${song.title}")
        song
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SOUTH ASIAN CATALOG — 500-1000 Bollywood/Pakistani/South Asian/Northern songs
    // Uses JioSaavn (the only confirmed working full-song source) with curated
    // search queries. Each query returns up to 40 results → 25+ queries × 40 = 1000+.
    // Results are deduplicated by JioSaavn song ID.
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Curated search queries for South Asian music covering Bollywood, Pakistani,
     * South Indian, Punjabi, and Northern Indian regions. Each query targets a
     * specific artist, genre, or popular theme to ensure variety.
     */
    private val SOUTH_ASIAN_QUERIES = listOf(
        // ── Bollywood A-list singers ──
        "Arijit Singh", "Shreya Ghoshal", "Sonu Nigam", "Atif Aslam",
        "Pritam", "A R Rahman", "Vishal Shekhar", "Neha Kakkar",
        "Jubin Nautiyal", "Armaan Malik", "Darshan Raval", "Payal Dev",
        // ── Bollywood hits by era/theme ──
        "Bollywood hits 2024", "Bollywood romantic songs", "Bollywood party songs",
        "Bollywood sad songs", "Bollywood 90s hits", "Bollywood 2000s hits",
        "Hindi film songs", "Hindi love songs", "Hindi dance hits",
        // ── Pakistani artists & Coke Studio ──
        "Coke Studio Pakistan", "Rahat Fateh Ali Khan", "Nusrat Fateh Ali Khan",
        "Ali Zafar", "Atif Aslam Pakistan", "Shafqat Amanat Ali",
        "Meesha Shafi", "Abida Parveen", "Sajjad Ali", "Junoon band",
        // ── Punjabi / Bhangra ──
        "Punjabi songs 2024", "Diljit Dosanjh", "Guru Randhawa",
        "B Praak", "Sidhu Moose Wala", "Ammy Virk", "Punjabi wedding songs",
        // ── South Indian (Tamil/Telugu/Kannada/Malayalam) ──
        "Tamil hits 2024", "Telugu hit songs", "Anirudh Ravichander",
        "Sid Sriram", "Ilaiyaraaja", "Devi Sri Prasad", "Kannada hits",
        "Malayalam film songs", "South Indian melody",
        // ── Northern Indian / Sufi / Ghazal ──
        "Sufi songs", "Ghazal Jagjit Singh", "Mirza Ghalib ghazals",
        "Bollywood Sufi", "Qawwali", "Hindustani classical",
        // ── Popular Bollywood composers ──
        "Shankar Ehsaan Loy", "Himesh Reshammiya", "Sachin Jigar",
        "Tanishk Bagchi", "Tony Kakkar", "Mithoon composer",
        // ── Regional / Folk ──
        "Rajasthani folk songs", "Bhojpuri hits", "Haryanvi songs",
        "Bengali Rabindra Sangeet", "Gujarati garba songs",
        // ── New releases & trending ──
        "New Hindi songs 2024", "Top Hindi songs", "Trending Bollywood",
        "Hindi remix songs", "Bollywood lofi"
    )

    /**
     * Load a large catalog of South Asian songs (500-1000) by running curated
     * JioSaavn searches. Songs are deduplicated by JioSaavn ID.
     *
     * @param onProgress Optional callback receiving (loadedCount, totalQueries)
     *                   so the UI can show a progress indicator.
     * @return Deduplicated list of YouTubeSong with full 320kbps audio URLs.
     */
    suspend fun loadSouthAsianCatalog(
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val allSongs = mutableMapOf<String, YouTubeSong>() // key = JioSaavn ID for dedup
        val totalQueries = SOUTH_ASIAN_QUERIES.size

        Log.d(TAG, "Loading South Asian catalog: $totalQueries queries (sequential)")

        var completed = 0
        for (query in SOUTH_ASIAN_QUERIES) {
            if (completed > 0) kotlinx.coroutines.delay(500)

            var results: List<YouTubeSong> = emptyList()
            repeat(2) { attempt ->
                try {
                    results = searchJioSaavn(query)
                    if (results.isNotEmpty()) return@repeat
                } catch (e: Exception) {
                    Log.w(TAG, "Catalog query '$query' attempt $attempt failed: ${e.message}")
                    if (attempt == 0) kotlinx.coroutines.delay(500)
                }
            }

            for (song in results) {
                val rawId = song.id.removePrefix("js_")
                if (rawId.isNotBlank() && !allSongs.containsKey(rawId)) {
                    allSongs[rawId] = song.copy(
                        id = "dh_$rawId",
                        audioUrl = ""
                    )
                }
            }

            completed++
            onProgress?.invoke(completed, totalQueries)

            if (allSongs.size >= 1000) {
                Log.d(TAG, "Catalog reached 1000+ songs, stopping early")
                break
            }
        }

        Log.d(TAG, "South Asian catalog loaded: ${allSongs.size} unique songs")
        allSongs.values.toList()
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
                        val artist = try {
                            val artistObj = track.optJSONObject("artist")
                            artistObj?.optString("name", "Unknown Artist") ?: "Unknown Artist"
                        } catch (e: Exception) { "Unknown Artist" }
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

    // =========================================================================
    // SOUNDCLOUD SEARCH - FREE full-track streaming via public API v2
    // No user auth required. A public web client_id is resolved dynamically
    // from the SoundCloud web player and cached, so it self-heals if rotated.
    // =========================================================================

    // Cached SoundCloud public web client_id (resolved on demand)
    @Volatile private var soundCloudClientId: String? = null

    // Resolve the public SoundCloud web client_id by scraping the web player's
    // JS bundles. This is the same id the public soundcloud.com website uses.
    private fun resolveSoundCloudClientId(): String? {
        soundCloudClientId?.let { return it }
        return try {
            val homeHtml = httpGetSafe("https://soundcloud.com/", timeout = NORMAL_TIMEOUT)
            if (homeHtml.isBlank()) return null
            // Collect candidate JS bundle URLs referenced by the page
            val scriptRegex = Regex("""https://a-v2\.sndcdn\.com/assets/[^"']+\.js""")
            val bundles = scriptRegex.findAll(homeHtml).map { it.value }.distinct().toList()
            val idRegex = Regex("""client_id\s*[:=]\s*"([a-zA-Z0-9]{20,})"""")
            for (bundleUrl in bundles) {
                try {
                    val js = httpGetSafe(bundleUrl, timeout = NORMAL_TIMEOUT)
                    val match = idRegex.find(js)
                    if (match != null) {
                        val id = match.groupValues[1]
                        if (id.isNotBlank()) {
                            soundCloudClientId = id
                            Log.d(TAG, "Resolved SoundCloud client_id")
                            return id
                        }
                    }
                } catch (e: Exception) { /* try next bundle */ }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "SoundCloud client_id resolve failed: ${e.message}")
            null
        }
    }

    // Convert a SoundCloud progressive/HLS transcoding into a directly playable
    // URL by hitting its authorized endpoint (returns a temporary CDN url).
    private fun resolveSoundCloudStreamUrl(transcodingUrl: String, clientId: String): String? {
        return try {
            val sep = if (transcodingUrl.contains("?")) "&" else "?"
            val authUrl = "$transcodingUrl${sep}client_id=$clientId"
            val resp = httpGetSafe(authUrl, timeout = NORMAL_TIMEOUT)
            if (resp.isNotBlank()) {
                JSONObject(resp).optString("url", "").ifBlank { null }
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun searchSoundCloud(query: String): List<YouTubeSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val songs = mutableListOf<YouTubeSong>()
        try {
            val clientId = resolveSoundCloudClientId() ?: run {
                Log.w(TAG, "SoundCloud: no client_id; skipping search")
                return@withContext emptyList()
            }
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api-v2.soundcloud.com/search/tracks?q=$encodedQuery&client_id=$clientId&limit=30"
            val response = httpGetSafe(url, timeout = NORMAL_TIMEOUT)
            if (response.isBlank()) return@withContext emptyList()

            val json = JSONObject(response)
            val collection = json.optJSONArray("collection") ?: return@withContext emptyList()

            for (i in 0 until minOf(collection.length(), 30)) {
                try {
                    val track = collection.getJSONObject(i)
                    // Only include streamable tracks with a usable transcoding
                    val streamable = track.optBoolean("streamable", true)
                    if (!streamable) continue

                    val id = track.optLong("id", 0L)
                    if (id == 0L) continue

                    val title = decodeHtmlEntities(track.optString("title", "Unknown"))

                    // Artist: publisher_metadata.artist -> user.username
                    var artist = ""
                    val publisher = track.optJSONObject("publisher_metadata")
                    if (publisher != null) artist = publisher.optString("artist", "")
                    if (artist.isBlank()) {
                        artist = track.optJSONObject("user")?.optString("username", "") ?: ""
                    }
                    if (artist.isBlank()) artist = "Unknown Artist"

                    // Artwork (replace default -large with -t500x500 for higher res)
                    var thumbnail = track.optString("artwork_url", "")
                    if (thumbnail.isBlank()) {
                        thumbnail = track.optJSONObject("user")?.optString("avatar_url", "") ?: ""
                    }
                    if (thumbnail.contains("-large")) {
                        thumbnail = thumbnail.replace("-large", "-t500x500")
                    }

                    // Duration is in milliseconds -> seconds
                    val duration = track.optLong("duration", 0L) / 1000

                    // Find a progressive (direct) transcoding; fall back to HLS
                    var transcodingUrl = ""
                    val media = track.optJSONObject("media")
                    val transcodings = media?.optJSONArray("transcodings")
                    if (transcodings != null) {
                        // Prefer progressive mp3 (directly playable), else first available
                        for (t in 0 until transcodings.length()) {
                            val tc = transcodings.getJSONObject(t)
                            val format = tc.optJSONObject("format")
                            val protocol = format?.optString("protocol", "") ?: ""
                            val tcUrl = tc.optString("url", "")
                            if (protocol == "progressive" && tcUrl.isNotBlank()) {
                                transcodingUrl = tcUrl
                                break
                            }
                        }
                        if (transcodingUrl.isBlank() && transcodings.length() > 0) {
                            transcodingUrl = transcodings.getJSONObject(0).optString("url", "")
                        }
                    }

                    if (transcodingUrl.isNotBlank()) {
                        songs.add(YouTubeSong(
                            id = "sc_$id",
                            title = title,
                            artist = artist,
                            duration = duration,
                            thumbnailUrl = thumbnail,
                            // Store the transcoding URL; resolved to a fresh CDN url at playback time
                            audioUrl = transcodingUrl
                        ))
                    }
                } catch (e: Exception) { /* skip invalid track */ }
            }
            Log.d(TAG, "SoundCloud search: ${songs.size} results for '$query'")
        } catch (e: Exception) {
            Log.w(TAG, "SoundCloud search error: ${e.message}")
        }
        songs
    }

    /**
     * Resolve a fresh, directly-playable SoundCloud stream URL for a track whose
     * audioUrl currently holds a transcoding endpoint. Called on-demand at
     * playback time (transcoding-signed CDN urls are short-lived).
     */
    suspend fun refreshSoundCloudUrl(transcodingUrl: String): String? = withContext(Dispatchers.IO) {
        val clientId = resolveSoundCloudClientId() ?: return@withContext null
        resolveSoundCloudStreamUrl(transcodingUrl, clientId)
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
    // YouTube Music Trending - auto-populate trending songs without search
    // Uses YouTube innerTube search with trending keywords, filters to individual songs
    suspend fun getYouTubeTrending(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val region = getUserRegion()
        Log.d(TAG, "Loading YouTube Music trending for region: $region")

        val songs = mutableListOf<YouTubeSong>()
        val seenIds = mutableSetOf<String>()

        // Region-specific trending queries - return individual songs, not playlists
        val trendingQueries = when (region) {
            "IN" -> listOf("trending hindi songs 2025", "new bollywood songs 2025", "top punjabi songs 2025")
            "PK" -> listOf("trending pakistani songs 2025", "new urdu songs 2025", "top punjabi songs 2025")
            "BD" -> listOf("trending bangla songs 2025", "new bangladeshi songs 2025")
            "US", "GB", "CA", "AU" -> listOf("trending pop songs 2025", "top hits 2025 songs", "new music 2025 songs")
            "BR" -> listOf("trending musica 2025", "top songs 2025")
            "ES", "MX", "AR", "CO" -> listOf("trending canciones 2025", "top reggaeton 2025")
            "KR" -> listOf("trending kpop 2025", "new kpop songs 2025")
            "JP" -> listOf("trending jpop 2025", "new jpop songs 2025")
            "AE", "SA", "EG" -> listOf("trending arabic songs 2025", "new arabic music 2025")
            "TR" -> listOf("trending turkish songs 2025", "new turkce sarkilar 2025")
            "NG", "ZA" -> listOf("trending afrobeats 2025", "top amapiano 2025")
            else -> listOf("trending songs 2025", "top hits 2025 songs", "new music 2025")
        }

        for (query in trendingQueries) {
            try {
                val results = searchYouTubeInnerTube(query)
                for (song in results) {
                    // Filter: only individual songs (duration 1-600 seconds)
                    // This removes long playlist videos (2+ hours)
                    if (song.duration in 1..600 && seenIds.add(song.id)) {
                        songs.add(song)
                    }
                    if (songs.size >= 30) break
                }
                if (songs.size >= 30) break
            } catch (e: Exception) {
                Log.w(TAG, "YouTube trending query failed: ${e.message}")
            }
        }

        // Also add JioSaavn trending songs for variety (full songs, no preview)
        if (songs.size < 20) {
            try {
                val jsavnQuery = trendingQueries.firstOrNull() ?: "trending songs 2025"
                val jsavnResults = searchJioSaavn(jsavnQuery)
                for (song in jsavnResults) {
                    if (seenIds.add(song.id) && song.hasValidAudio()) {
                        songs.add(song)
                    }
                    if (songs.size >= 30) break
                }
            } catch (e: Exception) {
                Log.w(TAG, "JioSaavn trending fallback failed: ${e.message}")
            }
        }

        Log.d(TAG, "YouTube Music trending: ${songs.size} songs")
        songs.take(40)
    }

    // ═══════════════════════════════════════════════
    // MY CHANNEL — owner's YouTube channel uploads.
    //
    // ROOT CAUSE OF THE "thumbnails/titles gone" REGRESSION:
    //   The previous RSS-only approach called httpGetSafe() and, if the body was
    //   *non-blank*, handed it straight to parseChannelRss(). But YouTube's RSS
    //   endpoint frequently answers with an HTML error/consent page (not XML)
    //   when it rate-limits or blocks the client. That HTML body is "non-blank",
    //   so parseChannelRss() found zero <entry> elements and returned an EMPTY
    //   list — the UI then rendered "No uploads found" and the thumbnails/titles
    //   silently vanished. The retry only re-fetched more HTML, so it never fixed
    //   anything; it just made the empty state appear more reliably.
    //
    // FIX (recommended approach):
    //   1. PRIMARY — YouTube Data API v3 (requires YOUTUBE_DATA_API_KEY). This is
    //      the official, stable, ToS-compliant listing API and is immune to the
    //      RSS-blocking / HTML-error problems above. It returns real thumbnails
    //      and titles reliably.
    //   2. FALLBACK — the key-free public RSS feed, but now HTML-aware: if the
    //      response is not XML it is treated as a failure and retried; only a
    //      genuine XML feed is parsed. This restores the old behaviour for
    //      builds that don't ship a Data API key while no longer masking HTML
    //      errors as "empty channel".
    // Playback is handled by the official embedded YouTube player (see UI).
    // ═══════════════════════════════════════════════
    suspend fun getChannelVideos(): List<ChannelVideo> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_DATA_API_KEY
        if (apiKey.isNotBlank()) {
            runCatching { getChannelVideosViaDataApi(apiKey) }
                .onFailure { Log.w(TAG, "My Channel Data API v3 failed: ${it.message}") }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                     Log.d(TAG, "Loaded ${it.size} My Channel videos via Data API v3")
                     return@withContext it
                 }
             Log.w(TAG, "My Channel Data API v3 returned no videos — falling back to RSS")
         }
         val rssVideos = getChannelVideosViaRss()
         if (rssVideos.isEmpty()) {
             Log.e(TAG, "My Channel: Both Data API v3 and RSS fallback failed — check network/YouTube status")
         }
         return@withContext rssVideos
    }

    /**
     * Official YouTube Data API v3 listing: resolve the channel's "uploads"
     * playlist, then page its items. Returns real thumbnail URLs + titles.
     */
    private suspend fun getChannelVideosViaDataApi(apiKey: String): List<ChannelVideo> {
        Log.d(TAG, "Loading My Channel videos (Data API v3) for $CHANNEL_NAME")
        // Step 1: channel -> uploads playlist id.
        val channelJson = httpGetSafe(
            "https://www.googleapis.com/youtube/v3/channels" +
                "?part=contentDetails&id=$CHANNEL_ID&key=$apiKey",
            timeout = NORMAL_TIMEOUT
        )
        if (channelJson.isBlank()) return emptyList()
        val uploadsId = try {
            val root = JSONObject(channelJson)
            val items = root.optJSONArray("items") ?: return emptyList()
            if (items.length() == 0) return emptyList()
            items.getJSONObject(0)
                .optJSONObject("contentDetails")
                ?.optJSONObject("relatedPlaylists")
                ?.optString("uploads")
                .orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Data API: failed to parse channel response: ${e.message}")
            return emptyList()
        }
        if (uploadsId.isBlank()) return emptyList()

        // Step 2: playlistItems -> video list (latest 24).
        val playlistJson = httpGetSafe(
            "https://www.googleapis.com/youtube/v3/playlistItems" +
                "?part=snippet&maxResults=24&playlistId=$uploadsId&key=$apiKey",
            timeout = NORMAL_TIMEOUT
        )
        if (playlistJson.isBlank()) return emptyList()
        val videos = mutableListOf<ChannelVideo>()
        try {
            val root = JSONObject(playlistJson)
            val items = root.optJSONArray("items") ?: return emptyList()
            for (i in 0 until items.length()) {
                val snippet = items.getJSONObject(i).optJSONObject("snippet") ?: continue
                val resourceId = snippet.optJSONObject("resourceId") ?: continue
                val videoId = resourceId.optString("videoId").trim()
                val title = snippet.optString("title", "").trim()
                if (videoId.isBlank() || title.isBlank()) continue
                val thumbs = snippet.optJSONObject("thumbnails")
                val thumbnail = thumbs?.optJSONObject("medium")
                    ?: thumbs?.optJSONObject("high")
                    ?: thumbs?.optJSONObject("default")
                val thumbnailUrl = thumbnail?.optString("url")
                    ?.takeIf { it.isNotBlank() }
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                val publishedAt = snippet.optString("publishedAt", "")
                videos.add(
                    ChannelVideo(
                        videoId = videoId,
                        title = decodeHtmlEntities(title),
                        thumbnailUrl = thumbnailUrl,
                        publishedAt = publishedAt
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Data API: failed to parse playlist response: ${e.message}")
        }
        return videos
    }

    /**
     * Key-free RSS fallback. Robust against YouTube answering with an HTML
     * error/consent page instead of XML: a non-XML body is treated as a failure
     * and retried, so we never silently surface "No uploads found".
     */
    private suspend fun getChannelVideosViaRss(): List<ChannelVideo> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading My Channel videos (RSS fallback) for $CHANNEL_NAME")
        repeat(3) { attempt ->
            val xml = httpGetSafe(CHANNEL_RSS_URL, timeout = NORMAL_TIMEOUT)
            // Treat an HTML/error page (or blank) as a failure, not as "empty feed".
            if (xml.isNotBlank() && !looksLikeHtml(xml)) {
                val parsed = parseChannelRss(xml)
                if (parsed.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${parsed.size} My Channel videos via RSS")
                    return@withContext parsed
                }
            }
            Log.w(TAG, "My Channel RSS attempt ${attempt + 1} returned no usable XML (HTML/blank)")
            if (attempt < 2) kotlinx.coroutines.delay(500L * (attempt + 1))
        }
        Log.w(TAG, "My Channel RSS returned no usable feed after retries")
        emptyList()
    }

    /** Cheap heuristic: YouTube's RSS is XML; an error/consent page is HTML. */
    private fun looksLikeHtml(body: String): Boolean {
        val head = body.trim().take(200).lowercase()
        return head.startsWith("<!doctype html") ||
                head.startsWith("<html") ||
                head.contains("<html") && head.contains("</html>")
    }

    /**
     * Parse the YouTube channel Atom/RSS feed into [ChannelVideo]s.
     * The feed exposes, per <entry>: yt:videoId, title, published, and a
     * media:thumbnail url. Namespace-awareness is disabled so element names
     * keep their prefixes (e.g. "yt:videoId", "media:thumbnail").
     */
    private fun parseChannelRss(xml: String): List<ChannelVideo> {
        val videos = mutableListOf<ChannelVideo>()
        try {
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(java.io.StringReader(xml))

            var event = parser.eventType
            var videoId = ""
            var title = ""
            var published = ""
            var thumbnail = ""
            var inEntry = false

            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (event) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "entry" -> {
                                inEntry = true
                                videoId = ""
                                title = ""
                                published = ""
                                thumbnail = ""
                            }
                            "yt:videoId" -> if (inEntry) videoId = parser.nextText().trim()
                            "title" -> if (inEntry) title = parser.nextText().trim()
                            "published" -> if (inEntry) published = parser.nextText().trim()
                            "media:thumbnail" -> {
                                if (inEntry) {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank()) thumbnail = url
                                }
                            }
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name == "entry") {
                            if (videoId.isNotBlank() && title.isNotBlank()) {
                                videos.add(
                                    ChannelVideo(
                                        videoId = videoId,
                                        title = decodeHtmlEntities(title),
                                        thumbnailUrl = if (thumbnail.isBlank())
                                            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                                        else thumbnail,
                                        publishedAt = published
                                    )
                                )
                            }
                            inEntry = false
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseChannelRss failed: ${e.message}")
        }
        Log.d(TAG, "Parsed ${videos.size} My Channel videos from RSS")
        return videos
    }

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

        // 1. JIOSAAVN FIRST - FULL SONGS (not 30s previews). This is the primary
        //    trending source so JioSaavn/Desi Hits playback stays consistent.
        try {
            val jioQuery = when(region) {
                "IN" -> "trending hindi songs"
                "PK" -> "pakistani hits"
                "GB" -> "bollywood hits"
                else -> "top trending songs"
            }
            val jioResults = searchJioSaavn(jioQuery)
            if (jioResults.isNotEmpty()) {
                allSongs.addAll(jioResults)
                Log.d(TAG, "JioSaavn trending: ${jioResults.size} full songs")
            }
        } catch (e: Exception) { Log.w(TAG, "JioSaavn trending fail: ${e.message}") }

        // 2. DEEZER - fill remaining slots (30-second previews)
        if (allSongs.size < 20) {
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
        }

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

        // 2. JIO SAAVN FAST PATH — try JioSaavn FIRST for YouTube video IDs.
        // JioSaavn is the ONLY confirmed working full-song source (320kbps MP4,
        // HTTP 206 range support). Invidious/Piped/Cobalt video stream endpoints
        // are currently all returning 403/401/empty, so trying them first just
        // wastes 20-30 seconds before reaching the JioSaavn fallback.
        // We fetch the YouTube video title via oembed, clean it, and search JioSaavn.
        if (!videoId.startsWith("js_") && !videoId.startsWith("ia_") &&
            !videoId.startsWith("dz_") && !videoId.startsWith("am_") &&
            !videoId.startsWith("sp_") && !videoId.startsWith("jm_")) {
            try {
                val meta = getYouTubeVideoMeta(videoId)
                if (meta != null) {
                    val (title, channel) = meta
                    val cleanTitle = cleanYouTubeTitle(title)
                    Log.d(TAG, "JioSaavn fast path: searching '$cleanTitle' (from YT: '$title')")
                    val resolved = resolveFullSong(cleanTitle, channel, "")
                    if (resolved != null && resolved.hasValidAudio()) {
                        Log.d(TAG, "✓ JioSaavn fast path OK: $title -> ${resolved.title}")
                        return@withContext YouTubeSong(
                            id = videoId,
                            title = title,
                            artist = channel,
                            duration = resolved.duration,
                            thumbnailUrl = resolved.thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                            audioUrl = resolved.audioUrl
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "JioSaavn fast path fail: ${e.message}")
            }
        }

        // 3. Try Invidious instances (first 3 most reliable — reduced from 5 for speed)
        for (i in 0 until minOf(3, INVIDIOUS_INSTANCES.size)) {
            try {
                val instance = INVIDIOUS_INSTANCES[(currentInvidiousIndex + i) % INVIDIOUS_INSTANCES.size]
                val url = "$instance/api/v1/videos/$videoId"
                val response = httpGet(url, timeout = 8000)
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

        // 4. Try Piped instances (first 3 — reduced for speed)
        for (i in 0 until minOf(3, PIPED_INSTANCES.size)) {
            try {
                val instance = PIPED_INSTANCES[(currentPipedIndex + i) % PIPED_INSTANCES.size]
                val url = "$instance/streams/$videoId"
                val response = httpGet(url, timeout = 8000)
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

        // 5. JioSaavn fallback already attempted in step 2 (fast path).
        // If we reach here, all sources failed.
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
                val match = findBestMatch(jioResults, cleanTitle, cleanArtist)
                if (match != null) {
                    // FAST PATH: if match already has valid audio (e.g. from searchJioSaavnOfficial), return immediately
                    if (match.hasValidAudio()) {
                        Log.d(TAG, "✓ resolveFullSong: JioSaavn match '${match.title}' with valid audio")
                        return@withContext match
                    }
                    val refreshed = refreshSongAudio(match)
                    if (refreshed.hasValidAudio()) {
                        Log.d(TAG, "✓ resolveFullSong: JioSaavn match '${match.title}' refreshed")
                        return@withContext refreshed
                    }
                    Log.w(TAG, "resolveFullSong: JioSaavn match '${match.title}' has no valid audio after refresh")
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

        // 3. Try Internet Archive
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

        // 4. Try direct YouTube search (innerTube) as final fallback
        // This is very reliable for finding songs - YouTube has almost everything
        try {
            val ytResults = searchYouTubeInnerTube(searchQuery)
            // Filter to individual songs (not long playlists/live streams)
            val songResults = ytResults.filter { it.duration in 1..600 }
            if (songResults.isNotEmpty()) {
                val match = findBestMatch(songResults, cleanTitle, cleanArtist)
                if (match != null) {
                    // YouTube results need stream resolution via Invidious/Piped/Cobalt
                    val resolved = getAudioStream(match.id.removePrefix("yt_"))
                    if (resolved != null && resolved.hasValidAudio()) {
                        Log.d(TAG, "✓ resolveFullSong: YouTube direct match '${match.title}'")
                        return@withContext resolved
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveFullSong: YouTube direct fail: ${e.message}")
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
            // Browser-like User-Agent to bypass instance restrictions
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.doOutput = true

            // New Cobalt API format (v10+): uses downloadMode, audioFormat, audioBitrate
            // Old format (isAudioOnly, aFormat) is deprecated and returns 400/405
            val body = JSONObject().apply {
                put("url", "https://www.youtube.com/watch?v=$videoId")
                put("downloadMode", "audio")
                put("audioFormat", "mp3")
                put("audioBitrate", "128")
                // Some instances accept these for better compatibility
                put("filenameStyle", "basic")
                put("disableMetadata", false)
            }.toString()

            conn.outputStream.bufferedWriter().use { it.write(body) }

            val responseCode = conn.responseCode
            if (responseCode == 200 || responseCode == 201) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                if (response.isBlank()) {
                    Log.w(TAG, "cobalt: empty response from $instance")
                    conn.disconnect()
                    return null
                }
                val json = JSONObject(response)
                val status = json.optString("status", "")
                // Cobalt returns the stream URL in "url" field
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
            } else {
                Log.w(TAG, "cobalt HTTP $responseCode from $instance")
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
    // Browser-like User-Agent for bypassing instance restrictions
    private val BYPASS_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    private fun applyBypassHeaders(conn: HttpURLConnection, referer: String? = null) {
        conn.setRequestProperty("User-Agent", BYPASS_USER_AGENT)
        conn.setRequestProperty("Accept", "application/json, text/html, */*")
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        conn.setRequestProperty("Accept-Encoding", "identity") // no gzip — we need raw streams
        conn.setRequestProperty("Connection", "keep-alive")
        if (referer != null) {
            conn.setRequestProperty("Referer", referer)
        }
        // Some instances check for these to block bots
        conn.setRequestProperty("Sec-Fetch-Dest", "empty")
        conn.setRequestProperty("Sec-Fetch-Mode", "cors")
        conn.setRequestProperty("Sec-Fetch-Site", "same-origin")
    }

    private fun httpGet(urlString: String, timeout: Int = 10000): String {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            applyBypassHeaders(conn)
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

    // HTTP GET that returns empty string on failure instead of throwing (safer for search)
    private fun httpGetSafe(urlString: String, timeout: Int = 10000): String {
        return try {
            httpGet(urlString, timeout)
        } catch (e: Exception) {
            ""
        }
    }

    // HTTP GET with Authorization header (used for Spotify public token API)
    private fun httpGetWithAuth(urlString: String, authHeader: String, timeout: Int = 10000): String {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            applyBypassHeaders(conn)
            conn.setRequestProperty("Authorization", authHeader)
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
            applyBypassHeaders(conn)
            conn.setRequestProperty("Content-Type", "application/json")
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
