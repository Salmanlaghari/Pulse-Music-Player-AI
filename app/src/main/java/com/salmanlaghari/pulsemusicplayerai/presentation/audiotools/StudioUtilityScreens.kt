package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

import android.net.Uri
import com.salmanlaghari.pulsemusicplayerai.presentation.ui.visualizer.VisualizerPreset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import com.salmanlaghari.pulsemusicplayerai.domain.model.AudioFormat
import com.salmanlaghari.pulsemusicplayerai.domain.model.CompressionPreset
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.delay

// --- 1. AUDIO CONVERTER SCREEN ---

@Composable
fun ConverterToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var targetFormat by remember { mutableStateOf(AudioFormat.MP3) }
    var outputFileName by remember { mutableStateOf("Converted_Audio_Track") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Audio Converter Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio File to Convert", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Audio"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Target Audio Format", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Format Selector Buttons Row Grid
                val formats = AudioFormat.values()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.take(3).forEach { format ->
                        val isSelected = targetFormat == format
                        Card(
                            modifier = Modifier.weight(1f).height(50.dp).clickable { targetFormat = format },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(format.name, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    formats.takeLast(3).forEach { format ->
                        val isSelected = targetFormat == format
                        Card(
                            modifier = Modifier.weight(1f).height(50.dp).clickable { targetFormat = format },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(format.name, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert Audio File", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another file", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.convertAudio(uri, outputFileName, targetFormat)
                        }
                    }
                ) {
                    Text("Export", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Helper time formatter for preview screen
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

// Helper bytes size formatter for preview screen
private fun formatBytes(bytes: Long): String {
    val sizeMb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(java.util.Locale.getDefault(), "%.2f MB", sizeMb)
}

@Composable
fun VideoPreviewEditScreen(
    file: com.salmanlaghari.pulsemusicplayerai.domain.model.ExportedFile,
    viewModel: AudioStudioViewModel,
    onNavigateBackToStudio: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Initialize ExoPlayer to play the newly generated MP4 video file
    val videoUri = Uri.parse(file.uriString)
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onNavigateBackToStudio()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preview & Edit Video", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Professional ExoPlayer View container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video Metadata details panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Render details:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Filename: ${file.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Duration: ${formatTime(file.duration)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                Text("Filesize: ${formatBytes(file.size)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Premium action buttons row: Save, Share, Discard/Re-render
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Discard/Re-render
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.deleteExport(file)
                    onNavigateBackToStudio()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Discard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }

            // Share
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.shareExport(file)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1.2f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Share", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            // Save to Device
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToLibrary()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.5f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save to Device", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// --- 5. PREMIUM VIDEO STUDIO COMPONENT ---

enum class VideoStudioType(val displayName: String, val description: String) {
    MP3_TO_MP4("MP3 → MP4 Visualizer", "Standard spectrum visualizer video from MP3 files."),
    ALBUM_ART("Album Art Video", "Overlay rotating or static album artwork in the center of the video."),
    LYRICS("Lyrics Video", "Incorporate synced text/lrc lyric sheets with beautiful backdrop flows."),
    WAVEFORM("Waveform Video", "Horizontal linear fluid waveforms drawing across the screen."),
    SPECTRUM("Spectrum Video", "Traditional dual-ended spectrogram bars responsive to sines."),
    NEON("Neon Video", "High-frequency neon colors and glowing elements outlining the waveform."),
    CIRCULAR("Circular Visualizer Video", "Dynamic concentric circular rings scaling with transients."),
    AUDIO_STATUS("Audio Status Video", "Compact, highly stylized status video overlay with track metadata."),
    STORY_9_16("Story Video (9:16)", "Portrait mode video layout customized for Instagram/TikTok stories."),
    YOUTUBE_16_9("YouTube Landscape (16:9)", "Cinema landscape layout with wide screen visualization ratios.")
}

@Composable
fun VideoStudioScreen(
    type: VideoStudioType,
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    var outputFileName by remember { mutableStateOf("${type.name}_Visual_Studio_Export") }
    var selectedPreset by remember { mutableStateOf(VisualizerPreset.CIRCULAR_BARS) }
    var selectedBgStyle by remember { mutableStateOf("Album Art") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var rotateAlbumArt by remember { mutableStateOf(true) }
    var lyricText by remember { mutableStateOf("Enjoy the premium acoustic sound...") }
    var waveThickness by remember { mutableStateOf(4.0f) }
    var barCount by remember { mutableStateOf(64f) }
    var neonGlowRadius by remember { mutableStateOf(10f) }
    var circularRadius by remember { mutableStateOf(150f) }
    var statusCardStyle by remember { mutableStateOf("Retro Vinyl") }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Live 16:9 Preview Player State variables
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewTimeMs by remember { mutableStateOf(0L) }
    var loopPreview by remember { mutableStateOf(true) }
    var isPreviewFullscreen by remember { mutableStateOf(false) }
    var audioDurationMs by remember { mutableStateOf(30_000L) }

    // Asynchronously resolve song track duration and details
    LaunchedEffect(selectedFiles) {
        val uri = selectedFiles.firstOrNull()
        if (uri != null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (durationStr != null) {
                    audioDurationMs = durationStr.toLong()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    // Drives the real-time preview animation phase & seek position progress
    var previewAnimPhase by remember { mutableStateOf(0f) }
    LaunchedEffect(isPreviewPlaying) {
        while (isPreviewPlaying) {
            delay(33) // ~30 fps
            previewTimeMs += 33
            previewAnimPhase += 0.15f
            if (previewTimeMs >= audioDurationMs) {
                if (loopPreview) {
                    previewTimeMs = 0
                } else {
                    isPreviewPlaying = false
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    // Live Video Preview drawing canvas helper logic
    @Composable
    fun LivePreviewCanvas(modifier: Modifier = Modifier) {
        val sourceSongName = selectedFiles.firstOrNull()?.lastPathSegment ?: "Pulse Acoustic Track"
        Canvas(modifier = modifier) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val stepX = width / 64f

            // 1. Draw beautiful canvas background style
            when (selectedBgStyle) {
                "Solid Color" -> {
                    drawRect(color = Color(0xFF0F172A))
                }
                "Gradient" -> {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                        )
                    )
                }
                else -> {
                    // Album Art rotation background mock simulation
                    drawRect(color = Color(0xFF030712))
                    val radius = height * 0.28f
                    val angle = if (rotateAlbumArt && isPreviewPlaying) (previewAnimPhase * 5) % 360 else 0f
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        color = Color(0xFF1E1B4B),
                        radius = radius - 8f,
                        center = center
                    )
                }
            }

            // 2. Draw dynamic visualizer wave responsive to the preview state and sines
            val path = Path()
            path.moveTo(0f, midY)
            val configAmp = if (type == VideoStudioType.WAVEFORM) waveThickness * 8f else 50f
            val liveAmp = if (isPreviewPlaying) configAmp + kotlin.math.sin(previewAnimPhase.toDouble()).toFloat() * 15f else configAmp

            for (j in 0..64) {
                val x = j * stepX
                val fluctuation = kotlin.math.sin(((j.toFloat() / 64f) * Math.PI * 4 + previewAnimPhase).toDouble()).toFloat() * liveAmp
                path.lineTo(x, midY + fluctuation)
            }
            drawPath(path = path, color = Color.Cyan, style = Stroke(width = waveThickness.coerceAtLeast(1.0f)))

            // 3. Repeating Watermark Overlay System (10-second repeating patterns)
            val currentPosSec = previewTimeMs / 1000
            val songOverlayVisible = (currentPosSec % 10) < 3
            val customTextOverlayVisible = ((currentPosSec + 5) % 10) < 3

            // Always display custom constant text watermark in corner
            drawContext.canvas.nativeCanvas.drawText(
                "Watermark: ${lyricText}",
                30f,
                50f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            )

            // Repeating Song Info Overlay (fades in every 10 seconds)
            if (songOverlayVisible) {
                drawContext.canvas.nativeCanvas.drawText(
                    "Track info: ${sourceSongName}",
                    width / 2f,
                    height - 60f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.CYAN
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }

            // Repeating Custom Overlay Text (e.g. "A D&E SONG MUSIC" repeating every 10 seconds)
            if (customTextOverlayVisible) {
                drawContext.canvas.nativeCanvas.drawText(
                    "A D&E SONG MUSIC",
                    width / 2f,
                    height - 120f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.MAGENTA
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(type.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio Track (MP3, WAV)", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(type.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Audio"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // HIGH-FIDELITY LIVE 16:9 PREVIEW PLAYER AT THE TOP
                Text("Live 16:9 Preview Player", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LivePreviewCanvas(modifier = Modifier.fillMaxSize())

                        // Interactive controller overlay
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { isPreviewFullscreen = !isPreviewFullscreen },
                                    modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen Toggle",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Progress, seek slider, and controls row
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { isPreviewPlaying = !isPreviewPlaying },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "PlayPause",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Slider(
                                        value = previewTimeMs.toFloat(),
                                        onValueChange = { previewTimeMs = it.toLong() },
                                        valueRange = 0f..audioDurationMs.toFloat().coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { loopPreview = !loopPreview },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = "Loop Toggle",
                                            tint = if (loopPreview) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(formatTime(previewTimeMs), fontSize = 10.sp, color = Color.White)
                                    Text(formatTime(audioDurationMs), fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Studio Configurations", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                when (type) {
                    VideoStudioType.MP3_TO_MP4 -> {
                        Text("Visualizer Preset", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(VisualizerPreset.values()) { pr ->
                                val isSelected = selectedPreset == pr
                                Card(
                                    modifier = Modifier.height(38.dp).clickable { selectedPreset = pr },
                                    shape = RoundedCornerShape(19.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                        Text(pr.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                    VideoStudioType.ALBUM_ART -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Rotate Album Artwork", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(onClick = { rotateAlbumArt = !rotateAlbumArt }) {
                                Icon(
                                    imageVector = if (rotateAlbumArt) Icons.Default.ScreenRotation else Icons.Default.ScreenLockPortrait,
                                    contentDescription = null, tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    VideoStudioType.LYRICS -> {
                        Text("Overlay Lyrics Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lyricText,
                            onValueChange = { lyricText = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    VideoStudioType.WAVEFORM -> {
                        Text("Wave Line Thickness (${waveThickness.toInt()}px)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(
                            value = waveThickness,
                            onValueChange = { waveThickness = it },
                            valueRange = 1f..12f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    VideoStudioType.SPECTRUM -> {
                        Text("Spectrum Columns Count (${barCount.toInt()} channels)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(
                            value = barCount,
                            onValueChange = { barCount = it },
                            valueRange = 16f..128f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    VideoStudioType.NEON -> {
                        Text("Neon Aura Blur Glow Radius (${neonGlowRadius.toInt()}dp)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(
                            value = neonGlowRadius,
                            onValueChange = { neonGlowRadius = it },
                            valueRange = 4f..30f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    VideoStudioType.CIRCULAR -> {
                        Text("Circle Baseline Radius (${circularRadius.toInt()}dp)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(
                            value = circularRadius,
                            onValueChange = { circularRadius = it },
                            valueRange = 50f..250f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    VideoStudioType.AUDIO_STATUS -> {
                        Text("Visual Theme Template", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Retro Vinyl", "Future Glass", "Cyber Minimal").forEach { template ->
                                val isSelected = statusCardStyle == template
                                Card(
                                    modifier = Modifier.weight(1f).height(44.dp).clickable { statusCardStyle = template },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(template, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                    VideoStudioType.STORY_9_16 -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))) {
                            Text("Portrait Aspect Ratio (9:16) enabled. Perfect for Instagram Stories, TikTok, and YouTube Shorts.", fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                    VideoStudioType.YOUTUBE_16_9 -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))) {
                            Text("Landscape Aspect Ratio (16:9) enabled. Tailored for cinematic displays and YouTube widescreen videos.", fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Select Video Backdrop", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Album Art", "Solid Color", "Gradient", "Image").forEach { style ->
                        val isSelected = selectedBgStyle == style
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { selectedBgStyle = style },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(style, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Resolution", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("720p (HD)", "1080p (Full HD)").forEach { res ->
                        val isSelected = (res.startsWith("720p") && selectedResolution == "720p") || (res.startsWith("1080p") && selectedResolution == "1080p")
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { selectedResolution = if (res.startsWith("720p")) "720p" else "1080p" },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(res, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Render and Export Video", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another audio file", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.exportVisualizerVideo(uri, outputFileName, selectedResolution, lyricText)
                        }
                    }
                ) {
                    Text("Export", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Direct Fullscreen overlay preview
    if (isPreviewFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            LivePreviewCanvas(modifier = Modifier.fillMaxSize())

            IconButton(
                onClick = { isPreviewFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// --- 2. AUDIO EXTRACTOR SCREEN ---

@Composable
fun ExtractorToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var outputFormat by remember { mutableStateOf("MP3") }
    var outputFileName by remember { mutableStateOf("Extracted_Video_Track") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Extract Audio from Video", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("video/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Video File (MP4, MKV, AVI, MOV)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Video"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Output Audio Format", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("MP3", "AAC").forEach { format ->
                        val isSelected = outputFormat == format
                        Card(
                            modifier = Modifier.weight(1f).height(55.dp).clickable { outputFormat = format },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(format, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract Audio Stream", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another video", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.extractAudio(uri, outputFileName, outputFormat)
                        }
                    }
                ) {
                    Text("Extract Track", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- 3. AUDIO COMPRESSOR SCREEN ---

@Composable
fun CompressorToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var selectedPreset by remember { mutableStateOf(CompressionPreset.MEDIUM) }
    var outputFileName by remember { mutableStateOf("Compressed_Audio_File") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Acoustic Compressor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio to Compress", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Audio"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Compression Level Presets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                listOf(
                    Pair(CompressionPreset.LOW, "Low Compression (Highest Audio Fidelity, Largest File Size)"),
                    Pair(CompressionPreset.MEDIUM, "Medium Compression (Standard Balance of File Size & Clarity)"),
                    Pair(CompressionPreset.HIGH, "High Compression (Ultra Small File Size, Lower Bitrate)")
                ).forEach { (preset, desc) ->
                    val isSelected = selectedPreset == preset
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedPreset = preset },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(preset.name, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compress File Size", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another track", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.compressAudio(uri, outputFileName, selectedPreset)
                        }
                    }
                ) {
                    Text("Compress", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- 4. SPEED & PITCH CHANGER SCREEN ---

@Composable
fun SpeedPitchToolScreen(
    viewModel: AudioStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    var selectedPitch by remember { mutableStateOf(1.0f) }
    var outputFileName by remember { mutableStateOf("Resampled_Audio") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectFiles(listOf(uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Speed & Pitch Lab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { filePickerLauncher.launch("audio/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Audio to Modify", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            val sourceUri = selectedFiles.first()
            val name = sourceUri.lastPathSegment ?: "Selected Audio"

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Speed Selector (0.5x, 0.75x, 1.25x, 1.5x, 2.0x)
                Text("Select Target Speed Export", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    speeds.take(3).forEach { speed ->
                        val isSelected = selectedSpeed == speed
                        Card(
                            modifier = Modifier.weight(1f).height(45.dp).clickable { selectedSpeed = speed },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${speed}x", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    speeds.takeLast(3).forEach { speed ->
                        val isSelected = selectedSpeed == speed
                        Card(
                            modifier = Modifier.weight(1f).height(45.dp).clickable { selectedSpeed = speed },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("${speed}x", fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Pitch Selector Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Target Pitch Adjustment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(String.format("%.2fx", selectedPitch), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = selectedPitch,
                    onValueChange = { selectedPitch = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export with Speed & Pitch", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Import another track", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Output Filename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { outputFileName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        val uri = selectedFiles.firstOrNull()
                        if (uri != null) {
                            viewModel.changeSpeedAndPitch(uri, outputFileName, selectedSpeed, selectedPitch)
                        }
                    }
                ) {
                    Text("Export Resampled", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}
