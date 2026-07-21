package com.salmanlaghari.pulsemusicplayerai.theme

import androidx.compose.ui.graphics.Color

// Design tokens from the HTML source of truth
val BgDeep = Color(0xFF0C0A1A)
val BgCard = Color(0xFF171429)
val BgCard2 = Color(0xFF1D1836)
val Purple = Color(0xFF7C3AED)
val PurpleLight = Color(0xFFA78BFA)
val Pink = Color(0xFFEC4899)
val Blue = Color(0xFF3B82F6)
val Green = Color(0xFF22C55E)
val Text = Color(0xFFF5F3FF)
val TextDim = Color(0xFF9D97B8)
val BorderColor = Color(0x10FFFFFF) // rgba(255,255,255,0.06) is ~6%, 0x10 is ~6% (16/255)

// Backward compatibility or generic mapping colors
val PurplePrimary = Purple
val CyanSecondary = Pink

val DarkBackground = BgDeep
val DarkSurface = BgCard
val DarkSurfaceVariant = BgCard2
val DarkOnBackground = Text
val DarkOnSurface = Text

val LightBackground = BgDeep
val LightSurface = BgCard
val LightSurfaceVariant = BgCard2
val LightOnBackground = Text
val LightOnSurface = Text
