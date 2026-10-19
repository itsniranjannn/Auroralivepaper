package com.niranjan.auroralivepaper.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private val NearBlack = Color(0xFF07090F)
private val DeepPurple = Color(0xFF2D1B4E)
private val Violet = Color(0xFF4A2E7A)
private val MidnightBlue = Color(0xFF1B2A4E)
private val AuroraGreen = Color(0xFF2E7A5C)
private val Cyan = Color(0xFF2C6E7A)
private val SoftTeal = Color(0xFF1F4E4A)
private val PinkEdge = Color(0xFF7A3E6E)

private object Motion {
    const val BgIn = 400
    const val LogoIn = 800
    const val TitleIn = 400
    const val SubtitleIn = 400
    const val ExitFade = 500
}

@Composable
fun AuroraIntroScreen(onFinished: () -> Unit) {
    var bgVisible by remember { mutableStateOf(false) }
    var logoVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }

    val screenAlpha = remember { Animatable(1f) }
    val contentScale = remember { Animatable(1f) }
    val titleOffsetY = remember { Animatable(20f) }

    val infinite = rememberInfiniteTransition(label = "flow")
    val blobDrift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blobDrift"
    )
    val loaderProgress by infinite.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "loaderProgress"
    )
    val titleShift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Reverse),
        label = "titleShift"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (bgVisible) 1f else 0f,
        animationSpec = tween(Motion.BgIn),
        label = "bgAlpha"
    )

    val logoScale = remember { Animatable(0.2f) }
    val logoDraw by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(Motion.LogoIn, easing = FastOutSlowInEasing),
        label = "logoDraw"
    )
    val logoGlowOnce = remember { Animatable(0f) }

    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(Motion.TitleIn),
        label = "titleAlpha"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (subtitleVisible) 1f else 0f,
        animationSpec = tween(Motion.SubtitleIn),
        label = "subtitleAlpha"
    )

    val particles = remember { List(4) { Pair(Random.nextFloat(), Random.nextFloat()) } }

    val stars = remember {
        val seed = Random(12)
        List(12) {
            Triple(
                seed.nextFloat(),
                seed.nextFloat() * 0.35f,
                seed.nextFloat()
            )
        }
    }

    val vignetteBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
            radius = 1200f
        )
    }

    LaunchedEffect(Unit) {
        delay(150); bgVisible = true
        delay(Motion.BgIn.toLong())
        logoVisible = true
        logoScale.animateTo(1.15f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 160f))
        logoGlowOnce.animateTo(1f, animationSpec = tween(400))
        logoScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f))
        logoGlowOnce.animateTo(0.6f, animationSpec = tween(400))

        delay(Motion.TitleIn.toLong()); titleVisible = true
        titleOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 220f))

        delay(Motion.SubtitleIn.toLong()); subtitleVisible = true
        delay(200); showLoading = true
        delay(1000)

        titleOffsetY.animateTo(-30f, animationSpec = tween(Motion.ExitFade, easing = FastOutSlowInEasing))
        contentScale.animateTo(0.94f, animationSpec = tween(Motion.ExitFade))
        screenAlpha.animateTo(0f, animationSpec = tween(Motion.ExitFade))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(NearBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().alpha(bgAlpha)) {
            drawRect(color = NearBlack)

            fun drawRibbon(color: Color, cx: Float, cy: Float, radius: Float, stretchX: Float, rotationDeg: Float) {
                rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
                    scale(scaleX = stretchX, scaleY = 1f, pivot = Offset(cx, cy)) {
                        drawCircle(
                            brush = Brush.radialGradient(colors = listOf(color, Color.Transparent)),
                            radius = radius,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            drawRibbon(AuroraGreen.copy(alpha = 0.38f), size.width * (0.2f + blobDrift * 0.15f), size.height * 0.18f, size.minDimension * 0.58f, 2.6f, -18f)
            drawRibbon(DeepPurple.copy(alpha = 0.32f), size.width * (0.1f + blobDrift * 0.12f), size.height * 0.32f, size.minDimension * 0.52f, 2.3f, -22f)
            drawRibbon(Violet.copy(alpha = 0.28f), size.width * (0.42f + blobDrift * 0.1f), size.height * 0.42f, size.minDimension * 0.48f, 2.5f, 15f)
            drawRibbon(MidnightBlue.copy(alpha = 0.30f), size.width * (0.6f - blobDrift * 0.12f), size.height * 0.55f, size.minDimension * 0.48f, 2.3f, -10f)
            drawRibbon(Cyan.copy(alpha = 0.28f), size.width * (0.75f - blobDrift * 0.1f), size.height * 0.68f, size.minDimension * 0.44f, 2.4f, 20f)
            drawRibbon(AuroraGreen.copy(alpha = 0.26f), size.width * (0.55f - blobDrift * 0.08f), size.height * 0.8f, size.minDimension * 0.52f, 2.6f, 8f)
            drawRibbon(SoftTeal.copy(alpha = 0.30f), size.width * (0.85f - blobDrift * 0.15f), size.height * 0.88f, size.minDimension * 0.48f, 2.2f, -15f)
            drawRibbon(PinkEdge.copy(alpha = 0.16f), size.width * (0.3f + blobDrift * 0.1f), size.height * 0.25f, size.minDimension * 0.42f, 2.8f, -25f)

            val grainSeed = Random(42)
            repeat(160) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.025f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(grainSeed.nextFloat() * size.width, grainSeed.nextFloat() * size.height)
                )
            }

            stars.forEach { (sx, sy, phase) ->
                val localTwinkle = ((twinkle + phase) % 1f)
                val starAlpha = (sin(localTwinkle * Math.PI).toFloat()).coerceIn(0.15f, 1f) * 0.85f
                val starSize = (1.2f + 1.5f * starAlpha)
                drawCircle(
                    color = Color.White.copy(alpha = starAlpha),
                    radius = starSize.dp.toPx(),
                    center = Offset(size.width * sx, size.height * sy)
                )
            }

            particles.forEach { (px, py) ->
                val driftY = (py + blobDrift * 0.06f) % 1f
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = 3.dp.toPx(),
                    center = Offset(size.width * px, size.height * driftY)
                )
            }

            drawRect(brush = vignetteBrush)
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(contentScale.value)
                .offset(y = titleOffsetY.value.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(logoScale.value)) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .alpha(logoGlowOnce.value * 0.5f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(colors = listOf(Color(0xFF7CE7D7).copy(alpha = 0.65f), Color(0xFFB38CFF).copy(alpha = 0.3f), Color.Transparent)),
                            radius = size.minDimension / 2f
                        )
                    }
                }
                Canvas(modifier = Modifier.size(86.dp)) {
                    val w = size.width
                    val h = size.height
                    val path1 = Path().apply {
                        moveTo(w * 0.1f, h * 0.75f)
                        cubicTo(w * 0.35f, h * 0.15f, w * 0.55f, h * 0.85f, w * 0.9f * logoDraw, h * 0.25f)
                    }
                    drawPath(path1, brush = Brush.linearGradient(listOf(Color(0xFFB38CFF), Color(0xFF7CE7D7), Color.White)), style = Stroke(width = 6.5.dp.toPx()))
                    val path2 = Path().apply {
                        moveTo(w * 0.15f, h * 0.55f)
                        cubicTo(w * 0.4f, h * 0.05f, w * 0.6f, h * 0.65f, w * 0.95f * logoDraw, h * 0.05f)
                    }
                    drawPath(path2, brush = Brush.linearGradient(listOf(Color(0xFF7CE7D7), Color.White)), style = Stroke(width = 4.dp.toPx()))
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 3D Shimmering "Aurora" Title with Radial Aura
            Box(contentAlignment = Alignment.Center) {
                // Background radial glow behind text
                Canvas(modifier = Modifier.size(200.dp, 60.dp).alpha(titleAlpha * 0.6f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF7CE7D7).copy(alpha = 0.45f), Color.Transparent)
                        ),
                        radius = size.width / 2f
                    )
                }

                val movingAuroraBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00F2FE),
                        Color(0xFF7CE7D7),
                        Color(0xFFB38CFF),
                        Color(0xFF82B1FF),
                        Color(0xFF00F2FE)
                    ),
                    start = Offset(titleShift * 350f - 175f, 0f),
                    end = Offset(titleShift * 350f + 275f, 0f)
                )

                // Soft shadow layer
                Text(
                    text = "Aurora",
                    style = TextStyle(
                        brush = movingAuroraBrush,
                        fontFamily = ManropeFamily,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    ),
                    modifier = Modifier
                        .alpha(titleAlpha * 0.35f)
                        .offset(x = 2.dp, y = 2.dp)
                )

                // Sharp foreground layer
                Text(
                    text = "Aurora",
                    style = TextStyle(
                        brush = movingAuroraBrush,
                        fontFamily = ManropeFamily,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    ),
                    modifier = Modifier.alpha(titleAlpha)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Glass Pill Badge for Subtitle
            Box(
                modifier = Modifier
                    .alpha(subtitleAlpha)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Bring your screen to life.",
                    style = TextStyle(
                        fontFamily = ManropeFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (showLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in 0..2) {
                        val phase = (loaderProgress - i).let { if (it < 0) it + 3 else it }
                        val pulse = when {
                            phase < 1f -> sin(phase * Math.PI).toFloat().coerceIn(0f, 1f)
                            else -> 0f
                        }
                        val dotSize = 6.dp + (4.dp * pulse)
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .background(Color(0xFF7CE7D7).copy(alpha = 0.6f + 0.4f * pulse), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
