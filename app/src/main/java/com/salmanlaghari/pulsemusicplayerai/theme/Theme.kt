package com.salmanlaghari.pulsemusicplayerai.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GlassmorphicColorScheme = darkColorScheme(
    primary = PurpleAccent,
    secondary = BlueAccent,
    tertiary = CyanGlow,
    background = BaseNavyBlue,
    surface = CardNavy,
    surfaceVariant = CardNavy2,
    onPrimary = TextLight,
    onSecondary = BaseNavyBlue,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextDim
)

@Composable
fun PulseMusicPlayerAITheme(
    darkTheme: Boolean = true, // Force premium dark theme consistently
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlassmorphicColorScheme,
        typography = Typography,
        content = content
    )
}
