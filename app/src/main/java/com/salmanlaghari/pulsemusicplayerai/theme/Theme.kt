package com.salmanlaghari.pulsemusicplayerai.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeonColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonBlue,
    tertiary = NeonCyan,
    background = NeonBackground,
    surface = NeonSurface,
    surfaceVariant = NeonSurfaceVariant,
    onPrimary = NeonTextPrimary,
    onSecondary = NeonBackground,
    onBackground = NeonTextPrimary,
    onSurface = NeonTextPrimary,
    onSurfaceVariant = NeonTextSecondary
)

@Composable
fun PulseMusicPlayerAITheme(
    darkTheme: Boolean = true, // Force premium dark theme consistently
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeonColorScheme,
        typography = Typography,
        content = content
    )
}
