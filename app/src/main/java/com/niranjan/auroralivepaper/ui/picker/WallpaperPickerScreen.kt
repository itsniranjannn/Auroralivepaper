package com.niranjan.auroralivepaper.ui.picker

import android.R
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.niranjan.auroralivepaper.AuroraWallpaperService
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.ui.components.AuroraAnimatedBackground
import com.niranjan.auroralivepaper.ui.components.AuroraGradientText
import com.niranjan.auroralivepaper.ui.components.PremiumMediaCard
import com.niranjan.auroralivepaper.ui.settings.performAuroraHaptic
import com.niranjan.auroralivepaper.ui.theme.AuroraFrostedBlue
import com.niranjan.auroralivepaper.ui.theme.AuroraRoyalViolet
import com.niranjan.auroralivepaper.ui.theme.AuroraStormyTeal
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily
import com.niranjan.auroralivepaper.util.WebVideoExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class RecentWallpaper(val path: String, val type: String, val timestamp: Long)

@OptIn(ExperimentalFoundationApi::class)
@Composable
@UnstableApi
fun WallpaperPickerScreen(
    onPreview: (path: String, type: String) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("") }

    var showHeader by remember { mutableStateOf(false) }
    var showHero by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    var isDragHovered by remember { mutableStateOf(false) }
    var recentsList by remember { mutableStateOf(getRecentWallpapers(context)) }

    val prefs = remember { context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE) }
    var activePath by remember { mutableStateOf(prefs.getString("media_path", null)) }
    var activeType by remember { mutableStateOf(prefs.getString("media_type", "video")) }

    LaunchedEffect(Unit) {
        delay(40); showHeader = true
        delay(60); showHero = true
        delay(80); showGrid = true
        delay(80); showRecents = true
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stagingPath = copyMediaToAppStorage(context, it, "staging_wallpaper_image.jpg")
            onPreview(stagingPath, "image")
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stagingPath = copyMediaToAppStorage(context, it, "staging_wallpaper_video.mp4")
            onPreview(stagingPath, "video")
        }
    }

    val pickGifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stagingPath = copyMediaToAppStorage(context, it, "staging_wallpaper_gif.gif")
            onPreview(stagingPath, "gif")
        }
    }

    val dndTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isDragHovered = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragHovered = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragHovered = false
                try {
                    val androidEvent = event.toAndroidDragEvent()
                    val clipData = androidEvent.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        val item = clipData.getItemAt(0)
                        val uri = item.uri
                        if (uri != null) {
                            val mime = context.contentResolver.getType(uri) ?: ""
                            val type = when {
                                mime.contains("gif", ignoreCase = true) -> "gif"
                                mime.contains("image", ignoreCase = true) -> "image"
                                else -> "video"
                            }
                            val ext = if (type == "gif") "gif" else if (type == "image") "jpg" else "mp4"
                            val stagingPath = copyMediaToAppStorage(context, uri, "staging_drag_drop.$ext")
                            onPreview(stagingPath, type)
                            return true
                        }
                    }
                } catch (e: Exception) {
                    Log.w("Aurora", "Error processing drag and drop event", e)
                }
                return false
            }
        }
    }

    AuroraAnimatedBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = dndTarget
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header Row
                AnimatedVisibility(
                    visible = showHeader,
                    enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { -20 }, animationSpec = tween(350))
                ) {
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
                            Text(
                                greeting,
                                color = Color.White.copy(alpha = 0.75f),
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            AuroraGradientText("Aurora", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        IconButton(
                            onClick = {
                                performAuroraHaptic(context, haptic)
                                onOpenSettings()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Text(
                    "Every unlock, a little more yours.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Hero "Now Playing" Card
                AnimatedVisibility(
                    visible = showHero,
                    enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(400))
                ) {
                    HeroNowPlayingCard(
                        activePath = activePath,
                        activeType = activeType ?: "video",
                        onClick = {
                            val path = activePath
                            if (path != null && File(path).exists()) {
                                performAuroraHaptic(context, haptic)
                                onPreview(path, activeType ?: "video")
                            }
                        },
                        onClear = {
                            performAuroraHaptic(context, haptic)
                            activePath?.let { File(it).delete() }
                            prefs.edit().remove("media_path").remove("media_type").putLong("media_version", System.currentTimeMillis()).apply()
                            activePath = null
                            status = "Wallpaper cleared"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Drag and drop dropzone banner
                if (isDragHovered) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF7CE7D7).copy(alpha = 0.15f))
                            .border(2.dp, Color(0xFF7CE7D7), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Drop wallpaper file here",
                            color = Color(0xFF7CE7D7),
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Recent Wallpapers Row (if any exist)
                if (recentsList.isNotEmpty()) {
                    AnimatedVisibility(
                        visible = showRecents,
                        enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(400))
                    ) {
                        Column {
                            Text(
                                "Recent Wallpapers",
                                color = Color.White.copy(alpha = 0.88f),
                                fontFamily = ManropeFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                recentsList.forEach { recent ->
                                    RecentCard(
                                        recent = recent,
                                        onClick = {
                                            performAuroraHaptic(context, haptic)
                                            if (File(recent.path).exists()) {
                                                onPreview(recent.path, recent.type)
                                            } else {
                                                status = "Media file no longer available"
                                            }
                                        },
                                        onDelete = {
                                            performAuroraHaptic(context, haptic)
                                            removeRecentWallpaper(context, recent.path)
                                            recentsList = getRecentWallpapers(context)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }

                // Upload Grid Section Header directly above cards
                Text(
                    "Choose Wallpaper",
                    color = Color.White.copy(alpha = 0.88f),
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Compact 2x2 Grid Cards
                AnimatedVisibility(
                    visible = showGrid,
                    enter = fadeIn(tween(450)) + slideInVertically(initialOffsetY = { 35 }, animationSpec = tween(450))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PremiumMediaCard(
                                label = "Video",
                                subtitle = "MP4 · MOV · WEBM",
                                icon = Icons.Filled.MovieFilter,
                                accent = AuroraRoyalViolet,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    performAuroraHaptic(context, haptic)
                                    pickVideoLauncher.launch(arrayOf("video/*"))
                                }
                            )
                            PremiumMediaCard(
                                label = "GIF",
                                subtitle = "Animated GIF",
                                icon = Icons.Filled.Gif,
                                accent = AuroraStormyTeal,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    performAuroraHaptic(context, haptic)
                                    pickGifLauncher.launch(arrayOf("image/gif"))
                                }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PremiumMediaCard(
                                label = "Image",
                                subtitle = "JPG · PNG · WEBP",
                                icon = Icons.Filled.Add,
                                accent = AuroraFrostedBlue,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    performAuroraHaptic(context, haptic)
                                    pickImageLauncher.launch(arrayOf("image/*"))
                                }
                            )
                            PremiumMediaCard(
                                label = "URL Upload",
                                subtitle = "Direct Link · Media",
                                icon = Icons.Filled.CloudDownload,
                                accent = Color(0xFF93B5FF),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    performAuroraHaptic(context, haptic)
                                    showUrlDialog = true
                                }
                            )
                        }
                    }
                }

                if (status.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(status, color = Color.White.copy(alpha = 0.85f), fontFamily = ManropeFamily, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            if (showUrlDialog) {
                UrlUploadDialog(
                    onDismiss = { showUrlDialog = false },
                    onImport = { urlInput, onError, onProgress ->
                        scope.launch(Dispatchers.IO) {
                            var tempFile: File? = null
                            try {
                                val rawInput = urlInput.trim()
                                var downloadUrl = if (!rawInput.startsWith("http://", ignoreCase = true) && !rawInput.startsWith("https://", ignoreCase = true)) {
                                    "https://$rawInput"
                                } else rawInput

                                onProgress("Connecting...")

                                // Check if this is an Instagram link
                                if (downloadUrl.contains("instagram.com/", ignoreCase = true)) {
                                    withContext(Dispatchers.Main) {
                                        onProgress("Resolving Instagram video...")
                                    }
                                    val resolved = WebVideoExtractor.resolveVideoUrl(context, downloadUrl)
                                    if (!resolved.isNullOrBlank()) {
                                        downloadUrl = resolved
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            onError("Could not extract video from this Instagram link. Ensure it is public.")
                                        }
                                        return@launch
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    onProgress("Downloading media...")
                                }

                                var connection = URL(downloadUrl).openConnection() as HttpURLConnection
                                connection.instanceFollowRedirects = true
                                connection.connectTimeout = 15000
                                connection.readTimeout = 20000
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                                connection.setRequestProperty("Accept", "video/*,image/*,*/*;q=0.8")
                                connection.connect()

                                var code = connection.responseCode
                                if (code in 300..399) {
                                    val loc = connection.getHeaderField("Location")
                                    if (!loc.isNullOrBlank()) {
                                        downloadUrl = loc
                                        connection = URL(downloadUrl).openConnection() as HttpURLConnection
                                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                        connection.connect()
                                        code = connection.responseCode
                                    }
                                }

                                if (code != HttpURLConnection.HTTP_OK) {
                                    withContext(Dispatchers.Main) {
                                        onError("HTTP error $code while downloading")
                                    }
                                    return@launch
                                }

                                val maxAllowedBytes = 100L * 1024L * 1024L // 100 MB
                                tempFile = File(context.filesDir, "download_temp_${System.currentTimeMillis()}.tmp")

                                var totalBytesRead = 0L
                                val buffer = ByteArray(8192)

                                connection.inputStream.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        var bytesRead: Int
                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            totalBytesRead += bytesRead
                                            if (totalBytesRead > maxAllowedBytes) {
                                                throw IllegalStateException("File exceeds 100 MB size cap.")
                                            }
                                            output.write(buffer, 0, bytesRead)
                                        }
                                    }
                                }

                                // Magic bytes inspection to detect actual binary media type
                                val finalType = detectRealFileType(tempFile)

                                val ext = if (finalType == "gif") "gif" else if (finalType == "video") "mp4" else "jpg"
                                val stagingFile = File(context.filesDir, "staging_url_wallpaper.$ext")
                                tempFile.renameTo(stagingFile)

                                withContext(Dispatchers.Main) {
                                    showUrlDialog = false
                                    onPreview(stagingFile.absolutePath, finalType)
                                }
                            } catch (e: Exception) {
                                tempFile?.delete()
                                withContext(Dispatchers.Main) {
                                    onError("Download failed: ${e.localizedMessage}")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun detectRealFileType(file: File): String {
    if (!file.exists() || file.length() < 12) return "image"
    return try {
        val bytes = ByteArray(12)
        FileInputStream(file).use { it.read(bytes) }
        val headerHex = bytes.joinToString("") { "%02X".format(it) }

        // MP4 / MOV / WEBM video magic bytes
        if (headerHex.contains("66747970") || headerHex.contains("1A45DFA3") || headerHex.contains("6D6F6F76")) {
            "video"
        } else if (headerHex.startsWith("47494638")) {
            "gif"
        } else {
            "image"
        }
    } catch (e: Exception) {
        "image"
    }
}

private fun extractInstagramCode(url: String): String? {
    val regex = Regex("""instagram\.com/(?:reel|reels|p)/([A-Za-z0-9_-]+)""")
    return regex.find(url)?.groupValues?.get(1)
}

@Composable
private fun HeroNowPlayingCard(
    activePath: String?,
    activeType: String,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val richMotion = remember(context) { SettingsPrefs.richMotion(context) }

    val infinite = rememberInfiniteTransition(label = "badgePulse")
    val animatedPulseAlpha by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val pulseAlpha = if (richMotion) animatedPulseAlpha else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AuroraRoyalViolet.copy(alpha = 0.22f),
                        Color.Transparent,
                        AuroraStormyTeal.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .clickable(enabled = activePath != null && File(activePath).exists(), onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Now Playing", color = Color.White.copy(alpha = 0.65f), fontFamily = ManropeFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)

                if (activePath != null && File(activePath).exists()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF7CE7D7).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF7CE7D7).copy(alpha = 0.4f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7CE7D7).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Active", color = Color(0xFF7CE7D7), fontFamily = ManropeFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove Wallpaper",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activePath == null || !File(activePath).exists()) {
                Text("No live wallpaper active", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Choose a video, GIF, or image below to bring your screen to life.", color = Color.White.copy(alpha = 0.6f), fontFamily = ManropeFamily, fontSize = 11.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (activeType) {
                            "gif" -> Icons.Filled.Gif
                            "image" -> Icons.Filled.Add
                            else -> Icons.Filled.Videocam
                        }
                        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeType == "gif") "Animated GIF Wallpaper" else if (activeType == "image") "Static Image Wallpaper" else "Video Live Wallpaper",
                            color = Color.White,
                            fontFamily = ManropeFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Tap to preview & manage", color = Color.White.copy(alpha = 0.6f), fontFamily = ManropeFamily, fontSize = 11.sp)
                    }
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun RecentCard(
    recent: RecentWallpaper,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(105.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (recent.type) {
                        "gif" -> Icons.Filled.Gif
                        "image" -> Icons.Filled.Add
                        else -> Icons.Filled.MovieFilter
                    }
                    Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove recent",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = recent.type.uppercase(),
                color = Color.White,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Re-apply",
                color = Color(0xFF7CE7D7),
                fontFamily = ManropeFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun UrlUploadDialog(
    onDismiss: () -> Unit,
    onImport: (url: String, onError: (String) -> Unit, onProgress: (String) -> Unit) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }
    var dialogStatus by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text("Import from URL", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Enter direct video/image URL or Instagram post link (HTTPS only, max 100 MB).", color = Color.White.copy(alpha = 0.7f), fontFamily = ManropeFamily, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = { Text("https://example.com/video.mp4", color = Color.White.copy(alpha = 0.35f), fontFamily = ManropeFamily) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF93B5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (dialogStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(dialogStatus, color = if (dialogStatus.contains("failed") || dialogStatus.contains("error") || dialogStatus.contains("exceeds")) Color(0xFFFF7675) else Color(0xFF7CE7D7), fontFamily = ManropeFamily, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isDownloading = true
                    onImport(
                        inputUrl.trim(),
                        { err ->
                            dialogStatus = err
                            isDownloading = false
                        },
                        { msg ->
                            dialogStatus = msg
                        }
                    )
                },
                enabled = inputUrl.isNotBlank() && !isDownloading
            ) {
                Text(if (isDownloading) "Downloading..." else "Import", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = ManropeFamily)
            }
        },
        containerColor = Color(0xFF141A29),
        shape = RoundedCornerShape(20.dp)
    )
}

fun applyImageWallpaper(context: Context, imagePath: String, flags: Int): Boolean {
    return try {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap != null) {
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, flags)
            true
        } else false
    } catch (e: Exception) {
        Log.w("Aurora", "Failed to apply image wallpaper", e)
        false
    }
}

fun getRecentWallpapers(context: Context): List<RecentWallpaper> {
    val prefs = context.getSharedPreferences("aurora_recents_prefs", Context.MODE_PRIVATE)
    val list = mutableListOf<RecentWallpaper>()
    for (i in 0 until 3) {
        val path = prefs.getString("recent_${i}_path", null)
        val type = prefs.getString("recent_${i}_type", null)
        val time = prefs.getLong("recent_${i}_time", 0L)
        if (path != null && type != null && File(path).exists()) {
            list.add(RecentWallpaper(path, type, time))
        }
    }
    return list
}

fun removeRecentWallpaper(context: Context, path: String) {
    val current = getRecentWallpapers(context).filter { it.path != path }
    File(path).delete()
    val prefs = context.getSharedPreferences("aurora_recents_prefs", Context.MODE_PRIVATE).edit()
    prefs.clear()
    current.forEachIndexed { index, recent ->
        prefs.putString("recent_${index}_path", recent.path)
        prefs.putString("recent_${index}_type", recent.type)
        prefs.putLong("recent_${index}_time", recent.timestamp)
    }
    prefs.apply()
}

fun addRecentWallpaper(context: Context, path: String, type: String) {
    val current = getRecentWallpapers(context).filter { it.path != path }.toMutableList()
    current.add(0, RecentWallpaper(path, type, System.currentTimeMillis()))

    // Total storage cap for recents (150 MB)
    val maxRecentsStorage = 150L * 1024L * 1024L
    while (current.size > 3 || (current.sumOf { File(it.path).length() } > maxRecentsStorage && current.size > 1)) {
        val removed = current.removeAt(current.lastIndex)
        File(removed.path).delete()
    }

    val prefs = context.getSharedPreferences("aurora_recents_prefs", Context.MODE_PRIVATE).edit()
    prefs.clear()
    current.forEachIndexed { index, recent ->
        prefs.putString("recent_${index}_path", recent.path)
        prefs.putString("recent_${index}_type", recent.type)
        prefs.putLong("recent_${index}_time", recent.timestamp)
    }
    prefs.apply()
}

@UnstableApi
fun launchLiveWallpaperPicker(context: Context) {
    try {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, AuroraWallpaperService::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        if (context is Activity) {
            context.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    } catch (e: Exception) {
        Log.w("AuroraPicker", "ACTION_CHANGE_LIVE_WALLPAPER failed, trying fallback intent", e)
        try {
            val fallback = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open Live Wallpaper picker", Toast.LENGTH_SHORT).show()
        }
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

fun promoteStagingToActive(context: Context, stagingPath: String, type: String): String {
    val ext = if (type == "gif") "gif" else if (type == "video") "mp4" else "jpg"
    val activeFile = File(context.filesDir, "current_wallpaper_$type.$ext")
    val stagingFile = File(stagingPath)
    if (stagingFile.exists() && stagingFile.absolutePath != activeFile.absolutePath) {
        stagingFile.copyTo(activeFile, overwrite = true)
        stagingFile.delete()
    }
    // Ensure world/service readable
    activeFile.setReadable(true, false)

    // Save to recents (keep top 3 wallpapers up to 150MB cap for 1-tap re-apply)
    val recentFile = File(context.filesDir, "recent_${System.currentTimeMillis()}.$ext")
    activeFile.copyTo(recentFile, overwrite = true)
    recentFile.setReadable(true, false)
    addRecentWallpaper(context, recentFile.absolutePath, type)

    saveWallpaperMediaPath(context, activeFile.absolutePath, type)
    return activeFile.absolutePath
}

fun cleanupStagingFiles(context: Context) {
    try {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("staging_") || file.name.startsWith("download_temp_") || file.name.endsWith(".tmp")) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        Log.w("Aurora", "Failed to clean up staging files", e)
    }
}

fun saveWallpaperMediaPath(context: Context, path: String, type: String) {
    val prefs = context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("media_path", path)
        .putString("media_type", type)
        .putLong("media_version", System.currentTimeMillis())
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
