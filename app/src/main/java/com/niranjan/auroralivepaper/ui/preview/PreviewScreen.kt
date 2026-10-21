package com.niranjan.auroralivepaper.ui.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.niranjan.auroralivepaper.data.SettingsPrefs
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

    var isSoundOn by remember { mutableStateOf(SettingsPrefs.playWallpaperSound(context)) }
    var volumeVal by remember { mutableStateOf(SettingsPrefs.wallpaperVolume(context)) }
    var exoPlayerInstance by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayerInstance?.release()
            exoPlayerInstance = null
        }
    }

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
                            val player = ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(mediaPath))))
                                repeatMode = ExoPlayer.REPEAT_MODE_ALL
                                volume = if (isSoundOn) volumeVal else 0f
                                prepare()
                                play()
                            }
                            this.player = player
                            exoPlayerInstance = player
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

        // Floating audio button in top right for videos
        if (mediaType == "video") {
            IconButton(
                onClick = {
                    isSoundOn = !isSoundOn
                    SettingsPrefs.setPlayWallpaperSound(context, isSoundOn)
                    exoPlayerInstance?.volume = if (isSoundOn) volumeVal else 0f
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D121F).copy(alpha = 0.85f))
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle sound",
                    tint = if (isSoundOn) Color(0xFF7CE7D7) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
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

                Spacer(modifier = Modifier.height(16.dp))

                if (mediaType == "video") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null,
                            tint = if (isSoundOn) Color(0xFF7CE7D7) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSoundOn) "Audio enabled (${(volumeVal * 100).toInt()}%)" else "Audio muted",
                                color = Color.White,
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Home screen only • Lock screen stays silent",
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = ManropeFamily,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isSoundOn,
                            onCheckedChange = { checked ->
                                isSoundOn = checked
                                SettingsPrefs.setPlayWallpaperSound(context, checked)
                                exoPlayerInstance?.volume = if (checked) volumeVal else 0f
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00B894),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }

                    if (isSoundOn) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                        ) {
                            Text("Volume", color = Color.White.copy(alpha = 0.5f), fontFamily = ManropeFamily, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = volumeVal,
                                onValueChange = { newVol ->
                                    volumeVal = newVol
                                    SettingsPrefs.setWallpaperVolume(context, newVol)
                                    exoPlayerInstance?.volume = newVol
                                },
                                valueRange = 0.05f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF7CE7D7),
                                    activeTrackColor = Color(0xFF7CE7D7),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

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
                    onClick = onSetLock,
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
