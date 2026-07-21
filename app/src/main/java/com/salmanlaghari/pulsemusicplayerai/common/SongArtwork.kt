package com.salmanlaghari.pulsemusicplayerai.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import kotlin.math.absoluteValue

@Composable
fun SongArtwork(
    song: Song?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: Dp = 24.dp
) {
    if (song == null) {
        DefaultPremiumFallback(modifier = modifier, iconSize = iconSize)
        return
    }

    SubcomposeAsyncImage(
        model = song,
        contentDescription = "Artwork for ${song.title}",
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            DefaultPremiumFallback(song = song, modifier = Modifier.fillMaxSize(), iconSize = iconSize)
        },
        error = {
            DefaultPremiumFallback(song = song, modifier = Modifier.fillMaxSize(), iconSize = iconSize)
        }
    )
}

@Composable
fun DefaultPremiumFallback(
    song: Song? = null,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    // Generate distinct premium gradient colors based on song title/artist hash
    val startColor: Color
    val endColor: Color

    if (song != null) {
        val hash = (song.title + song.artist).hashCode().absoluteValue
        val hue1 = (hash % 360).toFloat()
        val hue2 = ((hue1 + 140) % 360)

        startColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue1, 0.75f, 0.7f)))
        endColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue2, 0.85f, 0.4f)))
    } else {
        startColor = MaterialTheme.colorScheme.primary
        endColor = MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(startColor, endColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(iconSize)
        )
    }
}
