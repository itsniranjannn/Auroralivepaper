package com.niranjan.auroralivepaper.ui.preview

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.layout.navigationBarsPadding

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

    Box(modifier = Modifier.fillMaxSize()) {

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
                                setMediaItem(MediaItem.fromUri(Uri.fromFile(java.io.File(mediaPath))))
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
            "image" -> imageBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            if (mediaType == "video" || mediaType == "gif") {
                // Live wallpapers require the system's own Home/Lock/Both dialog —
                // so we offer just one clear action here.
                com.niranjan.auroralivepaper.ui.components.AuroraGradientButton(
                    text = "Apply Wallpaper",
                    onClick = onSetHome // reuse the existing home-apply path, which triggers the system picker
                )
            } else {
                Button(onClick = onSetHome, modifier = Modifier.fillMaxWidth()) {
                    Text("Set as Home Screen")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onSetLock, modifier = Modifier.fillMaxWidth()) {
                    Text("Set as Lock Screen")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onSetBoth, modifier = Modifier.fillMaxWidth()) {
                    Text("Set as Both")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}