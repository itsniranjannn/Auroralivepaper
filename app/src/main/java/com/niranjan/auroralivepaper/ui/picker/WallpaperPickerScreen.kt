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
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.niranjan.auroralivepaper.AuroraWallpaperService
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.ui.components.AuroraAnimatedBackground
import com.niranjan.auroralivepaper.ui.components.AuroraGradientButton
import com.niranjan.auroralivepaper.ui.components.AuroraGradientText
import com.niranjan.auroralivepaper.ui.components.PremiumMediaCard
import com.niranjan.auroralivepaper.ui.settings.performAuroraHaptic
import com.niranjan.auroralivepaper.ui.theme.AuroraFrostedBlue
import com.niranjan.auroralivepaper.ui.theme.AuroraRoyalViolet
import com.niranjan.auroralivepaper.ui.theme.AuroraStormyTeal
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

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }

    var showHeader by remember { mutableStateOf(false) }
    var showHero by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    var isDragHovered by remember { mutableStateOf(false) }
    var recentsList by remember { mutableStateOf(getRecentWallpapers(context)) }

    val prefs = remember { context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE) }
    val activePath = prefs.getString("media_path", null)
    val activeType = prefs.getString("media_type", "video")

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
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            selectedBitmap = bitmap
            status = "Image loaded — preview below"
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
                            val ext = if (type == "gif") "gif" else "mp4"
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(16.dp))

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
                            Text(greeting, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            AuroraGradientText("Aurora", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        IconButton(
                            onClick = {
                                performAuroraHaptic(context, haptic)
                                onOpenSettings()
                            },
                            modifier = Modifier
                                .size(44.dp)
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

                Spacer(modifier = Modifier.height(18.dp))

                // Hero "Now Playing" Card
                AnimatedVisibility(
                    visible = showHero,
                    enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(400))
                ) {
                    HeroNowPlayingCard(
                        activePath = activePath,
                        activeType = activeType ?: "video",
                        onClick = {
                            if (activePath != null && File(activePath).exists()) {
                                performAuroraHaptic(context, haptic)
                                onPreview(activePath, activeType ?: "video")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Drag and drop dropzone banner
                if (isDragHovered) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF7CE7D7).copy(alpha = 0.15f))
                            .border(2.dp, Color(0xFF7CE7D7), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Drop wallpaper file here",
                            color = Color(0xFF7CE7D7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
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
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                recentsList.forEach { recent ->
                                    RecentCard(recent) {
                                        performAuroraHaptic(context, haptic)
                                        if (File(recent.path).exists()) {
                                            onPreview(recent.path, recent.type)
                                        } else {
                                            status = "Media file no longer available"
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(22.dp))
                        }
                    }
                }

                // Upload Grid Section Header
                Text(
                    "Choose Wallpaper",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Grid with Spring Motion
                AnimatedVisibility(
                    visible = showGrid,
                    enter = fadeIn(tween(450)) + slideInVertically(initialOffsetY = { 35 }, animationSpec = tween(450))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                subtitle = "HTTPS · Max 100MB",
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

                // Selected Bitmap preview if set directly
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

                if (status.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(status, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(36.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.width(100.dp).height(2.dp)
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Built by Niranjan.K",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (showUrlDialog) {
                UrlUploadDialog(
                    onDismiss = { showUrlDialog = false },
                    onImport = { url, onError, onProgress ->
                        scope.launch(Dispatchers.IO) {
                            var tempFile: File? = null
                            try {
                                var currentUrlStr = url
                                if (!currentUrlStr.startsWith("https://", ignoreCase = true)) {
                                    withContext(Dispatchers.Main) {
                                        onError("Security error: URL must start with https://")
                                    }
                                    return@launch
                                }

                                var redirects = 0
                                var connection: HttpURLConnection
                                val contentType: String

                                while (true) {
                                    withContext(Dispatchers.Main) {
                                        onProgress("Connecting...")
                                    }

                                    connection = URL(currentUrlStr).openConnection() as HttpURLConnection
                                    connection.instanceFollowRedirects = false
                                    connection.connectTimeout = 10000
                                    connection.readTimeout = 15000
                                    connection.requestMethod = "GET"
                                    connection.connect()

                                    val code = connection.responseCode
                                    if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                                        code == HttpURLConnection.HTTP_MOVED_TEMP ||
                                        code == 307 || code == 308
                                    ) {
                                        redirects++
                                        if (redirects > 3) {
                                            withContext(Dispatchers.Main) {
                                                onError("Error: Too many redirects.")
                                            }
                                            return@launch
                                        }
                                        val loc = connection.getHeaderField("Location") ?: ""
                                        if (!loc.startsWith("https://", ignoreCase = true)) {
                                            withContext(Dispatchers.Main) {
                                                onError("Security error: Redirect target must use HTTPS.")
                                            }
                                            return@launch
                                        }
                                        currentUrlStr = loc
                                    } else if (code == HttpURLConnection.HTTP_OK) {
                                        contentType = connection.contentType ?: ""
                                        break
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            onError("HTTP error $code")
                                        }
                                        return@launch
                                    }
                                }

                                val maxAllowedBytes = 100L * 1024L * 1024L // 100 MB

                                val isGif = contentType.contains("gif", ignoreCase = true) || currentUrlStr.endsWith(".gif", ignoreCase = true)
                                val isVideo = contentType.contains("video", ignoreCase = true) || currentUrlStr.endsWith(".mp4", ignoreCase = true) || currentUrlStr.endsWith(".webm", ignoreCase = true) || currentUrlStr.endsWith(".mov", ignoreCase = true)
                                val isImage = contentType.contains("image", ignoreCase = true)

                                if (!isGif && !isVideo && !isImage) {
                                    withContext(Dispatchers.Main) {
                                        onError("Unsupported media type: expected video, GIF, or image.")
                                    }
                                    return@launch
                                }

                                val type = when {
                                    isGif -> "gif"
                                    isVideo -> "video"
                                    else -> "image"
                                }

                                val ext = if (type == "gif") "gif" else if (type == "video") "mp4" else "jpg"
                                tempFile = File(context.filesDir, "download_temp_${System.currentTimeMillis()}.tmp")

                                withContext(Dispatchers.Main) {
                                    onProgress("Downloading...")
                                }

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

                                val stagingFile = File(context.filesDir, "staging_url_wallpaper.$ext")
                                tempFile.renameTo(stagingFile)

                                withContext(Dispatchers.Main) {
                                    showUrlDialog = false
                                    onPreview(stagingFile.absolutePath, type)
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

@Composable
private fun HeroNowPlayingCard(
    activePath: String?,
    activeType: String,
    onClick: () -> Unit
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
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AuroraRoyalViolet.copy(alpha = 0.22f),
                        Color.Transparent,
                        AuroraStormyTeal.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .clickable(enabled = activePath != null && File(activePath).exists(), onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Now Playing", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)

                if (activePath != null && File(activePath).exists()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF7CE7D7).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF7CE7D7).copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7CE7D7).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active", color = Color(0xFF7CE7D7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activePath == null || !File(activePath).exists()) {
                Text("No live wallpaper active", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Choose a video, GIF, or image below to bring your screen to life.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (activeType) {
                            "gif" -> Icons.Filled.Gif
                            "image" -> Icons.Filled.Add
                            else -> Icons.Filled.Videocam
                        }
                        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeType == "gif") "Animated GIF Wallpaper" else if (activeType == "image") "Static Image Wallpaper" else "Video Live Wallpaper",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Tap to preview & manage", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (recent.type) {
                    "gif" -> Icons.Filled.Gif
                    "image" -> Icons.Filled.Add
                    else -> Icons.Filled.MovieFilter
                }
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = recent.type.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Re-apply",
                color = Color(0xFF7CE7D7),
                fontSize = 11.sp,
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
            Text("Import from URL", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Enter direct link to video, GIF, or image (HTTPS only, max 100 MB).", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = { Text("https://example.com/media.mp4", color = Color.White.copy(alpha = 0.35f)) },
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
                    Text(dialogStatus, color = if (dialogStatus.contains("failed") || dialogStatus.contains("error") || dialogStatus.contains("exceeds")) Color(0xFFFF7675) else Color(0xFF7CE7D7), fontSize = 12.sp)
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
                Text(if (isDownloading) "Downloading..." else "Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF141A29),
        shape = RoundedCornerShape(20.dp)
    )
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

fun promoteStagingToActive(context: Context, stagingPath: String, type: String): String {
    val ext = if (type == "gif") "gif" else if (type == "video") "mp4" else "jpg"
    val activeFile = File(context.filesDir, "current_wallpaper_$type.$ext")
    val stagingFile = File(stagingPath)
    if (stagingFile.exists() && stagingFile.absolutePath != activeFile.absolutePath) {
        stagingFile.copyTo(activeFile, overwrite = true)
        stagingFile.delete()
    }

    // Save to recents (keep top 3 wallpapers up to 150MB cap for 1-tap re-apply)
    val recentFile = File(context.filesDir, "recent_${System.currentTimeMillis()}.$ext")
    activeFile.copyTo(recentFile, overwrite = true)
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
