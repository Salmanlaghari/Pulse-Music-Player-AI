package com.salmanlaghari.pulsemusicplayerai.presentation.splash

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    // 1. Entrance animations — fast scale up + fade in (snappy, not sluggish)
    val scale = remember { Animatable(0.4f) }
    val opacity = remember { Animatable(0f) }
    val contentOpacity = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(16f) }

    // 2. Continuous rotating glow ring around the logo (clockwise)
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowRotation"
    )

    // 2b. Counter-rotating outer ring (counter-clockwise, premium dual-ring effect)
    val rotationReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowRotationReverse"
    )

    // 3. Pulsing glow scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 3b. Secondary pulse (slower, for outer ring breathing)
    val pulseScaleOuter by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseOuter"
    )

    // 4. Equalizer bars (7 bars, animated heights) - staggered via different durations
    val eqBars = listOf(
        infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(600, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq1"),
        infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(500, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq2"),
        infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(700, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq3"),
        infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(560, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq4"),
        infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(750, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq5"),
        infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(530, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq6"),
        infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(640, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq7")
    )

    // 5. Shimmer wave for loading text (alpha oscillation)
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    // 6. Loading progress bar fill — smooth, fast, and feels responsive
    val progress = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Fast, snappy scale-in (300ms — feels instant, not sluggish)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
        )
        // Quick fade in (250ms)
        opacity.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 250))
        contentOpacity.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 250, delayMillis = 80))
        // Tagline slides up quickly (200ms)
        taglineOffset.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200, delayMillis = 80, easing = EaseOutCubic))
        // Loading progress: fast fill to 60% then to 100% — feels like real loading
        progress.animateTo(
            targetValue = 0.6f,
            animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
        )
        // Brief settle delay so the 100% bar is visible, then go
        delay(100)
        onNavigateToHome()
    }

    // Modern glowing deep-navy / purple gradient background
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
        // Ambient floating particle dots (premium depth effect)
        val particles = listOf(
            Triple(0.15f, 0.2f, 3.dp),
            Triple(0.82f, 0.15f, 2.dp),
            Triple(0.08f, 0.7f, 2.5.dp),
            Triple(0.9f, 0.65f, 3.dp),
            Triple(0.25f, 0.85f, 2.dp),
            Triple(0.7f, 0.82f, 2.5.dp),
            Triple(0.5f, 0.1f, 2.dp),
            Triple(0.6f, 0.88f, 3.dp)
        )
        particles.forEachIndexed { index, (x, y, size) ->
            val particleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800 + index * 250, easing = EaseOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "particle$index"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (x * 360).dp, y = (y * 720).dp)
                        .size(size)
                        .clip(CircleShape)
                        .background(
                            if (index % 2 == 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        )
                        .alpha(particleAlpha * opacity.value)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(opacity.value)
        ) {
            // Rotating glow ring + pulsing logo (dual-ring premium design)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Outer counter-rotating glow ring (cyan, premium depth)
                Box(
                    modifier = Modifier
                        .size(162.dp)
                        .scale(pulseScaleOuter)
                        .rotate(rotationReverse)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.0f)
                                )
                            )
                        )
                )

                // Inner rotating glow ring (purple, clockwise)
                Box(
                    modifier = Modifier
                        .size(142.dp)
                        .scale(pulseScale)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                )
                            )
                        )
                )

                // Ring border (thin premium outline)
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0B0820),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    Color(0xFF0B0820)
                                )
                            )
                        )
                )

                // Inner solid disc to host the icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    Color(0xFF0B0820)
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Pulse Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name with gradient shimmer
            Text(
                text = "Pulse Music Player AI",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tagline with slide-up animation
            Text(
                text = "The Intelligent Sound System",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .offset(y = taglineOffset.value.dp)
                    .alpha(contentOpacity.value)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Animated equalizer bars (7 bars, premium loading indicator)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.alpha(contentOpacity.value)
            ) {
                eqBars.forEach { bar ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height((30 * bar.value).dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            // Loading progress bar — smooth fill that feels like real loading
            LinearProgressIndicator(
                progress = progress.value,
                modifier = Modifier
                    .width(170.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Loading text with shimmer effect — changes based on progress
            val loadingText = when {
                progress.value < 0.3f -> "Initializing…"
                progress.value < 0.6f -> "Loading music engine…"
                progress.value < 0.9f -> "Almost ready…"
                else -> "Ready to play!"
            }
            Text(
                text = loadingText,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = shimmer),
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(contentOpacity.value)
            )
        }
    }
}
