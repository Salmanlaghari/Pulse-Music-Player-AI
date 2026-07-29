package com.salmanlaghari.pulsemusicplayerai.data.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom sheet showing all rewarded ad options.
 * Users watch ads to unlock premium features temporarily.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardedAdSheet(
    activity: Activity,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "🎁 Watch & Unlock",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Watch a short ad to unlock premium features",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Ad-free for 1 hour
            RewardedAdOption(
                icon = Icons.Default.AutoAwesome,
                title = "1 Hour Ad-Free",
                subtitle = "Remove all ads for 60 minutes",
                onClick = {
                    AdManager.showRewardedAdFreeHour(activity) {
                        onDismiss()
                    }
                }
            )

            // Unlimited skips
            RewardedAdOption(
                icon = Icons.Default.SkipNext,
                title = "Unlimited Skips",
                subtitle = "Skip as many songs as you want",
                onClick = {
                    AdManager.showRewardedUnlimitedSkip(activity) {
                        onDismiss()
                    }
                }
            )

            // Pro Equalizer
            RewardedAdOption(
                icon = Icons.Default.Equalizer,
                title = "Pro Equalizer",
                subtitle = "Unlock advanced equalizer presets",
                onClick = {
                    AdManager.showRewardedProEqualizer(activity) {
                        onDismiss()
                    }
                }
            )

            // HQ Audio
            RewardedAdOption(
                icon = Icons.Default.HighQuality,
                title = "HQ 320kbps Audio",
                subtitle = "Enable high quality audio playback",
                onClick = {
                    AdManager.showRewardedHqAudio(activity) {
                        onDismiss()
                    }
                }
            )

            // Offline Download
            RewardedAdOption(
                icon = Icons.Default.MusicNote,
                title = "Download 1 Song",
                subtitle = "Save 1 song for offline listening",
                onClick = {
                    AdManager.showRewardedOfflineDownload(activity) {
                        onDismiss()
                    }
                }
            )

            // Premium Theme
            RewardedAdOption(
                icon = Icons.Default.Palette,
                title = "Premium Theme",
                subtitle = "Unlock exclusive app themes",
                onClick = {
                    AdManager.showRewardedPremiumTheme(activity) {
                        onDismiss()
                    }
                }
            )

            // Sleep Timer Extend
            RewardedAdOption(
                icon = Icons.Default.Timer,
                title = "Extend Sleep Timer",
                subtitle = "Add 30 minutes to your sleep timer",
                onClick = {
                    AdManager.showRewardedSleepTimer(activity) {
                        onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
private fun RewardedAdOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
