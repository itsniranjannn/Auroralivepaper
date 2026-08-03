package com.niranjan.auroralivepaper.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun StorageSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var videoSize by remember { mutableStateOf(0L) }
    var gifSize by remember { mutableStateOf(0L) }
    var imageSize by remember { mutableStateOf(0L) }
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val dir = context.filesDir
        videoSize = File(dir, "current_wallpaper_video.mp4").let { if (it.exists()) it.length() else 0L }
        gifSize = File(dir, "current_wallpaper_gif.gif").let { if (it.exists()) it.length() else 0L }
        imageSize = 0L
        animateIn = true
    }

    val totalBytes = videoSize + gifSize + imageSize
    val totalMb = totalBytes / (1024f * 1024f)
    val progress by animateFloatAsState(
        targetValue = if (animateIn) (totalMb / 500f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(800),
        label = "storageProgress"
    )

    SettingsAuroraBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            SettingsHeader(Icons.Filled.Storage, "Storage", "Manage your saved wallpapers.", onBack)

            SettingsSection("Storage") {
                Text("%.1f MB Used".format(totalMb), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF7CE7D7))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                StorageRow("Videos", videoSize)
                StorageRow("GIF", gifSize)
                StorageRow("Images", imageSize)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable {
                        File(context.filesDir, "current_wallpaper_video.mp4").delete()
                        File(context.filesDir, "current_wallpaper_gif.gif").delete()
                        videoSize = 0L
                        gifSize = 0L
                    }
                    .padding(16.dp)
            ) {
                Text("🗑 Clear Cache", color = Color(0xFFFF7675), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StorageRow(label: String, bytes: Long) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("%.1f MB".format(bytes / (1024f * 1024f)), color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
    }
}