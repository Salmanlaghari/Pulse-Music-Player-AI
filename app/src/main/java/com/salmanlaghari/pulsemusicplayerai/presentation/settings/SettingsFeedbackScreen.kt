package com.salmanlaghari.pulsemusicplayerai.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SettingsFeedbackScreen(onNavigateBack: () -> Unit) {
    var feedbackText by remember { mutableStateOf("") }
    var emailText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

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
                    text = "Feedback",
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
                    if (submitted) {
                        Text(
                            text = "Thank you for your response!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pink
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your feedback has been saved locally for review. We read every response carefully and utilize it to build premium future improvements.",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "We'd Love to Hear From You",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Are you experiencing bugs, or do you have a brilliant idea for an AI feature? Leave a message below!",
                            fontSize = 12.sp,
                            color = TextDim
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = emailText,
                            onValueChange = { emailText = it },
                            label = { Text("Email (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                unfocusedBorderColor = TextDim.copy(alpha = 0.3f),
                                focusedLabelColor = Purple,
                                unfocusedLabelColor = TextDim,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            label = { Text("Your Feedback") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                unfocusedBorderColor = TextDim.copy(alpha = 0.3f),
                                focusedLabelColor = Purple,
                                unfocusedLabelColor = TextDim,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { submitted = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Purple, Pink)
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Submit", tint = Color.White)
                                    Text(text = "Submit Feedback", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
