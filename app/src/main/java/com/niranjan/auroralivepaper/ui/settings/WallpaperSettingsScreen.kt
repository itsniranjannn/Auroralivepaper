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
    var rememberAudio by remember { mutableStateOf(SettingsPrefs.rememberAudioChoice(context)) }
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

            SettingsSection("Resolution") {
                RadioRow("Auto (Screen resolution)", resolution == "auto") {
                    resolution = "auto"
                    SettingsPrefs.setResolutionMode(context, "auto")
                }
                RadioRow("1080p (Cap max height)", resolution == "1080p") {
                    resolution = "1080p"
                    SettingsPrefs.setResolutionMode(context, "1080p")
                }
                RadioRow("1440p (Cap max height)", resolution == "1440p") {
                    resolution = "1440p"
                    SettingsPrefs.setResolutionMode(context, "1440p")
                }
                RadioRow("Original", resolution == "original") {
                    resolution = "original"
                    SettingsPrefs.setResolutionMode(context, "original")
                }
            }

            SettingsSection("Audio") {
                RadioRow("Muted by default", !rememberAudio) {
                    rememberAudio = false
                    SettingsPrefs.setRememberAudioChoice(context, false)
                }
                RadioRow("Enable audio output", rememberAudio) {
                    rememberAudio = true
                    SettingsPrefs.setRememberAudioChoice(context, true)
                }
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
