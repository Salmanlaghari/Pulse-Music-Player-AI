package com.salmanlaghari.pulsemusicplayerai.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GlassmorphicColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = CyanSecondary,
    tertiary = Purple2,
    background = BaseNearBlack,
    surface = GlassBg,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = TextLight,
    onSecondary = BaseNearBlack,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextDim
)

@Composable
fun PulseMusicPlayerAITheme(
    darkTheme: Boolean = true, // Consistently dark and premium app-wide
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlassmorphicColorScheme,
        typography = Typography,
        content = content
    )
}
