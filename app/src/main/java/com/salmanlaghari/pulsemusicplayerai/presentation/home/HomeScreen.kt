package com.salmanlaghari.pulsemusicplayerai.presentation.home

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.common.SongArtwork
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.PermissionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToAIHub: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()

    if (!isPermissionGranted) {
        PermissionScreen(onPermissionResult = { granted ->
            viewModel.setPermissionGranted(granted)
        })
    } else {
        HomeScreenContent(
            viewModel = viewModel,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateToAIHub = onNavigateToAIHub,
            onNavigateToFavorites = onNavigateToFavorites,
            onNavigateToLibrary = onNavigateToLibrary,
            onNavigateToEqualizer = onNavigateToEqualizer
        )
    }
}

@Composable
fun HomeScreenContent(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToAIHub: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val allSongs by viewModel.allSongs.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    // Dynamic color extraction for background ambience from active song artwork
    var accentBgColor by remember { mutableStateOf(Color(0xFF0F0C1F)) }

    LaunchedEffect(currentSong) {
        val song = currentSong
        if (song != null) {
            val color = withContext(Dispatchers.IO) {
                val cacheDir = File(context.cacheDir, "artwork_cache")
                val cacheFile = File(cacheDir, "song_${song.id}.jpg")
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    try {
                        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                        val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath, options)
                        if (bitmap != null) {
                            val avg = getAverageColor(bitmap)
                            bitmap.recycle()
                            Color(avg)
                        } else {
                            generateFallbackColor(song)
                        }
                    } catch (e: Exception) {
                        generateFallbackColor(song)
                    }
                } else {
                    generateFallbackColor(song)
                }
            }
            accentBgColor = color
        } else {
            accentBgColor = Color(0xFF0F0C1F)
        }
    }

    val animatedAccentBgColor by animateColorAsState(
        targetValue = accentBgColor,
        animationSpec = tween(1000),
        label = "HomeBgAccent"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedAccentBgColor.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header: title and search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pulse",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Pulse Energy",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToSearch()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Premium Welcome Card with subtle border and gradient overlay
            WelcomeCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Continue Listening Section with Dynamic Artwork background
            val songToContinue = currentSong ?: allSongs.firstOrNull()
            if (songToContinue != null) {
                SectionHeader(title = "Continue Listening", showSeeAll = false) {}
                Spacer(modifier = Modifier.height(12.dp))
                ContinueListeningCard(
                    song = songToContinue,
                    onClick = { viewModel.playSong(songToContinue) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Quick Access Grid Section with click triggers mapped correctly
            SectionHeader(title = "Quick Access", showSeeAll = false) {}
            Spacer(modifier = Modifier.height(12.dp))
            QuickAccessRow(
                onNavigateToAIHub = onNavigateToAIHub,
                onNavigateToFavorites = onNavigateToFavorites,
                onNavigateToLibrary = onNavigateToLibrary,
                onNavigateToEqualizer = onNavigateToEqualizer
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Recently Added list
            if (recentlyAdded.isNotEmpty()) {
                SectionHeader(title = "Recently Added") { viewModel.loadMusicData() }
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(songs = recentlyAdded, onSongClick = { viewModel.playSong(it, recentlyAdded) })
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. Favorite Songs list
            if (favoriteSongs.isNotEmpty()) {
                SectionHeader(title = "Favorite Songs") {}
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(songs = favoriteSongs, onSongClick = { viewModel.playSong(it, favoriteSongs) })
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. Recently Played
            if (allSongs.isNotEmpty()) {
                SectionHeader(title = "Recently Played") {}
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(songs = allSongs.take(5), onSongClick = { viewModel.playSong(it) })
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 6. Most Played placeholder
            if (allSongs.isNotEmpty()) {
                SectionHeader(title = "Most Played") {}
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(songs = allSongs.sortedBy { it.title.length }.take(5), onSongClick = { viewModel.playSong(it) })
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Welcome to Pulse AI Pro",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your flagship acoustic universe. Discover live spectrum visualizers, professional audio studio workflows, and intuitive music controls.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun ContinueListeningCard(
    song: Song,
    onClick: () -> Unit
) {
    InteractiveCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                SongArtwork(
                    song = song,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    iconSize = 28.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last listened • ${song.artist}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    showSeeAll: Boolean = true,
    onSeeAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )
        if (showSeeAll) {
            Text(
                text = "Scan Music",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }
    }
}

data class QuickAccessItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun QuickAccessRow(
    onNavigateToAIHub: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val items = listOf(
        QuickAccessItem("AI Assistant", Icons.Default.AutoAwesome, MaterialTheme.colorScheme.primary, onNavigateToAIHub),
        QuickAccessItem("My Favorites", Icons.Default.Favorite, MaterialTheme.colorScheme.secondary, onNavigateToFavorites),
        QuickAccessItem("Library", Icons.Default.LibraryMusic, MaterialTheme.colorScheme.primary, onNavigateToLibrary),
        QuickAccessItem("Equalizer", Icons.Default.Equalizer, MaterialTheme.colorScheme.secondary, onNavigateToEqualizer)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.take(2).forEach { item ->
            QuickAccessCard(item, modifier = Modifier.weight(1f))
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.takeLast(2).forEach { item ->
            QuickAccessCard(item, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun QuickAccessCard(item: QuickAccessItem, modifier: Modifier = Modifier) {
    InteractiveCard(
        onClick = item.onClick,
        modifier = modifier.height(64.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.color,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongHorizontalLazyRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(songs) { song ->
            InteractiveCard(
                onClick = { onSongClick(song) },
                modifier = Modifier
                    .width(135.dp)
                    .height(175.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SongArtwork(
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        iconSize = 32.dp
                    )
                    Column {
                        Text(
                            text = song.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Custom Glassmorphic and Animated Press/Scale Card
@Composable
fun InteractiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "ScaleTransition")

    Card(
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        onClick()
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

private fun getAverageColor(bitmap: android.graphics.Bitmap): Int {
    var redBucket = 0L
    var greenBucket = 0L
    var blueBucket = 0L
    var pixelCount = 0L
    for (y in 0 until bitmap.height step 4) {
        for (x in 0 until bitmap.width step 4) {
            val c = bitmap.getPixel(x, y)
            val a = android.graphics.Color.alpha(c)
            if (a > 128) {
                redBucket += android.graphics.Color.red(c)
                greenBucket += android.graphics.Color.green(c)
                blueBucket += android.graphics.Color.blue(c)
                pixelCount++
            }
        }
    }
    if (pixelCount == 0L) return android.graphics.Color.BLACK
    return android.graphics.Color.rgb(
        (redBucket / pixelCount).toInt(),
        (greenBucket / pixelCount).toInt(),
        (blueBucket / pixelCount).toInt()
    )
}

private fun generateFallbackColor(song: Song): Color {
    val hash = (song.title + song.artist).hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.35f)))
}

// Float player card
@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    onExpand: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    val song = currentSong ?: return

    InteractiveCard(
        onClick = onExpand,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    SongArtwork(
                        song = song,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        iconSize = 24.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.skipToPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Prev",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.skipToNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
