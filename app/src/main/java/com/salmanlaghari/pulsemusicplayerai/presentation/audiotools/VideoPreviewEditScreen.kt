package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.automirrored.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.VisualizerPreset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Live Visualizer Preview & Edit Screen for MP3 → MP4 Video Studio
 * Users can see real-time visualizer animation, change presets,
 * adjust colors, and export to video.
 */
@Composable
fun VideoPreviewEditScreen(
    onNavigateBack: () -> Unit,
    onExport: (Uri, String, VisualizerPreset, String, String) -> Unit = { _, _, _, _, _ -> }
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPreset by remember { mutableStateOf(VisualizerPreset.CIRCULAR_BARS) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var selectedBgStyle by remember { mutableStateOf("Gradient") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var outputFileName by remember { mutableStateOf("Visualizer_Video") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            isPlaying = true
        }
    }

    // Simulated playback animation
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Video Studio Pro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Live Preview & Edit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Visualizer Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background
                    val bgColors = when (selectedBgStyle) {
                        "Gradient" -> listOf(Color(0xFF1a1a2e), Color(0xFF16213e), Color(0xFF0f3460))
                        "Dark" -> listOf(Color(0xFF0a0a0a), Color(0xFF1a1a1a))
                        "Neon" -> listOf(Color(0xFF0d0d0d), Color(0xFF1a0033), Color(0xFF000d1a))
                        else -> listOf(Color(0xFF1a1a2e), Color(0xFF16213e))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(bgColors))
                    )

                    // Visualizer Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val centerY = h / 2
                        val centerX = w / 2
                        val p = if (isPlaying) animProgress else 0.5f

                        when (selectedPreset) {
                            VisualizerPreset.CIRCULAR_BARS -> {
                                val barCount = 48
                                val radius = minOf(w, h) * 0.28f
                                for (i in 0 until barCount) {
                                    val angle = (i.toFloat() / barCount) * 2 * Math.PI.toFloat() - Math.PI / 2
                                    val barHeight = (30 + 60 * sin(p * 2 * Math.PI + i * 0.3f).coerceIn(0f, 1f)).dp.toPx()
                                    val startX = centerX + radius * cos(angle)
                                    val startY = centerY + radius * sin(angle)
                                    val endX = centerX + (radius + barHeight) * cos(angle)
                                    val endY = centerY + (radius + barHeight) * sin(angle)
                                    drawLine(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFFFF6B35), Color(0xFF7C5CFF))
                                        ),
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 6.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                                // Center circle
                                drawCircle(
                                    color = Color(0xFFFF6B35).copy(alpha = 0.3f),
                                    radius = radius * 0.6f,
                                    center = Offset(centerX, centerY)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.8f),
                                    radius = radius * 0.55f,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                            VisualizerPreset.WAVEFORM -> {
                                val points = 100
                                val path = androidx.compose.ui.graphics.Path()
                                for (i in 0 until points) {
                                    val x = (i.toFloat() / points) * w
                                    val amplitude = h * 0.2f * sin(p * 2 * Math.PI + i * 0.15f).coerceIn(-1f, 1f)
                                    val y = centerY + amplitude
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C5CFF))),
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                                // Mirror wave
                                val mirrorPath = androidx.compose.ui.graphics.Path()
                                for (i in 0 until points) {
                                    val x = (i.toFloat() / points) * w
                                    val amplitude = h * 0.12f * sin(p * 2 * Math.PI + i * 0.15f + 1f).coerceIn(-1f, 1f)
                                    val y = centerY - amplitude
                                    if (i == 0) mirrorPath.moveTo(x, y) else mirrorPath.lineTo(x, y)
                                }
                                drawPath(
                                    path = mirrorPath,
                                    brush = Brush.horizontalGradient(listOf(Color(0xFFFF6B35), Color(0xFFE74C8D))),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            VisualizerPreset.BARS -> {
                                val barCount = 32
                                val barWidth = w / (barCount * 1.5f)
                                val gap = barWidth * 0.5f
                                for (i in 0 until barCount) {
                                    val barHeight = (40 + 100 * sin(p * 2 * Math.PI + i * 0.25f).coerceIn(0f, 1f)).dp.toPx()
                                    val x = i * (barWidth + gap) + gap
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF7C5CFF), Color(0xFFFF6B35))
                                        ),
                                        topLeft = Offset(x, centerY - barHeight / 2),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                                    )
                                }
                            }
                            VisualizerPreset.SPECTRUM -> {
                                val cols = 64
                                val colWidth = w / cols
                                for (i in 0 until cols) {
                                    val intensity = sin(p * 2 * Math.PI + i * 0.2f).coerceIn(0f, 1f)
                                    val colHeight = h * 0.6f * intensity
                                    val hue = (i.toFloat() / cols) * 360f
                                    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.8f, 0.9f)))
                                    drawRect(
                                        color = color.copy(alpha = 0.7f + 0.3f * intensity),
                                        topLeft = Offset(i * colWidth, centerY - colHeight / 2),
                                        size = androidx.compose.ui.geometry.Size(colWidth - 1, colHeight)
                                    )
                                }
                            }
                            VisualizerPreset.NEON_PULSE -> {
                                val pulseRadius = (80 + 40 * sin(p * 2 * Math.PI * 3).coerceIn(-1f, 1f)).dp.toPx()
                                // Outer glow
                                drawCircle(
                                    color = Color(0xFF7C5CFF).copy(alpha = 0.15f),
                                    radius = pulseRadius * 1.5f,
                                    center = Offset(centerX, centerY)
                                )
                                drawCircle(
                                    color = Color(0xFFFF6B35).copy(alpha = 0.25f),
                                    radius = pulseRadius * 1.2f,
                                    center = Offset(centerX, centerY)
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFE74C8D), Color(0xFF7C5CFF), Color.Transparent)
                                    ),
                                    radius = pulseRadius,
                                    center = Offset(centerX, centerY)
                                )
                                // Inner ring
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.6f),
                                    radius = pulseRadius * 0.4f,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                            else -> {
                                // Fallback simple bars
                                val barCount = 24
                                val barWidth = w / (barCount * 1.5f)
                                for (i in 0 until barCount) {
                                    val barHeight = (30 + 80 * sin(p * 2 * Math.PI + i * 0.3f).coerceIn(0f, 1f)).dp.toPx()
                                    val x = i * (barWidth + barWidth * 0.5f) + barWidth * 0.5f
                                    drawRect(
                                        color = Color(0xFF00E5FF).copy(alpha = 0.6f + 0.4f * sin(p * 2 * Math.PI + i * 0.3f).coerceIn(0f, 1f)),
                                        topLeft = Offset(x, centerY - barHeight / 2),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                                    )
                                }
                            }
                        }
                    }

                    // Track info overlay
                    if (selectedUri != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6B35),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedUri?.lastPathSegment ?: "Audio Track",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // "No audio" placeholder
                    if (selectedUri == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Select audio to preview",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { filePickerLauncher.launch("audio/*") },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { isPlaying = false; progress = 0f },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visualizer Preset Selector
            Text("Visualizer Style", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VisualizerPreset.values().toList()) { preset ->
                    val isSelected = selectedPreset == preset
                    Card(
                        modifier = Modifier
                            .height(38.dp)
                            .clickable { selectedPreset = preset },
                        shape = RoundedCornerShape(19.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text(
                                preset.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Background Style
            Text("Background Style", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Gradient", "Dark", "Neon").forEach { style ->
                    val isSelected = selectedBgStyle == style
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { selectedBgStyle = style },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(style, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resolution
            Text("Export Resolution", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("720p", "1080p").forEach { res ->
                    val isSelected = selectedResolution == res
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { selectedResolution = res },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(res, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Export Button
            Button(
                onClick = {
                    selectedUri?.let { uri ->
                        onExport(uri, outputFileName, selectedPreset, selectedBgStyle, selectedResolution)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedUri != null
            ) {
                Text("🎬  Render & Export Video", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
