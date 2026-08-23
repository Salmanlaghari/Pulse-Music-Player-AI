package com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

import com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.VisualizerTemplate

@Composable
fun VisualizerCanvas(
    preset: VisualizerPreset,
    isPlaying: Boolean,
    sensitivity: Float,
    speed: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    template: VisualizerTemplate? = null
) {
    var phase by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying, speed) {
        if (isPlaying) {
            val baseStep = 0.04f
            while (true) {
                withInfiniteAnimationFrameMillis { frameTime ->
                    phase += baseStep * speed
                }
            }
        }
    }

    // Retained data structures for Particle and Ambient systems to avoid re-allocations
    val stars = remember { List(100) { Star( (Math.random() * 2 - 1).toFloat(), (Math.random() * 2 - 1).toFloat(), (Math.random()).toFloat(), (1f + Math.random() * 3).toFloat() ) } }
    val matrixColumns = remember { List(40) { MatrixColumn( (Math.random()).toFloat(), (Math.random()).toFloat(), (0.01f + Math.random() * 0.02f).toFloat(), List(15) { ('A'..'Z').random().toString() } ) } }
    val bubbles = remember { List(30) { Bubble( (Math.random()).toFloat(), (Math.random()).toFloat(), (0.005f + Math.random() * 0.01f).toFloat(), (6f + Math.random() * 12).toFloat() ) } }
    val fireParticles = remember { List(40) { FireParticle( (Math.random() * 2 - 1).toFloat(), (Math.random()).toFloat(), (0.01f + Math.random() * 0.02f).toFloat() ) } }
    val meteors = remember { List(25) { Meteor( (Math.random() * 2 - 1).toFloat(), (Math.random() * 2 - 1).toFloat(), (0.02f + Math.random() * 0.03f).toFloat(), (15f + Math.random() * 15).toFloat() ) } }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = 0.99f
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val minDim = minOf(width, height)

        val tpl = template
        val pCol = tpl?.primaryColor ?: primaryColor
        val sCol = tpl?.secondaryColor ?: secondaryColor
        val tCol = tpl?.tertiaryColor ?: tertiaryColor
        val sens = sensitivity * (tpl?.glowIntensity ?: 1f)

        when (preset) {
            VisualizerPreset.CIRCULAR_RADIAL_BARS -> {
                drawCircularBars(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.CONCENTRIC -> {
                drawConcentricRings(phase, sens, pCol, tCol, centerX, centerY, minDim)
            }
            VisualizerPreset.RADIAL_WAVE -> {
                drawConcentricRings(phase, sens, pCol, tCol, centerX, centerY, minDim)
            }
            VisualizerPreset.SPIRAL_PULSE -> {
                drawSpiralGalaxy(phase, sens, pCol, tCol, centerX, centerY, minDim)
            }
            VisualizerPreset.ROTATING_RINGS -> {
                drawCircularBars(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.CLOCKWISE_SPIN -> {
                drawOrbitalRings(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.COUNTER_SPIN -> {
                drawOrbitalRings(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.PULSING_CORE -> {
                drawPulsingCore(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.EXPANDING_CIRCLES -> {
                drawFuturePulse(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.GALAXY_SPIN -> {
                drawGalaxyRing(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.DOUBLE_HELIX -> {
                drawDoubleHelix(phase, sens, pCol, tCol, width, centerY)
            }
            VisualizerPreset.ORBITING_DOTS -> {
                drawOrbitingDots(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.SOLAR_SYSTEM -> {
                drawOrbitalRings(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.VORTEX -> {
                drawTunnelWarp(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.MANDALA_ROTATE -> {
                drawKaleidoscope(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }

            // ---------- AVEE-STYLE TEMPLATES ----------
            VisualizerPreset.RADIAL_PULSE -> {
                drawRadialPulse(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.PARTICLE_BURST -> {
                drawParticleBurst(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.SPECTRUM_RINGS -> {
                drawSpectrumRings(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.KALEIDOSCOPE_WAVE -> {
                drawKaleidoscopeWave(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }

            // ---------- BARS ----------
            VisualizerPreset.VERTICAL_BARS -> {
                drawLinearBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.HORIZONTAL_BARS -> {
                drawLinearBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.MIRROR_BARS -> {
                drawMirrorBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.CENTERED_BARS -> {
                drawLinearBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.SIDE_BARS -> {
                drawSpectrumX(phase, sens, pCol, sCol, centerX, centerY, minDim)
            }
            VisualizerPreset.WAVE_BARS -> {
                drawFluidWave(phase, sens, pCol, sCol, width, centerY)
            }
            VisualizerPreset.STEP_BARS -> {
                drawCyberWave(phase, sens, pCol, width, height)
            }
            VisualizerPreset.RAINBOW_BARS -> {
                drawRainbowBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.GLOW_BARS -> {
                drawNeonBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.BARS_3D -> {
                drawLinearBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.STACKED_BARS -> {
                drawInfinityBars(phase, sens, pCol, sCol, width, height)
            }
            VisualizerPreset.BOUNCING_BARS -> {
                drawPeakBars(phase, sensitivity, primaryColor, secondaryColor, width, height)
            }
            VisualizerPreset.EQUALIZER_CLASSIC -> {
                drawLinearBars(phase, sensitivity, primaryColor, secondaryColor, width, height)
            }
            VisualizerPreset.SPECTRUM_FLAT -> {
                drawLinearBars(phase, sensitivity, primaryColor, secondaryColor, width, height)
            }
            VisualizerPreset.PEAK_METER -> {
                drawPeakBars(phase, sensitivity, primaryColor, secondaryColor, width, height)
            }

            // ---------- PARTICLES ----------
            VisualizerPreset.DOTS_PULSE -> {
                drawLinearBars(phase, sensitivity, primaryColor, secondaryColor, width, height)
            }
            VisualizerPreset.FLYING_PARTICLES -> {
                drawParticleOrb(phase, sensitivity, primaryColor, secondaryColor, tertiaryColor, centerX, centerY, minDim)
            }
            VisualizerPreset.RAIN_DROPS -> {
                drawMeteorShower(meteors, isPlaying, speed, phase, sensitivity, primaryColor, width, height)
            }
            VisualizerPreset.CONFETTI -> {
                drawFireworks(phase, sensitivity, primaryColor, secondaryColor, centerX, centerY)
            }
            VisualizerPreset.SPARKLES -> {
                drawColorBurst(phase, sensitivity, primaryColor, secondaryColor, centerX, centerY, minDim)
            }
            VisualizerPreset.NEBULA_FLOW -> {
                drawGalaxyCloud(phase, sensitivity, primaryColor, tertiaryColor, centerX, centerY, minDim)
            }
            VisualizerPreset.STAR_FIELD -> {
                drawStarfield(stars, isPlaying, speed, sensitivity, primaryColor, width, height, centerX, centerY)
            }
            VisualizerPreset.SMOKE_RINGS -> {
                drawSmokeTrails(phase, sensitivity, tertiaryColor, width, height)
            }
            VisualizerPreset.FIRE_SPARKS -> {
                drawFireworks(phase, sensitivity, primaryColor, secondaryColor, centerX, centerY)
            }
            VisualizerPreset.SNOW_FALL -> {
                drawSnowfall(phase, sensitivity, width, height)
            }

            // ---------- AMBIENT ----------
            VisualizerPreset.SOFT_PULSE -> {
                drawPulsingCore(phase, sensitivity, primaryColor, secondaryColor, centerX, centerY, minDim)
            }
            VisualizerPreset.BREATHING_GLOW -> {
                drawPulsingCore(phase, sensitivity, primaryColor, secondaryColor, centerX, centerY, minDim)
            }
            VisualizerPreset.GRADIENT_SHIFT -> {
                drawAuroraX(phase, sensitivity, primaryColor, tertiaryColor, width, height)
            }
            VisualizerPreset.COLOR_WAVE -> {
                drawPlasmaWave(phase, sensitivity, primaryColor, tertiaryColor, width, height)
            }
            VisualizerPreset.SLOW_FADE -> {
                drawFluidWave(phase, sensitivity, primaryColor, secondaryColor, width, centerY)
            }
            VisualizerPreset.DREAM_FLOW -> {
                drawAuroraFlow(phase, sensitivity, primaryColor, tertiaryColor, width, height)
            }
            VisualizerPreset.CALM_RIPPLE -> {
                drawWaterWaves(phase, sensitivity, secondaryColor, centerX, centerY, minDim)
            }
            VisualizerPreset.NIGHT_SKY -> {
                drawMatrixRain(matrixColumns, isPlaying, speed, phase, sensitivity, width, height)
            }
            VisualizerPreset.AURORA -> {
                drawAuroraFlow(phase, sensitivity, primaryColor, tertiaryColor, width, height)
            }
            VisualizerPreset.DEEP_SPACE -> {
                drawStarfield(stars, isPlaying, speed, sensitivity, primaryColor, width, height, centerX, centerY)
            }
        }
    }
}

// ==========================================
// RENDER METHODS FOR VISUALIZER ENGINE PRO
// ==========================================

private fun DrawScope.drawCircularBars(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val barCount = 72
    val innerRadius = minDim * 0.25f
    val maxBarLen = minDim * 0.2f * sensitivity
    for (i in 0 until barCount) {
        val angle = (i.toFloat() / barCount) * 360f
        val rad = Math.toRadians(angle.toDouble())
        val factor = sin(phase + i * 0.15f).absoluteValue
        val barLen = innerRadius + (factor * maxBarLen)
        val sx = cx + (innerRadius * cos(rad)).toFloat()
        val sy = cy + (innerRadius * sin(rad)).toFloat()
        val ex = cx + (barLen * cos(rad)).toFloat()
        val ey = cy + (barLen * sin(rad)).toFloat()
        drawLine(
            color = (if (i % 2 == 0) pCol else sCol).copy(alpha = 0.5f + factor * 0.5f),
            start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = 6f, cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawConcentricRings(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val rings = 5
    val baseRadius = minDim * 0.12f
    for (r in 0 until rings) {
        val scale = 1f + sin(phase - r * 0.4f).absoluteValue * 0.35f * sensitivity
        val radius = baseRadius * (r + 1) * scale
        val alpha = (1f - (r.toFloat() / rings)) * 0.7f
        drawCircle(
            color = (if (r % 2 == 0) pCol else tCol).copy(alpha = alpha),
            radius = radius, center = Offset(cx, cy), style = Stroke(width = 4f + r)
        )
    }
}

private fun DrawScope.drawLinearBars(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, height: Float) {
    val barCount = 30
    val barWidth = width / (barCount * 1.5f)
    val spacing = barWidth * 0.5f
    val startX = (width - (barCount * (barWidth + spacing) - spacing)) / 2f
    val maxBarHeight = height * 0.5f * sensitivity
    for (i in 0 until barCount) {
        val factor = sin(phase + i * 0.25f).absoluteValue
        val barHeight = 15f + factor * maxBarHeight
        val x = startX + i * (barWidth + spacing)
        val y = height - barHeight - 40f
        drawRoundRect(
            brush = Brush.verticalGradient(colors = listOf(pCol, sCol)),
            topLeft = Offset(x, y), size = Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

private fun DrawScope.drawParticleOrb(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val orbRadius = minDim * 0.15f * (1f + sin(phase * 1.5f).absoluteValue * 0.1f * sensitivity)
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(pCol.copy(alpha = 0.8f), Color.Transparent), center = Offset(cx, cy), radius = orbRadius * 1.5f),
        radius = orbRadius * 1.5f, center = Offset(cx, cy)
    )
    val particles = 45
    for (i in 0 until particles) {
        val orbitAngle = (i.toFloat() / particles) * 360f + (phase * 15f)
        val rad = Math.toRadians(orbitAngle.toDouble())
        val orbitRadius = orbRadius * 1.3f + sin(phase * 2f + i * 0.5f).absoluteValue * 80f * sensitivity
        val px = cx + (orbitRadius * cos(rad)).toFloat()
        val py = cy + (orbitRadius * sin(rad)).toFloat()
        val pSize = 6f + sin(phase + i).absoluteValue * 10f
        drawCircle(color = if (i % 3 == 0) tCol else sCol, radius = pSize, center = Offset(px, py))
    }
}

private fun DrawScope.drawStarfield(stars: List<Star>, isPlaying: Boolean, speed: Float, sensitivity: Float, pCol: Color, width: Float, height: Float, cx: Float, cy: Float) {
    for (star in stars) {
        if (isPlaying) {
            star.z -= 0.008f * speed
            if (star.z <= 0f) {
                star.z = 1f
                star.x = (Math.random() * 2 - 1).toFloat()
                star.y = (Math.random() * 2 - 1).toFloat()
            }
        }
        val sx = cx + (star.x / star.z) * (width * 0.3f)
        val sy = cy + (star.y / star.z) * (height * 0.3f)
        if (sx in 0f..width && sy in 0f..height) {
            val sizeFinal = star.size * ((1f - star.z) * 12f * sensitivity).coerceAtLeast(0.5f)
            drawCircle(color = pCol.copy(alpha = (1f - star.z).coerceIn(0f, 1f)), radius = sizeFinal, center = Offset(sx, sy))
        }
    }
}

private fun DrawScope.drawFluidWave(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, cy: Float) {
    val path1 = Path()
    val path2 = Path()
    val wavePoints = 8
    val stepX = width / (wavePoints - 1)
    for (i in 0 until wavePoints) {
        val px = i * stepX
        val y1Offset = sin(phase + i * 0.8f) * 120f * sensitivity
        val y2Offset = cos(phase * 1.2f + i * 0.6f) * 90f * sensitivity
        if (i == 0) {
            path1.moveTo(px, cy + y1Offset)
            path2.moveTo(px, cy + 30f + y2Offset)
        } else {
            val prevX = (i - 1) * stepX
            val prevY1 = cy + sin(phase + (i - 1) * 0.8f) * 120f * sensitivity
            val prevY2 = cy + 30f + cos(phase * 1.2f + (i - 1) * 0.6f) * 90f * sensitivity
            path1.quadraticBezierTo(prevX + stepX / 2f, prevY1, px, cy + y1Offset)
            path2.quadraticBezierTo(prevX + stepX / 2f, prevY2, px, cy + 30f + y2Offset)
        }
    }
    drawPath(path = path1, color = pCol.copy(alpha = 0.6f), style = Stroke(width = 8f, cap = StrokeCap.Round))
    drawPath(path = path2, color = sCol.copy(alpha = 0.5f), style = Stroke(width = 6f, cap = StrokeCap.Round))
}

private fun DrawScope.drawIsometricGrid(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val cols = 6
    val rows = 6
    val sizeIso = minDim * 0.05f
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val isoX = cx + (c - r) * sizeIso * 1.5f
            val isoY = cy * 0.7f + (c + r) * sizeIso * 0.75f
            val depth = sin(phase + (r + c) * 0.4f).absoluteValue
            val barLen = (20f + depth * sizeIso * 3f) * sensitivity
            val topPath = Path().apply {
                moveTo(isoX, isoY - barLen)
                lineTo(isoX + sizeIso * 1.5f, isoY - sizeIso * 0.75f - barLen)
                lineTo(isoX, isoY - sizeIso * 1.5f - barLen)
                lineTo(isoX - sizeIso * 1.5f, isoY - sizeIso * 0.75f - barLen)
                close()
            }
            drawPath(topPath, tCol.copy(alpha = 0.9f))
            val leftPath = Path().apply {
                moveTo(isoX - sizeIso * 1.5f, isoY - sizeIso * 0.75f - barLen)
                lineTo(isoX, isoY - barLen)
                lineTo(isoX, isoY)
                lineTo(isoX - sizeIso * 1.5f, isoY - sizeIso * 0.75f)
                close()
            }
            drawPath(leftPath, pCol.copy(alpha = 0.7f))
            val rightPath = Path().apply {
                moveTo(isoX, isoY - barLen)
                lineTo(isoX + sizeIso * 1.5f, isoY - sizeIso * 0.75f - barLen)
                lineTo(isoX + sizeIso * 1.5f, isoY - sizeIso * 0.75f)
                lineTo(isoX, isoY)
                close()
            }
            drawPath(rightPath, sCol.copy(alpha = 0.5f))
        }
    }
}

private fun DrawScope.drawFloatingBubbles(bubbles: List<Bubble>, isPlaying: Boolean, speed: Float, phase: Float, sensitivity: Float, pCol: Color, width: Float, height: Float) {
    for (bubble in bubbles) {
        if (isPlaying) {
            bubble.yPct -= bubble.speed * speed
            if (bubble.yPct <= -0.1f) {
                bubble.yPct = 1.1f
                bubble.xPct = (Math.random()).toFloat()
            }
        }
        val bx = bubble.xPct * width
        val by = bubble.yPct * height
        val pulse = 1f + sin(phase + bubble.xPct * 10f).absoluteValue * 0.3f * sensitivity
        val bSize = bubble.size * pulse
        drawCircle(brush = Brush.radialGradient(colors = listOf(pCol.copy(alpha = 0.6f), Color.Transparent), center = Offset(bx, by), radius = bSize), radius = bSize, center = Offset(bx, by))
        drawCircle(color = Color.White.copy(alpha = 0.3f), radius = bSize, center = Offset(bx, by), style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawKaleidoscope(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val segments = 8
    val maxLen = minDim * 0.35f * sensitivity
    for (s in 0 until segments) {
        val angle = s * (360f / segments)
        rotate(degrees = angle + phase * 10f, pivot = Offset(cx, cy)) {
            val path = Path()
            path.moveTo(cx, cy)
            val wave = sin(phase * 2f).absoluteValue * maxLen
            val endX = cx + wave
            val endY = cy + sin(phase * 1.5f) * 100f
            path.lineTo(endX, endY)
            drawPath(path = path, color = sCol.copy(alpha = 0.6f), style = Stroke(width = 5f, cap = StrokeCap.Round))
            drawCircle(color = pCol, radius = 12f * (1f + sin(phase).absoluteValue), center = Offset(endX, endY))
        }
    }
}

private fun DrawScope.drawDoubleHelix(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, width: Float, cy: Float) {
    val nodes = 20
    val span = width * 0.8f
    val startX = (width - span) / 2f
    val spacing = span / (nodes - 1)
    for (i in 0 until nodes) {
        val nx = startX + i * spacing
        val offset = i * 0.4f + phase * 2f
        val yA = cy + sin(offset) * 120f * sensitivity
        val yB = cy - sin(offset) * 120f * sensitivity
        drawLine(color = Color.White.copy(alpha = 0.25f), start = Offset(nx, yA), end = Offset(nx, yB), strokeWidth = 3f)
        drawCircle(color = pCol.copy(alpha = 0.8f), radius = 12f + cos(offset) * 6f, center = Offset(nx, yA))
        drawCircle(color = tCol.copy(alpha = 0.8f), radius = 12f - cos(offset) * 6f, center = Offset(nx, yB))
    }
}

private fun DrawScope.drawTunnelWarp(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val rings = 7
    val base = minDim * 0.04f
    for (i in 0 until rings) {
        val size = (i + 1) * base * (1f + sin(phase - i * 0.3f).absoluteValue * 0.4f * sensitivity)
        rotate(degrees = phase * 15f + i * 10f, pivot = Offset(cx, cy)) {
            drawRect(
                color = if (i % 2 == 0) pCol.copy(alpha = 0.6f) else sCol.copy(alpha = 0.4f),
                topLeft = Offset(cx - size, cy - size), size = Size(size * 2f, size * 2f),
                style = Stroke(width = 4f + i)
            )
        }
    }
}

private fun DrawScope.drawHeartbeatPulsar(phase: Float, sensitivity: Float, pCol: Color, cx: Float, cy: Float, width: Float) {
    val path = Path()
    path.moveTo(0f, cy)
    val pts = 40
    val step = width / (pts - 1)
    for (i in 0 until pts) {
        val hx = i * step
        val dist = (cx - hx).absoluteValue / cx
        val amp = (1f - dist).coerceAtLeast(0f)
        val wave = sin(phase * 4f) * 150f + cos(phase * 2f) * 80f
        val hy = cy + wave * amp * sensitivity
        if (i == 0) path.moveTo(hx, hy) else path.lineTo(hx, hy)
    }
    drawPath(path = path, color = pCol, style = Stroke(width = 6f, cap = StrokeCap.Round))
    drawCircle(color = Color.Red.copy(alpha = 0.85f), radius = 24f * (1f + sin(phase * 4f).absoluteValue * 0.3f * sensitivity), center = Offset(cx, cy))
}

private fun DrawScope.drawFlameSpectrum(phase: Float, sensitivity: Float, width: Float, height: Float) {
    val flames = 24
    val fWidth = width / (flames * 1.5f)
    val spacing = fWidth * 0.5f
    val startX = (width - (flames * (fWidth + spacing) - spacing)) / 2f
    for (i in 0 until flames) {
        val amp = sin(phase + i * 0.4f).absoluteValue
        val fHeight = (30f + amp * height * 0.55f) * sensitivity
        drawRoundRect(
            brush = Brush.verticalGradient(colors = listOf(Color.Yellow, Color.Red)),
            topLeft = Offset(startX + i * (fWidth + spacing), height - fHeight - 20f),
            size = Size(fWidth, fHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(fWidth / 2f, fWidth / 2f)
        )
    }
}

private fun DrawScope.drawMatrixRain(matrix: List<MatrixColumn>, isPlaying: Boolean, speed: Float, phase: Float, sensitivity: Float, width: Float, height: Float) {
    for (col in matrix) {
        if (isPlaying) {
            col.yPct += col.speed * speed
            if (col.yPct >= 1.1f) {
                col.yPct = -0.1f
                col.xPct = (Math.random()).toFloat()
            }
        }
        val mx = col.xPct * width
        val myBase = col.yPct * height
        for (charIdx in col.chars.indices) {
            val my = myBase - (charIdx * 24f)
            if (my in 0f..height) {
                val alpha = (1f - (charIdx.toFloat() / col.chars.size)) * (0.3f + sin(phase * 2f + charIdx).absoluteValue * 0.7f) * sensitivity
                drawCircle(color = Color.Green.copy(alpha = alpha.coerceIn(0f, 1f)), radius = 4f + alpha * 8f, center = Offset(mx, my))
            }
        }
    }
}

private fun DrawScope.drawSoundRibbon(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, cy: Float) {
    val path = Path()
    val ribCount = 50
    val step = width / (ribCount - 1)
    for (i in 0 until ribCount) {
        val rx = i * step
        val ry = cy + sin(phase + i * 0.3f) * 140f * sensitivity + cos(phase * 0.5f + i * 0.1f) * 60f
        if (i == 0) path.moveTo(rx, ry) else {
            val prevRx = (i - 1) * step
            val prevRy = cy + sin(phase + (i - 1) * 0.3f) * 140f * sensitivity + cos(phase * 0.5f + (i - 1) * 0.1f) * 60f
            path.quadraticBezierTo(prevRx + step / 2f, prevRy, rx, ry)
        }
    }
    drawPath(path = path, color = pCol, style = Stroke(width = 8f, cap = StrokeCap.Round))
}

// --- New 37 Flagship Visualizer Drawings ---

private fun DrawScope.drawNeonBars(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, height: Float) {
    val count = 20
    val barW = width / (count * 2)
    val spacing = barW
    for (i in 0 until count) {
        val factor = sin(phase + i * 0.3f).absoluteValue * sensitivity
        val h = factor * height * 0.3f
        // Top neon bar
        drawRoundRect(color = pCol.copy(alpha = 0.8f), topLeft = Offset(i * (barW + spacing), 0f), size = Size(barW, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW/2, barW/2))
        // Bottom neon bar
        drawRoundRect(color = sCol.copy(alpha = 0.8f), topLeft = Offset(i * (barW + spacing), height - h), size = Size(barW, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW/2, barW/2))
    }
}

private fun DrawScope.drawGalaxyRing(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val stars = 60
    val r = minDim * 0.28f
    for (i in 0 until stars) {
        val angle = (i.toFloat() / stars) * 360f + phase * 20f
        val rad = Math.toRadians(angle.toDouble())
        val dist = r + sin(phase * 3f + i).absoluteValue * 40f * sensitivity
        val px = cx + (dist * cos(rad)).toFloat()
        val py = cy + (dist * sin(rad)).toFloat()
        drawCircle(color = if (i % 2 == 0) pCol else sCol, radius = 6f + sin(phase + i).absoluteValue * 6f, center = Offset(px, py))
    }
}

private fun DrawScope.drawAuroraFlow(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, width: Float, height: Float) {
    val path = Path()
    path.moveTo(0f, height * 0.7f)
    val pts = 6
    val step = width / (pts - 1)
    for (i in 0 until pts) {
        val px = i * step
        val py = height * 0.7f + sin(phase + i * 1.2f) * 100f * sensitivity
        if (i == 0) path.moveTo(px, py) else path.quadraticBezierTo(px - step/2, py - 40f, px, py)
    }
    path.lineTo(width, height)
    path.lineTo(0f, height)
    path.close()
    drawPath(path = path, brush = Brush.verticalGradient(listOf(pCol.copy(alpha = 0.4f), tCol.copy(alpha = 0.05f))))
}

private fun DrawScope.drawFireSpectrum(fire: List<FireParticle>, isPlaying: Boolean, speed: Float, phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, height: Float) {
    for (p in fire) {
        if (isPlaying) {
            p.yPct -= p.speed * speed
            if (p.yPct <= 0.2f) { p.yPct = 0.9f; p.xScale = (Math.random() * 2 - 1).toFloat() }
        }
        val px = width/2 + p.xScale * width * 0.3f
        val py = height * p.yPct
        val size = (10f + sin(phase * 4f + p.xScale).absoluteValue * 20f) * sensitivity
        drawCircle(brush = Brush.radialGradient(listOf(Color.Red.copy(alpha = 0.6f), Color.Yellow.copy(alpha = 0.1f)), center = Offset(px, py), radius = size), radius = size, center = Offset(px, py))
    }
}

private fun DrawScope.drawWaterWaves(phase: Float, sensitivity: Float, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val rings = 6
    for (i in 0 until rings) {
        val r = (minDim * 0.08f * (i + 1)) * (1f + sin(phase - i * 0.5f).absoluteValue * 0.2f * sensitivity)
        drawCircle(color = sCol.copy(alpha = (1f - (i.toFloat() / rings)) * 0.5f), radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))
    }
}

private fun DrawScope.drawPlasmaWave(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, width: Float, height: Float) {
    for (i in 0..3) {
        val path = Path()
        val cy = height * (0.3f + i * 0.15f)
        path.moveTo(0f, cy)
        for (x in 0..10) {
            val px = x * (width / 10f)
            val py = cy + sin(phase * 2f + x * 0.8f + i) * 60f * sensitivity
            if (x == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path = path, color = if (i % 2 == 0) pCol.copy(alpha = 0.4f) else tCol.copy(alpha = 0.4f), style = Stroke(width = 6f))
    }
}

private fun DrawScope.drawFrequencyLines(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, cy: Float) {
    val path1 = Path()
    val path2 = Path()
    path1.moveTo(0f, cy)
    path2.moveTo(0f, cy)
    for (i in 0..12) {
        val x = i * (width / 12f)
        val y1 = cy + sin(phase * 3f + i * 0.5f) * 120f * sensitivity
        val y2 = cy + cos(phase * 3f + i * 0.5f) * 120f * sensitivity
        if (i == 0) { path1.moveTo(x, y1); path2.moveTo(x, y2) } else { path1.lineTo(x, y1); path2.lineTo(x, y2) }
    }
    drawPath(path = path1, color = pCol.copy(alpha = 0.8f), style = Stroke(width = 4f))
    drawPath(path = path2, color = sCol.copy(alpha = 0.8f), style = Stroke(width = 4f))
}

private fun DrawScope.drawColorBurst(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val lines = 36
    val radius = minDim * 0.35f * (0.5f + sin(phase * 3f).absoluteValue * 0.5f * sensitivity)
    for (i in 0 until lines) {
        val angle = (i.toFloat() / lines) * 360f
        val rad = Math.toRadians(angle.toDouble())
        val ex = cx + (radius * cos(rad)).toFloat()
        val ey = cy + (radius * sin(rad)).toFloat()
        drawLine(color = if (i % 2 == 0) pCol else sCol, start = Offset(cx, cy), end = Offset(ex, ey), strokeWidth = 3f)
    }
}

private fun DrawScope.drawSpiralGalaxy(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val arms = 3
    val dots = 40
    for (a in 0 until arms) {
        val baseAngle = a * (360f / arms)
        for (d in 0 until dots) {
            val factor = d.toFloat() / dots
            val angle = baseAngle + (factor * 360f) + (phase * 25f)
            val rad = Math.toRadians(angle.toDouble())
            val r = factor * minDim * 0.45f * sensitivity
            val px = cx + (r * cos(rad)).toFloat()
            val py = cy + (r * sin(rad)).toFloat()
            drawCircle(color = if (d % 2 == 0) pCol else tCol, radius = 4f + factor * 8f, center = Offset(px, py))
        }
    }
}

private fun DrawScope.drawRainbowRing(phase: Float, sensitivity: Float, cx: Float, cy: Float, minDim: Float) {
    val count = 60
    val radius = minDim * 0.28f * sensitivity
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 360f
        val rad = Math.toRadians(angle.toDouble())
        val px = cx + (radius * cos(rad)).toFloat()
        val py = cy + (radius * sin(rad)).toFloat()
        val hue = (i.toFloat() / count) * 360f
        val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.9f, 0.9f)))
        drawCircle(color = color, radius = (8f + sin(phase + i).absoluteValue * 6f) * sensitivity, center = Offset(px, py))
    }
}

private fun DrawScope.drawLaserBeams(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, width: Float, height: Float) {
    val beams = 16
    for (i in 0 until beams) {
        val angle = (i.toFloat() / beams) * 360f + phase * 10f
        val rad = Math.toRadians(angle.toDouble())
        val len = maxOf(width, height) * sensitivity
        val ex = cx + (len * cos(rad)).toFloat()
        val ey = cy + (len * sin(rad)).toFloat()
        drawLine(color = if (i % 2 == 0) pCol else sCol, start = Offset(cx, cy), end = Offset(ex, ey), strokeWidth = 4f)
    }
}

private fun DrawScope.drawCrystalMesh(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val nodes = 8
    val radius = minDim * 0.22f * (1f + sin(phase * 2f).absoluteValue * 0.3f * sensitivity)
    val pts = List(nodes) { idx ->
        val angle = (idx.toFloat() / nodes) * 360f + phase * 15f
        val rad = Math.toRadians(angle.toDouble())
        Offset(cx + (radius * cos(rad)).toFloat(), cy + (radius * sin(rad)).toFloat())
    }
    for (i in 0 until nodes) {
        for (j in i + 1 until nodes) {
            drawLine(color = pCol.copy(alpha = 0.3f), start = pts[i], end = pts[j], strokeWidth = 2f)
        }
        drawCircle(color = sCol, radius = 10f, center = pts[i])
    }
}

private fun DrawScope.drawSmokeTrails(phase: Float, sensitivity: Float, tCol: Color, width: Float, height: Float) {
    val path = Path()
    val cy = height / 2f
    path.moveTo(0f, cy)
    for (x in 0..15) {
        val px = x * (width / 15f)
        val py = cy + sin(phase * 1.5f + x * 0.5f) * 160f * sensitivity + cos(phase * 0.8f + x * 0.3f) * 80f
        if (x == 0) path.moveTo(px, py) else path.quadraticBezierTo(px - (width/30f), py - 30f, px, py)
    }
    drawPath(path = path, color = tCol.copy(alpha = 0.5f), style = Stroke(width = 12f, cap = StrokeCap.Round))
}

private fun DrawScope.drawCyberGrid(phase: Float, sensitivity: Float, sCol: Color, width: Float, height: Float) {
    val lines = 12
    val step = height / lines
    for (i in 0 until lines) {
        val y = i * step + (phase * 15f) % step
        drawLine(color = sCol.copy(alpha = 0.25f * sensitivity), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 2f)
    }
    val vertLines = 12
    val vStep = width / vertLines
    for (i in 0 until vertLines) {
        val x = i * vStep
        drawLine(color = sCol.copy(alpha = 0.2f * sensitivity), start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 2f)
    }
}

private fun DrawScope.drawInfinityLoop(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val path = Path()
    val steps = 60
    val radius = minDim * 0.3f * sensitivity
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * 2f * Math.PI
        val sinT = sin(t)
        val cosT = cos(t)
        // Lemniscate of Bernoulli
        val denom = 1f + sinT * sinT
        val x = cx + (radius * cosT) / denom
        val y = cy + (radius * sinT * cosT) / denom
        rotate(degrees = phase * 20f, pivot = Offset(cx, cy)) {
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
    }
    drawPath(path = path, color = pCol, style = Stroke(width = 6f, cap = StrokeCap.Round))
}

private fun DrawScope.drawRetroGrid(phase: Float, sensitivity: Float, pCol: Color, width: Float, height: Float) {
    val horizon = height * 0.6f
    // Draw horizontal perspective lines moving down
    val lines = 10
    val step = (height - horizon) / lines
    for (i in 0 until lines) {
        val factor = i.toFloat() / lines
        val y = horizon + factor * factor * (height - horizon) + (phase * 10f) % step
        if (y < height) {
            drawLine(color = pCol.copy(alpha = 0.35f * sensitivity), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 3f)
        }
    }
    // Draw vertical radial lines
    val vLines = 14
    for (i in 0..vLines) {
        val factor = i.toFloat() / vLines
        val startX = width * factor
        drawLine(color = pCol.copy(alpha = 0.25f), start = Offset(width/2, horizon), end = Offset(startX, height), strokeWidth = 2f)
    }
}

private fun DrawScope.drawLightningBolt(phase: Float, sensitivity: Float, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val path = Path()
    path.moveTo(cx, cy - minDim * 0.4f)
    var curX = cx
    var curY = cy - minDim * 0.4f
    val steps = 8
    val stepY = (minDim * 0.8f) / steps
    for (i in 0 until steps) {
        curY += stepY
        curX += (if (sin(phase * 10f + i) > 0) 30f else -30f) * sensitivity
        path.lineTo(curX, curY)
    }
    drawPath(path = path, color = tCol, style = Stroke(width = 6f, cap = StrokeCap.Round))
}

private fun DrawScope.drawOrbitalRings(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val rings = 3
    for (r in 0 until rings) {
        val radiusX = minDim * 0.3f * (r + 1) * 0.5f
        val radiusY = minDim * 0.1f * (r + 1) * 0.5f * sensitivity
        rotate(degrees = r * 45f + phase * 15f, pivot = Offset(cx, cy)) {
            drawOval(
                color = if (r % 2 == 0) pCol else sCol,
                topLeft = Offset(cx - radiusX, cy - radiusY),
                size = Size(radiusX * 2, radiusY * 2),
                style = Stroke(width = 4f)
            )
        }
    }
}

private fun DrawScope.drawMirrorSymmetry(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, cy: Float) {
    val pts = 30
    val step = (width/2) / (pts - 1)
    val pathLeft = Path()
    val pathRight = Path()
    for (i in 0 until pts) {
        val lx = i * step
        val rx = width - lx
        val wave = sin(phase * 4f + i * 0.5f) * 100f * sensitivity
        if (i == 0) {
            pathLeft.moveTo(lx, cy + wave)
            pathRight.moveTo(rx, cy + wave)
        } else {
            pathLeft.lineTo(lx, cy + wave)
            pathRight.lineTo(rx, cy + wave)
        }
    }
    drawPath(path = pathLeft, color = pCol, style = Stroke(width = 5f))
    drawPath(path = pathRight, color = sCol, style = Stroke(width = 5f))
}

private fun DrawScope.drawHexagonMesh(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val sizeHex = minDim * 0.08f
    for (r in -2..2) {
        for (c in -2..2) {
            val offsetX = if (r % 2 == 0) 0f else sizeHex * 1.5f
            val hx = cx + c * sizeHex * 3f + offsetX
            val hy = cy + r * sizeHex * 0.86f * 2f
            val pulse = sin(phase * 3f + r + c).absoluteValue * sensitivity
            val currentSize = sizeHex * (0.6f + pulse * 0.4f)
            val path = Path()
            for (i in 0..5) {
                val angle = i * 60f
                val rad = Math.toRadians(angle.toDouble())
                val px = hx + currentSize * cos(rad).toFloat()
                val py = hy + currentSize * sin(rad).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path = path, color = if ((r+c)%2==0) pCol.copy(alpha = 0.5f) else sCol.copy(alpha = 0.5f), style = Stroke(width = 3f))
        }
    }
}

private fun DrawScope.drawEnergyShield(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val radius = minDim * 0.32f
    drawCircle(color = pCol.copy(alpha = 0.1f + sin(phase * 5f).absoluteValue * 0.15f * sensitivity), radius = radius, center = Offset(cx, cy))
    drawCircle(color = sCol, radius = radius + sin(phase * 8f).absoluteValue * 12f * sensitivity, center = Offset(cx, cy), style = Stroke(width = 4f))
}

private fun DrawScope.drawDiamondGlow(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val diamonds = 4
    val base = minDim * 0.08f
    for (d in 0 until diamonds) {
        val size = base * (d + 1) * (1f + sin(phase - d * 0.5f).absoluteValue * 0.3f * sensitivity)
        val path = Path().apply {
            moveTo(cx, cy - size)
            lineTo(cx + size, cy)
            lineTo(cx, cy + size)
            lineTo(cx - size, cy)
            close()
        }
        drawPath(path = path, color = if (d % 2 == 0) pCol.copy(alpha = 0.6f) else tCol.copy(alpha = 0.4f), style = Stroke(width = 4f))
    }
}

private fun DrawScope.drawMeteorShower(meteors: List<Meteor>, isPlaying: Boolean, speed: Float, phase: Float, sensitivity: Float, pCol: Color, width: Float, height: Float) {
    for (m in meteors) {
        if (isPlaying) {
            m.xPct += m.speed * speed
            m.yPct += m.speed * speed
            if (m.xPct > 1.1f || m.yPct > 1.1f) { m.xPct = -0.2f; m.yPct = (Math.random() * 1.5 - 0.5).toFloat() }
        }
        val mx = m.xPct * width
        val my = m.yPct * height
        val len = m.len * (1f + sin(phase * 4f).absoluteValue * 0.5f * sensitivity)
        drawLine(
            brush = Brush.linearGradient(listOf(pCol.copy(alpha = 0.8f), Color.Transparent), start = Offset(mx, my), end = Offset(mx - len, my - len)),
            start = Offset(mx, my), end = Offset(mx - len, my - len), strokeWidth = 4f, cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnowfall(phase: Float, sensitivity: Float, width: Float, height: Float) {
    val flakes = 35
    for (i in 0 until flakes) {
        val fx = (width / flakes) * i + sin(phase + i).absoluteValue * 30f * sensitivity
        val fy = (height * (i.toFloat() / flakes) + phase * 40f) % height
        drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 5f + sin(phase + i).absoluteValue * 5f, center = Offset(fx, fy))
    }
}

private fun DrawScope.drawFireworks(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float) {
    val sparks = 24
    val radius = 100f * (sin(phase * 5f).absoluteValue + 0.1f) * sensitivity
    for (i in 0 until sparks) {
        val angle = (i.toFloat() / sparks) * 360f
        val rad = Math.toRadians(angle.toDouble())
        val px = cx + (radius * cos(rad)).toFloat()
        val py = cy + (radius * sin(rad)).toFloat()
        drawCircle(color = if (i % 2 == 0) pCol else sCol, radius = 5f, center = Offset(px, py))
    }
}

private fun DrawScope.drawOceanTides(phase: Float, sensitivity: Float, sCol: Color, width: Float, height: Float) {
    val path = Path()
    val baseline = height * 0.8f
    path.moveTo(0f, baseline)
    for (x in 0..10) {
        val px = x * (width / 10f)
        val py = baseline + sin(phase * 1.5f + x * 0.8f) * 40f * sensitivity
        if (x == 0) path.moveTo(px, py) else path.quadraticBezierTo(px - (width/20f), py - 10f, px, py)
    }
    path.lineTo(width, height)
    path.lineTo(0f, height)
    path.close()
    drawPath(path = path, color = sCol.copy(alpha = 0.5f))
}

private fun DrawScope.drawSpectrumX(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val len = minDim * 0.35f * (0.6f + sin(phase * 4f).absoluteValue * 0.4f * sensitivity)
    drawLine(color = pCol, start = Offset(cx - len, cy - len), end = Offset(cx + len, cy + len), strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(color = sCol, start = Offset(cx - len, cy + len), end = Offset(cx + len, cy - len), strokeWidth = 8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawUltraBass(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val radius = minDim * 0.28f + sin(phase * 8f) * 16f * sensitivity
    drawCircle(color = pCol.copy(alpha = 0.2f), radius = radius + 30f, center = Offset(cx, cy))
    drawCircle(color = sCol, radius = radius, center = Offset(cx, cy), style = Stroke(width = 8f))
}

private fun DrawScope.drawAuroraX(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, width: Float, height: Float) {
    drawAuroraFlow(phase, sensitivity, pCol, tCol, width, height)
    drawAuroraFlow(phase + 2f, sensitivity * 0.8f, tCol, pCol, width, height)
}

private fun DrawScope.drawGlassRing(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val r = minDim * 0.3f * (1f + sin(phase * 3f).absoluteValue * 0.15f * sensitivity)
    drawCircle(color = pCol.copy(alpha = 0.15f), radius = r, center = Offset(cx, cy))
    drawCircle(color = sCol, radius = r, center = Offset(cx, cy), style = Stroke(width = 12f))
}

private fun DrawScope.drawPlasmaX(phase: Float, sensitivity: Float, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val radius = minDim * 0.25f
    val sparks = 8
    for (i in 0 until sparks) {
        val angle = (i.toFloat() / sparks) * 360f + phase * 40f
        val rad = Math.toRadians(angle.toDouble())
        val dis = radius + sin(phase * 6f + i).absoluteValue * 40f * sensitivity
        val px = cx + (dis * cos(rad)).toFloat()
        val py = cy + (dis * sin(rad)).toFloat()
        drawLine(color = tCol, start = Offset(cx, cy), end = Offset(px, py), strokeWidth = 3f)
    }
}

private fun DrawScope.drawCyberWave(phase: Float, sensitivity: Float, pCol: Color, width: Float, height: Float) {
    val cy = height / 2f
    val step = width / 40f
    for (i in 0..40) {
        val x = i * step
        val h = sin(phase * 4f + i * 0.5f).absoluteValue * 120f * sensitivity
        drawRect(color = pCol, topLeft = Offset(x, cy - h/2), size = Size(step * 0.6f, h))
    }
}

private fun DrawScope.drawInfinityBars(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, height: Float) {
    drawLinearBars(phase, sensitivity, pCol, sCol, width, height)
    rotate(degrees = 180f, pivot = Offset(width/2, height/2)) {
        drawLinearBars(phase + 3f, sensitivity, sCol, pCol, width, height)
    }
}

private fun DrawScope.drawFuturePulse(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val radius = minDim * 0.32f
    drawCircle(color = pCol.copy(alpha = 0.1f), radius = radius, center = Offset(cx, cy))
    drawCircle(color = sCol, radius = radius * (0.8f + sin(phase * 3f).absoluteValue * 0.2f * sensitivity), center = Offset(cx, cy), style = Stroke(width = 4f))
}

private fun DrawScope.drawDigitalStorm(phase: Float, sensitivity: Float, pCol: Color, width: Float, height: Float) {
    val count = 25
    for (i in 0 until count) {
        val x = (width / count) * i
        val factor = sin(phase * 5f + i).absoluteValue
        val y = (height * factor + (phase * 80f)) % height
        drawCircle(color = pCol.copy(alpha = factor * sensitivity), radius = 6f + factor * 6f, center = Offset(x, y))
    }
}

private fun DrawScope.drawPrism(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val size = minDim * 0.2f * (1f + sin(phase * 3f).absoluteValue * 0.2f * sensitivity)
    val path = Path().apply {
        moveTo(cx, cy - size)
        lineTo(cx + size * 0.86f, cy + size * 0.5f)
        lineTo(cx - size * 0.86f, cy + size * 0.5f)
        close()
    }
    drawPath(path = path, color = pCol.copy(alpha = 0.3f))
    drawPath(path = path, color = sCol, style = Stroke(width = 4f))
}

private fun DrawScope.drawQuantum(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val count = 50
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 360f + phase * 30f
        val rad = Math.toRadians(angle.toDouble())
        val dist = minDim * 0.3f * sin(phase * 2f + i * 0.1f).absoluteValue * sensitivity
        val px = cx + (dist * cos(rad)).toFloat()
        val py = cy + (dist * sin(rad)).toFloat()
        drawCircle(color = if (i % 2 == 0) pCol else tCol, radius = 5f, center = Offset(px, py))
    }
}

private fun DrawScope.drawPulsingCore(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val baseRadius = minDim * 0.15f
    val pulse = sin(phase * 2f).absoluteValue * sensitivity
    val radius = baseRadius * (1f + pulse * 0.5f)
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(pCol.copy(alpha = 0.9f), pCol.copy(alpha = 0.0f)), center = Offset(cx, cy), radius = radius),
        radius = radius, center = Offset(cx, cy)
    )
    drawCircle(color = sCol, radius = radius * 0.3f, center = Offset(cx, cy))
}

private fun DrawScope.drawOrbitingDots(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val count = 30
    val orbitRadius = minDim * 0.3f
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 360f + phase * 20f
        val rad = Math.toRadians(angle.toDouble())
        val px = cx + (orbitRadius * cos(rad)).toFloat()
        val py = cy + (orbitRadius * sin(rad)).toFloat()
        drawCircle(color = if (i % 2 == 0) pCol else sCol, radius = 4f + sin(phase + i).absoluteValue * 4f, center = Offset(px, py))
    }
}

private fun DrawScope.drawMirrorBars(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, height: Float) {
    val barCount = 20
    val barWidth = width / (barCount * 2f)
    val maxBarHeight = height * 0.35f * sensitivity
    val centerY = height / 2f
    for (i in 0 until barCount) {
        val factor = sin(phase + i * 0.3f).absoluteValue
        val barHeight = 10f + factor * maxBarHeight
        val x = (width / 2f) + (i * barWidth)
        drawRoundRect(brush = Brush.verticalGradient(listOf(pCol, sCol)), topLeft = Offset(x, centerY - barHeight), size = Size(barWidth, barHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
        drawRoundRect(brush = Brush.verticalGradient(listOf(pCol, sCol)), topLeft = Offset(x, centerY), size = Size(barWidth, barHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
    }
}

private fun DrawScope.drawRainbowBars(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, height: Float) {
    val barCount = 30
    val barWidth = width / (barCount * 1.5f)
    val spacing = barWidth * 0.5f
    val startX = (width - (barCount * (barWidth + spacing) - spacing)) / 2f
    val maxBarHeight = height * 0.5f * sensitivity
    for (i in 0 until barCount) {
        val factor = sin(phase + i * 0.25f).absoluteValue
        val barHeight = 15f + factor * maxBarHeight
        val x = startX + i * (barWidth + spacing)
        val y = height - barHeight - 40f
        val hue = (i * 12f + phase * 50f) % 360f
        drawRoundRect(
            brush = Brush.horizontalGradient(colors = listOf(Color.hsv(hue, 1f, 1f), Color.hsv((hue + 60f) % 360f, 1f, 1f))),
            topLeft = Offset(x, y), size = Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

private fun DrawScope.drawPeakBars(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, width: Float, height: Float) {
    val barCount = 30
    val barWidth = width / (barCount * 1.5f)
    val spacing = barWidth * 0.5f
    val startX = (width - (barCount * (barWidth + spacing) - spacing)) / 2f
    val maxBarHeight = height * 0.5f * sensitivity
    for (i in 0 until barCount) {
        val factor = sin(phase + i * 0.25f).absoluteValue
        val barHeight = 15f + factor * maxBarHeight
        val x = startX + i * (barWidth + spacing)
        val y = height - barHeight - 40f
        drawRoundRect(
            brush = Brush.verticalGradient(colors = listOf(pCol, sCol)),
            topLeft = Offset(x, y), size = Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
        )
        drawRect(color = Color.White, topLeft = Offset(x, y), size = Size(barWidth, 4f))
    }
}

private fun DrawScope.drawGalaxyCloud(phase: Float, sensitivity: Float, pCol: Color, tCol: Color, cx: Float, cy: Float, minDim: Float) {
    val count = 60
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 360f + phase * 10f
        val rad = Math.toRadians(angle.toDouble())
        val dist = minDim * 0.25f * sin(phase * 1.5f + i * 0.2f).absoluteValue * sensitivity
        val px = cx + (dist * cos(rad)).toFloat()
        val py = cy + (dist * sin(rad)).toFloat()
        drawCircle(color = if (i % 3 == 0) pCol else tCol, radius = 3f + sin(phase + i).absoluteValue * 5f, center = Offset(px, py))
    }
}

// Retained data class structs for visualizer models
private class Star(var x: Float, var y: Float, var z: Float, val size: Float)
private class MatrixColumn(var xPct: Float, var yPct: Float, val speed: Float, val chars: List<String>)
private class Bubble(var xPct: Float, var yPct: Float, val speed: Float, val size: Float)
private class FireParticle(var xScale: Float, var yPct: Float, val speed: Float)
private class Meteor(var xPct: Float, var yPct: Float, val speed: Float, val len: Float)

// ---------- AVEE-STYLE TEMPLATE RENDERERS ----------
private fun DrawScope.drawRadialPulse(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val ringCount = 6
    val maxRadius = minDim * 0.45f
    for (r in 0 until ringCount) {
        val baseRadius = maxRadius * ((r + 1).toFloat() / ringCount)
        val pulse = sin(phase * 2f + r * 0.8f).absoluteValue * sensitivity * maxRadius * 0.25f
        val radius = baseRadius + pulse
        drawCircle(
            color = pCol.copy(alpha = 0.15f + (1f - r.toFloat() / ringCount) * 0.4f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f + (ringCount - r).toFloat() * 0.5f)
        )
    }
    drawCircle(
        color = sCol.copy(alpha = 0.9f),
        radius = 12f + sin(phase * 4f).absoluteValue * 8f * sensitivity,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawParticleBurst(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val count = 80
    val beat = sin(phase * 3f).absoluteValue
    val maxDist = minDim * 0.4f * (0.6f + beat * sensitivity * 0.4f)
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 360f + phase * 15f
        val rad = Math.toRadians(angle.toDouble())
        val dist = maxDist * (0.2f + sin(phase * 2f + i * 0.4f).absoluteValue * 0.8f)
        val px = cx + (dist * cos(rad)).toFloat()
        val py = cy + (dist * sin(rad)).toFloat()
        val alpha = 0.3f + beat * 0.7f
        drawCircle(
            color = if (i % 2 == 0) pCol.copy(alpha = alpha) else sCol.copy(alpha = alpha),
            radius = 2f + beat * 4f,
            center = Offset(px, py)
        )
    }
}

private fun DrawScope.drawSpectrumRings(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val rings = 10
    val pointsPerRing = 64
    val maxRadius = minDim * 0.42f
    for (r in 0 until rings) {
        val baseRadius = maxRadius * ((r + 1).toFloat() / rings)
        val points = pointsPerRing / (r + 1)
        for (i in 0 until points) {
            val angle = (i.toFloat() / points) * 360f
            val rad = Math.toRadians(angle.toDouble())
            val wave = sin(phase * 3f + i * 0.3f + r * 0.5f).absoluteValue
            val radius = baseRadius + wave * minDim * 0.06f * sensitivity
            val px = cx + (radius * cos(rad)).toFloat()
            val py = cy + (radius * sin(rad)).toFloat()
            drawCircle(
                color = if (i % 2 == 0) pCol else sCol,
                radius = 2f + wave * 3f,
                center = Offset(px, py)
            )
        }
    }
}

private fun DrawScope.drawKaleidoscopeWave(phase: Float, sensitivity: Float, pCol: Color, sCol: Color, cx: Float, cy: Float, minDim: Float) {
    val segments = 8
    val pointsPerSegment = 24
    val maxRadius = minDim * 0.35f
    for (seg in 0 until segments) {
        val baseAngle = (seg.toFloat() / segments) * 360f
        for (i in 0 until pointsPerSegment) {
            val t = i.toFloat() / pointsPerSegment
            val angle = baseAngle + t * 60f
            val rad = Math.toRadians(angle.toDouble())
            val wave = sin(phase * 2.5f + i * 0.4f + seg * 0.7f).absoluteValue
            val radius = t * maxRadius * (0.7f + wave * 0.3f * sensitivity)
            val px = cx + (radius * cos(rad)).toFloat()
            val py = cy + (radius * sin(rad)).toFloat()
            drawCircle(
                color = if (i % 3 == 0) pCol else if (i % 3 == 1) sCol else Color.White,
                radius = 1.5f + wave * 3f,
                center = Offset(px, py)
            )
        }
    }
}
