package com.niranjan.auroralivepaper.ui.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.niranjan.auroralivepaper.ui.components.AuroraGradientButton
import java.io.File

@UnstableApi
@Composable
fun PreviewScreen(
    mediaPath: String?,
    mediaType: String, // "video", "gif", or "image"
    imageBitmap: Bitmap? = null,
    onSetHome: () -> Unit,
    onSetLock: () -> Unit,
    onSetBoth: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val loadedBitmap = remember(mediaPath, imageBitmap) {
        imageBitmap ?: if (mediaPath != null && File(mediaPath).exists()) {
            BitmapFactory.decodeFile(mediaPath)
        } else null
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        when (mediaType) {
            "video" -> if (mediaPath != null) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            player = ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(mediaPath))))
                                repeatMode = ExoPlayer.REPEAT_MODE_ALL
                                volume = 0f
                                prepare()
                                play()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            "image", "gif" -> {
                if (mediaType == "image" && loadedBitmap != null) {
                    Image(
                        bitmap = loadedBitmap.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (mediaPath != null) {
                    // Fallback image / GIF display
                    val bmp = remember(mediaPath) { BitmapFactory.decodeFile(mediaPath) }
                    bmp?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Frosted Glass Action Sheet at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF0F1422).copy(alpha = 0.92f))
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (mediaType == "image") "Set Image Wallpaper" else "Set Live Wallpaper",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Preview how it looks on your device.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (mediaType == "video" || mediaType == "gif") {
                    AuroraGradientButton(
                        text = "Apply Wallpaper",
                        onClick = onSetHome
                    )
                } else {
                    Button(
                        onClick = onSetHome,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B4E)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Home Screen", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSetLock,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F4E4A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Lock Screen", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSetBoth,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B2A68)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Both", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }
    }
}
