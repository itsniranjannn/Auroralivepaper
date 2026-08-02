package com.niranjan.auroralivepaper.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.ui.theme.AuroraCoral
import com.niranjan.auroralivepaper.ui.theme.AuroraTeal
import com.niranjan.auroralivepaper.ui.theme.AuroraViolet
import kotlinx.coroutines.delay

@Composable
fun AuroraIntroScreen(onFinished: () -> Unit) {
    val fullTitle = "Aurora"
    val subtitle = "Every screen, a little more yours."

    var orbVisible by remember { mutableStateOf(false) }
    var titleChars by remember { mutableStateOf(0) }
    var subtitleWords by remember { mutableStateOf(0) }
    var loadingProgress by remember { mutableStateOf(0f) }
    val screenAlpha = remember { Animatable(1f) }
    val screenScale = remember { Animatable(1f) }

    val orbScale by animateFloatAsState(
        targetValue = if (orbVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "orbScale"
    )

    val infinite = rememberInfiniteTransition(label = "gradientFlow")
    val flow by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "flow"
    )

    val loadingAnim by animateFloatAsState(
        targetValue = loadingProgress,
        animationSpec = tween(600),
        label = "loadingBar"
    )

    LaunchedEffect(Unit) {
        delay(300)
        orbVisible = true
        delay(600)
        // typewriter title
        for (i in 1..fullTitle.length) {
            titleChars = i
            delay(60)
        }
        delay(200)
        // word-by-word subtitle
        val words = subtitle.split(" ")
        for (i in 1..words.size) {
            subtitleWords = i
            delay(120)
        }
        delay(200)
        loadingProgress = 1f
        delay(700)
        screenScale.animateTo(1.05f, animationSpec = tween(350))
        screenAlpha.animateTo(0f, animationSpec = tween(350))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .scale(screenScale.value)
            .background(
                Brush.linearGradient(
                    colors = listOf(AuroraViolet, AuroraTeal, AuroraCoral),
                    start = Offset(flow * 800f, 0f),
                    end = Offset(flow * 800f + 600f, 800f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .scale(orbScale)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = fullTitle.take(titleChars),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle.split(" ").take(subtitleWords).joinToString(" "),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(loadingAnim)
                        .background(Color.White)
                )
            }
        }
    }
}