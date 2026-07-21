package com.salmanlaghari.pulsemusicplayerai.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salmanlaghari.pulsemusicplayerai.theme.CyanGlow
import com.salmanlaghari.pulsemusicplayerai.theme.GlassBg
import com.salmanlaghari.pulsemusicplayerai.theme.GlassBorder
import com.salmanlaghari.pulsemusicplayerai.theme.Purple1

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = GlassBorder,
    borderBrush: Brush? = null,
    containerColor: Color = GlassBg,
    backgroundBrush: Brush? = null,
    is3D: Boolean = false,
    hasShine: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val graphicsModifier = if (is3D) {
        Modifier.graphicsLayer {
            rotationX = 3f
            rotationY = -2f
            cameraDistance = 16f * density
        }
    } else {
        Modifier
    }

    val borderModifier = if (borderBrush != null) {
        Modifier.border(borderWidth, borderBrush, shape)
    } else {
        Modifier.border(borderWidth, borderColor, shape)
    }

    val shineModifier = if (hasShine) {
        Modifier.drawWithContent {
            drawContent()
            // Draw diagonal reflection shine
            val gradient = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                    Color.Transparent
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 2.0f)
            )
            drawRect(brush = gradient)
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(graphicsModifier)
            .clip(shape)
            .then(borderModifier)
            .then(
                if (backgroundBrush != null) {
                    Modifier.background(backgroundBrush)
                } else {
                    Modifier.background(containerColor)
                }
            )
            .then(shineModifier)
            .then(clickModifier)
    ) {
        content()
    }
}
