package com.niranjan.auroralivepaper.ui.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
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
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily
import java.io.File

@UnstableApi
@Composable
fun PreviewScreen(
    mediaPath: String?,
    mediaType: String, // "video", "gif", or "image"
    imageBitmap: Bitmap? = null,
    onSetHome: (scalingMode: String) -> Unit,
    onSetLock: (scalingMode: String) -> Unit,
    onSetBoth: (scalingMode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showGuidanceDialog by remember { mutableStateOf(false) }
    var guidanceMessage by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<((String) -> Unit)?>(null) }

    var scalingMode by remember { mutableStateOf(SettingsPrefs.scalingMode(context)) }
    var isSoundOn by remember { mutableStateOf(SettingsPrefs.playWallpaperSound(context)) }
    var volumeVal by remember { mutableStateOf(SettingsPrefs.wallpaperVolume(context)) }
    var exoPlayerInstance by remember { mutableStateOf<ExoPlayer?>(null) }
    var isUiVisible by remember { mutableStateOf(true) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isUiVisible = !isUiVisible
            }
    ) {

        // ====================================================================
        // Background Wallpaper Viewport (100% full screen)
        // ====================================================================
        when (mediaType) {
            "video" -> if (mediaPath != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false
                            resizeMode = if (scalingMode == "fit") {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                            val player = ExoPlayer.Builder(ctx).build().apply {
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
                    update = { view ->
                        view.resizeMode = if (scalingMode == "fit") {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
                        contentScale = if (scalingMode == "fit") ContentScale.Fit else ContentScale.Crop
                    )
                }
            }
        }

        // ====================================================================
        // Floating Top Bar: Back Button (Left) & Aspect / Audio Controls (Right)
        // ====================================================================
        AnimatedVisibility(
            visible = isUiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0D121F).copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Right Controls: Fit/Fill Toggle & Audio Toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Aspect Ratio Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0D121F).copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .clickable {
                                val newMode = if (scalingMode == "fill") "fit" else "fill"
                                scalingMode = newMode
                                SettingsPrefs.setScalingMode(context, newMode)
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (scalingMode == "fit") Icons.Filled.FitScreen else Icons.Filled.CropFree,
                                contentDescription = "Scale Mode",
                                tint = Color(0xFF7CE7D7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (scalingMode == "fit") "Fit (Letterbox)" else "Fill (Screen)",
                                color = Color.White,
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Video Sound Toggle
                    if (mediaType == "video") {
                        IconButton(
                            onClick = {
                                isSoundOn = !isSoundOn
                                SettingsPrefs.setPlayWallpaperSound(context, isSoundOn)
                                exoPlayerInstance?.volume = if (isSoundOn) volumeVal else 0f
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF0D121F).copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Toggle sound",
                                tint = if (isSoundOn) Color(0xFF7CE7D7) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // Compact Floating Bottom Action Dock (Takes minimal screen space)
        // ====================================================================
        AnimatedVisibility(
            visible = isUiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0D121F).copy(alpha = 0.88f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Optional Volume Slider if sound is enabled
                        if (mediaType == "video" && isSoundOn) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = Color(0xFF7CE7D7),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Slider(
                                    value = volumeVal,
                                    onValueChange = { newVol ->
                                        volumeVal = newVol
                                        SettingsPrefs.setWallpaperVolume(context, newVol)
                                        exoPlayerInstance?.volume = newVol
                                    },
                                    valueRange = 0.05f..1f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF7CE7D7),
                                        activeTrackColor = Color(0xFF7CE7D7),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${(volumeVal * 100).toInt()}%",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = ManropeFamily,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Compact Horizontal 3-Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Home Screen Button
                            Button(
                                onClick = {
                                    if (mediaType == "video" || mediaType == "gif") {
                                        guidanceMessage = "Choose 'Home screen' on the next screen."
                                        pendingAction = onSetHome
                                        showGuidanceDialog = true
                                    } else {
                                        onSetHome(scalingMode)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00B894).copy(alpha = 0.90f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Home,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Home",
                                    fontFamily = ManropeFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // 2. Lock Screen Button
                            Button(
                                onClick = { onSetLock(scalingMode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6C5CE7).copy(alpha = 0.90f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lock",
                                    fontFamily = ManropeFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // 3. Both Screens Button
                            Button(
                                onClick = {
                                    if (mediaType == "video" || mediaType == "gif") {
                                        guidanceMessage = "Choose 'Home screen and lock screen' on the next screen."
                                        pendingAction = onSetBoth
                                        showGuidanceDialog = true
                                    } else {
                                        onSetBoth(scalingMode)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0984E3).copy(alpha = 0.90f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Wallpaper,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Both",
                                    fontFamily = ManropeFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // System Guidance Dialog (For Wallpaper Service Handoff)
        // ====================================================================
        if (showGuidanceDialog) {
            AlertDialog(
                onDismissRequest = { showGuidanceDialog = false },
                title = {
                    Text(
                        "Setting Live Wallpaper",
                        color = Color.White,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.Bold
                    )
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
                            pendingAction?.invoke(scalingMode)
                            pendingAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                    ) {
                        Text(
                            "Continue to Set Wallpaper",
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                containerColor = Color(0xFF141A29),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
