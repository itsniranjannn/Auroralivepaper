package com.niranjan.auroralivepaper.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.niranjan.auroralivepaper.data.AuroraDebugLogger
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SettingsAuroraBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            SettingsHeader(Icons.Filled.Info, "About", "Version, privacy and credits.", onBack)

            SettingsSection("Version") {
                Text("Aurora Live Paper 1.0.0", color = Color.White, fontSize = 14.sp)
            }

            SettingsSection("Diagnostics") {
                Text(
                    "Export runtime engine events, recovery attempts, and power state changes for debugging.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontFamily = ManropeFamily
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        try {
                            val logFile = AuroraDebugLogger.getLogFile(context)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                logFile
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Aurora Debug Log"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to export log: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Export Debug Log",
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
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
