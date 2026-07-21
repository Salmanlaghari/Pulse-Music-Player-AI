package com.salmanlaghari.pulsemusicplayerai.presentation.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun SettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Screen Header
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Preference Section 1: Appearance
            Text(
                text = "APPEARANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleLight,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = Pink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Dark Mode",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Lock app-wide default dark interface",
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                    }

                    Switch(
                        checked = true, // Locked to Dark Theme natively
                        onCheckedChange = { },
                        enabled = false, // Always locked on dark theme for premium presentation
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Purple,
                            checkedTrackColor = Purple.copy(alpha = 0.4f),
                            disabledCheckedThumbColor = Purple,
                            disabledCheckedTrackColor = Purple.copy(alpha = 0.25f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preference Section 2: Info & Support
            Text(
                text = "INFO & SUPPORT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleLight,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column {
                    SettingsClickableItem(
                        title = "About",
                        subtitle = "Get app details and credits",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                    SettingsClickableItem(
                        title = "Feedback",
                        subtitle = "Share bugs, ideas or reviews",
                        icon = Icons.Default.Feedback,
                        onClick = onNavigateToFeedback
                    )
                    SettingsClickableItem(
                        title = "Privacy Policy",
                        subtitle = "Read our data use agreement",
                        icon = Icons.Default.Security,
                        onClick = onNavigateToPrivacy
                    )
                    SettingsClickableItem(
                        title = "Terms of Service",
                        subtitle = "View license terms & conditions",
                        icon = Icons.Default.Lock,
                        onClick = onNavigateToTerms
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preference Section 3: Version details
            Text(
                text = "SYSTEM INFO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleLight,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Version",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Release Build 1.0.0 (Session 7 Polished)",
                            fontSize = 11.sp,
                            color = TextDim
                        )
                    }
                    Text(
                        text = "v1.0.0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pink
                    )
                }
            }

            Spacer(modifier = Modifier.height(160.dp)) // Safe padding space for floating bottom components
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PurpleLight,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextDim
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = TextDim.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
