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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
fun SettingsAboutScreen(onNavigateBack: () -> Unit) {
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
                    text = "About",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Pulse Music Info",
                        tint = Pink,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pulse Music Player AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Version 1.0.0 (Session 7 Polished)",
                        fontSize = 12.sp,
                        color = TextDim
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pulse Music Player AI is a professional sound ecosystem combining modern audio cutting-edge features with dynamic UI customization, local sound processing tools, and future automated AI capabilities.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Developer: Salman Laghari",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleLight
                    )
                }
            }
        }
    }
}
