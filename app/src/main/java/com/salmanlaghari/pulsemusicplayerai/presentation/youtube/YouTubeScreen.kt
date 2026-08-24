package com.salmanlaghari.pulsemusicplayerai.presentation.youtube

import java.util.Locale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import java.text.SimpleDateFormat
import com.salmanlaghari.pulsemusicplayerai.domain.model.ChannelVideo
import com.salmanlaghari.pulsemusicplayerai.domain.model.YouTubeSong
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdMobBanner
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager
import com.salmanlaghari.pulsemusicplayerai.common.PulseBranding
import com.salmanlaghari.pulsemusicplayerai.common.premiumPressScale
import com.salmanlaghari.pulsemusicplayerai.theme.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch

enum class MusicSource { ALL, JIOSAAVN, DESI_HITS, APPLE_MUSIC, PAGAL_WORLD, YOUTUBE_MUSIC, SOUNDCLOUD, ARCHIVE, DEEZER, MY_CHANNEL }
enum class ViewMode { LIST, GRID }

@Composable
fun YouTubeScreen(
    viewModel: YouTubeViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToChannelPlayer: (videoId: String, title: String) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isTrendingLoading by viewModel.isTrendingLoading.collectAsState()
    val youTubeTrending by viewModel.youTubeTrending.collectAsState()
    val isYouTubeTrendingLoading by viewModel.isYouTubeTrendingLoading.collectAsState()
    val southAsianSongs by viewModel.southAsianSongs.collectAsState()
    val isSouthAsianLoading by viewModel.isSouthAsianLoading.collectAsState()
    val southAsianProgress by viewModel.southAsianProgress.collectAsState()
    val southAsianLoadedAtMs by viewModel.southAsianLoadedAtMs.collectAsState()
    val hasNewSouthAsianContent by viewModel.hasNewSouthAsianContent.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPlayLoading by viewModel.isPlayLoading.collectAsState()
    val playLoadingMessage by viewModel.playLoadingMessage.collectAsState()
    val channelVideos by viewModel.channelVideos.collectAsState()
    val isChannelLoading by viewModel.isChannelLoading.collectAsState()
    val channelError by viewModel.channelError.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf(MusicSource.MY_CHANNEL) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }

    // Auto-load YouTube trending when YouTube Music tab is selected (no search needed)
    androidx.compose.runtime.LaunchedEffect(selectedSource) {
        if (selectedSource == MusicSource.YOUTUBE_MUSIC) {
            viewModel.loadYouTubeTrending()
        }
        if (selectedSource == MusicSource.DESI_HITS) {
            viewModel.syncSouthAsianCatalog()
        }
        if (selectedSource == MusicSource.MY_CHANNEL) {
            viewModel.loadChannelVideos()
        }
    }

    // Helper function to play song and navigate
    fun playAndNavigate(song: YouTubeSong, queue: List<YouTubeSong>) {
        scope.launch {
            val success = viewModel.playSong(song, queue)
            if (success) {
                onNavigateToPlayer()
            }
        }
    }

    // Search with selected source — routes to the correct platform or aggregated sync
    fun searchWithSource(query: String) {
        when (selectedSource) {
            MusicSource.ALL -> viewModel.searchAllSources(query)
            MusicSource.JIOSAAVN -> viewModel.searchJioSaavn(query)
            MusicSource.DESI_HITS -> viewModel.searchDesiHits(query)
            MusicSource.APPLE_MUSIC -> viewModel.searchAppleMusic(query)
            MusicSource.PAGAL_WORLD -> viewModel.searchPagalWorld(query)
            MusicSource.YOUTUBE_MUSIC -> viewModel.searchYouTubeMusic(query)
            MusicSource.SOUNDCLOUD -> viewModel.searchSoundCloud(query)
            MusicSource.ARCHIVE -> viewModel.search(query)
            MusicSource.DEEZER -> viewModel.search(query)
            MusicSource.MY_CHANNEL -> { /* My Channel shows uploads directly; no cross-source search */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BaseDeepNavy, BaseNavyBlue, BaseNavyBlue)
                )
            )
    ) {
        // Loading overlay when resolving audio
        if (isPlayLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanGlow, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = playLoadingMessage.ifBlank { "Loading..." },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (playLoadingMessage.startsWith("Finding")) "Searching all music sources" else "Preparing your track",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Region Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YouTube Music",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    // Region indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎵 Deezer + Archive",
                            fontSize = 12.sp,
                            color = CyanGlow
                        )
                        Text(
                            text = " • ",
                            fontSize = 12.sp,
                            color = TextDim
                        )
                        Text(
                            text = getRegionFlag(Locale.getDefault().country),
                            fontSize = 12.sp
                        )
                        Text(
                            text = " ${getRegionName(Locale.getDefault().country)}",
                            fontSize = 12.sp,
                            color = TextDim
                        )
                    }
                }
                if (currentlyPlaying != null) {
                    Card(
                        onClick = onNavigateToPlayer,
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PurplePrimary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Now Playing",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    // The My Channel tab shows the owner's uploads directly, so the
                    // global search box is ignored there (no cross-source search).
                    if (selectedSource == MusicSource.MY_CHANNEL) {
                        searchQuery = q
                        isSearchActive = false
                    } else {
                        searchQuery = q
                        if (q.length >= 3) {
                            searchWithSource(q)
                            isSearchActive = true
                        } else if (q.isEmpty()) {
                            viewModel.clearSearch()
                            isSearchActive = false
                        }
                    }
                },
                placeholder = {
                    Text(
                        "Search songs, artists...",
                        color = TextDim,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = CyanGlow
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.clearSearch()
                            isSearchActive = false
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextDim
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassBg,
                    unfocusedContainerColor = GlassBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyanGlow
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Source Selection Tabs — scrollable to fit all synced platforms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Tabs (scrollable) — "My Channel" first (default), then "Desi Hits"
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        SourceChip(
                            text = "📺 My Channel",
                            selected = selectedSource == MusicSource.MY_CHANNEL,
                            onClick = {
                                selectedSource = MusicSource.MY_CHANNEL
                                viewModel.clearSearch()
                                isSearchActive = false
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "🎭 Desi Hits",
                            selected = selectedSource == MusicSource.DESI_HITS,
                            onClick = {
                                selectedSource = MusicSource.DESI_HITS
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "🎶 JioSaavn",
                            selected = selectedSource == MusicSource.JIOSAAVN,
                            onClick = {
                                selectedSource = MusicSource.JIOSAAVN
                                if (searchQuery.length >= 3) searchWithSource(searchQuery)
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "▶ YouTube Music",
                            selected = selectedSource == MusicSource.YOUTUBE_MUSIC,
                            onClick = {
                                selectedSource = MusicSource.YOUTUBE_MUSIC
                                if (searchQuery.length >= 3) searchWithSource(searchQuery)
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "🍎 Apple Music",
                            selected = selectedSource == MusicSource.APPLE_MUSIC,
                            onClick = {
                                selectedSource = MusicSource.APPLE_MUSIC
                                if (searchQuery.length >= 3) searchWithSource(searchQuery)
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "🎧 PagalWorld",
                            selected = selectedSource == MusicSource.PAGAL_WORLD,
                            onClick = {
                                selectedSource = MusicSource.PAGAL_WORLD
                                if (searchQuery.length >= 3) searchWithSource(searchQuery)
                                else viewModel.loadPagalWorldLatest() // Fresh Picks: newest uploads
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "☁ SoundCloud",
                            selected = selectedSource == MusicSource.SOUNDCLOUD,
                            onClick = {
                                selectedSource = MusicSource.SOUNDCLOUD
                                if (searchQuery.length >= 3) searchWithSource(searchQuery)
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "🌐 All",
                            selected = selectedSource == MusicSource.ALL,
                            onClick = {
                                selectedSource = MusicSource.ALL
                                if (searchQuery.length >= 3) searchWithSource(searchQuery)
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "🌐 Archive",
                            selected = selectedSource == MusicSource.ARCHIVE,
                            onClick = {
                                selectedSource = MusicSource.ARCHIVE
                                if (searchQuery.length >= 3) viewModel.search(searchQuery)
                            }
                        )
                    }
                    item {
                        SourceChip(
                            text = "🎵 Deezer",
                            selected = selectedSource == MusicSource.DEEZER,
                            onClick = {
                                selectedSource = MusicSource.DEEZER
                                if (searchQuery.length >= 3) searchWithSource(searchQuery)
                            }
                        )
                    }
                    item {
                        if (selectedSource == MusicSource.MY_CHANNEL) {
                            IconButton(onClick = { viewModel.loadChannelVideos(force = true) }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh channel",
                                    tint = CyanGlow
                                )
                            }
                        }
                    }
                }
                
                // View Mode Toggle
                IconButton(
                    onClick = { 
                        viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST 
                    }
                ) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Toggle View",
                        tint = CyanGlow
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isSearchActive) {
                // Search Results
                Text(
                    text = "SEARCH RESULTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyanGlow)
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found",
                            color = TextDim,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(searchResults) { index, song ->
                            YouTubeSongCard(
                                song = song,
                                isCurrentlyPlaying = currentlyPlaying?.id == song.id,
                                onClick = { playAndNavigate(song, searchResults) }
                            )
                        }
                    }
                }
            } else if (selectedSource == MusicSource.YOUTUBE_MUSIC && searchQuery.isBlank()) {
                // YouTube Music tab - show trending songs without requiring search
                Text(
                    text = "YOUTUBE MUSIC - TRENDING NOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isYouTubeTrendingLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CyanGlow)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading trending songs...",
                                color = TextDim,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (youTubeTrending.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No trending songs available",
                                color = TextDim,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.loadYouTubeTrending() }
                            ) {
                                Text(
                                    text = "Retry",
                                    color = CyanGlow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(youTubeTrending) { index, song ->
                            YouTubeSongCard(
                                song = song,
                                isCurrentlyPlaying = currentlyPlaying?.id == song.id,
                                onClick = { playAndNavigate(song, youTubeTrending) }
                            )
                        }
                    }
                }
            } else if (selectedSource == MusicSource.MY_CHANNEL) {
                // My Channel tab — owner's YouTube uploads via the official player
                Text(
                    text = "MY CHANNEL — A D&E SONG MUSIC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isChannelLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyanGlow)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading channel uploads...",
                                color = TextDim,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (channelError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📺", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = channelError ?: "Something went wrong",
                                color = TextDim,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.loadChannelVideos(force = true) }
                            ) {
                                Text(
                                    text = "Retry",
                                    color = CyanGlow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (channelVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No uploads found on this channel yet.",
                            color = TextDim,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(channelVideos) { index, video ->
                            ChannelVideoCard(
                                video = video,
                                onClick = {
                                    onNavigateToChannelPlayer(video.videoId, video.title)
                                }
                            )
                        }
                    }
                }
            } else if (selectedSource == MusicSource.DESI_HITS && searchQuery.isBlank()) {
                // Desi Hits tab — Bollywood / Pakistani / South Asian / Northern songs catalog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DESI HITS — BOLLYWOOD & SOUTH ASIAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary,
                            letterSpacing = 1.5.sp
                        )
                        if (southAsianLoadedAtMs > 0 && !isSouthAsianLoading) {
                            val lastSync = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(southAsianLoadedAtMs))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (hasNewSouthAsianContent) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Green, CircleShape)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.Green
                                )
                                Text(
                                    text = "Synced at $lastSync",
                                    fontSize = 10.sp,
                                    color = Color.Green,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.loadSouthAsianCatalog(force = true) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Desi Hits",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isSouthAsianLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CyanGlow)
                            Spacer(modifier = Modifier.height(12.dp))
                            val (completed, total) = southAsianProgress
                            val progressText = if (total > 0) {
                                "Loading $completed / $total playlists…"
                            } else {
                                "Loading Desi Hits catalog…"
                            }
                            Text(
                                text = progressText,
                                color = TextDim,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Fetching Bollywood, Pakistani, Punjabi & South Indian songs",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (southAsianSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎭",
                                fontSize = 40.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Desi Hits available right now",
                                color = TextDim,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.loadSouthAsianCatalog() }
                            ) {
                                Text(
                                    text = "Retry",
                                    color = CyanGlow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Column {
                        Text(
                            text = "${southAsianSongs.size} songs loaded • Tap to play",
                            color = TextDim,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = southAsianSongs.size,
                                key = { index -> southAsianSongs[index].id }
                            ) { index ->
                                val song = southAsianSongs[index]
                                YouTubeSongCard(
                                    song = song,
                                    isCurrentlyPlaying = currentlyPlaying?.id == song.id,
                                    onClick = { playAndNavigate(song, southAsianSongs) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Library-style list (no Top Picks cards — just a clean song list)
                Text(
                    text = "TRENDING NOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isTrendingLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyanGlow)
                    }
                } else if (trendingSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🎵",
                                fontSize = 40.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No trending songs available",
                                color = TextDim,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadTrending() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PurplePrimary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                } else {
                    // Clean library-style list — all songs in a single list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(trendingSongs) { index, song ->
                            YouTubeSongCard(
                                song = song,
                                isCurrentlyPlaying = currentlyPlaying?.id == song.id,
                                onClick = { playAndNavigate(song, trendingSongs) }
                            )
                        }
                    }
                }
            }

            // AdMob Banner at bottom of YouTube
            AdMobBanner(
                adUnitId = AdManager.getBannerLibraryId(),
                modifier = Modifier.fillMaxWidth()
            )

            // Pulse branding footer
            PulseBranding(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun YouTubeSongCard(
    song: YouTubeSong,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                PurplePrimary.copy(alpha = 0.25f)
            else GlassBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardNavy),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyPlaying) CyanGlow else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${formatDuration(song.duration)}",
                    fontSize = 12.sp,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Source badge — shows which platform this song is from
            Text(
                text = song.sourceType,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextDim,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardNavy)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

@Composable
fun ChannelVideoCard(
    video: ChannelVideo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardNavy),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Small play overlay to signal "tap to play in YouTube player"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "A D&E Song Music • ${formatChannelPublished(video.publishedAt)}",
                    fontSize = 12.sp,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Source badge
            Text(
                text = "My Channel",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextDim,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardNavy)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

// Render the RSS published timestamp as a friendly "x days ago" string.
// Uses SimpleDateFormat with the ISO-8601 'XXX' timezone (API 24+ compatible).
private fun formatChannelPublished(publishedAt: String): String {
    if (publishedAt.isBlank()) return ""
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val date = sdf.parse(publishedAt) ?: return publishedAt.take(10)
        val diffSeconds = (System.currentTimeMillis() - date.time) / 1000
        when {
            diffSeconds < 60 -> "just now"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            diffSeconds < 86400 * 30 -> "${diffSeconds / 86400}d ago"
            diffSeconds < 86400 * 365 -> "${diffSeconds / (86400 * 30)}mo ago"
            else -> "${diffSeconds / (86400 * 365)}y ago"
        }
    } catch (e: Exception) {
        publishedAt.take(10)
    }
}

// Source Chip Component
@Composable
fun SourceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .premiumPressScale(interactionSource)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PurplePrimary.copy(alpha = 0.3f) else GlassBg)
            .border(
                width = 1.dp,
                color = if (selected) PurplePrimary else GlassBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) CyanGlow else TextDim
        )
    }
}

// Helper function to get region flag emoji
private fun getRegionFlag(countryCode: String): String {
    return when (countryCode) {
        "IN" -> "🇮🇳"  // India
        "US" -> "🇺🇸"  // USA
        "GB" -> "🇬🇧"  // UK
        "PK" -> "🇵🇰"  // Pakistan
        "BD" -> "🇧🇩"  // Bangladesh
        "AE" -> "🇦🇪"  // UAE
        "SA" -> "🇸🇦"  // Saudi Arabia
        "DE" -> "🇩🇪"  // Germany
        "FR" -> "🇫🇷"  // France
        "ES" -> "🇪🇸"  // Spain
        "IT" -> "🇮🇹"  // Italy
        "BR" -> "🇧🇷"  // Brazil
        "MX" -> "🇲🇽"  // Mexico
        "JP" -> "🇯🇵"  // Japan
        "KR" -> "🇰🇷"  // Korea
        "CN" -> "🇨🇳"  // China
        "ID" -> "🇮🇩"  // Indonesia
        "TH" -> "🇹🇭"  // Thailand
        "VN" -> "🇻🇳"  // Vietnam
        "PH" -> "🇵🇭"  // Philippines
        "MY" -> "🇲🇾"  // Malaysia
        "TR" -> "🇹🇷"  // Turkey
        "EG" -> "🇪🇬"  // Egypt
        "NG" -> "🇳🇬"  // Nigeria
        "ZA" -> "🇿🇦"  // South Africa
        "AR" -> "🇦🇷"  // Argentina
        "CO" -> "🇨🇴"  // Colombia
        "CL" -> "🇨🇱"  // Chile
        "CA" -> "🇨🇦"  // Canada
        "AU" -> "🇦🇺"  // Australia
        "NZ" -> "🇳🇿"  // New Zealand
        "RU" -> "🇷🇺"  // Russia
        else -> "🌍"
    }
}

// Helper function to get region name
private fun getRegionName(countryCode: String): String {
    return when (countryCode) {
        "IN" -> "India"
        "US" -> "USA"
        "GB" -> "UK"
        "PK" -> "Pakistan"
        "BD" -> "Bangladesh"
        "AE" -> "UAE"
        "SA" -> "Saudi"
        "DE" -> "Germany"
        "FR" -> "France"
        "ES" -> "Spain"
        "IT" -> "Italy"
        "BR" -> "Brazil"
        "MX" -> "Mexico"
        "JP" -> "Japan"
        "KR" -> "Korea"
        "CN" -> "China"
        "ID" -> "Indonesia"
        "TH" -> "Thailand"
        "VN" -> "Vietnam"
        "PH" -> "Philippines"
        "MY" -> "Malaysia"
        "TR" -> "Turkey"
        "EG" -> "Egypt"
        "NG" -> "Nigeria"
        "ZA" -> "South Africa"
        "AR" -> "Argentina"
        "CO" -> "Colombia"
        "CL" -> "Chile"
        "CA" -> "Canada"
        "AU" -> "Australia"
        "NZ" -> "New Zealand"
        "RU" -> "Russia"
        else -> "World"
    }
}
