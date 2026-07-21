package com.salmanlaghari.pulsemusicplayerai.presentation.aihub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
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
fun AIHubScreen() {
    val aiCards = listOf(
        AICardItem("AI Prompt Library", "Explore custom audio prompt libraries & presets.", Icons.Default.SpeakerNotes, false),
        AICardItem("AI Prompt Generator", "Let AI construct premium prompt scripts.", Icons.Default.AutoAwesome, false),
        AICardItem("AI Music Assistant", "Instant conversational intelligent chatbot.", Icons.Default.Mic, false),
        AICardItem("AI Playlist Generator", "Enter a mood and let AI build custom playlists.", Icons.Default.QueueMusic, false),
        AICardItem("AI Lyrics Assistant", "Generate matching lyrics for any melody or theme.", Icons.Default.Lyrics, false),
        AICardItem("AI Image Generator", "Visualize custom backdrops using AI art technology.", Icons.Default.Image, true),
        AICardItem("AI Video Generator", "Turn track files into custom dynamic MP4 music videos.", Icons.Default.Movie, true),
        AICardItem("AI Voice Generator", "Generate professional studio clone voices.", Icons.Default.MusicNote, true)
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
                    text = "AI Hub",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "✨",
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

            // Professional Intro Banner with radial glowing effects
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
                                    colors = listOf(Pink.copy(alpha = 0.18f), Color.Transparent),
                                    radius = 350f
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Pulse Neural Center",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "A modern showcase of revolutionary artificial intelligence music technologies. Experience advanced lyric writers, art visualizers, and music assistants.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = TextDim
                        )
                    }
                }
            }

            // Modern 2-column Grid of AI Cards (using GlassmorphicCard)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(aiCards) { card ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (card.isComingSoon) Pink.copy(alpha = 0.15f)
                                        else Purple.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = card.icon,
                                    contentDescription = card.title,
                                    tint = if (card.isComingSoon) Pink else PurpleLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = card.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (card.isComingSoon) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Pink.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "COMING SOON",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Pink,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = card.description,
                                        fontSize = 10.sp,
                                        color = TextDim,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(160.dp)) // Safe space padding for navigation and players
        }
    }
}

data class AICardItem(val title: String, val description: String, val icon: ImageVector, val isComingSoon: Boolean)
