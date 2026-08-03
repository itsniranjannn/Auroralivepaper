package com.niranjan.auroralivepaper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.data.SettingsPrefs
import androidx.compose.ui.unit.dp
@Composable
fun ExperienceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var haptics by remember { mutableStateOf(SettingsPrefs.hapticsEnabled(context)) }
    var richMotion by remember { mutableStateOf(SettingsPrefs.richMotion(context)) }

    SettingsAuroraBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            SettingsHeader(Icons.Filled.Palette, "Experience", "Haptics, motion and appearance.", onBack)

            SettingsSection("Feedback") {
                CheckRow("Haptic Feedback", haptics) {
                    haptics = it; SettingsPrefs.setHapticsEnabled(context, it)
                }
            }
            SettingsSection("Motion") {
                CheckRow("Rich Motion", richMotion) {
                    richMotion = it; SettingsPrefs.setRichMotion(context, it)
                }
            }
            SettingsSection("Appearance") {
                Text("Aurora Dark", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Light Mode — Coming Soon", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }
    }
}