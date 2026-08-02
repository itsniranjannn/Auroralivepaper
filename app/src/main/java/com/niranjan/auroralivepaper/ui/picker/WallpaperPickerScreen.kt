package com.niranjan.auroralivepaper.ui.picker

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.niranjan.auroralivepaper.AuroraWallpaperService
import java.io.File
import androidx.media3.common.util.UnstableApi
@Composable
@UnstableApi
fun WallpaperPickerScreen(onPreview: (path: String, type: String) -> Unit) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    var lockFrameSeconds by remember { mutableStateOf(1f) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            selectedBitmap = bitmap
            status = "Image loaded — preview below"
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = copyMediaToAppStorage(context, it, "current_wallpaper_video.mp4")
            saveWallpaperMediaPath(context, path, "video")
            onPreview(path, "video")
        }
    }

    val pickGifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = copyMediaToAppStorage(context, it, "current_wallpaper_gif.gif")
            saveWallpaperMediaPath(context, path, "gif")
            onPreview(path, "gif")
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) {
            Text("Choose Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Selected wallpaper preview",
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                try {
                    WallpaperManager.getInstance(context).setBitmap(bmp)
                    status = "Wallpaper applied!"
                } catch (e: Exception) {
                    status = "Failed: ${e.message}"
                }
            }) {
                Text("Set as Wallpaper")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { pickVideoLauncher.launch(arrayOf("video/*")) }) {
            Text("Choose Video for Live Wallpaper")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { pickGifLauncher.launch(arrayOf("image/gif")) }) {
            Text("Choose GIF for Live Wallpaper")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launchLiveWallpaperPicker(context) }) {
            Text("Set Aurora Live Wallpaper")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(status)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val prefs = context.getSharedPreferences("aurora_prefs", android.content.Context.MODE_PRIVATE)
            val path = prefs.getString("media_path", null)
            val type = prefs.getString("media_type", "video")

            if (path == null) {
                status = "Pick a video or GIF first"
            } else {
                val success = if (type == "gif") {
                    applyLockScreenFromGif(context, path)
                } else {
                    applyLockScreenFromVideo(context, path, lockFrameSeconds)
                }
                status = if (success) "Lock screen updated!" else "Lock screen failed"
            }
        }) {
            Text("Set as Lock Screen")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Lock screen frame: ${lockFrameSeconds.toInt()}s into video")
        Slider(
            value = lockFrameSeconds,
            onValueChange = { lockFrameSeconds = it },
            valueRange = 0f..30f,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}
@UnstableApi
fun launchLiveWallpaperPicker(context: android.content.Context) {
    val intent = android.content.Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
    intent.putExtra(
        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
        android.content.ComponentName(context, AuroraWallpaperService::class.java)
    )
    context.startActivity(intent)
}

fun copyMediaToAppStorage(context: android.content.Context, uri: Uri, fileName: String): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val outputFile = File(context.filesDir, fileName)
    inputStream?.use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return outputFile.absolutePath
}

fun saveWallpaperMediaPath(context: android.content.Context, path: String, type: String) {
    val prefs = context.getSharedPreferences("aurora_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit()
        .putString("media_path", path)
        .putString("media_type", type)
        .apply()
}

fun applyLockScreenFromVideo(context: android.content.Context, videoPath: String, seconds: Float): Boolean {
    return try {
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        val micros = (seconds * 1_000_000).toLong()
        val frame = retriever.getFrameAtTime(micros)
        retriever.release()
        if (frame != null) {
            WallpaperManager.getInstance(context)
                .setBitmap(frame, null, true, WallpaperManager.FLAG_LOCK)
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

fun applyLockScreenFromGif(context: android.content.Context, gifPath: String): Boolean {
    return try {
        val movie = android.graphics.Movie.decodeStream(java.io.FileInputStream(gifPath))
        if (movie != null) {
            val bitmap = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            movie.setTime(0)
            movie.draw(canvas, 0f, 0f)
            WallpaperManager.getInstance(context)
                .setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            true
        } else false
    } catch (e: Exception) {
        false
    }
}