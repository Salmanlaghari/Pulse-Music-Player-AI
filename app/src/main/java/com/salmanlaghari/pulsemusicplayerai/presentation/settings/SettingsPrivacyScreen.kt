package com.salmanlaghari.pulsemusicplayerai.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.pulsemusicplayerai.common.GlassmorphicCard
import com.salmanlaghari.pulsemusicplayerai.theme.BgDeep
import com.salmanlaghari.pulsemusicplayerai.theme.Pink
import com.salmanlaghari.pulsemusicplayerai.theme.Purple
import com.salmanlaghari.pulsemusicplayerai.theme.PurpleLight
import com.salmanlaghari.pulsemusicplayerai.theme.TextDim

@Composable
fun SettingsPrivacyScreen(onNavigateBack: () -> Unit) {
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PurpleLight
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Privacy Policy",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "Pulse Music Player AI Privacy Agreement",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Pink
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Last updated: July 2024\n\n" +
                                "At Pulse Music Player AI, we take your personal privacy very seriously. We do not transmit your local music database, audio recordings, or media files to external servers.\n\n" +
                                "1. Data collection: No personal profile fields are requested. Audio analysis for features (like Cutter, Compressor) runs strictly locally on-device.\n\n" +
                                "2. Storage Access: We request READ storage permissions to dynamically load and build your physical mp3 and sound library within the app. These files are never shared or backed up to any cloud without your explicit manual action.\n\n" +
                                "3. Settings: User choices are kept isolated inside local DataStore file preferences.\n\n" +
                                "For any further assistance regarding security protocols, reach out via our feedback forum.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
