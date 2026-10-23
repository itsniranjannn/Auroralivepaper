package com.niranjan.auroralivepaper.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily

@Composable
fun PremiumMediaCard(
    label: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 380.0f),
        label = "cardPressScale"
    )

    val specularBorderBrush = remember(accent) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                accent.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.04f)
            ),
            start = Offset(0.0f, 0.0f),
            end = Offset(300.0f, 300.0f)
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f),
                        Color(0xFF0F1520).copy(alpha = 0.88f)
                    )
                )
            )
            .border(1.dp, specularBorderBrush, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                if (SettingsPrefs.hapticsEnabled(context)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset((-8).dp, (-8).dp)
                .size(72.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    radius = size.width / 1.5f
                ),
                radius = size.width / 1.5f
            )
        }

        Column {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.22f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = accent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFamily
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontFamily = ManropeFamily
            )
        }
    }
}
