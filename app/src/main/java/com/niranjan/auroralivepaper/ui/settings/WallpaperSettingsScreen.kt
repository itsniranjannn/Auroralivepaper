package com.niranjan.auroralivepaper.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.data.SettingsPrefs

@Composable
fun WallpaperSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var playSound by remember { mutableStateOf(SettingsPrefs.playWallpaperSound(context)) }
    var resolution by remember { mutableStateOf(SettingsPrefs.resolutionMode(context)) }
    var loopAuto by remember { mutableStateOf(SettingsPrefs.loopAutomatically(context)) }

    SettingsAuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(Icons.Filled.VideoLibrary, "Wallpaper", "Control how wallpapers behave.", onBack)

            SettingsSection("Playback") {
                CheckRow("Loop automatically", loopAuto) {
                    loopAuto = it
                    SettingsPrefs.setLoopAutomatically(context, it)
                }
            }

            SettingsSection("Resolution Cap") {
                RadioRow("Auto (Screen size)", resolution == "auto") {
                    resolution = "auto"
                    SettingsPrefs.setResolutionMode(context, "auto")
                }
                RadioRow("1080p (Max 1920 height)", resolution == "1080p") {
                    resolution = "1080p"
                    SettingsPrefs.setResolutionMode(context, "1080p")
                }
                RadioRow("1440p (Max 2560 height)", resolution == "1440p") {
                    resolution = "1440p"
                    SettingsPrefs.setResolutionMode(context, "1440p")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Limits the render surface size to reduce GPU load. It doesn't change the video file itself.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            SettingsSection("Audio") {
                CheckRow("Play wallpaper sound", playSound) {
                    playSound = it
                    SettingsPrefs.setPlayWallpaperSound(context, it)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Note: Wallpaper audio plays alongside background media without requesting audio focus.",
                    color = Color.White.copy(alpha = 0.5f),
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
                    Text("Launcher Tips", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Some launchers support wallpaper scrolling. Enable scrolling in your launcher for a parallax effect.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
