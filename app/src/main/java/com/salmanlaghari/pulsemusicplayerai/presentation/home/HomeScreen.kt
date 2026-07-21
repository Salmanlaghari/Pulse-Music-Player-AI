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
import androidx.compose.ui.draw.shadow
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
import com.salmanlaghari.pulsemusicplayerai.common.GlassmorphicCard
import com.salmanlaghari.pulsemusicplayerai.common.SongArtwork
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModel
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.PermissionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue
import com.salmanlaghari.pulsemusicplayerai.theme.*

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
    var accentBgColor by remember { mutableStateOf(Purple3) }

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
            accentBgColor = Purple3
        }
    }

    val animatedAccentBgColor by animateColorAsState(
        targetValue = accentBgColor,
        animationSpec = tween(1000),
        label = "HomeBgAccent"
    )

    // Base background with purple and cyan radial glows over near-black base
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BaseDeepPurple, BaseNearBlack, BaseNearBlack)
                )
            )
    ) {
        // Glowing cyan radial mesh background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanGlow.copy(alpha = 0.12f), Color.Transparent),
                        radius = 800f
                    )
                )
        )

        // Glowing dynamic accent radial mesh background overlay (artwork color integration)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(animatedAccentBgColor.copy(alpha = 0.22f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFA78BFA), CyanGlow)
                            )
                        ),
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡",
                        fontSize = 20.sp,
                        modifier = Modifier.shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = CyanGlowSoft,
                            spotColor = CyanGlow
                        )
                    )
                }

                // Glassmorphic Search pill
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .background(GlassBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToSearch()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyanGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Premium Welcome Card with 3D tilt and diagonal shine
            WelcomeCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Continue Listening Section with Glassmorphic styling
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

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun WelcomeCard() {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Purple2.copy(alpha = 0.45f),
                spotColor = CyanGlow
            ),
        shape = RoundedCornerShape(22.dp),
        is3D = true,
        hasShine = true,
        backgroundBrush = Brush.linearGradient(
            colors = listOf(Purple1, Purple3, CyanGlow)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Welcome to Pulse AI Pro",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your flagship acoustic universe. Discover live spectrum visualizers, professional audio studio workflows, and intuitive music controls.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.5.sp,
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
    GlassmorphicCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(18.dp),
                clip = false
            ),
        shape = RoundedCornerShape(18.dp),
        containerColor = GlassBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Purple1, spotColor = Purple3)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Purple1, Purple3)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎵", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = song.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last listened • ${song.artist}",
                        fontSize = 11.5.sp,
                        color = TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .shadow(8.dp, CircleShape, ambientColor = Purple1, spotColor = Purple3)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Purple1, Purple3)
                        )
                    )
                    .clickable { onClick() },
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextDim,
            letterSpacing = 1.8.sp
        )
        if (showSeeAll) {
            Text(
                text = "Scan Music",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }
    }
}

data class QuickAccessItem(
    val title: String,
    val icon: String,
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
        QuickAccessItem("AI Assistant", "✨", onNavigateToAIHub),
        QuickAccessItem("My Favorites", "🩵", onNavigateToFavorites),
        QuickAccessItem("Library", "🎧", onNavigateToLibrary),
        QuickAccessItem("Equalizer", "📊", onNavigateToEqualizer)
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
    GlassmorphicCard(
        onClick = item.onClick,
        modifier = modifier
            .height(72.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), clip = false),
        shape = RoundedCornerShape(18.dp),
        containerColor = GlassBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Purple1.copy(alpha = 0.35f), CyanGlow.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.icon,
                    fontSize = 17.sp
                )
            }
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
            GlassmorphicCard(
                onClick = { onSongClick(song) },
                modifier = Modifier
                    .width(135.dp)
                    .height(175.dp),
                shape = RoundedCornerShape(14.dp),
                containerColor = GlassBg
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
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontSize = 11.sp,
                            color = TextDim,
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
    containerColor: Color = GlassBg,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "ScaleTransition")

    GlassmorphicCard(
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
        containerColor = containerColor
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

// Float player card styled with premium 3D Glassmorphism
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

    GlassmorphicCard(
        onClick = onExpand,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 14.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), clip = false),
        shape = RoundedCornerShape(18.dp),
        containerColor = Color(0xF2171429) // Frosted glass backing matching bottom mini player in html
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
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Pink, Purple1)
                                )
                            )
                    )

                    Column {
                        Text(
                            text = song.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            fontSize = 11.sp,
                            color = TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "⏮",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.clickable { viewModel.skipToPrevious() }
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Purple1)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "⏭",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.clickable { viewModel.skipToNext() }
                    )
                }
            }

            val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF2A2545))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Purple1, CyanGlow)
                            )
                        )
                )
            }
        }
    }
}
