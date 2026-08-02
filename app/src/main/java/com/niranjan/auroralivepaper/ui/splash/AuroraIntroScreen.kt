package com.niranjan.auroralivepaper.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.ui.theme.AuroraCoral
import com.niranjan.auroralivepaper.ui.theme.AuroraTeal
import com.niranjan.auroralivepaper.ui.theme.AuroraViolet
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.alpha
@Composable
fun AuroraIntroScreen(onFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradientSweep")
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepOffset"
    )

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(700))
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(AuroraViolet, AuroraTeal, AuroraCoral),
                    start = Offset(sweep, 0f),
                    end = Offset(sweep + 500f, 500f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Aurora Live Paper",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.alpha(alpha.value)
        )
    }
}
