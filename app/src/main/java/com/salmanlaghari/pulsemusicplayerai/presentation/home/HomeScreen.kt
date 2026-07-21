package com.salmanlaghari.pulsemusicplayerai.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.common.GlassmorphicCard
import com.salmanlaghari.pulsemusicplayerai.theme.BgCard
import com.salmanlaghari.pulsemusicplayerai.theme.BgCard2
import com.salmanlaghari.pulsemusicplayerai.theme.BgDeep
import com.salmanlaghari.pulsemusicplayerai.theme.BorderColor
import com.salmanlaghari.pulsemusicplayerai.theme.Pink
import com.salmanlaghari.pulsemusicplayerai.theme.Purple
import com.salmanlaghari.pulsemusicplayerai.theme.PurpleLight
import com.salmanlaghari.pulsemusicplayerai.theme.TextDim

@Composable
fun HomeScreen() {
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
                .padding(bottom = 160.dp) // Spacing so it's not hidden by the floating mini player and bottom nav
        ) {
            // 1. Top Bar Section
            TopbarSection()

            // 2. Hero Section
            HeroSection()

            // 3. Continue Listening Section
            ContinueListeningSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Quick Access Grid
            QuickAccessSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Recently Added Section
            RecentlyAddedSection()
        }
    }
}

@Composable
fun TopbarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pulse",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
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

        // Top Icons: Search & Avatar
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(BgCard)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextDim,
                    modifier = Modifier.size(16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Purple, Pink)
                        )
                    )
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎧",
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun HeroSection() {
    // Recreating the radial gradient style with a custom design background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF14102B))
    ) {
        // Gradient blobs/highlights overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Pink.copy(alpha = 0.45f), Color.Transparent),
                        radius = 400f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC0C0A1A))
                    )
                )
        )

        // Hero Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Good Evening 👋",
                fontSize = 13.sp,
                color = TextDim,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Feel the Music,\nFeel the Pulse.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your local audio engine is live. Play, create and enjoy without limits.",
                fontSize = 12.5.sp,
                color = TextDim,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(240.dp)
            )
        }
    }
}

@Composable
fun ContinueListeningSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continue Listening",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .clickable { }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    color = TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gradient Thumb
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Pink, Purple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎵", fontSize = 18.sp)
                }

                // Track Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "In The End",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Linkin Park • Hybrid Theory",
                        fontSize = 12.sp,
                        color = TextDim
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2A2545))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Purple, PurpleLight)
                                        )
                                    )
                            )
                        }
                        Text(
                            text = "2:45 / 3:36",
                            fontSize = 10.sp,
                            color = TextDim
                        )
                    }
                }

                // Play circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Purple,
                            spotColor = Purple
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Purple, Color(0xFF5B21B6))
                            )
                        )
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAccessSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Quick Access",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val items = listOf(
            QuickAccessItem("AI Assistant", "Smart Audio Tools", "🤖", Brush.linearGradient(listOf(Purple.copy(alpha = 0.25f), Purple.copy(alpha = 0.05f)))),
            QuickAccessItem("My Favorites", "Your Liked Tracks", "❤️", Brush.linearGradient(listOf(Pink.copy(alpha = 0.25f), Pink.copy(alpha = 0.05f)))),
            QuickAccessItem("Library", "Songs, Albums, Artists", "🎵", Brush.linearGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.25f), Color(0xFF3B82F6).copy(alpha = 0.05f)))),
            QuickAccessItem("Equalizer", "Sound Perfected", "📶", Brush.linearGradient(listOf(Color(0xFF22C55E).copy(alpha = 0.25f), Color(0xFF22C55E).copy(alpha = 0.05f))))
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickAccessCard(items[0], modifier = Modifier.weight(1f))
                QuickAccessCard(items[1], modifier = Modifier.weight(1f))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickAccessCard(items[2], modifier = Modifier.weight(1f))
                QuickAccessCard(items[3], modifier = Modifier.weight(1f))
            }
        }
    }
}

data class QuickAccessItem(
    val title: String,
    val sub: String,
    val icon: String,
    val brush: Brush
)

@Composable
fun QuickAccessCard(item: QuickAccessItem, modifier: Modifier = Modifier) {
    GlassmorphicCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        backgroundBrush = item.brush
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = item.icon,
                fontSize = 20.sp
            )
            Column {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.sub,
                    fontSize = 10.5.sp,
                    color = TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentlyAddedSection() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Added",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .clickable { }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    color = TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Scroll list
        val recentItems = listOf(
            RecentSongItem("Believer", "Imagine Dragons", Brush.linearGradient(listOf(Pink, Purple))),
            RecentSongItem("Perfect", "Ed Sheeran", Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
            RecentSongItem("Hasi Ban Gaye", "Ami Mishra", Brush.linearGradient(listOf(Color(0xFF3B1D1D), Color(0xFF0F0A0A)))),
            RecentSongItem("Photograph", "Ed Sheeran", Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF111827))))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            recentItems.forEach { item ->
                Column(
                    modifier = Modifier.width(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(item.brush)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.artist,
                        fontSize = 10.5.sp,
                        color = TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class RecentSongItem(
    val name: String,
    val artist: String,
    val brush: Brush
)

@Composable
fun MiniPlayerPlaceholder() {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = Color(0xF2171429) // rgba(23,20,41,0.95)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Pink, Purple)
                            )
                        )
                )

                Column {
                    Text(
                        text = "Believer",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Imagine Dragons",
                        fontSize = 11.sp,
                        color = TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "⏮",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.clickable { }
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Purple)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏸",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "⏭",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.clickable { }
                )
                Text(
                    text = "☰",
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.clickable { }
                )
            }
        }
    }
}
