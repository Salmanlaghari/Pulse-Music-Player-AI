package com.salmanlaghari.pulsemusicplayerai.theme

import androidx.compose.ui.graphics.Color

// === Premium Neon Design System ===
// Background
val NeonBackground = Color(0xFF05060A)
val NeonBackgroundSecondary = Color(0xFF0B0D14)

// Surfaces / Glass
val NeonSurface = Color(0xFF10131D)
val NeonSurfaceVariant = Color(0xFF181C2A)
val NeonSurfaceHigh = Color(0xFF1E2235)
val NeonGlass = Color(0x12FFFFFF)
val NeonGlassBorder = Color(0x18FFFFFF)

// Accent Colors
val NeonCyan = Color(0xFF00E5FF)
val NeonBlue = Color(0xFF2979FF)
val NeonPurple = Color(0xFF7C4DFF)
val NeonPink = Color(0xFFFF2BD6)
val NeonMagenta = Color(0xFFFF00A8)

// Gradients
val NeonGradientCyanToPurple = listOf(NeonCyan, NeonPurple)
val NeonGradientBlueToPink = listOf(NeonBlue, NeonPink)
val NeonGradientPurpleToMagenta = listOf(NeonPurple, NeonMagenta)
val NeonGradientFull = listOf(NeonCyan, NeonBlue, NeonPurple, NeonPink, NeonMagenta)

// Text
val NeonTextPrimary = Color(0xFFF8F9FC)
val NeonTextSecondary = Color(0xFFB0B8D0)
val NeonTextMuted = Color(0xFF6B7280)

// Glow variants
val NeonCyanGlow = Color(0x4D00E5FF)
val NeonPurpleGlow = Color(0x4D7C4DFF)
val NeonPinkGlow = Color(0x4DFF2BD6)

// Backward Compatibility Mapping
val PurplePrimary = NeonPurple
val CyanSecondary = NeonCyan
val GlassBg = NeonGlass
val CardNavy = NeonSurface

val DarkBackground = NeonBackground
val DarkSurface = NeonSurface
val DarkSurfaceVariant = NeonSurfaceVariant
val DarkOnBackground = NeonTextPrimary
val DarkOnSurface = NeonTextPrimary

val LightBackground = NeonBackground
val LightSurface = NeonSurface
val LightSurfaceVariant = NeonSurfaceVariant
val LightOnBackground = NeonTextPrimary
val LightOnSurface = NeonTextPrimary
