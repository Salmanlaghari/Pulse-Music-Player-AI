package com.salmanlaghari.pulsemusicplayerai.presentation.home

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.salmanlaghari.pulsemusicplayerai.common.PulseBranding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.salmanlaghari.pulsemusicplayerai.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToYouTube: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    if (!isPermissionGranted) {
        PermissionScreen(onPermissionResult = { viewModel.setPermissionGranted(it) })
    } else {
        HomeScreenContent(viewModel, onNavigateToSearch, onNavigateToPlayer, onNavigateToYouTube, onNavigateToFavorites, onNavigateToLibrary, onNavigateToEqualizer)
    }
}

@Composable
fun HomeScreenContent(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToYouTube: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToEqualizer: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allSongs by viewModel.allSongs.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val scrollState = rememberScrollState()
    var accentBgColor by remember { mutableStateOf(CardNavy) }

    LaunchedEffect(currentSong) {
        val song = currentSong
        accentBgColor = if (song == null) CardNavy else withContext(Dispatchers.IO) {
            val file = File(File(context.cacheDir, "artwork_cache"), "song_${song.id}.jpg")
            if (file.exists() && file.length() > 0) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = 4 })?.let { bitmap ->
                        val color = Color(getAverageColor(bitmap))
                        bitmap.recycle()
                        color
                    } ?: generateFallbackColor(song)
                } catch (_: Exception) { generateFallbackColor(song) }
            } else generateFallbackColor(song)
        }
    }
    val animatedAccentBgColor by animateColorAsState(accentBgColor, tween(1000), label = "HomeBgAccent")

    Box(modifier = Modifier.fillMaxSize().background(NeonBackground)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(NeonPurple.copy(alpha = 0.12f), Color.Transparent), radius = 900f)))
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(NeonCyan.copy(alpha = 0.08f), Color.Transparent), radius = 700f)))
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pulse", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚡", fontSize = 20.sp, modifier = Modifier.shadow(elevation = 12.dp, shape = CircleShape, clip = false, ambientColor = CyanGlowSoft, spotColor = CyanGlow))
                }
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigateToSearch() }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanGlow, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            WelcomeCard()
            Spacer(modifier = Modifier.height(24.dp))
            val songToContinue = currentSong ?: allSongs.firstOrNull()
            if (songToContinue != null) {
                SectionHeader(title = "Continue Listening", showSeeAll = false) {}
                Spacer(modifier = Modifier.height(12.dp))
                ContinueListeningCard(song = songToContinue, onClick = { viewModel.playSong(songToContinue) })
                Spacer(modifier = Modifier.height(24.dp))
            }
            SectionHeader(title = "Quick Access", showSeeAll = false)
            Spacer(modifier = Modifier.height(12.dp))
            QuickAccessRow(onNavigateToYouTube, onNavigateToFavorites, onNavigateToLibrary, onNavigateToEqualizer)
            Spacer(modifier = Modifier.height(24.dp))
            if (recentlyAdded.isNotEmpty()) {
                SectionHeader(title = "Recently Added") { onNavigateToLibrary() }
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(recentlyAdded) { viewModel.playSong(it, recentlyAdded) }
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (favoriteSongs.isNotEmpty()) {
                SectionHeader(title = "Favorite Songs") { onNavigateToFavorites() }
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(favoriteSongs) { viewModel.playSong(it, favoriteSongs) }
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (allSongs.isNotEmpty()) {
                SectionHeader(title = "Recently Played") { onNavigateToLibrary() }
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(allSongs.take(5)) { viewModel.playSong(it) }
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Most Played") { onNavigateToLibrary() }
                Spacer(modifier = Modifier.height(12.dp))
                SongHorizontalLazyRow(allSongs.sortedBy { it.title.length }.take(5)) { viewModel.playSong(it) }
                Spacer(modifier = Modifier.height(24.dp))
            }
            PulseBranding(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun WelcomeCard() {
    GlassmorphicCard(modifier = Modifier.fillMaxWidth().height(140.dp).shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = PurplePrimary.copy(alpha = 0.45f), spotColor = CyanGlow), shape = RoundedCornerShape(22.dp), is3D = true, hasShine = true, backgroundBrush = Brush.linearGradient(listOf(PurplePrimary, CardNavy2, CyanSecondary))) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.CenterStart) {
            Column {
                Text("Welcome to Pulse AI Pro", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Your flagship acoustic universe. Discover live spectrum visualizers, professional audio studio workflows, and intuitive music controls.", color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
fun ContinueListeningCard(song: Song, onClick: () -> Unit) {
    GlassmorphicCard(onClick = onClick, modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = NeonPurpleGlow, spotColor = NeonPurpleGlow), shape = RoundedCornerShape(20.dp), containerColor = NeonGlass) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                SongArtwork(song, Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)), iconSize = 28.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("Last listened • ${song.artist}", fontSize = 12.sp, color = NeonTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = NeonPurpleGlow, spotColor = NeonPurpleGlow).background(Brush.linearGradient(listOf(NeonPurple, NeonPink))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, showSeeAll: Boolean = true, onSeeAllClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDim, letterSpacing = 1.8.sp)
        if (showSeeAll) Text("See All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanGlow, modifier = Modifier.clickable { onSeeAllClick() })
    }
}

data class QuickAccessItem(val title: String, val icon: String, val onClick: () -> Unit)

@Composable
fun QuickAccessRow(onNavigateToYouTube: () -> Unit, onNavigateToFavorites: () -> Unit, onNavigateToLibrary: () -> Unit, onNavigateToEqualizer: () -> Unit) {
    val items = listOf(QuickAccessItem("YouTube Music", "▶️", onNavigateToYouTube), QuickAccessItem("My Favorites", "🩵", onNavigateToFavorites), QuickAccessItem("Library", "🎧", onNavigateToLibrary), QuickAccessItem("Equalizer", "📊", onNavigateToEqualizer))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items.take(2).forEach { QuickAccessCard(it, Modifier.weight(1f)) } }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items.takeLast(2).forEach { QuickAccessCard(it, Modifier.weight(1f)) } }
}

@Composable
fun QuickAccessCard(item: QuickAccessItem, modifier: Modifier = Modifier) {
    GlassmorphicCard(onClick = item.onClick, modifier = modifier.height(80.dp).shadow(8.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = NeonPurpleGlow, spotColor = NeonPurpleGlow), shape = RoundedCornerShape(18.dp), containerColor = NeonGlass) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(brush = Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.4f), NeonCyan.copy(alpha = 0.25f))), shape = RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(item.icon, fontSize = 20.sp)
            }
            Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SongHorizontalLazyRow(songs: List<Song>, onSongClick: (Song) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 12.dp)) {
        items(songs, key = { it.id }) { song ->
            GlassmorphicCard(onClick = { onSongClick(song) }, modifier = Modifier.width(135.dp).height(175.dp), shape = RoundedCornerShape(14.dp), containerColor = GlassBg) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    SongArtwork(song, Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(12.dp)), iconSize = 32.dp)
                    Column {
                        Text(song.title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(song.artist, fontSize = 11.sp, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveCard(onClick: () -> Unit, modifier: Modifier = Modifier, containerColor: Color = GlassBg, content: @Composable ColumnScope.() -> Unit) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "ScaleTransition")
    GlassmorphicCard(modifier = modifier.scale(scale).pointerInput(Unit) {
        detectTapGestures(onPress = {
            isPressed = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            tryAwaitRelease()
            isPressed = false
        }, onTap = { onClick() })
    }, shape = RoundedCornerShape(18.dp), containerColor = containerColor) {
        Column(modifier = Modifier.fillMaxSize()) { content() }
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
            if (android.graphics.Color.alpha(c) > 128) {
                redBucket += android.graphics.Color.red(c)
                greenBucket += android.graphics.Color.green(c)
                blueBucket += android.graphics.Color.blue(c)
                pixelCount++
            }
        }
    }
    if (pixelCount == 0L) return android.graphics.Color.BLACK
    return android.graphics.Color.rgb((redBucket / pixelCount).toInt(), (greenBucket / pixelCount).toInt(), (blueBucket / pixelCount).toInt())
}

private fun generateFallbackColor(song: Song): Color {
    val hash = (song.title + song.artist).hashCode().absoluteValue
    return Color(android.graphics.Color.HSVToColor(floatArrayOf((hash % 360).toFloat(), 0.6f, 0.35f)))
}
