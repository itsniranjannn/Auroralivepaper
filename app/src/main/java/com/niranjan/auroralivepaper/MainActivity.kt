package com.niranjan.auroralivepaper

import android.Manifest
import android.app.AlertDialog
import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.ui.components.AuroraLivingBackground
import com.niranjan.auroralivepaper.ui.picker.WallpaperPickerScreen
import com.niranjan.auroralivepaper.ui.picker.applyImageWallpaper
import com.niranjan.auroralivepaper.ui.picker.applyLockScreenFromGif
import com.niranjan.auroralivepaper.ui.picker.applyLockScreenFromVideo
import com.niranjan.auroralivepaper.ui.picker.cleanupStagingFiles
import com.niranjan.auroralivepaper.ui.picker.exportVideoToMediaStore
import com.niranjan.auroralivepaper.ui.picker.isSamsungVideoLockAvailable
import com.niranjan.auroralivepaper.ui.picker.launchLiveWallpaperPicker
import com.niranjan.auroralivepaper.ui.picker.launchSamsungVideoLockEditor
import com.niranjan.auroralivepaper.ui.picker.promoteStagingToActive
import com.niranjan.auroralivepaper.ui.picker.saveWallpaperMediaPath
import com.niranjan.auroralivepaper.ui.picker.isAuroraWallpaperActive
import com.niranjan.auroralivepaper.ui.picker.trimVideoTo15s
import com.niranjan.auroralivepaper.ui.preview.PreviewScreen
import com.niranjan.auroralivepaper.ui.splash.AuroraIntroScreen
import com.niranjan.auroralivepaper.ui.theme.AuroraLivePaperTheme
import com.niranjan.auroralivepaper.ui.settings.SettingsScreen
import com.niranjan.auroralivepaper.ui.settings.WallpaperSettingsScreen
import com.niranjan.auroralivepaper.ui.settings.BatterySettingsScreen
import com.niranjan.auroralivepaper.ui.settings.StorageSettingsScreen
import com.niranjan.auroralivepaper.ui.settings.ExperienceSettingsScreen
import com.niranjan.auroralivepaper.ui.settings.AboutScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
class MainActivity : ComponentActivity() {

    private var checkedPickerReturn = false
    private var pendingLockExportAction: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingLockExportAction?.invoke()
        } else {
            Toast.makeText(this, "Storage permission required to export video for lock screen", Toast.LENGTH_SHORT).show()
        }
        pendingLockExportAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        cleanupStagingFiles(this)
        enableEdgeToEdge()
        setContent {
            AuroraLivePaperTheme {
                val navController = rememberNavController()
                val transitionBoost = remember { Animatable(1.0f) }
                val scope = rememberCoroutineScope()

                AuroraLivingBackground(transitionBoost = transitionBoost.value) {
                    NavHost(
                        navController = navController,
                        startDestination = "intro",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("intro") {
                            AuroraIntroScreen(
                                onTransitionStart = {
                                    scope.launch {
                                        transitionBoost.animateTo(2.5f, tween(150, easing = FastOutSlowInEasing))
                                        transitionBoost.animateTo(1.0f, tween(500, easing = FastOutSlowInEasing))
                                    }
                                },
                                onFinished = {
                                    navController.navigate("home") {
                                        popUpTo("intro") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            "home",
                            enterTransition = {
                                fadeIn(animationSpec = tween(400)) + slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(400)
                                )
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(300)
                                )
                            }
                        ) {
                            val haptic = LocalHapticFeedback.current
                            LaunchedEffect(Unit) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = Color.Transparent
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    WallpaperPickerScreen(
                                        onPreview = { path, type ->
                                            navController.navigate("preview/${type}?path=${Uri.encode(path)}")
                                        },
                                        onOpenSettings = {
                                            navController.navigate("settings")
                                        }
                                    )
                                }
                            }
                        }

                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { navController.navigate(it) }
                        )
                    }

                    composable("settings_wallpaper") {
                        WallpaperSettingsScreen(onBack = { navController.popBackStack() })
                    }
                    composable("settings_battery") {
                        BatterySettingsScreen(onBack = { navController.popBackStack() })
                    }
                    composable("settings_storage") {
                        StorageSettingsScreen(onBack = { navController.popBackStack() })
                    }
                    composable("settings_experience") {
                        ExperienceSettingsScreen(onBack = { navController.popBackStack() })
                    }
                    composable("settings_about") {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }

                    composable(
                        route = "preview/{type}?path={path}",
                        arguments = listOf(
                            navArgument("type") { type = NavType.StringType },
                            navArgument("path") { type = NavType.StringType; nullable = true }
                        ),
                        enterTransition = {
                            fadeIn(animationSpec = tween(400)) + slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(400)
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300)
                            )
                        }
                    ) { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: "video"
                        val path = backStackEntry.arguments?.getString("path")

                        PreviewScreen(
                            mediaPath = path,
                            mediaType = type,
                            onSetHome = { scalingMode ->
                                path?.let { stagingPath ->
                                    val activePath = promoteStagingToActive(this@MainActivity, stagingPath, type)
                                    val activeFile = File(activePath)
                                    if (!activeFile.exists() || !activeFile.canRead()) {
                                        Toast.makeText(this@MainActivity, "Error: Media file not readable", Toast.LENGTH_SHORT).show()
                                        return@let
                                    }
                                    if (type == "image") {
                                        val ok = applyImageWallpaper(this@MainActivity, activePath, WallpaperManager.FLAG_SYSTEM, scalingMode)
                                        Toast.makeText(this@MainActivity, if (ok) "Home wallpaper set!" else "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                                    } else {
                                        saveWallpaperMediaPath(this@MainActivity, activePath, type, scalingMode)
                                        val isOurServiceActive = isAuroraWallpaperActive(this@MainActivity)

                                        if (isOurServiceActive) {
                                            Toast.makeText(this@MainActivity, "Wallpaper updated!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            checkedPickerReturn = true
                                            launchLiveWallpaperPicker(this@MainActivity)
                                        }
                                    }
                                }
                                navController.popBackStack("home", false)
                            },
                            onSetLock = { scalingMode ->
                                path?.let { stagingPath ->
                                    val activePath = promoteStagingToActive(this@MainActivity, stagingPath, type)
                                    val activeFile = File(activePath)
                                    if (!activeFile.exists() || !activeFile.canRead()) {
                                        Toast.makeText(this@MainActivity, "Error: Media file not readable", Toast.LENGTH_SHORT).show()
                                        return@let
                                    }
                                    if (type == "image") {
                                        val ok = applyImageWallpaper(this@MainActivity, activePath, WallpaperManager.FLAG_LOCK, scalingMode)
                                        Toast.makeText(this@MainActivity, if (ok) "Lock wallpaper set!" else "Failed", Toast.LENGTH_SHORT).show()
                                    } else if (type == "gif") {
                                        val ok = applyLockScreenFromGif(this@MainActivity, activePath)
                                        Toast.makeText(this@MainActivity, if (ok) "GIFs can't play on the lock screen. Use Set as Both for live." else "Failed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (isSamsungVideoLockAvailable(this@MainActivity)) {
                                            val performLockExport = {
                                                lifecycleScope.launch {
                                                    Toast.makeText(this@MainActivity, "Preparing video for Lock screen...", Toast.LENGTH_SHORT).show()
                                                    var trimmedPath: String? = null
                                                    try {
                                                        trimmedPath = trimVideoTo15s(this@MainActivity, activePath)
                                                    } catch (e: Exception) {
                                                        Log.e("MainActivity", "Video trimming failed", e)
                                                    }

                                                    if (trimmedPath.isNullOrBlank()) {
                                                        Toast.makeText(this@MainActivity, "Could not trim the video", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }

                                                    val contentUri = exportVideoToMediaStore(this@MainActivity, trimmedPath)
                                                    if (contentUri != null) {
                                                        val launched = launchSamsungVideoLockEditor(this@MainActivity, contentUri)
                                                        if (!launched) {
                                                            Toast.makeText(this@MainActivity, "Could not launch video lock editor", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "Could not export video to MediaStore", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }

                                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                pendingLockExportAction = { performLockExport() }
                                                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                            } else {
                                                performLockExport()
                                            }
                                        } else {
                                            AlertDialog.Builder(this@MainActivity)
                                                .setTitle("Lock Screen Setup")
                                                .setMessage("To set this video on your lock screen, choose 'Home screen and lock screen' in the live wallpaper picker.")
                                                .setPositiveButton("Continue") { _, _ ->
                                                    saveWallpaperMediaPath(this@MainActivity, activePath, type, scalingMode)
                                                    checkedPickerReturn = true
                                                    launchLiveWallpaperPicker(this@MainActivity)
                                                }
                                                .setNegativeButton("Cancel", null)
                                                .show()
                                        }
                                    }
                                }
                                navController.popBackStack("home", false)
                            },
                            onSetBoth = { scalingMode ->
                                path?.let { stagingPath ->
                                    val activePath = promoteStagingToActive(this@MainActivity, stagingPath, type)
                                    val activeFile = File(activePath)
                                    if (!activeFile.exists() || !activeFile.canRead()) {
                                        Toast.makeText(this@MainActivity, "Error: Media file not readable", Toast.LENGTH_SHORT).show()
                                        return@let
                                    }
                                    if (type == "image") {
                                        val ok = applyImageWallpaper(this@MainActivity, activePath, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, scalingMode)
                                        Toast.makeText(this@MainActivity, if (ok) "Wallpaper applied to Both!" else "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                                    } else {
                                        saveWallpaperMediaPath(this@MainActivity, activePath, type, scalingMode)
                                        val isOurServiceActive = isAuroraWallpaperActive(this@MainActivity)
                                        if (isOurServiceActive) {
                                            Toast.makeText(this@MainActivity, "Wallpaper updated for Both!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            checkedPickerReturn = true
                                            launchLiveWallpaperPicker(this@MainActivity)
                                        }
                                    }
                                }
                                navController.popBackStack("home", false)
                            },
                            onDismiss = {
                                cleanupStagingFiles(this@MainActivity)
                                navController.popBackStack("home", false)
                            }
                        )
                    }
                }
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        checkedPickerReturn = false
    }
}
