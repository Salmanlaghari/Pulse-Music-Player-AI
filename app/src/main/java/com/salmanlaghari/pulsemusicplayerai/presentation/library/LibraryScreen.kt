package com.salmanlaghari.pulsemusicplayerai.presentation.library

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun LibraryScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Playlists", "Songs", "Artists", "Folders")

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Playlist",
                        tint = PurpleLight,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Category Row / Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PurpleLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PurpleLight
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.White else TextDim
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic items list based on selected category
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> LibraryItemsList(getPlaylists())
                    1 -> LibraryItemsList(getSongs())
                    2 -> LibraryItemsList(getArtists())
                    3 -> LibraryItemsList(getFolders())
                }
            }

            Spacer(modifier = Modifier.height(160.dp)) // Safe padding for persistent floating components
        }

        // Floating button with gradient background to create playlists
        FloatingActionButton(
            onClick = { },
            containerColor = Color.Transparent,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 175.dp, end = 20.dp) // Adjusted to float above floating player perfectly
                .shadow(elevation = 12.dp, shape = CircleShape, clip = false, ambientColor = Purple, spotColor = Pink)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Purple, Pink)
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Playlist",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LibraryItemsList(items: List<LibraryItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
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
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = PurpleLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 12.sp,
                                color = TextDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = TextDim
                        )
                    }
                }
            }
        }
    }
}

data class LibraryItem(val title: String, val subtitle: String, val icon: ImageVector)

private fun getPlaylists() = listOf(
    LibraryItem("My AI Beats", "14 songs • Created by AI", Icons.Default.LibraryMusic),
    LibraryItem("Liked Tracks", "320 songs • Local Library", Icons.Default.Favorite),
    LibraryItem("Chill Vibes Only", "45 songs • Playlist", Icons.Default.LibraryMusic),
    LibraryItem("Midnight Glow", "12 songs • Retro Synth", Icons.Default.LibraryMusic)
)

private fun getSongs() = listOf(
    LibraryItem("Neural Symphony 1.0", "AI Mastermind • 3:45", Icons.Default.MusicNote),
    LibraryItem("Neon Nights", "Retro Horizon • 4:12", Icons.Default.MusicNote),
    LibraryItem("Synthesized Soul", "Acoustic Bot • 2:58", Icons.Default.MusicNote),
    LibraryItem("Golden Future", "Humanized Synth • 3:20", Icons.Default.MusicNote)
)

private fun getArtists() = listOf(
    LibraryItem("AI Mastermind", "5 local songs", Icons.Default.History),
    LibraryItem("Retro Horizon", "2 local songs", Icons.Default.History),
    LibraryItem("Acoustic Bot", "12 local songs", Icons.Default.History)
)

private fun getFolders() = listOf(
    LibraryItem("Music", "/storage/emulated/0/Music", Icons.Default.Folder),
    LibraryItem("Downloads", "/storage/emulated/0/Download", Icons.Default.Folder),
    LibraryItem("WhatsApp Audio", "/storage/emulated/0/WhatsApp/Media", Icons.Default.Folder)
)
