package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.salmanlaghari.pulsemusicplayerai.common.GlassmorphicCard
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.VideoStudioType
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.VideoStudioScreen
import com.salmanlaghari.pulsemusicplayerai.theme.*

enum class StudioScreen {
    MAIN_LIST,
    CUTTER,
    MERGER,
    CONVERTER,
    VIDEO_STUDIO,
    EXTRACTOR,
    COMPRESSOR,
    SPEED_PITCH,
    LIBRARY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioToolsScreen() {
    val context = LocalContext.current
    val studioViewModel: AudioStudioViewModel = viewModel(
        factory = AudioStudioViewModelFactory(context.applicationContext)
    )

    var currentScreen by remember { mutableStateOf(StudioScreen.MAIN_LIST) }
    var showVideoStudioSheet by remember { mutableStateOf(false) }
    var selectedVideoType by remember { mutableStateOf(VideoStudioType.MP3_TO_MP4) }

    val isProcessing by studioViewModel.isProcessing.collectAsState()
    val progress by studioViewModel.progress.collectAsState()
    val statusMessage by studioViewModel.statusMessage.collectAsState()
    val showResultDialog by studioViewModel.showResultDialog.collectAsState()

    // Handle system back press
    BackHandler(enabled = currentScreen != StudioScreen.MAIN_LIST) {
        studioViewModel.clearSelection()
        currentScreen = StudioScreen.MAIN_LIST
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Active studio cockpit routing
        when (currentScreen) {
            StudioScreen.MAIN_LIST -> {
                AudioToolsMainList(
                    onNavigateToTool = { screen ->
                        studioViewModel.clearSelection()
                        currentScreen = screen
                    },
                    onOpenVideoStudio = {
                        showVideoStudioSheet = true
                    }
                )
            }
            StudioScreen.CUTTER -> {
                CutterToolScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        studioViewModel.clearSelection()
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
            StudioScreen.MERGER -> {
                MergerToolScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        studioViewModel.clearSelection()
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
            StudioScreen.CONVERTER -> {
                ConverterToolScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        studioViewModel.clearSelection()
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
            StudioScreen.VIDEO_STUDIO -> {
                VideoStudioScreen(
                    type = selectedVideoType,
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        studioViewModel.clearSelection()
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
            StudioScreen.EXTRACTOR -> {
                ExtractorToolScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        studioViewModel.clearSelection()
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
            StudioScreen.COMPRESSOR -> {
                CompressorToolScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        studioViewModel.clearSelection()
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
            StudioScreen.SPEED_PITCH -> {
                SpeedPitchToolScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        studioViewModel.clearSelection()
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
            StudioScreen.LIBRARY -> {
                StudioLibraryScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = {
                        currentScreen = StudioScreen.MAIN_LIST
                    }
                )
            }
        }

        // 1. Background processing HUD overlay (Progress & Cancellation)
        if (isProcessing) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassmorphicCard(
                        modifier = Modifier.width(280.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = { progress.toFloat() / 100f },
                                modifier = Modifier.size(64.dp),
                                color = CyanGlow,
                                trackColor = GlassBorder
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Processing Audio...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = statusMessage,
                                fontSize = 13.sp,
                                color = TextDim,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Glow Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFFEF4444))
                                    .clickable { studioViewModel.cancelActiveOperation() }
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel Task", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Success or Failure dialog
        if (showResultDialog != null) {
            val (success, file) = showResultDialog!!
            AlertDialog(
                onDismissRequest = { studioViewModel.closeResultDialog() },
                containerColor = BaseDeepPurple,
                title = {
                    Text(
                        text = if (success) "Export Successful! 🎉" else "Process Failed ❌",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    if (success && file != null) {
                        Column {
                            Text(
                                text = "Your output track is safely saved under the system's Music/PulseAudioStudio/ directory.",
                                fontSize = 13.sp,
                                color = TextLight
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            GlassmorphicCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("File: ${file.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                                    Text("Format: ${file.format}", fontSize = 11.sp, color = CyanGlow)
                                    Text("Size: ${formatBytes(file.size)}", fontSize = 11.sp, color = TextDim)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "An unexpected error occurred while processing your audio track. Please verify input details or permission scopes.",
                            fontSize = 13.sp,
                            color = TextLight
                        )
                    }
                },
                confirmButton = {
                    if (success && file != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    studioViewModel.closeResultDialog()
                                    currentScreen = StudioScreen.LIBRARY
                                }
                            ) {
                                Text("Go to Library", fontWeight = FontWeight.Bold, color = CyanGlow)
                            }
                            TextButton(onClick = { studioViewModel.closeResultDialog() }) {
                                Text("Close", color = TextDim)
                            }
                        }
                    } else {
                        TextButton(onClick = { studioViewModel.closeResultDialog() }) {
                            Text("Dismiss", fontWeight = FontWeight.Bold, color = CyanGlow)
                        }
                    }
                }
            )
        }

        // --- 10 Dedicated Video Studio Templates Selector Sheet ---
        if (showVideoStudioSheet) {
            ModalBottomSheet(
                onDismissRequest = { showVideoStudioSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = BaseDeepPurple,
                tonalElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video Studio Pro", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        IconButton(onClick = { showVideoStudioSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select a professional video rendering template to convert your audio track:", fontSize = 12.sp, color = TextDim)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(VideoStudioType.values()) { type ->
                            GlassmorphicCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .clickable {
                                        selectedVideoType = type
                                        showVideoStudioSheet = false
                                        currentScreen = StudioScreen.VIDEO_STUDIO
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Purple1.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = CyanGlow,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(type.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(type.description, fontSize = 11.sp, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = CyanGlow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioToolsMainList(
    onNavigateToTool: (StudioScreen) -> Unit,
    onOpenVideoStudio: () -> Unit
) {
    val toolsList = listOf(
        AudioToolData("Merge Tracks", "Merge two or more MP3 files together seamlessly", Icons.Default.MergeType, StudioScreen.MERGER),
        AudioToolData("Audio Converter", "Convert audio files to any format (MP3, WAV, AAC...)", Icons.Default.Transform, StudioScreen.CONVERTER),
        AudioToolData("Video Studio Pro", "Turn MP3 into MP4 with live spectrum/waveform visualizer — export ready-to-post video", Icons.Default.Movie, StudioScreen.VIDEO_STUDIO),
        AudioToolData("Extract Audio", "Pull high quality music track directly from video files", Icons.Default.SpeakerNotes, StudioScreen.EXTRACTOR),
        AudioToolData("Compressor", "Reduce file size while preserving audio quality", Icons.Default.SyncAlt, StudioScreen.COMPRESSOR)
    )

    // Base background with purple and cyan radial glows over near-black base
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BaseDeepPurple, BaseNearBlack, BaseNearBlack)
                )
            )
    ) {
        // Glowing radial mesh background overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanGlow.copy(alpha = 0.12f), Color.Transparent),
                        radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Purple1.copy(alpha = 0.16f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Audio Studio",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFA78BFA), CyanGlow)
                            )
                        ),
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡",
                        fontSize = 20.sp,
                        modifier = Modifier.shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = CyanGlowSoft,
                            spotColor = CyanGlow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Professional Intro Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp), clip = false),
                shape = RoundedCornerShape(20.dp),
                borderBrush = Brush.horizontalGradient(listOf(Purple1.copy(alpha = 0.5f), CyanGlow.copy(alpha = 0.5f))),
                backgroundBrush = Brush.linearGradient(
                    colors = listOf(Purple1.copy(alpha = 0.16f), CyanGlow.copy(alpha = 0.08f))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pulse Audio Lab",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC9B6FF)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CyanGlow.copy(alpha = 0.12f))
                                .border(1.dp, CyanGlow.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .clickable { onNavigateToTool(StudioScreen.LIBRARY) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("📁 Studio Library", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "A suite of premium offline utility tools to trim, transform, extract, merge, compress, and resample your music files.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = TextDim
                    )
                }
            }

            // Tools List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(toolsList) { tool ->
                    val isFeatured = tool.targetScreen == StudioScreen.VIDEO_STUDIO
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isFeatured) 100.dp else 80.dp)
                            .shadow(if (isFeatured) 18.dp else 8.dp, RoundedCornerShape(18.dp), clip = false),
                        shape = RoundedCornerShape(18.dp),
                        is3D = isFeatured,
                        hasShine = isFeatured,
                        borderBrush = if (isFeatured) Brush.horizontalGradient(listOf(CyanGlow.copy(alpha = 0.6f), Purple1.copy(alpha = 0.4f))) else null,
                        backgroundBrush = if (isFeatured) {
                            Brush.linearGradient(
                                colors = listOf(CyanGlow.copy(alpha = 0.16f), Purple1.copy(alpha = 0.12f))
                            )
                        } else null,
                        onClick = {
                            if (tool.targetScreen == StudioScreen.VIDEO_STUDIO) {
                                onOpenVideoStudio()
                            } else {
                                onNavigateToTool(tool.targetScreen)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isFeatured) 44.dp else 48.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .shadow(6.dp, RoundedCornerShape(13.dp), ambientColor = Purple1, spotColor = CyanGlow)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Purple1.copy(alpha = 0.4f), CyanGlow.copy(alpha = 0.22f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.title,
                                        tint = CyanGlow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tool.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (isFeatured) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 6.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(CyanGlow, Color(0xFF7EF9FF))
                                                        )
                                                    )
                                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("AVee Style", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF04262A))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tool.description,
                                        fontSize = 11.sp,
                                        color = TextDim,
                                        maxLines = 2,
                                        lineHeight = 14.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Tool",
                                tint = CyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024f
    val mb = kb * kb
    return if (bytes >= mb) {
        String.format("%.2f MB", bytes / mb)
    } else {
        String.format("%.2f KB", bytes / kb)
    }
}

data class AudioToolData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val targetScreen: StudioScreen
)
