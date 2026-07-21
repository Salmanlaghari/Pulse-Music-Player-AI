package com.salmanlaghari.pulsemusicplayerai.presentation.audiotools

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.common.GlassmorphicCard
import com.salmanlaghari.pulsemusicplayerai.theme.BgDeep
import com.salmanlaghari.pulsemusicplayerai.theme.Pink
import com.salmanlaghari.pulsemusicplayerai.theme.Purple
import com.salmanlaghari.pulsemusicplayerai.theme.PurpleLight
import com.salmanlaghari.pulsemusicplayerai.theme.TextDim

@Composable
fun AudioToolsScreen() {
    val toolsList = listOf(
        AudioToolItem("MP3 Cutter", "Cut, trim, and make ringtones out of any sound file.", Icons.Default.ContentCut),
        AudioToolItem("Audio Merger", "Merge two or more MP3 files together easily.", Icons.Default.MergeType),
        AudioToolItem("Audio Converter", "Convert audio files to any format (MP3, WAV, FLAC, etc.)", Icons.Default.Transform),
        AudioToolItem("MP3 to MP4", "Convert audio files to video with custom visual backdrops.", Icons.Default.QueueMusic),
        AudioToolItem("Extract Audio", "Pull high quality music track files directly from video files.", Icons.Default.SpeakerNotes),
        AudioToolItem("Compressor", "Reduce file size without sacrificing beautiful acoustic details.", Icons.Default.SyncAlt),
        AudioToolItem("Speed Changer", "Alter speed/pitch of any audio track easily.", Icons.Default.SlowMotionVideo)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgDeep, Color(0xFF120E24))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Tools",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "⚡",
                    fontSize = 22.sp,
                    color = PurpleLight,
                    modifier = Modifier.shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = PurpleLight,
                        spotColor = PurpleLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Professional Intro Card (Glassmorphic layout with gradient overlay highlight)
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Purple.copy(alpha = 0.2f), Color.Transparent),
                                    radius = 350f
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Pulse Audio Lab",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "A suite of premium offline utility tools to trim, transform, extract, merge, compress, and supercharge your music files.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = TextDim
                        )
                    }
                }
            }

            // List of tools
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(toolsList) { tool ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(14.dp)
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Purple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.title,
                                        tint = PurpleLight,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = tool.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tool.description,
                                        fontSize = 11.sp,
                                        color = TextDim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Tool",
                                tint = PurpleLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(160.dp)) // Safe space padding for navigation and players
        }
    }
}

data class AudioToolItem(val title: String, val description: String, val icon: ImageVector)
