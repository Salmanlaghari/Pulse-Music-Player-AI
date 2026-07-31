package com.salmanlaghari.pulsemusicplayerai.presentation.splash

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium animated loading overlay shown while the app loads its music data
 * (MediaStore scan, albums, artists, etc.) after the splash screen.
 *
 * This replaces the blank/blue screen the user would otherwise see during the
 * 5-30 second initial data load. The animation includes:
 *  - A rotating dual glow ring around the music note icon
 *  - Pulsing logo
 *  - Animated equalizer bars
 *  - Rotating loading text that cycles through status messages
 *
 * This overlay is transparent-safe: it sits on top of the home screen and
 * disappears the moment data loading completes.
 */
@Composable
fun LoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingOverlay")

    // Rotating glow ring (clockwise)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadRotation"
    )

    // Counter-rotating outer ring
    val rotationReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadRotationRev"
    )

    // Pulsing glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadPulse"
    )

    // Equalizer bars (5 bars)
    val eqBars = listOf(
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, easing = EaseOutCubic), RepeatMode.Reverse), label = "loadEq1"),
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(420, easing = EaseOutCubic), RepeatMode.Reverse), label = "loadEq2"),
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, easing = EaseOutCubic), RepeatMode.Reverse), label = "loadEq3"),
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(480, easing = EaseOutCubic), RepeatMode.Reverse), label = "loadEq4"),
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(550, easing = EaseOutCubic), RepeatMode.Reverse), label = "loadEq5")
    )

    // Shimmer for loading text
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadShimmer"
    )

    // Rotating dotted indicator angle (for the "loading dots" visual)
    val dotAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadDotAngle"
    )

    // Full-screen gradient background (matches app theme)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0820),
                        Color(0xFF150F2D),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rotating dual glow ring + pulsing logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                // Outer counter-rotating glow ring
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .rotate(rotationReverse)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.0f)
                                )
                            )
                        )
                )

                // Inner rotating glow ring
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(pulseScale)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                )
                            )
                        )
                )

                // Inner disc
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color(0xFF0B0820)
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Loading",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App name
            Text(
                text = "Pulse Music Player AI",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated equalizer bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                eqBars.forEach { bar ->
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height((24 * bar.value).dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rotating loading dots (3 dots orbiting)
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                val dotPositions = listOf(0f, 120f, 240f)
                dotPositions.forEach { baseAngle ->
                    val angleRad = Math.toRadians((baseAngle + dotAngle).toDouble())
                    val x = (Math.cos(angleRad) * 16).toFloat()
                    val y = (Math.sin(angleRad) * 16).toFloat()
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Loading text with shimmer — cycles through status messages
            val loadingText = "Loading your music…"
            Text(
                text = loadingText,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = shimmer),
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
