package com.niranjan.auroralivepaper.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var show0 by remember { mutableStateOf(false) }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }
    var show3 by remember { mutableStateOf(false) }
    var show4 by remember { mutableStateOf(false) }
    var show5 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60); show0 = true
        delay(60); show1 = true
        delay(60); show2 = true
        delay(60); show3 = true
        delay(60); show4 = true
        delay(60); show5 = true
    }

    val prefs = context.getSharedPreferences("aurora_prefs", android.content.Context.MODE_PRIVATE)
    val mediaType = prefs.getString("media_type", null)
    val mediaPath = prefs.getString("media_path", null)

    SettingsAuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero header
            Spacer(modifier = Modifier.height(20.dp))
            Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            Text(
                "Personalize your experience.",
                color = com.niranjan.auroralivepaper.ui.theme.AuroraTokens.TextTertiary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(48.dp).height(2.dp).background(Color.White.copy(alpha = 0.15f)))

            Spacer(modifier = Modifier.height(24.dp))

            // Richer Current Wallpaper summary card
            fadeSlide(show0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2D1B4E).copy(alpha = 0.18f), Color.Transparent, Color(0xFF1F4E4A).copy(alpha = 0.14f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text("Current Wallpaper", color = com.niranjan.auroralivepaper.ui.theme.AuroraTokens.TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        if (mediaPath == null) {
                            Text("No wallpaper set yet", color = Color.White, fontWeight = FontWeight.Medium)
                        } else {
                            val (icon, label) = when (mediaType) {
                                "video" -> Icons.Filled.Videocam to "Video"
                                "gif" -> Icons.Filled.Gif to "GIF"
                                else -> Icons.Filled.Image to "Image"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                val isActive = remember { com.niranjan.auroralivepaper.ui.picker.isAuroraWallpaperActive(context) }
                                val badgeColor = if (isActive) com.niranjan.auroralivepaper.ui.theme.AuroraTokens.Mint else com.niranjan.auroralivepaper.ui.theme.AuroraTokens.Warning
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        if (isActive) "Applied" else "Saved, not applied",
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val soundOn = com.niranjan.auroralivepaper.data.SettingsPrefs.playWallpaperSound(context)
                            val scaling = com.niranjan.auroralivepaper.data.SettingsPrefs.scalingMode(context)
                            val modified = remember(mediaPath) {
                                val f = java.io.File(mediaPath)
                                if (f.exists()) java.text.DateFormat.getDateTimeInstance(
                                    java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT
                                ).format(java.util.Date(f.lastModified())) else null
                            }
                            InfoLine(if (soundOn) "Sound on (home screen only)" else "Muted")
                            InfoLine(if (scaling == "fit") "Scaling: Fit" else "Scaling: Fill")
                            if (modified != null) InfoLine("Updated $modified")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            fadeSlide(show1) {
                SettingsCategoryCard(Icons.Filled.VideoLibrary, "Wallpaper", "Playback, scaling and audio") { onNavigate("settings_wallpaper") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            fadeSlide(show2) {
                SettingsCategoryCard(Icons.Filled.BatteryChargingFull, "Battery & Performance", "Manage power usage") { onNavigate("settings_battery") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            fadeSlide(show3) {
                SettingsCategoryCard(Icons.Filled.Storage, "Storage", "Manage saved wallpapers") { onNavigate("settings_storage") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            fadeSlide(show4) {
                SettingsCategoryCard(Icons.Filled.Palette, "Experience", "Haptics, motion and appearance") { onNavigate("settings_experience") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            fadeSlide(show5) {
                SettingsCategoryCard(Icons.Filled.Info, "About", "Version, privacy and credits") { onNavigate("settings_about") }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Text(text, color = com.niranjan.auroralivepaper.ui.theme.AuroraTokens.TextTertiary, fontSize = 12.sp)
}

@Composable
private fun fadeSlide(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { 24 }, animationSpec = tween(350))
    ) {
        content()
    }
}