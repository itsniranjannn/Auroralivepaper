package com.niranjan.auroralivepaper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    SettingsAuroraBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            SettingsHeader(Icons.Filled.Info, "About", "Version, privacy and credits.", onBack)

            SettingsSection("Version") {
                Text("Aurora Live Paper 1.0.0", color = Color.White, fontSize = 14.sp)
            }
            SettingsSection("Privacy") {
                Text(
                    "Aurora never sends your media anywhere. Everything you choose stays on your device.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            SettingsSection("Open Source") {
                listOf(
                    "Media3 (ExoPlayer)",
                    "Jetpack Compose",
                    "AndroidX Navigation",
                    "Manrope typeface (SIL OFL)"
                ).forEach {
                    Text("• $it", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF93B5FF).copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Designed & Developed by Niranjan.K",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
