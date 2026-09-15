package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandPurple

@Composable
fun ScanningAnimationOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_transition")
    val scanYProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val currentY = height * scanYProgress

            // Draw glowing scan band
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        BrandCyan.copy(alpha = 0.25f),
                        BrandPurple.copy(alpha = 0.5f),
                        BrandCyan.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    startY = (currentY - 60f).coerceAtLeast(0f),
                    endY = (currentY + 60f).coerceAtMost(height)
                ),
                topLeft = Offset(0f, (currentY - 60f).coerceAtLeast(0f)),
                size = Size(width, 120f)
            )

            // Draw bright central laser line
            drawLine(
                color = BrandCyan,
                start = Offset(0f, currentY),
                end = Offset(width, currentY),
                strokeWidth = 4f
            )
        }
    }
}
