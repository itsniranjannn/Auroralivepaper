package com.niranjan.auroralivepaper.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.data.SettingsPrefs
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun BatterySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var pauseOnSaver by remember { mutableStateOf(SettingsPrefs.pauseOnBatterySaver(context)) }
    var pauseThreshold by remember { mutableStateOf(SettingsPrefs.pauseBelowThreshold(context)) }
    var threshold by remember { mutableStateOf(SettingsPrefs.batteryThreshold(context).toFloat()) }

    SettingsAuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(Icons.Filled.BatteryChargingFull, "Battery & Performance", "Manage power usage.", onBack)

            SettingsSection("Battery") {
                CheckRow("Pause wallpaper when Battery Saver is on", pauseOnSaver) {
                    pauseOnSaver = it; SettingsPrefs.setPauseOnBatterySaver(context, it)
                }
                Spacer(modifier = Modifier.height(8.dp))
                CheckRow("Pause below battery threshold", pauseThreshold) {
                    pauseThreshold = it; SettingsPrefs.setPauseBelowThreshold(context, it)
                }
                if (pauseThreshold) {
                    Text("${threshold.toInt()}%", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it; SettingsPrefs.setBatteryThreshold(context, it.toInt()) },
                        valueRange = 5f..50f
                    )
                }
            }

            // Performance Mode removed — was not wired to any real behavior yet.
            // Re-add once actual quality/battery tiers are implemented in the wallpaper engine.

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}