package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

import androidx.activity.compose.BackHandler
import com.salmanlaghari.pulsemusicplayerai.presentation.MusicViewModel
import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.VideoStudioType
import com.salmanlaghari.pulsemusicplayerai.presentation.audiotools.VideoStudioScreen

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
fun AudioToolsScreen(
    musicViewModel: MusicViewModel,
    onNavigateToTemplateStudio: () -> Unit
) {
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
                    musicViewModel = musicViewModel,
                    onNavigateToTemplateStudio = onNavigateToTemplateStudio,
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
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = { progress.toFloat() / 100f },
                                modifier = Modifier.size(64.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Processing Audio Track...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = statusMessage,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { studioViewModel.cancelActiveOperation() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel Task", fontWeight = FontWeight.Bold, color = Color.White)
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
                title = {
                    Text(
                        text = if (success) "Export Successful! 🎉" else "Process Failed ❌",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    if (success && file != null) {
                        Column {
                            Text(
                                text = "Your output track is safely saved under the system's Music/PulseAudioStudio/ directory.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("File: ${file.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Format: ${file.format}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Size: ${formatBytes(file.size)}", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "An unexpected error occurred while processing your audio track. Please verify input details or permission scopes.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                                Text("Go to Library", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { studioViewModel.closeResultDialog() }) {
                                Text("Close")
                            }
                        }
                    } else {
                        TextButton(onClick = { studioViewModel.closeResultDialog() }) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
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
                containerColor = MaterialTheme.colorScheme.surface,
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
                            Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video Studio Pro", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { showVideoStudioSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select a professional video rendering template to convert your audio track:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(VideoStudioType.values()) { type ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .clickable {
                                        selectedVideoType = type
                                        showVideoStudioSheet = false
                                        currentScreen = StudioScreen.VIDEO_STUDIO
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(type.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(type.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
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
        AudioToolData("MP3 Cutter", "Cut, trim, and make ringtones out of any sound file.", Icons.Default.ContentCut, StudioScreen.CUTTER),
        AudioToolData("Audio Merger", "Merge two or more MP3 files together easily.", Icons.Default.MergeType, StudioScreen.MERGER),
        AudioToolData("Audio Converter", "Convert audio files to any format (MP3, WAV, FLAC, etc.)", Icons.Default.Transform, StudioScreen.CONVERTER),
        AudioToolData("Video Studio Pro", "Convert MP3 files into 10+ premium animated video templates.", Icons.Default.Movie, StudioScreen.VIDEO_STUDIO),
        AudioToolData("Extract Audio", "Pull high quality music track files directly from video files.", Icons.Default.SpeakerNotes, StudioScreen.EXTRACTOR),
        AudioToolData("Compressor", "Reduce file size without sacrificing beautiful acoustic details.", Icons.Default.SyncAlt, StudioScreen.COMPRESSOR),
        AudioToolData("Speed Changer", "Alter speed/pitch of any audio track easily.", Icons.Default.SlowMotionVideo, StudioScreen.SPEED_PITCH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Audio Studio",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = "Audio Power",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Professional Intro Card with Studio Library Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pulse Audio Lab",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier.clickable { onNavigateToTool(StudioScreen.LIBRARY) },
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Studio Library", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A suite of premium offline utility tools to trim, transform, extract, merge, compress, and resample your music files.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Tools List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(toolsList) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable {
                            if (tool.targetScreen == StudioScreen.VIDEO_STUDIO) {
                                onOpenVideoStudio()
                            } else {
                                onNavigateToTool(tool.targetScreen)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = tool.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tool.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Tool",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
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
