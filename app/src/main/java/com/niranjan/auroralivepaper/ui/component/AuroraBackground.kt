package com.niranjan.auroralivepaper.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.niranjan.auroralivepaper.ui.theme.*

@Composable
fun AuroraAnimatedBackground(content: @Composable () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "bgMotion")

    val t1 by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow1"
    )
    val t2 by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow2"
    )

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().background(AuroraBackgroundDark)) {
            // Soft moving glow blob 1 (violet)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraRoyalViolet.copy(alpha = 0.35f), androidx.compose.ui.graphics.Color.Transparent)
                ),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * (0.2f + t1 * 0.5f), size.height * 0.25f)
            )
            // Soft moving glow blob 2 (teal)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraStormyTeal.copy(alpha = 0.30f), androidx.compose.ui.graphics.Color.Transparent)
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * (0.8f - t2 * 0.4f), size.height * 0.75f)
            )
        }
        content()
    }
}