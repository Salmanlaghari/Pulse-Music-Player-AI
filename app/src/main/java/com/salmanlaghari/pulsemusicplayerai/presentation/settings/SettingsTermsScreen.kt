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
fun SettingsTermsScreen(onNavigateBack: () -> Unit) {
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
                    text = "Terms of Service",
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
                        text = "Pulse Music Player AI Terms and Conditions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Pink
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Welcome to Pulse Music Player AI!\n\n" +
                                "These terms govern your usage of this platform and software application. By setting up and launching our application, you agree to comply with all specified licensing guidelines:\n\n" +
                                "1. License: We grant you a revocable, limited, non-exclusive license to use the app for personal, non-commercial entertainment purposes.\n\n" +
                                "2. Prohibited Uses: You may not reverse engineer, decompile, or modify internal sound processing binaries. You may not distribute custom music generated through automated prompts unless the source assets belong completely to you or are licensed appropriately.\n\n" +
                                "3. Disclaimers: All audio features (like speed conversion, compression) are provided 'as is' without warranty of any kind. We are not responsible for any file data corruption on target memory storage systems.\n\n" +
                                "By continuing, you confirm full adherence to our usage policies.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
