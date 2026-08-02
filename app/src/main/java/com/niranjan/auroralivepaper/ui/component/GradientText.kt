package com.niranjan.auroralivepaper.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.niranjan.auroralivepaper.ui.theme.AuroraFrostedBlue
import com.niranjan.auroralivepaper.ui.theme.AuroraRoyalViolet

@Composable
fun AuroraGradientText(text: String, fontSize: androidx.compose.ui.unit.TextUnit, fontWeight: androidx.compose.ui.text.font.FontWeight) {
    val infinite = rememberInfiniteTransition(label = "textGlow")
    val shift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )
    val brush = Brush.horizontalGradient(
        colors = listOf(AuroraRoyalViolet, AuroraFrostedBlue, AuroraRoyalViolet),
        startX = shift * 200f,
        endX = shift * 200f + 400f
    )
    Text(
        text = text,
        style = TextStyle(brush = brush, fontSize = fontSize, fontWeight = fontWeight)
    )
}