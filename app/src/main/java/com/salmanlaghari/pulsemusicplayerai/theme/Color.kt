package com.salmanlaghari.pulsemusicplayerai.theme

import androidx.compose.ui.graphics.Color

// Premium Design tokens from HTML source of truth
val BaseNearBlack = Color(0xFF08060F)
val BaseDeepPurple = Color(0xFF150D2B)
val CyanGlow = Color(0xFF3FF0FF)
val CyanGlowSoft = Color(0x803FF0FF) // rgba(63,240,255,0.5)
val Purple1 = Color(0xFF8B5CF6)
val Purple2 = Color(0xFF5B21B6)
val Purple3 = Color(0xFF4C1FB0)
val Pink = Color(0xFFEC4899) // Exact HTML value
val TextDim = Color(0xFF9AA3BA)
val TextLight = Color(0xFFF2F4FB)
val GlassBg = Color(0x0DFFFFFF) // rgba(255,255,255,0.05)
val GlassBorder = Color(0x1AFFFFFF) // rgba(255,255,255,0.1)

// Backward Compatibility Mapping
val PurplePrimary = Purple1
val CyanSecondary = CyanGlow

val DarkBackground = BaseNearBlack
val DarkSurface = GlassBg
val DarkSurfaceVariant = Color(0xFF130E26)
val DarkOnBackground = TextLight
val DarkOnSurface = TextLight

val LightBackground = BaseNearBlack
val LightSurface = GlassBg
val LightSurfaceVariant = Color(0xFF130E26)
val LightOnBackground = TextLight
val LightOnSurface = TextLight
