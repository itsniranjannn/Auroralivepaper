package com.niranjan.auroralivepaper.ui.picker

import android.R
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Movie
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.niranjan.auroralivepaper.AuroraWallpaperService
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.ui.components.AuroraGradientText
import com.niranjan.auroralivepaper.ui.components.PremiumMediaCard
import com.niranjan.auroralivepaper.ui.settings.performAuroraHaptic
import com.niranjan.auroralivepaper.ui.theme.AuroraFrostedBlue
import com.niranjan.auroralivepaper.ui.theme.AuroraRoyalViolet
import com.niranjan.auroralivepaper.ui.theme.AuroraStormyTeal
import com.niranjan.auroralivepaper.ui.theme.ManropeFamily
import com.niranjan.auroralivepaper.util.InstagramExtractResult
import com.niranjan.auroralivepaper.util.MediaValidator
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
import java.nio.ByteBuffer
import java.util.Calendar

data class RecentWallpaper(val path: String, val type: String, val timestamp: Long)

private val SpecularBorderBrush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.04f)
    )
)

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

    // Staggered spring waterfall entrance animatables
    val headerAlpha = remember { Animatable(0f) }
    val headerOffsetY = remember { Animatable(-20f) }
    val settingsRotation = remember { Animatable(45f) }

    val heroAlpha = remember { Animatable(0f) }
    val heroOffsetY = remember { Animatable(32f) }

    val recentsAlpha = remember { Animatable(0f) }
    val recentsOffsetX = remember { Animatable(40f) }

    val gridHeaderAlpha = remember { Animatable(0f) }
    val card0Scale = remember { Animatable(0.92f) }
    val card0Alpha = remember { Animatable(0f) }
    val card1Scale = remember { Animatable(0.92f) }
    val card1Alpha = remember { Animatable(0f) }
    val card2Scale = remember { Animatable(0.92f) }
    val card2Alpha = remember { Animatable(0f) }
    val card3Scale = remember { Animatable(0.92f) }
    val card3Alpha = remember { Animatable(0f) }

    var showUrlDialog by remember { mutableStateOf(false) }

    var isDragHovered by remember { mutableStateOf(false) }
    var recentsList by remember { mutableStateOf(getRecentWallpapers(context)) }

    val prefs = remember { context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE) }
    var activePath by remember { mutableStateOf(prefs.getString("media_path", null)) }
    var activeType by remember { mutableStateOf(prefs.getString("media_type", "video")) }

    val wm = remember(context) { WallpaperManager.getInstance(context) }
    var activeWallpaperInfo by remember { mutableStateOf(try { wm.wallpaperInfo } catch (e: Exception) { null }) }

    DisposableEffect(Unit) {
        val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            activeWallpaperInfo = try { wm.wallpaperInfo } catch (e: Exception) { null }
            activePath = prefs.getString("media_path", null)
            activeType = prefs.getString("media_type", "video")
        }
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }

    val isOurWallpaperActive = isAuroraWallpaperActive(context)

    LaunchedEffect(Unit) {
        val richMotion = SettingsPrefs.richMotion(context)
        if (!richMotion) {
            headerAlpha.snapTo(1f)
            headerOffsetY.snapTo(0f)
            settingsRotation.snapTo(0f)
            heroAlpha.snapTo(1f)
            heroOffsetY.snapTo(0f)
            recentsAlpha.snapTo(1f)
            recentsOffsetX.snapTo(0f)
            gridHeaderAlpha.snapTo(1f)
            card0Alpha.snapTo(1f)
            card0Scale.snapTo(1f)
            card1Alpha.snapTo(1f)
            card1Scale.snapTo(1f)
            card2Alpha.snapTo(1f)
            card2Scale.snapTo(1f)
            card3Alpha.snapTo(1f)
            card3Scale.snapTo(1f)
        } else {
            val springSpec = spring<Float>(dampingRatio = 0.78f, stiffness = 200f)

            // Header Row + Settings Gear 45deg rotation
            launch { headerAlpha.animateTo(1f, tween(240)) }
            launch { headerOffsetY.animateTo(0f, springSpec) }
            launch { settingsRotation.animateTo(0f, springSpec) }

            // Hero "Now Playing" card slides up + fades in
            delay(90)
            launch { heroAlpha.animateTo(1f, tween(260)) }
            launch { heroOffsetY.animateTo(0f, springSpec) }

            // "Recent Wallpapers" slides in from right + fades in
            delay(90)
            launch { recentsAlpha.animateTo(1f, tween(260)) }
            launch { recentsOffsetX.animateTo(0f, springSpec) }

            // 2x2 Bento Grid cards scale in with 50ms per-card stagger
            delay(90)
            launch { gridHeaderAlpha.animateTo(1f, tween(260)) }
            launch {
                card0Alpha.animateTo(1f, tween(260))
                card0Scale.animateTo(1f, springSpec)
            }

            delay(50)
            launch {
                card1Alpha.animateTo(1f, tween(260))
                card1Scale.animateTo(1f, springSpec)
            }

            delay(50)
            launch {
                card2Alpha.animateTo(1f, tween(260))
                card2Scale.animateTo(1f, springSpec)
            }

            delay(50)
            launch {
                card3Alpha.animateTo(1f, tween(260))
                card3Scale.animateTo(1f, springSpec)
            }
        }
    }

    var pendingValidationMedia by remember { mutableStateOf<Pair<String, String>?>(null) }
    var validationWarnings by remember { mutableStateOf<List<String>>(emptyList()) }

    val handleMediaSelected: (String, String) -> Unit = { stagingPath, type ->
        scope.launch {
            val warnings = validateStagedMedia(context, stagingPath, type)
            if (warnings.isNotEmpty()) {
                pendingValidationMedia = Pair(stagingPath, type)
                validationWarnings = warnings
            } else {
                onPreview(stagingPath, type)
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stagingPath = copyMediaToAppStorage(context, it, "staging_wallpaper_image.jpg")
            handleMediaSelected(stagingPath, "image")
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stagingPath = copyMediaToAppStorage(context, it, "staging_wallpaper_video.mp4")
            handleMediaSelected(stagingPath, "video")
        }
    }

    val pickGifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stagingPath = copyMediaToAppStorage(context, it, "staging_wallpaper_gif.gif")
            handleMediaSelected(stagingPath, "gif")
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
                            handleMediaSelected(stagingPath, type)
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

            // Header Row (t + 0ms)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = headerOffsetY.value.dp)
                    .alpha(headerAlpha.value),
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
                        .graphicsLayer {
                            rotationZ = settingsRotation.value
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, SpecularBorderBrush, RoundedCornerShape(14.dp))
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
                color = Color.White.copy(alpha = 0.65f),
                fontFamily = ManropeFamily,
                fontSize = 12.sp,
                modifier = Modifier
                    .offset(y = headerOffsetY.value.dp)
                    .alpha(headerAlpha.value)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hero "Now Playing" Card (t + 90ms)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = heroOffsetY.value.dp)
                    .alpha(heroAlpha.value)
            ) {
                HeroNowPlayingCard(
                    activePath = if (isOurWallpaperActive) activePath else null,
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
                        val version = System.currentTimeMillis()
                        prefs.edit().remove("media_path").remove("media_type").putLong("media_version", version).apply()
                        try {
                            val reloadIntent = Intent(com.niranjan.auroralivepaper.AuroraWallpaperService.ACTION_RELOAD_WALLPAPER).apply {
                                setPackage(context.packageName)
                                putExtra("media_version", version)
                            }
                            context.sendBroadcast(reloadIntent)
                        } catch (_: Exception) {}
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

            // Recent Wallpapers Row (t + 180ms)
            if (recentsList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = recentsOffsetX.value.dp)
                        .alpha(recentsAlpha.value)
                ) {
                    Text(
                        "Recent Wallpapers",
                        color = Color.White.copy(alpha = 0.88f),
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
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

            // Choose Wallpaper Grid Section Header (t + 270ms)
            Text(
                "Choose Wallpaper",
                color = Color.White.copy(alpha = 0.88f),
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.alpha(gridHeaderAlpha.value)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 2x2 Bento Grid Cards with Staggered Entrance & Semantic Accents
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
                        accent = Color(0xFF9D4EDD), // Electric Violet
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = card0Scale.value
                                scaleY = card0Scale.value
                                alpha = card0Alpha.value
                            },
                        onClick = {
                            performAuroraHaptic(context, haptic)
                            pickVideoLauncher.launch(arrayOf("video/*"))
                        }
                    )
                    PremiumMediaCard(
                        label = "GIF",
                        subtitle = "Animated GIF",
                        icon = Icons.Filled.Gif,
                        accent = Color(0xFF00F5D4), // Bioluminescent Cyan
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = card1Scale.value
                                scaleY = card1Scale.value
                                alpha = card1Alpha.value
                            },
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
                        accent = Color(0xFF00BFA5), // Emerald Mint
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = card2Scale.value
                                scaleY = card2Scale.value
                                alpha = card2Alpha.value
                            },
                        onClick = {
                            performAuroraHaptic(context, haptic)
                            pickImageLauncher.launch(arrayOf("image/*"))
                        }
                    )
                    PremiumMediaCard(
                        label = "URL Upload",
                        subtitle = "Direct Link · Media",
                        icon = Icons.Filled.CloudDownload,
                        accent = Color(0xFF4EA8DE), // Sky Indigo
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = card3Scale.value
                                scaleY = card3Scale.value
                                alpha = card3Alpha.value
                            },
                        onClick = {
                            performAuroraHaptic(context, haptic)
                            showUrlDialog = true
                        }
                    )
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

                                val lowerUrl = downloadUrl.lowercase()
                                if (lowerUrl.contains("youtube.com") || lowerUrl.contains("m.youtube.com") || lowerUrl.contains("youtu.be") || lowerUrl.contains("music.youtube.com") || lowerUrl.contains("/shorts/")) {
                                    withContext(Dispatchers.Main) {
                                        onError("YouTube links aren't supported. Download the video to your phone and use Video import instead.")
                                    }
                                    return@launch
                                }

                                onProgress("Connecting...")

                                if (downloadUrl.contains("instagram.com/", ignoreCase = true)) {
                                    withContext(Dispatchers.Main) {
                                        onProgress("Resolving Instagram video...")
                                    }
                                    val extractResult = WebVideoExtractor.resolveVideoUrl(context, downloadUrl)
                                    when (extractResult) {
                                        is InstagramExtractResult.Success -> {
                                            downloadUrl = extractResult.url
                                        }
                                        is InstagramExtractResult.PrivateOrLogin -> {
                                            withContext(Dispatchers.Main) {
                                                onError("This post looks private or needs a login.\nTip: save the video to your phone from Instagram, then use Video import.")
                                            }
                                            return@launch
                                        }
                                        is InstagramExtractResult.Blocked -> {
                                            withContext(Dispatchers.Main) {
                                                onError("Instagram blocked the request, try again in a few minutes.\nTip: save the video to your phone from Instagram, then use Video import.")
                                            }
                                            return@launch
                                        }
                                        is InstagramExtractResult.NotFound -> {
                                            withContext(Dispatchers.Main) {
                                                onError("Post not found.\nTip: save the video to your phone from Instagram, then use Video import.")
                                            }
                                            return@launch
                                        }
                                        is InstagramExtractResult.NetworkError -> {
                                            withContext(Dispatchers.Main) {
                                                onError("Network error connecting to Instagram. Check your connection.\nTip: save the video to your phone from Instagram, then use Video import.")
                                            }
                                            return@launch
                                        }
                                        is InstagramExtractResult.Unknown -> {
                                            withContext(Dispatchers.Main) {
                                                onError("Couldn't read this reel. Try again, or save the video from Instagram and use Video import.")
                                            }
                                            return@launch
                                        }
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    onProgress("Downloading media...")
                                }

                                var connection = URL(downloadUrl).openConnection() as HttpURLConnection
                                connection.instanceFollowRedirects = true
                                connection.connectTimeout = 15000
                                connection.readTimeout = 20000
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                                connection.setRequestProperty("Referer", "https://www.instagram.com/")
                                connection.setRequestProperty("Accept", "video/*,image/*,*/*;q=0.8")
                                connection.connect()

                                var code = connection.responseCode
                                if (code in 300..399) {
                                    val loc = connection.getHeaderField("Location")
                                    if (!loc.isNullOrBlank()) {
                                        downloadUrl = loc
                                        connection = URL(downloadUrl).openConnection() as HttpURLConnection
                                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                                        connection.setRequestProperty("Referer", "https://www.instagram.com/")
                                        connection.connect()
                                        code = connection.responseCode
                                    }
                                }

                                val contentType = connection.contentType ?: ""
                                if (contentType.startsWith("text/", ignoreCase = true)) {
                                    connection.disconnect()
                                    withContext(Dispatchers.Main) {
                                        onError("This link is a web page, not a video or image file.")
                                    }
                                    return@launch
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

                                val finalType = detectRealFileType(tempFile)
                                if (finalType == null) {
                                    tempFile?.delete()
                                    withContext(Dispatchers.Main) {
                                        onError("This link is a web page, not a video or image file.")
                                    }
                                    return@launch
                                }

                                val ext = if (finalType == "gif") "gif" else if (finalType == "video") "mp4" else "jpg"
                                val stagingFile = File(context.filesDir, "staging_url_${System.currentTimeMillis()}.$ext")
                                tempFile.copyTo(stagingFile, overwrite = true)
                                tempFile.delete()

                                if (finalType == "video") {
                                    val retriever = MediaMetadataRetriever()
                                    try {
                                        retriever.setDataSource(stagingFile.absolutePath)
                                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                        val hasVideoStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                                        val durationMs = durationStr?.toLongOrNull() ?: 0L
                                        if (durationMs <= 0 || hasVideoStr != "yes") {
                                            stagingFile.delete()
                                            withContext(Dispatchers.Main) {
                                                onError("This file can't be played as a video.")
                                            }
                                            return@launch
                                        }
                                    } catch (e: Exception) {
                                        stagingFile.delete()
                                        withContext(Dispatchers.Main) {
                                            onError("This file can't be played as a video.")
                                        }
                                        return@launch
                                    } finally {
                                        try { retriever.release() } catch (_: Exception) {}
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    showUrlDialog = false
                                    handleMediaSelected(stagingFile.absolutePath, finalType)
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

            if (pendingValidationMedia != null && validationWarnings.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = {
                        pendingValidationMedia?.first?.let { File(it).delete() }
                        pendingValidationMedia = null
                        validationWarnings = emptyList()
                    },
                    title = {
                        Text("Media Warning", color = Color.White, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text(
                                "This media file has parameters that may affect performance:",
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = ManropeFamily,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            validationWarnings.forEach { warning ->
                                Text(
                                    "• $warning",
                                    color = Color(0xFFFFB74D),
                                    fontFamily = ManropeFamily,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val (path, type) = pendingValidationMedia!!
                                pendingValidationMedia = null
                                validationWarnings = emptyList()
                                onPreview(path, type)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B894))
                        ) {
                            Text("Use anyway", fontFamily = ManropeFamily, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingValidationMedia?.first?.let { File(it).delete() }
                                pendingValidationMedia = null
                                validationWarnings = emptyList()
                            }
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = ManropeFamily)
                        }
                    },
                    containerColor = Color(0xFF141A29),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }

suspend fun validateStagedMedia(context: Context, stagingPath: String, mediaType: String): List<String> = withContext(Dispatchers.IO) {
    var width = 0
    var height = 0
    var fps = 0f
    var durationMs = 0L
    val file = File(stagingPath)
    val sizeBytes = if (file.exists()) file.length() else 0L

    var retriever: MediaMetadataRetriever? = null
    var extractor: MediaExtractor? = null

    try {
        if (mediaType == "image") {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(stagingPath, options)
            width = options.outWidth
            height = options.outHeight
        } else {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(stagingPath)

            val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            width = wStr?.toIntOrNull() ?: 0
            height = hStr?.toIntOrNull() ?: 0
            durationMs = durStr?.toLongOrNull() ?: 0L

            if (mediaType == "video") {
                try {
                    extractor = MediaExtractor()
                    extractor.setDataSource(stagingPath)
                    for (i in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(i)
                        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("video/")) {
                            if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                                fps = try {
                                    format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                                } catch (_: Throwable) {
                                    format.getFloat(MediaFormat.KEY_FRAME_RATE)
                                }
                            }
                            break
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    } catch (_: Throwable) {
        // If metadata cannot be read, continue silently
    } finally {
        try { retriever?.release() } catch (_: Throwable) {}
        try { extractor?.release() } catch (_: Throwable) {}
    }

    MediaValidator.evaluate(width, height, fps, durationMs, sizeBytes)
}

private fun detectRealFileType(file: File): String? {
    if (!file.exists() || file.length() < 12) return null
    return try {
        val bufferSize = minOf(file.length().toInt(), 1024)
        val bytes = ByteArray(bufferSize)
        FileInputStream(file).use { it.read(bytes) }
        val headerHex = bytes.joinToString("") { "%02X".format(it) }
        val headerText = String(bytes, Charsets.UTF_8).lowercase()

        if (headerText.contains("<!doctype") || headerText.contains("<html") || headerText.contains("<head")) {
            return null
        }

        if (headerHex.contains("66747970") ||
            headerHex.contains("6D6F6F76") ||
            headerHex.contains("6D646174") ||
            headerHex.contains("1A45DFA3"))
        {
            "video"
        } else if (headerHex.startsWith("47494638")) {
            "gif"
        } else if (headerHex.startsWith("FFD8FF") || headerHex.startsWith("89504E47") || headerHex.startsWith("52494646")) {
            "image"
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
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

    val posterBitmap by produceState<Bitmap?>(initialValue = null, key1 = activePath) {
        value = withContext(Dispatchers.IO) {
            if (activePath != null && File(activePath).exists()) {
                try {
                    if (activeType == "image") {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeFile(activePath, opts)
                    } else {
                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(activePath)
                        val bm = mmr.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: mmr.getFrameAtTime(0L)
                        mmr.release()
                        bm
                    }
                } catch (_: Throwable) {
                    null
                }
            } else {
                null
            }
        }
    }

    val infinite = rememberInfiniteTransition("badgePulse")
    val pulseRingScale by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRingScale"
    )
    val pulseRingAlpha by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRingAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        AuroraRoyalViolet.copy(alpha = 0.22f),
                        Color(0xFF0D111E).copy(alpha = 0.40f),
                        AuroraStormyTeal.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.dp, SpecularBorderBrush, RoundedCornerShape(22.dp))
            .clickable(
                enabled = activePath != null && File(activePath).exists(),
                onClick = onClick
            )
    ) {
        if (posterBitmap != null) {
            Image(
                bitmap = posterBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.22f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF07090F).copy(alpha = 0.45f),
                                Color(0xFF07090F).copy(alpha = 0.80f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Now Playing",
                    color = Color.White.copy(alpha = 0.65f),
                    fontFamily = ManropeFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (activePath != null && File(activePath).exists()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF00BFA5).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF00BFA5).copy(alpha = 0.40f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (richMotion) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .graphicsLayer {
                                                scaleX = pulseRingScale
                                                scaleY = pulseRingScale
                                                alpha = pulseRingAlpha
                                            }
                                            .clip(CircleShape)
                                            .background(Color(0xFF00BFA5))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00BFA5))
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "Active",
                                color = Color(0xFF00BFA5),
                                fontFamily = ManropeFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
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

            if (activePath != null && File(activePath).exists()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (activeType) {
                            "gif" -> Icons.Filled.Gif
                            "image" -> Icons.Filled.Add
                            else -> Icons.Filled.Videocam
                        }
                        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(20.dp))
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
                        Text("Tap to preview & manage", color = Color.White.copy(alpha = 0.65f), fontFamily = ManropeFamily, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
                                )
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Preview",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            } else {
                Text(
                    "No live wallpaper active",
                    color = Color.White,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Choose a video, GIF, or image below to bring your screen to life.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = ManropeFamily,
                    fontSize = 11.sp
                )
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
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = recent.path) {
        value = withContext(Dispatchers.IO) {
            try {
                val file = File(recent.path)
                if (!file.exists()) return@withContext null
                if (recent.type == "image") {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(recent.path, opts)
                } else {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(recent.path)
                    val frame = mmr.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: mmr.getFrameAtTime(0L)
                    mmr.release()
                    frame
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141922))
            .border(1.dp, SpecularBorderBrush, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color(0xFF07090F).copy(alpha = 0.80f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (recent.type) {
                        "gif" -> Icons.Filled.Gif
                        "image" -> Icons.Filled.Add
                        else -> Icons.Filled.MovieFilter
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Column {
                Text(
                    text = recent.type.uppercase(),
                    color = Color.White,
                    fontFamily = ManropeFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = "Re-apply",
                    color = Color(0xFF00E5FF),
                    fontFamily = ManropeFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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

    val clipboardManager = LocalClipboardManager.current
    var clipboardUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val clipText = clipboardManager.getText()?.text?.trim()
            if (!clipText.isNullOrBlank()) {
                val lower = clipText.lowercase()
                if (lower.startsWith("http://") || lower.startsWith("https://") || lower.contains("instagram.com")) {
                    clipboardUrl = clipText
                }
            }
        } catch (_: Exception) {}
    }

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
                    enabled = !isDownloading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF93B5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (clipboardUrl != null && inputUrl.isBlank() && !isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = { inputUrl = clipboardUrl ?: "" },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF93B5FF).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93B5FF).copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentPaste,
                                contentDescription = null,
                                tint = Color(0xFF93B5FF),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Paste detected link from clipboard",
                                color = Color(0xFF93B5FF),
                                fontFamily = ManropeFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF93B5FF),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                if (dialogStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        dialogStatus,
                        color = if (dialogStatus.contains("failed") || dialogStatus.contains("error") || dialogStatus.contains("exceeds") || dialogStatus.contains("web page") || dialogStatus.contains("YouTube") || dialogStatus.contains("can't be played") || dialogStatus.contains("Tip:") || dialogStatus.contains("Couldn't read")) Color(0xFFFF7675) else Color(0xFF7CE7D7),
                        fontFamily = ManropeFamily,
                        fontSize = 12.sp
                    )
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

fun isSamsungVideoLockAvailable(context: Context): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
            setDataAndType(Uri.parse("content://media/external/video/media/1"), "video/mp4")
            setComponent(ComponentName("com.samsung.android.app.dressroom", "com.samsung.android.app.dressroom.presentation.SetAsWallPaperActivity"))
        }
        val pm = context.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo != null
    } catch (e: Exception) {
        false
    }
}

fun isAuroraWallpaperActive(context: Context): Boolean {
    return try {
        val wm = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val homeInfo = wm.getWallpaperInfo(WallpaperManager.FLAG_SYSTEM)
            val lockInfo = wm.getWallpaperInfo(WallpaperManager.FLAG_LOCK)

            val isHome = homeInfo != null &&
                    homeInfo.packageName == context.packageName &&
                    homeInfo.serviceName == AuroraWallpaperService::class.java.name

            val isLock = lockInfo != null &&
                    lockInfo.packageName == context.packageName &&
                    lockInfo.serviceName == AuroraWallpaperService::class.java.name

            isHome || isLock
        } else {
            val info = wm.wallpaperInfo
            info != null &&
                    info.packageName == context.packageName &&
                    info.serviceName == AuroraWallpaperService::class.java.name
        }
    } catch (_: Throwable) {
        false
    }
}

fun applyImageWallpaper(context: Context, imagePath: String, flags: Int, scalingMode: String = "fill"): Boolean {
    var bitmap: Bitmap? = null
    var resultBitmap: Bitmap? = null
    return try {
        val dm = context.resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, boundsOptions)
        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight
        if (origW <= 0 || origH <= 0) return false

        val targetW = screenW * 2
        val targetH = screenH * 2
        var sampleSize = 1
        while ((origW / sampleSize) > targetW || (origH / sampleSize) > targetH) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        bitmap = BitmapFactory.decodeFile(imagePath, decodeOptions) ?: return false

        val wm = WallpaperManager.getInstance(context)
        resultBitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(android.graphics.Color.BLACK)

        val scale = if (scalingMode == "fit") {
            minOf(screenW.toFloat() / bitmap.width, screenH.toFloat() / bitmap.height)
        } else {
            maxOf(screenW.toFloat() / bitmap.width, screenH.toFloat() / bitmap.height)
        }
        val dx = (screenW - bitmap.width * scale) / 2f
        val dy = (screenH - bitmap.height * scale) / 2f
        val matrix = android.graphics.Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(bitmap, matrix, null)
        wm.setBitmap(resultBitmap, null, true, flags)
        true
    } catch (t: Throwable) {
        Log.w("Aurora", "Failed to apply image wallpaper", t)
        false
    } finally {
        try { bitmap?.recycle() } catch (_: Throwable) {}
        try { resultBitmap?.recycle() } catch (_: Throwable) {}
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
    outputFile.setReadable(true, false)
    return outputFile.absolutePath
}

fun promoteStagingToActive(context: Context, stagingPath: String, type: String): String {
    val ext = if (type == "gif") "gif" else if (type == "video") "mp4" else "jpg"
    val timestamp = System.currentTimeMillis()
    val activeFile = File(context.filesDir, "wallpaper_${timestamp}.$ext")
    val stagingFile = File(stagingPath)
    if (stagingFile.exists()) {
        stagingFile.copyTo(activeFile, overwrite = true)
        if (stagingFile.name.startsWith("staging_") || stagingFile.name.startsWith("download_temp_") || stagingFile.name.endsWith(".tmp")) {
            stagingFile.delete()
        }
    }
    activeFile.setReadable(true, false)

    cleanupOldActiveWallpapers(context, activeFile.name)

    val recentFile = File(context.filesDir, "recent_${timestamp}.$ext")
    activeFile.copyTo(recentFile, overwrite = true)
    recentFile.setReadable(true, false)
    addRecentWallpaper(context, recentFile.absolutePath, type)

    return activeFile.absolutePath
}

suspend fun trimVideoTo15s(context: Context, inputPath: String): String? = withContext(Dispatchers.IO) {
    var extractor: MediaExtractor? = null
    var muxer: MediaMuxer? = null
    var trimmedFile: File? = null
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inputPath)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000L
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        retriever.release()

        if (durationMs <= 15000L) {
            return@withContext inputPath
        }

        trimmedFile = File(context.filesDir, "trimmed_lock_${System.currentTimeMillis()}.mp4")
        extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        val trackCount = extractor.trackCount
        muxer = MediaMuxer(trimmedFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer.setOrientationHint(rotation)

        val indexMap = HashMap<Int, Int>()
        var maxInputSize = 1024 * 1024

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                val newIndex = muxer.addTrack(format)
                indexMap[i] = newIndex
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    if (size > maxInputSize) maxInputSize = size
                }
            }
        }

        muxer.start()

        val buffer = ByteBuffer.allocate(maxInputSize)
        val bufferInfo = MediaCodec.BufferInfo()

        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        while (true) {
            val trackIndex = extractor.sampleTrackIndex
            if (trackIndex < 0) break

            val sampleTime = extractor.sampleTime
            if (sampleTime > 15_000_000L) break

            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            bufferInfo.presentationTimeUs = sampleTime
            val sampleFlags = extractor.sampleFlags
            var codecFlags = 0
            if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
            }
            bufferInfo.flags = codecFlags

            val muxerTrackIndex = indexMap[trackIndex]
            if (muxerTrackIndex != null) {
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            }

            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        muxer = null
        extractor.release()
        extractor = null

        trimmedFile.setReadable(true, false)
        trimmedFile.absolutePath
    } catch (e: Exception) {
        Log.e("AuroraPicker", "Video trim failed, deleting partial file", e)
        muxer?.release()
        extractor?.release()
        trimmedFile?.delete()
        null
    }
}

fun exportVideoToMediaStore(context: Context, videoPath: String): Uri? {
    return try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "Aurora_lock_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Aurora")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(videoPath).use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        uri
    } catch (e: Exception) {
        Log.e("AuroraPicker", "Failed to export video to MediaStore", e)
        null
    }
}

fun launchSamsungVideoLockEditor(context: Context, contentUri: Uri): Boolean {
    // Launch Order:
    // (a) Intent(Intent.ACTION_SET_WALLPAPER) with SetAsWallPaperActivity
    try {
        val intentA = Intent(Intent.ACTION_SET_WALLPAPER).apply {
            setDataAndType(contentUri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            setComponent(ComponentName("com.samsung.android.app.dressroom", "com.samsung.android.app.dressroom.presentation.SetAsWallPaperActivity"))
        }
        context.startActivity(intentA)
        Log.d("AuroraPicker", "Successfully launched (a) SetAsWallPaperActivity")
        return true
    } catch (eA: Exception) {
        Log.w("AuroraPicker", "Launch (a) SetAsWallPaperActivity failed, trying (b) SHOW_VIDEO_WALLPAPER_EDITOR", eA)
    }

    // (b) Intent("com.samsung.intent.action.SHOW_VIDEO_WALLPAPER_EDITOR") with EditMainActivity
    try {
        val intentB = Intent("com.samsung.intent.action.SHOW_VIDEO_WALLPAPER_EDITOR").apply {
            setDataAndType(contentUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            setComponent(ComponentName("com.samsung.android.app.dressroom", "com.samsung.android.app.dressroom.presentation.preview.EditMainActivity"))
        }
        context.startActivity(intentB)
        Log.d("AuroraPicker", "Successfully launched (b) EditMainActivity")
        return true
    } catch (eB: Exception) {
        Log.w("AuroraPicker", "Launch (b) EditMainActivity failed, trying (c) Gallery fallback", eB)
    }

    // (c) Gallery with instruction dialog
    try {
        val intentC = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "video/*")
            setPackage("com.sec.android.gallery3d")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intentC)
        Toast.makeText(context, "In Gallery tap the three dots > Set as wallpaper > Lock screen.", Toast.LENGTH_LONG).show()
        Log.d("AuroraPicker", "Successfully launched (c) Gallery")
        return true
    } catch (eC: Exception) {
        Log.e("AuroraPicker", "All Samsung video lock launches failed", eC)
        return false
    }
}

fun cleanupOldActiveWallpapers(context: Context, currentFileName: String) {
    try {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("wallpaper_") && file.name != currentFileName) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        Log.w("Aurora", "Error cleaning old active wallpapers", e)
    }
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

fun saveWallpaperMediaPath(
    context: Context,
    path: String,
    type: String,
    scalingMode: String = com.niranjan.auroralivepaper.data.SettingsPrefs.scalingMode(context)
) {
    val version = System.currentTimeMillis()
    val prefs = context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("media_path", path)
        .putString("media_type", type)
        .putString("scaling_mode", scalingMode)
        .putLong("media_version", version)
        .apply()

    com.niranjan.auroralivepaper.data.SettingsPrefs.setScalingMode(context, scalingMode)

    try {
        val reloadIntent = Intent(com.niranjan.auroralivepaper.AuroraWallpaperService.ACTION_RELOAD_WALLPAPER).apply {
            setPackage(context.packageName)
            putExtra("media_path", path)
            putExtra("media_type", type)
            putExtra("scaling_mode", scalingMode)
            putExtra("media_version", version)
        }
        context.sendBroadcast(reloadIntent)
    } catch (e: Exception) {
        Log.w("AuroraPicker", "Failed to send reload broadcast", e)
    }
}

fun applyLockScreenFromVideo(context: Context, videoPath: String, seconds: Float = 1f): Boolean {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 1000L

        val percentages = listOf(0.50f, 0.35f, 0.65f, 0.20f, 0.80f, 0.10f)
        var bestFrame: Bitmap? = null
        var maxLuma = -1f

        for (pct in percentages) {
            val targetMs = (durationMs * pct).toLong().coerceIn(10L, durationMs)
            val micros = targetMs * 1000L
            val frame = retriever.getFrameAtTime(micros, MediaMetadataRetriever.OPTION_CLOSEST)
            if (frame != null) {
                val scaled16 = Bitmap.createScaledBitmap(frame, 16, 16, true)
                var totalLuma = 0f
                for (x in 0 until 16) {
                    for (y in 0 until 16) {
                        val px = scaled16.getPixel(x, y)
                        val r = (px shr 16) and 0xFF
                        val g = (px shr 8) and 0xFF
                        val b = px and 0xFF
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        totalLuma += luma
                    }
                }
                scaled16.recycle()
                val avgLuma = totalLuma / 256f
                Log.d("AuroraPicker", "Frame at ${(pct * 100).toInt()}% (${targetMs}ms): luma=$avgLuma")

                if (avgLuma > maxLuma) {
                    maxLuma = avgLuma
                    bestFrame = frame
                }

                if (avgLuma > 25f) {
                    break
                }
            }
        }
        retriever.release()

        if (bestFrame != null) {
            val softwareBitmap = if (bestFrame.config == Bitmap.Config.HARDWARE) {
                bestFrame.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bestFrame
            }

            val metrics = context.resources.displayMetrics
            val screenW = metrics.widthPixels
            val screenH = metrics.heightPixels

            val srcW = softwareBitmap.width
            val srcH = softwareBitmap.height

            val scale = maxOf(screenW.toFloat() / srcW, screenH.toFloat() / srcH)
            val scaledW = (srcW * scale).toInt()
            val scaledH = (srcH * scale).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(softwareBitmap, scaledW, scaledH, true)
            val cropX = ((scaledW - screenW) / 2).coerceAtLeast(0)
            val cropY = ((scaledH - screenH) / 2).coerceAtLeast(0)

            val finalCropped = Bitmap.createBitmap(scaledBitmap, cropX, cropY, screenW, screenH)

            val success = try {
                WallpaperManager.getInstance(context)
                    .setBitmap(finalCropped, null, true, WallpaperManager.FLAG_LOCK)
                true
            } catch (e: Exception) {
                Log.e("AuroraPicker", "WallpaperManager.setBitmap failed", e)
                false
            }

            Log.d("AuroraPicker", "applyLockScreenFromVideo result=$success, maxLuma=$maxLuma")
            success
        } else false
    } catch (e: Exception) {
        Log.e("AuroraPicker", "applyLockScreenFromVideo failed", e)
        false
    }
}

fun applyLockScreenFromGif(context: Context, gifPath: String): Boolean {
    var gifBitmap: Bitmap? = null
    var resultBitmap: Bitmap? = null
    return try {
        val movie = FileInputStream(gifPath).use { stream ->
            Movie.decodeStream(stream)
        } ?: return false

        val gifW = if (movie.width() > 0) movie.width() else 720
        val gifH = if (movie.height() > 0) movie.height() else 1280
        val dm = context.resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels

        gifBitmap = Bitmap.createBitmap(gifW, gifH, Bitmap.Config.ARGB_8888)
        val gifCanvas = Canvas(gifBitmap)
        movie.setTime(0)
        movie.draw(gifCanvas, 0f, 0f)

        resultBitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(resultBitmap)
        resultCanvas.drawColor(android.graphics.Color.BLACK)

        val scale = maxOf(screenW.toFloat() / gifW, screenH.toFloat() / gifH)
        val dx = (screenW - gifW * scale) / 2f
        val dy = (screenH - gifH * scale) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        resultCanvas.drawBitmap(gifBitmap, matrix, null)

        WallpaperManager.getInstance(context)
            .setBitmap(resultBitmap, null, true, WallpaperManager.FLAG_LOCK)
        true
    } catch (t: Throwable) {
        Log.e("AuroraPicker", "applyLockScreenFromGif failed", t)
        false
    } finally {
        try { gifBitmap?.recycle() } catch (_: Throwable) {}
        try { resultBitmap?.recycle() } catch (_: Throwable) {}
    }
}
