package com.salmanlaghari.pulsemusicplayerai.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.theme.CyanGlow
import com.salmanlaghari.pulsemusicplayerai.theme.PurpleAccent
import com.salmanlaghari.pulsemusicplayerai.theme.TextDim

/**
 * Premium press-scale modifier: gently scales any clickable element down to 0.96
 * while pressed, with a snappy 120ms tween. Pairs well with a clickable that uses
 * the same [interactionSource] and no indication for a clean, modern feel.
 */
fun Modifier.premiumPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "premiumPressScale"
    )
    this.scale(scale)
}

/**
 * Premium fade-in modifier: animates alpha from 0 to 1 over 350ms when the
 * composable first enters composition. Great for list items and cards.
 */
fun Modifier.premiumFadeIn(
    durationMillis: Int = 350,
    delayMillis: Int = 0
): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis)
        )
    }
    this.graphicsLayer { this.alpha = alpha.value }
}

/**
 * PulseBranding — a light premium footer widget showing an animated equalizer
 * (three staggered vertical bars with a breathing glow) next to the "Pulse"
 * wordmark in the app's signature cyan glow color. Designed to sit at the bottom
 * of scrollable feeds (Home, YouTube) as a subtle brand signature.
 */
@Composable
fun PulseBranding(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseBranding")

    // Three equalizer bars with staggered, slightly different timing so the
    // animation never looks mechanical.
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    // Breathing glow on the wordmark.
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = modifier
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BrandBar(scaleFraction = bar1)
            Spacer(modifier = Modifier.width(3.dp))
            BrandBar(scaleFraction = bar2)
            Spacer(modifier = Modifier.width(3.dp))
            BrandBar(scaleFraction = bar3)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Pulse",
                color = CyanGlow.copy(alpha = 0.5f + 0.5f * glowAlpha),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Music Player AI",
            color = TextDim.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp
        )
    }
}

@Composable
private fun BrandBar(scaleFraction: Float) {
    Box(
        modifier = Modifier
            .size(width = 4.dp, height = 18.dp)
            .graphicsLayer {
                scaleX = scaleFraction
                scaleY = scaleFraction
            }
            .clip(RoundedCornerShape(2.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CyanGlow, PurpleAccent)
                )
            )
    )
}

/**
 * PulseDot — a small breathing glowing dot with an expanding halo, suitable
 * for "now playing" indicators or as a subtle live marker.
 */
@Composable
fun PulseDot(
    modifier: Modifier = Modifier,
    color: Color = CyanGlow
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val halo by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo"
    )
    Box(
        modifier = modifier.size(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Expanding halo
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = 1f + (1f - halo) * 1.6f
                    scaleY = 1f + (1f - halo) * 1.6f
                    alpha = halo
                }
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        // Core dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer { alpha = breathe }
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}
