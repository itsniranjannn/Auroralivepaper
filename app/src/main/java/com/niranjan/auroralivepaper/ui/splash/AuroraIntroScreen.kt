package com.niranjan.auroralivepaper.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AuroraIntroScreen(
    onTransitionStart: () -> Unit = {},
    onFinished: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val logoTrimProgress = remember { Animatable(0.0f) }
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0.0f) }

    val titleAlpha = remember { Animatable(0.0f) }
    val titleOffsetY = remember { Animatable(24.0f) }
    val titleOffsetX = remember { Animatable(0.0f) }
    val titleScale = remember { Animatable(1.0f) }

    val subtitleAlpha = remember { Animatable(0.0f) }
    val subtitleOffsetY = remember { Animatable(16.0f) }
    val laserLineWidth = remember { Animatable(0.0f) }

    val loaderVisible = remember { mutableStateOf(false) }
    val activePillIndex = remember { Animatable(0.0f) }

    val rippleAlpha = remember { Animatable(0.0f) }
    val rippleRadius = remember { Animatable(0.0f) }
    val screenFadeOut = remember { Animatable(1.0f) }

    val shimmerTransition = rememberInfiniteTransition("shimmerTransition")
    val titleShimmerShift by shimmerTransition.animateFloat(
        initialValue = -200.0f,
        targetValue = 600.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "titleShimmerShift"
    )
    val breathingGlowScale by shimmerTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingGlowScale"
    )

    val pathMeasure1 = remember { PathMeasure() }
    val pathMeasure2 = remember { PathMeasure() }
    val segmentPath1 = remember { Path() }
    val segmentPath2 = remember { Path() }

    LaunchedEffect(Unit) {
        delay(120L)
        logoAlpha.animateTo(1.0f, tween(300))
        logoScale.animateTo(1.0f, spring(0.62f, 220.0f))
        logoTrimProgress.animateTo(1.0f, tween(900, easing = FastOutSlowInEasing))

        delay(100L)
        titleAlpha.animateTo(1.0f, tween(400))
        titleOffsetY.animateTo(0.0f, spring(0.75f, 400.0f))

        delay(200L)
        coroutineScope.launch {
            subtitleAlpha.animateTo(1.0f, tween(350))
            subtitleOffsetY.animateTo(0.0f, spring(0.8f, 400.0f))
        }
        coroutineScope.launch {
            laserLineWidth.animateTo(80.0f, spring(0.65f, 200.0f))
        }

        delay(350L)
        loaderVisible.value = true
        activePillIndex.animateTo(1.0f, spring(0.5f, 300.0f))

        delay(320L)
        activePillIndex.animateTo(2.0f, spring(0.5f, 300.0f))

        delay(400L)
        onTransitionStart()

        coroutineScope.launch {
            rippleAlpha.snapTo(0.65f)
            rippleRadius.animateTo(180.0f, tween(550, easing = FastOutSlowInEasing))
            rippleAlpha.animateTo(0.0f, tween(350, easing = LinearEasing))
        }
        coroutineScope.launch {
            logoScale.animateTo(0.55f, tween(450, easing = FastOutSlowInEasing))
            logoAlpha.animateTo(0.0f, tween(350))
        }
        coroutineScope.launch {
            subtitleAlpha.animateTo(0.0f, tween(250))
            laserLineWidth.animateTo(0.0f, tween(250))
        }
        coroutineScope.launch {
            titleScale.animateTo(0.68f, tween(500, easing = FastOutSlowInEasing))
            titleOffsetX.animateTo(-75.0f, tween(500, easing = FastOutSlowInEasing))
            titleOffsetY.animateTo(-240.0f, tween(500, easing = FastOutSlowInEasing))
        }

        delay(420L)
        screenFadeOut.animateTo(0.0f, tween(220))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenFadeOut.value)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                if (rippleAlpha.value > 0.0f) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        drawCircle(
                            color = Color(0xFF4DD0E1).copy(alpha = rippleAlpha.value),
                            radius = rippleRadius.value.dp.toPx(),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(breathingGlowScale)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4DD0E1).copy(alpha = 0.55f),
                                    Color(0xFF651FFF).copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension / 2.0f
                        )
                    }
                }

                Canvas(modifier = Modifier.size(92.dp)) {
                    val w = size.width
                    val h = size.height

                    val wavePath1 = Path().apply {
                        moveTo(0.12f * w, 0.74f * h)
                        cubicTo(0.35f * w, 0.14f * h, 0.58f * w, 0.86f * h, 0.88f * w, 0.26f * h)
                    }

                    val wavePath2 = Path().apply {
                        moveTo(0.18f * w, 0.54f * h)
                        cubicTo(0.42f * w, 0.06f * h, 0.62f * w, 0.66f * h, 0.94f * w, 0.08f * h)
                    }

                    val progress = logoTrimProgress.value

                    pathMeasure1.setPath(wavePath1, false)
                    segmentPath1.reset()
                    pathMeasure1.getSegment(0.0f, pathMeasure1.length * progress, segmentPath1, true)
                    drawPath(
                        path = segmentPath1,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF651FFF),
                                Color(0xFF4DD0E1),
                                Color.White
                            )
                        ),
                        style = Stroke(
                            width = 6.5f.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    pathMeasure2.setPath(wavePath2, false)
                    segmentPath2.reset()
                    pathMeasure2.getSegment(0.0f, pathMeasure2.length * progress, segmentPath2, true)
                    drawPath(
                        path = segmentPath2,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4DD0E1),
                                Color(0xFF00BFA5),
                                Color.White
                            )
                        ),
                        style = Stroke(
                            width = 4.2f.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .offset(x = titleOffsetX.value.dp, y = titleOffsetY.value.dp)
                    .scale(titleScale.value)
                    .alpha(titleAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(width = 230.dp, height = 64.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4DD0E1).copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width / 2.2f
                    )
                }

                Text(
                    text = "Aurora",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF4DD0E1),
                                Color(0xFFB388FF),
                                Color(0xFF00BFA5),
                                Color(0xFF00E5FF)
                            ),
                            start = Offset(titleShimmerShift, 0.0f),
                            end = Offset(titleShimmerShift + 260.0f, 0.0f)
                        ),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = ManropeFamily,
                        letterSpacing = 1.8.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .offset(y = subtitleOffsetY.value.dp)
                    .alpha(subtitleAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bring your screen to life.",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ManropeFamily,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(laserLineWidth.value.dp)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF4DD0E1),
                                    Color(0xFF00BFA5),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(38.dp))

            if (loaderVisible.value) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 3) {
                        val isActive = abs(activePillIndex.value - i.toFloat()) < 0.5f
                        val width = if (isActive) 24.dp else 6.dp
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(width)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isActive) Color(0xFF4DD0E1).copy(alpha = 0.95f)
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }
    }
}
