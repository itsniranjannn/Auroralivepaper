package com.niranjan.auroralivepaper.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.delay
import kotlin.random.Random

private val NearBlack = Color(0xFF07090F)
private val DeepPurple = Color(0xFF2D1B4E)
private val Violet = Color(0xFF4A2E7A)
private val MidnightBlue = Color(0xFF1B2A4E)
private val AuroraGreen = Color(0xFF2E7A5C)   // classic aurora borealis green
private val Cyan = Color(0xFF2C6E7A)
private val SoftTeal = Color(0xFF1F4E4A)
private val PinkEdge = Color(0xFF7A3E6E)      // faint pink edge glow, real auroras have this

private object Motion {
    const val BgIn = 400
    const val LogoIn = 900
    const val TitleIn = 300
    const val SubtitleIn = 300
    const val ExitFade = 600
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
    val titleOffsetY = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "flow")
    val blobDrift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blobDrift"
    )
    val loaderProgress by infinite.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "loaderProgress"
    )
    // moving gradient shift for the "Aurora" title — like light passing through it
    val titleShift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "titleShift"
    )
    // twinkle cycle for stars — each star uses a phase offset off this
    val twinkle by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (bgVisible) 1f else 0f,
        animationSpec = tween(Motion.BgIn),
        label = "bgAlpha"
    )

    val logoScale = remember { Animatable(0.3f) }
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
        targetValue = if (subtitleVisible) 0.6f else 0f,
        animationSpec = tween(Motion.SubtitleIn),
        label = "subtitleAlpha"
    )

    val particles = remember { List(3) { Pair(Random.nextFloat(), Random.nextFloat()) } }

    // stars scattered near the top third of the screen, each with its own twinkle phase + size
    val stars = remember {
        val seed = Random(7)
        List(9) {
            Triple(
                seed.nextFloat(),                     // x (0..1)
                seed.nextFloat() * 0.28f,              // y (top ~28% of screen)
                seed.nextFloat()                       // phase offset (0..1)
            )
        }
    }

    val vignetteBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
            radius = 1200f
        )
    }

    LaunchedEffect(Unit) {
        delay(200); bgVisible = true
        delay(Motion.BgIn.toLong())
        logoVisible = true
        logoScale.animateTo(1.12f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 140f))
        logoGlowOnce.animateTo(1f, animationSpec = tween(500))
        logoScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f))
        logoGlowOnce.animateTo(0.5f, animationSpec = tween(500))

        delay(Motion.TitleIn.toLong()); titleVisible = true
        delay(Motion.SubtitleIn.toLong()); subtitleVisible = true
        delay(300); showLoading = true
        delay(900)

        titleOffsetY.animateTo(-36f, animationSpec = tween(Motion.ExitFade, easing = FastOutSlowInEasing))
        contentScale.animateTo(0.92f, animationSpec = tween(Motion.ExitFade))
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

            // Richer, more authentic aurora palette — green-dominant with purple/pink/teal accents
            drawRibbon(AuroraGreen.copy(alpha = 0.34f), size.width * (0.2f + blobDrift * 0.15f), size.height * 0.18f, size.minDimension * 0.55f, 2.6f, -18f)
            drawRibbon(DeepPurple.copy(alpha = 0.30f), size.width * (0.1f + blobDrift * 0.12f), size.height * 0.32f, size.minDimension * 0.5f, 2.3f, -22f)
            drawRibbon(Violet.copy(alpha = 0.26f), size.width * (0.42f + blobDrift * 0.1f), size.height * 0.42f, size.minDimension * 0.45f, 2.5f, 15f)
            drawRibbon(MidnightBlue.copy(alpha = 0.28f), size.width * (0.6f - blobDrift * 0.12f), size.height * 0.55f, size.minDimension * 0.45f, 2.3f, -10f)
            drawRibbon(Cyan.copy(alpha = 0.26f), size.width * (0.75f - blobDrift * 0.1f), size.height * 0.68f, size.minDimension * 0.4f, 2.4f, 20f)
            drawRibbon(AuroraGreen.copy(alpha = 0.24f), size.width * (0.55f - blobDrift * 0.08f), size.height * 0.8f, size.minDimension * 0.5f, 2.6f, 8f)
            drawRibbon(SoftTeal.copy(alpha = 0.28f), size.width * (0.85f - blobDrift * 0.15f), size.height * 0.88f, size.minDimension * 0.45f, 2.2f, -15f)
            drawRibbon(PinkEdge.copy(alpha = 0.14f), size.width * (0.3f + blobDrift * 0.1f), size.height * 0.25f, size.minDimension * 0.4f, 2.8f, -25f)

            // grain
            val grainSeed = Random(42)
            repeat(140) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.02f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(grainSeed.nextFloat() * size.width, grainSeed.nextFloat() * size.height)
                )
            }

            // twinkling stars near the top
            stars.forEach { (sx, sy, phase) ->
                val localTwinkle = ((twinkle + phase) % 1f)
                val starAlpha = (kotlin.math.sin(localTwinkle * Math.PI).toFloat()).coerceIn(0.1f, 1f) * 0.7f
                val starSize = (1.2f + 1.3f * starAlpha)
                drawCircle(
                    color = Color.White.copy(alpha = starAlpha),
                    radius = starSize.dp.toPx(),
                    center = Offset(size.width * sx, size.height * sy)
                )
            }

            particles.forEach { (px, py) ->
                val driftY = (py + blobDrift * 0.05f) % 1f
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
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
                        .size(160.dp)
                        .alpha(logoGlowOnce.value * 0.4f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(colors = listOf(SoftTeal.copy(alpha = 0.6f), Color.Transparent)),
                            radius = size.minDimension / 2f
                        )
                    }
                }
                Canvas(modifier = Modifier.size(80.dp)) {
                    val w = size.width
                    val h = size.height
                    val path1 = Path().apply {
                        moveTo(w * 0.1f, h * 0.75f)
                        cubicTo(w * 0.35f, h * 0.15f, w * 0.55f, h * 0.85f, w * 0.9f * logoDraw, h * 0.25f)
                    }
                    drawPath(path1, brush = Brush.linearGradient(listOf(DeepPurple, SoftTeal, Color.White)), style = Stroke(width = 6.dp.toPx()))
                    val path2 = Path().apply {
                        moveTo(w * 0.15f, h * 0.55f)
                        cubicTo(w * 0.4f, h * 0.05f, w * 0.6f, h * 0.65f, w * 0.95f * logoDraw, h * 0.05f)
                    }
                    drawPath(path2, brush = Brush.linearGradient(listOf(SoftTeal.copy(alpha = 0.8f), Color.White)), style = Stroke(width = 3.5.dp.toPx()))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aurora title: MOVING gradient brush (light shifting through it) + soft glow duplicate
            Box {
                val movingAuroraBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFFB38CFF), Color(0xFF7CE7D7), Color(0xFF93B5FF), Color(0xFFB38CFF)),
                    start = Offset(titleShift * 300f - 150f, 0f),
                    end = Offset(titleShift * 300f + 250f, 0f)
                )
                Text(
                    text = "Aurora",
                    style = TextStyle(
                        brush = movingAuroraBrush,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    modifier = Modifier
                        .alpha(titleAlpha * 0.35f)
                        .offset(x = 1.5.dp, y = 1.5.dp)
                )
                Text(
                    text = "Aurora",
                    style = TextStyle(
                        brush = movingAuroraBrush,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    modifier = Modifier.alpha(titleAlpha)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Bring your screen\nto life.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = subtitleAlpha)
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (showLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in 0..2) {
                        val phase = (loaderProgress - i).let { if (it < 0) it + 3 else it }
                        val pulse = when {
                            phase < 1f -> kotlin.math.sin(phase * Math.PI).toFloat().coerceIn(0f, 1f)
                            else -> 0f
                        }
                        val dotSize = 6.dp + (4.dp * pulse)
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .background(Color.White.copy(alpha = 0.4f + 0.6f * pulse), CircleShape)
                        )
                    }
                }
            }
        }
    }
}