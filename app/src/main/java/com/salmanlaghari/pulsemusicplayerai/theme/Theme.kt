package com.salmanlaghari.pulsemusicplayerai.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PremiumColorScheme = darkColorScheme(
    primary = Purple,
    secondary = Pink,
    tertiary = PurpleLight,
    background = BgDeep,
    surface = BgCard,
    surfaceVariant = BgCard2,
    onPrimary = Text,
    onSecondary = BgDeep,
    onBackground = Text,
    onSurface = Text,
    onSurfaceVariant = TextDim
)

@Composable
fun PulseMusicPlayerAITheme(
    darkTheme: Boolean = true, // Force premium dark theme consistently
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumColorScheme,
        typography = Typography,
        content = content
    )
}
