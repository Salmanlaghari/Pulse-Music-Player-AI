package com.salmanlaghari.pulsemusicplayerai.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salmanlaghari.pulsemusicplayerai.theme.BgCard
import com.salmanlaghari.pulsemusicplayerai.theme.BorderColor

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = BorderColor,
    containerColor: Color = BgCard,
    backgroundBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .then(
                if (backgroundBrush != null) {
                    Modifier.background(backgroundBrush)
                } else {
                    Modifier.background(containerColor)
                }
            )
            .then(clickModifier)
    ) {
        content()
    }
}
