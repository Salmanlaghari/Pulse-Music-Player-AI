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
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import com.salmanlaghari.pulsemusicplayerai.domain.model.YouTubeSong
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdMobBanner
import com.salmanlaghari.pulsemusicplayerai.data.ads.AdManager
import com.salmanlaghari.pulsemusicplayerai.theme.*
import kotlinx.coroutines.launch

enum class MusicSource { ALL, ARCHIVE, DEEZER }
enum class ViewMode { LIST, GRID }

@Composable
fun YouTubeScreen(
    viewModel: YouTubeViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isTrendingLoading by viewModel.isTrendingLoading.collectAsState()
    val currentlyPlaying by viewModel.currentlyPlaying.collectAsState()
    val isPlayLoading by viewModel.isPlayLoading.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf(MusicSource.ALL) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }

    // Helper function to play song and navigate
    fun playAndNavigate(song: YouTubeSong, queue: List<YouTubeSong>) {
        scope.launch {
            val success = viewModel.playSong(song, queue)
            if (success) {
                onNavigateToPlayer()
            }
        }
    }

    // Search with selected source
    fun searchWithSource(query: String) {
        viewModel.search(query)
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
                        text = "Loading audio...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trying multiple sources",
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
                onValueChange = {
                    searchQuery = it
                    if (it.length >= 3) {
                        searchWithSource(it)
                        isSearchActive = true
                    } else if (it.isEmpty()) {
                        viewModel.clearSearch()
                        isSearchActive = false
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

            // Source Selection Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceChip(
                        text = "🎵 All",
                        selected = selectedSource == MusicSource.ALL,
                        onClick = { 
                            selectedSource = MusicSource.ALL
                            if (searchQuery.length >= 3) searchWithSource(searchQuery)
                        }
                    )
                    SourceChip(
                        text = "🌐 Archive",
                        selected = selectedSource == MusicSource.ARCHIVE,
                        onClick = { 
                            selectedSource = MusicSource.ARCHIVE
                            if (searchQuery.length >= 3) viewModel.search(searchQuery)
                        }
                    )
                    SourceChip(
                        text = "🎵 Deezer",
                        selected = selectedSource == MusicSource.DEEZER,
                        onClick = { 
                            selectedSource = MusicSource.DEEZER
                            if (searchQuery.length >= 3) searchWithSource(searchQuery)
                        }
                    )
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
            } else {
                // Trending Section
                Text(
                    text = "TRENDING NOW 🔥",
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
                    // Top 5 Horizontal Cards
                    Text(
                        text = "Top Picks",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 12.dp)
                    ) {
                        items(trendingSongs.take(5)) { song ->
                            TrendingCard(
                                song = song,
                                isCurrentlyPlaying = currentlyPlaying?.id == song.id,
                                onClick = { playAndNavigate(song, trendingSongs) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // All Trending List
                    Text(
                        text = "All Trending",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(trendingSongs.drop(5)) { index, song ->
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

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TrendingCard(
    song: YouTubeSong,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                PurplePrimary.copy(alpha = 0.3f)
            else GlassBg
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(CardNavy)
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play overlay
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = song.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    fontSize = 10.sp,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

// Source Chip Component
@Composable
fun SourceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PurplePrimary.copy(alpha = 0.3f) else GlassBg)
            .border(
                width = 1.dp,
                color = if (selected) PurplePrimary else GlassBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
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
