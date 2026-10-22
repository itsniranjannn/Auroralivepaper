package com.niranjan.auroralivepaper.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily
import kotlin.math.roundToInt

@Composable
fun WallpaperSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var playSound by remember { mutableStateOf(SettingsPrefs.playWallpaperSound(context)) }
    var volume by remember { mutableStateOf(SettingsPrefs.wallpaperVolume(context)) }
    var resolution by remember { mutableStateOf(SettingsPrefs.resolutionMode(context)) }
    var scalingMode by remember { mutableStateOf(SettingsPrefs.scalingMode(context)) }
    var loopAuto by remember { mutableStateOf(SettingsPrefs.loopAutomatically(context)) }

    SettingsAuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(Icons.Filled.VideoLibrary, "Wallpaper", "Control playback, resolution, and audio.", onBack)

            SettingsSection("Playback") {
                CheckRow("Loop automatically", loopAuto) {
                    loopAuto = it
                    performAuroraHaptic(context, haptic)
                    SettingsPrefs.setLoopAutomatically(context, it)
                }
            }

            SettingsSection("Audio & Sound Controls") {
                CheckRow("Play wallpaper sound", playSound) {
                    playSound = it
                    performAuroraHaptic(context, haptic)
                    SettingsPrefs.setPlayWallpaperSound(context, it)
                }

                Text(
                    text = "Sound only plays on the Home Screen when unlocked. Lock screen is automatically kept quiet.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontFamily = ManropeFamily,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )

                if (playSound) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Wallpaper Volume",
                            color = Color.White,
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            "${(volume * 100).roundToInt()}%",
                            color = Color(0xFF7CE7D7),
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            SettingsPrefs.setWallpaperVolume(context, it)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF7CE7D7),
                            activeTrackColor = Color(0xFF7CE7D7),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Note: Wallpaper audio level operates independently from phone ringer/notification volume.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            SettingsSection("Aspect Ratio & Scaling") {
                RadioRow("Fill (Full Screen - Recommended)", scalingMode == "fill") {
                    scalingMode = "fill"
                    performAuroraHaptic(context, haptic)
                    SettingsPrefs.setScalingMode(context, "fill")
                }
                RadioRow("Fit (Preserve entire video with letterbox)", scalingMode == "fit") {
                    scalingMode = "fit"
                    performAuroraHaptic(context, haptic)
                    SettingsPrefs.setScalingMode(context, "fit")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Fill zooms to eliminate black bars. Fit displays the full original video without cropping any edges.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            SettingsSection("Resolution Cap") {
                RadioRow("Auto (Screen size)", resolution == "auto") {
                    resolution = "auto"
                    performAuroraHaptic(context, haptic)
                    SettingsPrefs.setResolutionMode(context, "auto")
                }
                RadioRow("1080p (Max 1920 height)", resolution == "1080p") {
                    resolution = "1080p"
                    performAuroraHaptic(context, haptic)
                    SettingsPrefs.setResolutionMode(context, "1080p")
                }
                RadioRow("1440p (Max 2560 height)", resolution == "1440p") {
                    resolution = "1440p"
                    performAuroraHaptic(context, haptic)
                    SettingsPrefs.setResolutionMode(context, "1440p")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Limits the render surface size to reduce GPU load. It doesn't change the video file itself.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Launcher Tips", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Some launchers support wallpaper scrolling. Enable scrolling in your launcher for a parallax effect.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontFamily = ManropeFamily,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
