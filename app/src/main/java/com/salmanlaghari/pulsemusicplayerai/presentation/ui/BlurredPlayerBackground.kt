package com.salmanlaghari.pulsemusicplayerai.presentation.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.salmanlaghari.pulsemusicplayerai.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun BlurredPlayerBackground(
    song: Song?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF150F2D)) }

    LaunchedEffect(song) {
        if (song != null) {
            val color = withContext(Dispatchers.IO) {
                // Try to find cached artwork file to extract dominant color
                val cacheDir = File(context.cacheDir, "artwork_cache")
                val cacheFile = File(cacheDir, "song_${song.id}.jpg")
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    try {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4 // Scale down to make it tiny
                        }
                        val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath, options)
                        if (bitmap != null) {
                            val avgColor = getAverageColor(bitmap)
                            bitmap.recycle()
                            Color(avgColor)
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
            dominantColor = color
        } else {
            dominantColor = Color(0xFF0C0C15)
        }
    }

    // Smoothly animate the background color transitions
    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "BgColorTransition"
    )

    // Blend the dominant color with a dark ambient palette to create a premium blurred glow effect
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.45f),
                        Color(0xFF0E0E18).copy(alpha = 0.96f),
                        Color(0xFF040408)
                    )
                )
            )
    ) {
        // Subtle ambient layer to enhance depth and glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
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
            if (a > 128) { // Only count non-transparent pixels
                redBucket += android.graphics.Color.red(c)
                greenBucket += android.graphics.Color.green(c)
                blueBucket += android.graphics.Color.blue(c)
                pixelCount++
            }
        }
    }
    if (pixelCount == 0L) return android.graphics.Color.BLACK
    val r = (redBucket / pixelCount).toInt()
    val g = (greenBucket / pixelCount).toInt()
    val b = (blueBucket / pixelCount).toInt()
    return android.graphics.Color.rgb(r, g, b)
}

private fun generateFallbackColor(song: Song): Color {
    val hash = (song.title + song.artist).hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    // Return a rich, dark/vibrant color suitable for background
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.65f, 0.45f)))
}
