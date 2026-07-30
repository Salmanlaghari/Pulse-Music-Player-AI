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
    // 1. Entrance animations — scale up + fade in (smooth, premium feel)
    val scale = remember { Animatable(0.3f) }
    val opacity = remember { Animatable(0f) }
    val contentOpacity = remember { Animatable(0f) }

    // 2. Continuous rotating glow ring around the logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowRotation"
    )

    // 3. Pulsing glow scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = EaseOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 4. Equalizer bars (5 bars, animated heights) - staggered via different durations
    val eqBars = listOf(
        infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq1"),
        infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(650, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq2"),
        infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq3"),
        infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(720, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq4"),
        infiniteTransition.animateFloat(0.4f, 1f, infiniteRepeatable(tween(680, easing = EaseOutCubic), RepeatMode.Reverse), label = "eq5")
    )

    // 5. Loading progress bar fill
    val progress = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Smooth overshoot scale-in
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = EaseOutCubic)
        )
        opacity.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 700))
        contentOpacity.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 600, delayMillis = 200))
        // Animate loading progress to ~95% then navigate
        progress.animateTo(
            targetValue = 0.95f,
            animationSpec = tween(durationMillis = 1800, easing = EaseOutCubic)
        )
        delay(150)
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(opacity.value)
        ) {
            // Rotating glow ring + pulsing logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Conic-style rotating glow ring (sweep gradient simulated with radial)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
                                )
                            )
                        )
                )
                // Inner solid disc to host the icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
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
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App name
            Text(
                text = "Pulse Music Player AI",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The Intelligent Sound System",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated equalizer bars (subtle, premium loading indicator)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.alpha(contentOpacity.value)
            ) {
                eqBars.forEach { bar ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height((28 * bar.value).dp)
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

            Spacer(modifier = Modifier.height(28.dp))

            // Loading progress bar
            LinearProgressIndicator(
                progress = progress.value,
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Syncing your music…",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(contentOpacity.value)
            )
        }
    }
}
