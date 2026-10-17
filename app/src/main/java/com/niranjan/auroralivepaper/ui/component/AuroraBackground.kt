package com.niranjan.auroralivepaper.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.niranjan.auroralivepaper.data.SettingsPrefs
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

@Composable
fun AuroraAnimatedBackground(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val richMotion = remember(context) { SettingsPrefs.richMotion(context) }

    val infinite = rememberInfiniteTransition(label = "auroraRibbonMotion")

    val animatedBlobDrift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blobDrift"
    )

    val animatedTwinkle by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )

    val blobDrift = if (richMotion) animatedBlobDrift else 0f
    val twinkle = if (richMotion) animatedTwinkle else 0f

    val stars = remember {
        val seed = Random(19)
        List(12) {
            Triple(
                seed.nextFloat(),
                seed.nextFloat() * 0.45f,
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

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().background(NearBlack)) {
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

            // Authentic aurora ribbons
            drawRibbon(AuroraGreen.copy(alpha = 0.30f), size.width * (0.2f + blobDrift * 0.15f), size.height * 0.18f, size.minDimension * 0.55f, 2.6f, -18f)
            drawRibbon(DeepPurple.copy(alpha = 0.28f), size.width * (0.1f + blobDrift * 0.12f), size.height * 0.32f, size.minDimension * 0.5f, 2.3f, -22f)
            drawRibbon(Violet.copy(alpha = 0.24f), size.width * (0.42f + blobDrift * 0.1f), size.height * 0.42f, size.minDimension * 0.45f, 2.5f, 15f)
            drawRibbon(MidnightBlue.copy(alpha = 0.26f), size.width * (0.6f - blobDrift * 0.12f), size.height * 0.55f, size.minDimension * 0.45f, 2.3f, -10f)
            drawRibbon(Cyan.copy(alpha = 0.24f), size.width * (0.75f - blobDrift * 0.1f), size.height * 0.68f, size.minDimension * 0.4f, 2.4f, 20f)
            drawRibbon(AuroraGreen.copy(alpha = 0.22f), size.width * (0.55f - blobDrift * 0.08f), size.height * 0.8f, size.minDimension * 0.5f, 2.6f, 8f)
            drawRibbon(SoftTeal.copy(alpha = 0.26f), size.width * (0.85f - blobDrift * 0.15f), size.height * 0.88f, size.minDimension * 0.45f, 2.2f, -15f)
            drawRibbon(PinkEdge.copy(alpha = 0.12f), size.width * (0.3f + blobDrift * 0.1f), size.height * 0.25f, size.minDimension * 0.4f, 2.8f, -25f)

            // Subtle grain
            val grainSeed = Random(101)
            repeat(100) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.018f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(grainSeed.nextFloat() * size.width, grainSeed.nextFloat() * size.height)
                )
            }

            // Twinkling stars
            stars.forEach { (sx, sy, phase) ->
                val localTwinkle = (twinkle + phase) % 1f
                val starAlpha = (sin(localTwinkle * Math.PI).toFloat()).coerceIn(0.1f, 1f) * 0.6f
                val starSize = 1.2f + 1.2f * starAlpha
                drawCircle(
                    color = Color.White.copy(alpha = starAlpha),
                    radius = starSize.dp.toPx(),
                    center = Offset(size.width * sx, size.height * sy)
                )
            }

            drawRect(brush = vignetteBrush)
        }
        content()
    }
}
