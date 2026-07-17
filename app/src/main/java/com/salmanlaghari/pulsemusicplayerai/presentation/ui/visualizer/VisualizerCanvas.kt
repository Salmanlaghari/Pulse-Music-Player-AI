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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerCanvas(
    preset: VisualizerPreset,
    isPlaying: Boolean,
    sensitivity: Float, // Sensitivity scale multiplier (e.g. 0.5f to 2.0f)
    speed: Float,       // Playback speed multiplier (e.g. 0.5f to 2.0f)
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary
) {
    // Continuous phase ticker driving physics and wave animation math
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

    // Static structures for Starfield and Matrix Rain to avoid re-allocations
    val stars = remember {
        List(80) {
            Star(
                x = (Math.random() * 2 - 1).toFloat(),
                y = (Math.random() * 2 - 1).toFloat(),
                z = (Math.random()).toFloat(),
                size = (1f + Math.random() * 3).toFloat()
            )
        }
    }

    val matrixColumns = remember {
        List(40) {
            MatrixColumn(
                xPct = (Math.random()).toFloat(),
                yPct = (Math.random()).toFloat(),
                speed = (0.01f + Math.random() * 0.02f).toFloat(),
                chars = List(15) { ('A'..'Z').random().toString() }
            )
        }
    }

    val bubbles = remember {
        List(25) {
            Bubble(
                xPct = (Math.random()).toFloat(),
                yPct = (Math.random()).toFloat(),
                speed = (0.005f + Math.random() * 0.01f).toFloat(),
                size = (8f + Math.random() * 16).toFloat()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val minDim = minOf(width, height)

        when (preset) {
            VisualizerPreset.CIRCULAR_BARS -> {
                // Circular/Radial Bars arrangement extending outward
                val barCount = 72
                val innerRadius = minDim * 0.25f
                val maxBarLen = minDim * 0.2f * sensitivity

                for (i in 0 until barCount) {
                    val angle = (i.toFloat() / barCount) * 360f
                    val radian = Math.toRadians(angle.toDouble())

                    // Calculate bar height dynamically
                    val frequencyFactor = sin(phase + i * 0.15f).absoluteValue
                    val barLen = innerRadius + (frequencyFactor * maxBarLen)

                    val startX = centerX + (innerRadius * cos(radian)).toFloat()
                    val startY = centerY + (innerRadius * sin(radian)).toFloat()
                    val endX = centerX + (barLen * cos(radian)).toFloat()
                    val endY = centerY + (barLen * sin(radian)).toFloat()

                    // Rotate colors dynamically across spectrum
                    val alpha = 0.5f + (frequencyFactor * 0.5f)
                    val color = if (i % 2 == 0) primaryColor else secondaryColor

                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            VisualizerPreset.CONCENTRIC_RINGS -> {
                // Multiple concentric wave rings expanding/pulsating
                val ringCount = 5
                val baseRadius = minDim * 0.12f

                for (r in 0 until ringCount) {
                    val scaleFactor = 1f + sin(phase - r * 0.4f).absoluteValue * 0.35f * sensitivity
                    val radius = baseRadius * (r + 1) * scaleFactor
                    val alpha = (1f - (r.toFloat() / ringCount)) * 0.7f

                    drawCircle(
                        color = if (r % 2 == 0) primaryColor.copy(alpha = alpha) else tertiaryColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 4f + r)
                    )
                }
            }

            VisualizerPreset.LINEAR_BARS -> {
                // Classic visualizer bars standing on a baseline
                val barCount = 30
                val barWidth = width / (barCount * 1.5f)
                val spacing = barWidth * 0.5f
                val startX = (width - (barCount * (barWidth + spacing) - spacing)) / 2f
                val maxBarHeight = height * 0.5f * sensitivity

                for (i in 0 until barCount) {
                    val hFactor = sin(phase + i * 0.25f).absoluteValue
                    val barHeight = (15f + hFactor * maxBarHeight)

                    val x = startX + i * (barWidth + spacing)
                    val y = height - barHeight - 40f

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            VisualizerPreset.PARTICLE_ORB -> {
                // Central glowing orb with circling orbiting particles
                val orbRadius = minDim * 0.15f * (1f + sin(phase * 1.5f).absoluteValue * 0.1f * sensitivity)

                // Draw main glow orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.8f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = orbRadius * 1.5f
                    ),
                    radius = orbRadius * 1.5f,
                    center = Offset(centerX, centerY)
                )

                // Render orbiting particles
                val particleCount = 45
                for (i in 0 until particleCount) {
                    val orbitAngle = (i.toFloat() / particleCount) * 360f + (phase * 15f)
                    val rad = Math.toRadians(orbitAngle.toDouble())
                    val orbitRadius = orbRadius * 1.3f + sin(phase * 2f + i * 0.5f).absoluteValue * 80f * sensitivity

                    val px = centerX + (orbitRadius * cos(rad)).toFloat()
                    val py = centerY + (orbitRadius * sin(rad)).toFloat()
                    val pSize = 6f + sin(phase + i).absoluteValue * 10f

                    drawCircle(
                        color = if (i % 3 == 0) tertiaryColor else secondaryColor,
                        radius = pSize,
                        center = Offset(px, py)
                    )
                }
            }

            VisualizerPreset.STARFIELD -> {
                // Moving forward stars responding to dynamic simulation pace
                for (star in stars) {
                    if (isPlaying) {
                        star.z -= 0.008f * speed
                        if (star.z <= 0f) {
                            star.z = 1f
                            star.x = (Math.random() * 2 - 1).toFloat()
                            star.y = (Math.random() * 2 - 1).toFloat()
                        }
                    }

                    val sx = centerX + (star.x / star.z) * (width * 0.3f)
                    val sy = centerY + (star.y / star.z) * (height * 0.3f)

                    if (sx in 0f..width && sy in 0f..height) {
                        val activeScale = (1f - star.z) * 12f * sensitivity
                        val sizeFinal = star.size * activeScale.coerceAtLeast(0.5f)
                        val opacity = (1f - star.z).coerceIn(0f, 1f)

                        drawCircle(
                            color = primaryColor.copy(alpha = opacity),
                            radius = sizeFinal,
                            center = Offset(sx, sy)
                        )
                    }
                }
            }

            VisualizerPreset.FLUID_WAVE -> {
                // Fluid Bezier curves layering smoothly
                val path1 = Path()
                val path2 = Path()

                path1.moveTo(0f, centerY)
                path2.moveTo(0f, centerY + 30f)

                val wavePoints = 8
                val stepX = width / (wavePoints - 1)

                for (i in 0 until wavePoints) {
                    val px = i * stepX
                    val y1Offset = sin(phase + i * 0.8f) * 120f * sensitivity
                    val y2Offset = cos(phase * 1.2f + i * 0.6f) * 90f * sensitivity

                    if (i == 0) {
                        path1.moveTo(px, centerY + y1Offset)
                        path2.moveTo(px, centerY + 30f + y2Offset)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevY1 = centerY + sin(phase + (i - 1) * 0.8f) * 120f * sensitivity
                        val prevY2 = centerY + 30f + cos(phase * 1.2f + (i - 1) * 0.6f) * 90f * sensitivity

                        path1.quadraticBezierTo(prevX + stepX / 2f, prevY1, px, centerY + y1Offset)
                        path2.quadraticBezierTo(prevX + stepX / 2f, prevY2, px, centerY + 30f + y2Offset)
                    }
                }

                drawPath(
                    path = path1,
                    color = primaryColor.copy(alpha = 0.6f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = path2,
                    color = secondaryColor.copy(alpha = 0.5f),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            VisualizerPreset.ISOMETRIC_GRID -> {
                // 3D Isometric cuboids pulsing
                val cols = 6
                val rows = 6
                val sizeIsometric = minDim * 0.05f

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        // Math projections for Isometric perspective
                        val isoX = centerX + (c - r) * sizeIsometric * 1.5f
                        val isoY = centerY * 0.7f + (c + r) * sizeIsometric * 0.75f

                        val depthVal = sin(phase + (r + c) * 0.4f).absoluteValue
                        val barLen = (20f + depthVal * sizeIsometric * 3f) * sensitivity

                        // Draw top facet
                        val topPath = Path().apply {
                            moveTo(isoX, isoY - barLen)
                            lineTo(isoX + sizeIsometric * 1.5f, isoY - sizeIsometric * 0.75f - barLen)
                            lineTo(isoX, isoY - sizeIsometric * 1.5f - barLen)
                            lineTo(isoX - sizeIsometric * 1.5f, isoY - sizeIsometric * 0.75f - barLen)
                            close()
                        }
                        drawPath(topPath, tertiaryColor.copy(alpha = 0.9f))

                        // Draw left side facet
                        val leftPath = Path().apply {
                            moveTo(isoX - sizeIsometric * 1.5f, isoY - sizeIsometric * 0.75f - barLen)
                            lineTo(isoX, isoY - barLen)
                            lineTo(isoX, isoY)
                            lineTo(isoX - sizeIsometric * 1.5f, isoY - sizeIsometric * 0.75f)
                            close()
                        }
                        drawPath(leftPath, primaryColor.copy(alpha = 0.7f))

                        // Draw right side facet
                        val rightPath = Path().apply {
                            moveTo(isoX, isoY - barLen)
                            lineTo(isoX + sizeIsometric * 1.5f, isoY - sizeIsometric * 0.75f - barLen)
                            lineTo(isoX + sizeIsometric * 1.5f, isoY - sizeIsometric * 0.75f)
                            lineTo(isoX, isoY)
                            close()
                        }
                        drawPath(rightPath, secondaryColor.copy(alpha = 0.5f))
                    }
                }
            }

            VisualizerPreset.FLOATING_BUBBLES -> {
                // Bubbles rising and scaling dynamically
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
                    val pulseScale = 1f + sin(phase + bubble.xPct * 10f).absoluteValue * 0.3f * sensitivity
                    val bSize = bubble.size * pulseScale

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(bx, by),
                            radius = bSize
                        ),
                        radius = bSize,
                        center = Offset(bx, by)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = bSize,
                        center = Offset(bx, by),
                        style = Stroke(width = 2f)
                    )
                }
            }

            VisualizerPreset.KALEIDOSCOPE -> {
                // Symmetric reflected visualizer geometry
                val segments = 8
                val maxLen = minDim * 0.35f * sensitivity

                for (s in 0 until segments) {
                    val angleOffset = s * (360f / segments)
                    rotate(degrees = angleOffset + phase * 10f, pivot = Offset(centerX, centerY)) {
                        val path = Path()
                        path.moveTo(centerX, centerY)

                        val waveValue = sin(phase * 2f).absoluteValue * maxLen
                        val endPtX = centerX + waveValue
                        val endPtY = centerY + sin(phase * 1.5f) * 100f

                        path.lineTo(endPtX, endPtY)
                        drawPath(
                            path = path,
                            color = secondaryColor.copy(alpha = 0.6f),
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )

                        drawCircle(
                            color = primaryColor,
                            radius = 12f * (1f + sin(phase).absoluteValue),
                            center = Offset(endPtX, endPtY)
                        )
                    }
                }
            }

            VisualizerPreset.DOUBLE_HELIX -> {
                // DNA rotating double helix wave nodes
                val nodeCount = 20
                val spanX = width * 0.8f
                val startNodeX = (width - spanX) / 2f
                val spacingNode = spanX / (nodeCount - 1)

                for (i in 0 until nodeCount) {
                    val nx = startNodeX + i * spacingNode
                    val angleOffset = i * 0.4f + phase * 2f

                    // Strand A
                    val yA = centerY + sin(angleOffset) * 120f * sensitivity
                    val sizeA = 12f + cos(angleOffset) * 6f

                    // Strand B
                    val yB = centerY - sin(angleOffset) * 120f * sensitivity
                    val sizeB = 12f - cos(angleOffset) * 6f

                    // Connector line
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(nx, yA),
                        end = Offset(nx, yB),
                        strokeWidth = 3f
                    )

                    // Node A
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.8f),
                        radius = sizeA,
                        center = Offset(nx, yA)
                    )

                    // Node B
                    drawCircle(
                        color = tertiaryColor.copy(alpha = 0.8f),
                        radius = sizeB,
                        center = Offset(nx, yB)
                    )
                }
            }

            VisualizerPreset.TUNNEL_WARP -> {
                // Concentric shapes giving depth tunnel perception
                val tunnelRings = 7
                val baseTunnelSize = minDim * 0.04f

                for (i in 0 until tunnelRings) {
                    val sizeFactor = (i + 1) * baseTunnelSize * (1f + sin(phase - i * 0.3f).absoluteValue * 0.4f * sensitivity)
                    val rotationDeg = phase * 15f + i * 10f

                    rotate(degrees = rotationDeg, pivot = Offset(centerX, centerY)) {
                        drawRect(
                            color = if (i % 2 == 0) primaryColor.copy(alpha = 0.6f) else secondaryColor.copy(alpha = 0.4f),
                            topLeft = Offset(centerX - sizeFactor, centerY - sizeFactor),
                            size = Size(sizeFactor * 2f, sizeFactor * 2f),
                            style = Stroke(width = 4f + i)
                        )
                    }
                }
            }

            VisualizerPreset.HEARTBEAT_PULSAR -> {
                // Pulsar waveform expanding symmetrically from the center
                val heartbeatPath = Path()
                heartbeatPath.moveTo(0f, centerY)

                val pointsPulsar = 40
                val pxStep = width / (pointsPulsar - 1)

                for (i in 0 until pointsPulsar) {
                    val hx = i * pxStep
                    val distFromCenter = (centerX - hx).absoluteValue / centerX
                    val amp = (1f - distFromCenter).coerceAtLeast(0f)

                    // Generate a double-peak heartbeat rhythm
                    val wavePattern = sin(phase * 4f) * 150f + cos(phase * 2f) * 80f
                    val hy = centerY + wavePattern * amp * sensitivity

                    if (i == 0) {
                        heartbeatPath.moveTo(hx, hy)
                    } else {
                        heartbeatPath.lineTo(hx, hy)
                    }
                }

                drawPath(
                    path = heartbeatPath,
                    color = primaryColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Draw central pulsing heart node
                val pulsarHeartRadius = 24f * (1f + sin(phase * 4f).absoluteValue * 0.3f * sensitivity)
                drawCircle(
                    color = Color.Red.copy(alpha = 0.85f),
                    radius = pulsarHeartRadius,
                    center = Offset(centerX, centerY)
                )
            }

            VisualizerPreset.FLAME_SPECTRUM -> {
                // Rising trails of dynamic warm glow
                val flameCount = 24
                val flameWidth = width / (flameCount * 1.5f)
                val spacingFlame = flameWidth * 0.5f
                val startFlameX = (width - (flameCount * (flameWidth + spacingFlame) - spacingFlame)) / 2f

                for (i in 0 until flameCount) {
                    val waveAmp = sin(phase + i * 0.4f).absoluteValue
                    val fHeight = (30f + waveAmp * height * 0.55f) * sensitivity

                    val fx = startFlameX + i * (flameWidth + spacingFlame)
                    val fy = height - fHeight - 20f

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Yellow, Color.Red)
                        ),
                        topLeft = Offset(fx, fy),
                        size = Size(flameWidth, fHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(flameWidth / 2f, flameWidth / 2f)
                    )
                }
            }

            VisualizerPreset.MATRIX_RAIN -> {
                // Falling digital code columns reacting to ticks
                for (col in matrixColumns) {
                    if (isPlaying) {
                        col.yPct += col.speed * speed
                        if (col.yPct >= 1.1f) {
                            col.yPct = -0.1f
                            col.xPct = (Math.random()).toFloat()
                        }
                    }

                    val mcx = col.xPct * width
                    val mcyBase = col.yPct * height

                    val columnCount = col.chars.size
                    for (charIndex in 0 until columnCount) {
                        val mcy = mcyBase - (charIndex * 24f)
                        if (mcy in 0f..height) {
                            val activeVal = sin(phase * 2f + charIndex).absoluteValue
                            val charAlpha = (1f - (charIndex.toFloat() / columnCount)) * (0.3f + activeVal * 0.7f) * sensitivity

                            // Render Matrix streams using beautiful thin indicators
                            drawCircle(
                                color = Color.Green.copy(alpha = charAlpha),
                                radius = 4f + charAlpha * 8f,
                                center = Offset(mcx, mcy)
                            )
                        }
                    }
                }
            }

            VisualizerPreset.SOUND_RIBBON -> {
                // Single ribbon line wrapping across
                val ribbonPath = Path()
                ribbonPath.moveTo(0f, centerY)

                val ribCount = 50
                val rxStep = width / (ribCount - 1)

                for (i in 0 until ribCount) {
                    val rx = i * rxStep
                    val ry = centerY + sin(phase + i * 0.3f) * 140f * sensitivity + cos(phase * 0.5f + i * 0.1f) * 60f

                    if (i == 0) {
                        ribbonPath.moveTo(rx, ry)
                    } else {
                        val prevRx = (i - 1) * rxStep
                        val prevRy = centerY + sin(phase + (i - 1) * 0.3f) * 140f * sensitivity + cos(phase * 0.5f + (i - 1) * 0.1f) * 60f

                        ribbonPath.quadraticBezierTo(prevRx + rxStep / 2f, prevRy, rx, ry)
                    }
                }

                drawPath(
                    path = ribbonPath,
                    color = primaryColor,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )

                // Extra secondary offset ribbon to create complex layered depth
                val layerPath = Path()
                layerPath.moveTo(0f, centerY)
                for (i in 0 until ribCount) {
                    val rx = i * rxStep
                    val ry = centerY + cos(phase + i * 0.25f) * 120f * sensitivity + sin(phase * 0.4f + i * 0.15f) * 50f
                    if (i == 0) {
                        layerPath.moveTo(rx, ry)
                    } else {
                        layerPath.lineTo(rx, ry)
                    }
                }
                drawPath(
                    path = layerPath,
                    color = secondaryColor.copy(alpha = 0.5f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// Optimization models for starfield, matrix code elements, and rising bubbles
private class Star(
    var x: Float,
    var y: Float,
    var z: Float,
    val size: Float
)

private class MatrixColumn(
    var xPct: Float,
    var yPct: Float,
    val speed: Float,
    val chars: List<String>
)

private class Bubble(
    var xPct: Float,
    var yPct: Float,
    val speed: Float,
    val size: Float
)
