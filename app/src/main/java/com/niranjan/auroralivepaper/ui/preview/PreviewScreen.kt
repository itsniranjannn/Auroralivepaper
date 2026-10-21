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
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily
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
    var showGuidanceDialog by remember { mutableStateOf(false) }
    var guidanceMessage by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                val bmp = loadedBitmap ?: remember(mediaPath) {
                    if (mediaPath != null) BitmapFactory.decodeFile(mediaPath) else null
                }
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Frosted Glass Action Sheet at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF0D121F).copy(alpha = 0.95f))
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (mediaType == "image") "Set Image Wallpaper" else "Set Live Wallpaper",
                    color = Color.White,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Full resolution preview",
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (mediaType == "video" || mediaType == "gif") {
                            guidanceMessage = "Choose 'Home screen' on the next screen."
                            pendingAction = onSetHome
                            showGuidanceDialog = true
                        } else {
                            onSetHome()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00B894),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Set as Home Screen", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (mediaType == "video" || mediaType == "gif") {
                            guidanceMessage = "Choose 'Home screen and lock screen' on the next screen."
                            pendingAction = onSetLock
                            showGuidanceDialog = true
                        } else {
                            onSetLock()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C5CE7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Set as Lock Screen", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (mediaType == "video" || mediaType == "gif") {
                            guidanceMessage = "Choose 'Home screen and lock screen' on the next screen."
                            pendingAction = onSetBoth
                            showGuidanceDialog = true
                        } else {
                            onSetBoth()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0984E3),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Set as Both", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.75f), fontFamily = ManropeFamily, fontSize = 14.sp)
                }
            }
        }

        if (showGuidanceDialog) {
            AlertDialog(
                onDismissRequest = { showGuidanceDialog = false },
                title = {
                    Text("Setting Live Wallpaper", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        guidanceMessage,
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = ManropeFamily,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showGuidanceDialog = false
                            pendingAction?.invoke()
                            pendingAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                    ) {
                        Text("Continue to Set Wallpaper", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF141A29),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
