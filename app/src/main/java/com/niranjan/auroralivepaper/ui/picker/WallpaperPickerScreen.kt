package com.niranjan.auroralivepaper.ui.picker

import android.R
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.niranjan.auroralivepaper.AuroraWallpaperService
import com.niranjan.auroralivepaper.ui.components.AuroraAnimatedBackground
import com.niranjan.auroralivepaper.ui.components.AuroraGradientButton
import com.niranjan.auroralivepaper.ui.components.AuroraGradientText
import com.niranjan.auroralivepaper.ui.components.PremiumMediaCard
import com.niranjan.auroralivepaper.ui.theme.AuroraFrostedBlue
import com.niranjan.auroralivepaper.ui.theme.AuroraRoyalViolet
import com.niranjan.auroralivepaper.ui.theme.AuroraStormyTeal
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.util.Calendar

@Composable
@UnstableApi
fun WallpaperPickerScreen(
    onPreview: (path: String, type: String) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }

    var showCard1 by remember { mutableStateOf(false) }
    var showCard2 by remember { mutableStateOf(false) }
    var showCard3 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100); showCard1 = true
        delay(80); showCard2 = true
        delay(80); showCard3 = true
    }

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

    AuroraAnimatedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val greeting = remember {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        when {
                            hour < 12 -> "Good Morning"
                            hour < 18 -> "Good Afternoon"
                            else -> "Good Evening"
                        }
                    }
                    Text(greeting, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    AuroraGradientText("Aurora", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Text(
                "Every unlock, a little more yours.",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "Choose Wallpaper",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(
                    visible = showCard1,
                    modifier = Modifier.weight(1f),
                    enter = fadeIn(tween(400)) +
                            slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(400)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(400))
                ) {
                    PremiumMediaCard(
                        label = "Video",
                        subtitle = "MP4 · MOV · WEBM",
                        icon = Icons.Filled.MovieFilter,
                        accent = AuroraRoyalViolet,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { pickVideoLauncher.launch(arrayOf("video/*")) }
                    )
                }
                AnimatedVisibility(
                    visible = showCard2,
                    modifier = Modifier.weight(1f),
                    enter = fadeIn(tween(400)) +
                            slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(400)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(400))
                ) {
                    PremiumMediaCard(
                        label = "GIF",
                        subtitle = "Animated",
                        icon = Icons.Filled.MovieFilter,
                        accent = AuroraStormyTeal,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { pickGifLauncher.launch(arrayOf("image/gif")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = showCard3,
                enter = fadeIn(tween(400)) +
                        slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(400)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(400))
            ) {
                PremiumMediaCard(
                    label = "Image",
                    subtitle = "JPG · PNG · WEBP",
                    icon = Icons.Filled.Add,
                    accent = AuroraFrostedBlue,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { pickImageLauncher.launch(arrayOf("image/*")) }
                )
            }

            AnimatedVisibility(
                visible = selectedBitmap != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedBitmap?.let { bmp ->
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected wallpaper preview",
                            modifier = Modifier
                                .height(220.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AuroraGradientButton(
                            text = "Set as Wallpaper",
                            onClick = {
                                try {
                                    WallpaperManager.getInstance(context).setBitmap(bmp)
                                    status = "Wallpaper applied!"
                                } catch (e: Exception) {
                                    status = "Failed: ${e.message}"
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = status.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(status, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.width(140.dp).height(2.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Built by Niranjan.K",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@UnstableApi
fun launchLiveWallpaperPicker(context: Context) {
    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
    intent.putExtra(
        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
        ComponentName(context, AuroraWallpaperService::class.java)
    )
    context.startActivity(intent)
    if (context is Activity) {
        context.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}

fun copyMediaToAppStorage(context: Context, uri: Uri, fileName: String): String {
    val inputStream = context.contentResolver.openInputStream(uri)
    val outputFile = File(context.filesDir, fileName)
    inputStream?.use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return outputFile.absolutePath
}

fun saveWallpaperMediaPath(context: Context, path: String, type: String) {
    val prefs = context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("media_path", path)
        .putString("media_type", type)
        .apply()
}

fun applyLockScreenFromVideo(context: Context, videoPath: String, seconds: Float): Boolean {
    return try {
        val retriever = MediaMetadataRetriever()
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

fun applyLockScreenFromGif(context: Context, gifPath: String): Boolean {
    return try {
        val movie = Movie.decodeStream(FileInputStream(gifPath))
        if (movie != null) {
            val bitmap = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
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
