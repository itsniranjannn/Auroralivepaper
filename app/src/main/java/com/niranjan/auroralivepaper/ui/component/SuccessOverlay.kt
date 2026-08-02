package com.niranjan.auroralivepaper.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.niranjan.auroralivepaper.ui.theme.AuroraGlow
import kotlinx.coroutines.delay

@Composable
fun SuccessOverlay(visible: Boolean, onDone: () -> Unit) {
    if (!visible) return
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(visible) {
        scale.animateTo(1.1f, animationSpec = spring(dampingRatio = 0.4f))
        scale.animateTo(1f, animationSpec = tween(150))
        delay(1000)
        onDone()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value)) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AuroraGlow, modifier = Modifier.size(64.dp))
        Text("Wallpaper Applied", color = Color.White, modifier = Modifier.padding(top = 8.dp))
    }
}