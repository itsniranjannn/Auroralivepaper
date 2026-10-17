package com.niranjan.auroralivepaper.ui.settings

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.data.SettingsPrefs
import kotlin.random.Random

private val NearBlack = Color(0xFF07090F)
private val DeepPurple = Color(0xFF2D1B4E)
private val SoftTeal = Color(0xFF1F4E4A)

fun performAuroraHaptic(
    context: Context,
    haptic: HapticFeedback,
    type: HapticFeedbackType = HapticFeedbackType.TextHandleMove
) {
    if (SettingsPrefs.hapticsEnabled(context)) {
        haptic.performHapticFeedback(type)
    }
}

@Composable
fun SettingsAuroraBackground(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val richMotion = remember(context) { SettingsPrefs.richMotion(context) }

    val infinite = rememberInfiniteTransition(label = "settingsFlow")
    val animatedDrift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(24000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "drift"
    )
    val drift = if (richMotion) animatedDrift else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().background(NearBlack)) {
            drawCircle(
                brush = Brush.radialGradient(colors = listOf(DeepPurple.copy(alpha = 0.22f), Color.Transparent)),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * (0.2f + drift * 0.1f), size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(colors = listOf(SoftTeal.copy(alpha = 0.20f), Color.Transparent)),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * (0.8f - drift * 0.1f), size.height * 0.85f)
            )
            val seed = Random(11)
            repeat(70) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.015f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(seed.nextFloat() * size.width, seed.nextFloat() * size.height)
                )
            }
            repeat(2) { i ->
                val py = (i * 0.5f + drift * 0.03f) % 1f
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(size.width * (0.3f + i * 0.4f), size.height * py)
                )
            }
        }
        content()
    }
}

/** Category card with icon, gradient background, press-scale animation, and haptic tap feedback. */
@Composable
fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "cardPress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DeepPurple.copy(alpha = 0.10f),
                        Color.Transparent,
                        SoftTeal.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null) {
                performAuroraHaptic(context, haptic)
                onClick()
            }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
        }
    }
}

/** Header with a real back-arrow icon button + title + subtitle. */
@Composable
fun SettingsHeader(icon: ImageVector, title: String, subtitle: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        IconButton(onClick = {
            performAuroraHaptic(context, haptic)
            onBack()
        }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White.copy(alpha = 0.85f)
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    }
    Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DeepPurple.copy(alpha = 0.06f), Color.Transparent, SoftTeal.copy(alpha = 0.05f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable {
            performAuroraHaptic(context, haptic)
            onClick()
        }.padding(vertical = 8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun CheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = {
            performAuroraHaptic(context, haptic)
            onCheckedChange(it)
        })
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}
