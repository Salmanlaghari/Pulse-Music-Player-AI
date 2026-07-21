package com.salmanlaghari.pulsemusicplayerai.presentation.splash

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.theme.BgDeep
import com.salmanlaghari.pulsemusicplayerai.theme.Pink
import com.salmanlaghari.pulsemusicplayerai.theme.Purple
import com.salmanlaghari.pulsemusicplayerai.theme.PurpleLight
import com.salmanlaghari.pulsemusicplayerai.theme.TextDim
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = { OvershootInterpolator(2f).getInterpolation(it) }
            )
        )
        opacity.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )

        pulseScale.animateTo(
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    LaunchedEffect(key1 = true) {
        delay(2500)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgDeep, Color(0xFF120E24), BgDeep)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale.value)
            ) {
                // Background radial glow using Pink and Purple
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Purple.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Text(
                    text = "⚡",
                    fontSize = 54.sp,
                    color = PurpleLight,
                    modifier = Modifier.shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = PurpleLight,
                        spotColor = Pink
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Pulse Music Player",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.scale(pulseScale.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Feel the Music, Feel the Pulse",
                color = TextDim,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
