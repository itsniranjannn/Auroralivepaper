package com.niranjan.auroralivepaper.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.niranjan.auroralivepaper.data.SettingsPrefs
import kotlin.math.sin
import kotlin.random.Random

private val NearBlack = Color(0xFF07090F)
private val BioluminescentCyan = Color(0xFF4DD0E1)
private val DeepElectricViolet = Color(0xFF651FFF)
private val EmeraldMint = Color(0xFF00BFA5)
private val AuroraTeal = Color(0xFF00E5FF)

private data class StarParticle(
    val x: Float,
    val y: Float,
    val phase: Float,
    val radius: Float
)

private data class BokehOrb(
    val x: Float,
    val initialY: Float,
    val radius: Float,
    val driftSpeed: Float,
    val color: Color
)

@Composable
fun AuroraLivingBackground(
    modifier: Modifier = Modifier,
    transitionBoost: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val richMotion = remember(context) { SettingsPrefs.richMotion(context) }

    val infinite = rememberInfiniteTransition("livingAuroraMotion")
    val basePhase by infinite.animateFloat(
        initialValue = 0.0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraBasePhase"
    )
    val twinklePhase by infinite.animateFloat(
        initialValue = 0.0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTwinkle"
    )
    val orbDrift by infinite.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bokehDrift"
    )

    val distantStars = remember {
        val rand = Random(42)
        List(26) {
            StarParticle(
                x = rand.nextFloat(),
                y = rand.nextFloat() * 0.75f,
                phase = rand.nextFloat() * 2.0f * 3.1415927f,
                radius = (rand.nextFloat() * 0.9f) + 0.8f
            )
        }
    }

    val bokehOrbs = remember {
        val rand = Random(88)
        List(7) { i ->
            val color = when (i % 3) {
                0 -> BioluminescentCyan
                1 -> DeepElectricViolet
                else -> EmeraldMint
            }
            BokehOrb(
                x = (rand.nextFloat() * 0.84f) + 0.08f,
                initialY = rand.nextFloat(),
                radius = (rand.nextFloat() * 18.0f) + 14.0f,
                driftSpeed = (rand.nextFloat() * 0.05f) + 0.04f,
                color = color
            )
        }
    }

    val vignetteBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.65f)
            ),
            radius = 1350.0f
        )
    }

    val dynamicPhase = if (richMotion) basePhase * transitionBoost else 0.0f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack)
        ) {
            val w = size.width
            val h = size.height

            val path1 = Path()
            val path2 = Path()
            val path3 = Path()

            val p1 = dynamicPhase
            path1.moveTo((-w) * 0.15f, (0.22f * h) + (sin(p1) * 35.0f))
            path1.cubicTo(
                w * 0.25f, (0.12f * h) + (sin(p1 + 1.2f) * 55.0f),
                w * 0.65f, (0.38f * h) + (sin(2.5f + p1) * 65.0f),
                w * 1.15f, (h * 0.25f) + (sin(p1 + 3.8f) * 45.0f)
            )
            path1.lineTo(w * 1.15f, (0.58f * h) + (sin(1.5f + p1) * 40.0f))
            path1.cubicTo(
                w * 0.7f, (0.68f * h) + (sin(3.2f + p1) * 50.0f),
                0.3f * w, (0.44f * h) + (sin(0.8f + p1) * 45.0f),
                (-w) * 0.15f, 0.48f * h
            )
            path1.close()
            drawPath(
                path = path1,
                brush = Brush.linearGradient(
                    colors = listOf(
                        BioluminescentCyan.copy(alpha = 0.4f),
                        DeepElectricViolet.copy(alpha = 0.32f),
                        AuroraTeal.copy(alpha = 0.2f)
                    ),
                    start = Offset((-w) * 0.1f, h * 0.15f),
                    end = Offset(w * 1.1f, h * 0.6f)
                ),
                blendMode = BlendMode.Screen
            )

            val p2 = (0.85f * dynamicPhase) + 1.6f
            path2.moveTo((-w) * 0.2f, (0.38f * h) + (sin(p2) * 40.0f))
            path2.cubicTo(
                w * 0.2f, (sin(1.5f + p2) * 60.0f) + (0.52f * h),
                w * 0.75f, (0.28f * h) + (sin(2.8f + p2) * 70.0f),
                w * 1.2f, (0.48f * h) + (sin(p2 + 4.2f) * 50.0f)
            )
            path2.lineTo(w * 1.2f, (h * 0.75f) + (sin(1.8f + p2) * 45.0f))
            path2.cubicTo(
                w * 0.65f, (h * 0.6f) + (sin(3.0f + p2) * 55.0f),
                w * 0.25f, (0.78f * h) + (sin(0.9f + p2) * 45.0f),
                (-w) * 0.2f, h * 0.65f
            )
            path2.close()
            drawPath(
                path = path2,
                brush = Brush.linearGradient(
                    colors = listOf(
                        DeepElectricViolet.copy(alpha = 0.38f),
                        EmeraldMint.copy(alpha = 0.32f),
                        BioluminescentCyan.copy(alpha = 0.25f)
                    ),
                    start = Offset((-w) * 0.1f, 0.35f * h),
                    end = Offset(w * 1.1f, h * 0.75f)
                ),
                blendMode = BlendMode.Screen
            )

            val p3 = (dynamicPhase * 0.7f) + 3.1f
            path3.moveTo((-w) * 0.1f, (h * 0.6f) + (sin(p3) * 35.0f))
            path3.cubicTo(
                w * 0.35f, (0.78f * h) + (sin(1.4f + p3) * 50.0f),
                w * 0.68f, (0.55f * h) + (sin(2.6f + p3) * 60.0f),
                w * 1.15f, (0.72f * h) + (sin(3.9f + p3) * 40.0f)
            )
            path3.lineTo(w * 1.15f, 0.92f * h)
            path3.cubicTo(
                w * 0.7f, (0.85f * h) + (sin(2.0f + p3) * 40.0f),
                w * 0.3f, (0.94f * h) + (sin(p3 + 0.6f) * 45.0f),
                (-w) * 0.1f, h * 0.82f
            )
            path3.close()
            drawPath(
                path = path3,
                brush = Brush.linearGradient(
                    colors = listOf(
                        EmeraldMint.copy(alpha = 0.32f),
                        BioluminescentCyan.copy(alpha = 0.28f),
                        DeepElectricViolet.copy(alpha = 0.18f)
                    ),
                    start = Offset((-w) * 0.1f, 0.55f * h),
                    end = Offset(w * 1.1f, 0.95f * h)
                ),
                blendMode = BlendMode.Screen
            )

            for (star in distantStars) {
                val twinkle = ((0.5f * sin(twinklePhase + star.phase).coerceIn(0.0f, 1.0f)) + 0.2f).coerceIn(0.15f, 0.75f)
                drawCircle(
                    color = Color.White.copy(alpha = twinkle),
                    radius = star.radius.dp.toPx(),
                    center = Offset(star.x * w, star.y * h)
                )
            }

            if (richMotion) {
                for (bokehOrb in bokehOrbs) {
                    val initialY = (((bokehOrb.initialY - (((orbDrift * bokehOrb.driftSpeed) * 8.0f) * transitionBoost)) % 1.0f) + 1.0f) % 1.0f
                    val rPx = bokehOrb.radius.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                bokehOrb.color.copy(alpha = 0.16f),
                                bokehOrb.color.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = rPx
                        ),
                        radius = rPx,
                        center = Offset(bokehOrb.x * w, h * initialY)
                    )
                }
            }

            drawRect(brush = vignetteBrush)
        }
        content()
    }
}

@Composable
fun AuroraAnimatedBackground(
    content: @Composable () -> Unit
) {
    AuroraLivingBackground(
        transitionBoost = 1.0f,
        content = content
    )
}
