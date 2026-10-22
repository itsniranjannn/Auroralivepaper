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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import kotlinx.coroutines.launch
import java.io.File

@UnstableApi
class MainActivity : ComponentActivity() {

    private var checkedPickerReturn = false
    private var pendingLockStillFallback = false
    private var pendingLockPath: String? = null
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
                NavHost(navController = navController, startDestination = "intro") {

                    composable("intro") {
                        AuroraIntroScreen(onFinished = {
                            navController.navigate("home") {
                                popUpTo("intro") { inclusive = true }
                            }
                        })
                    }

                    composable(
                        "home",
                        enterTransition = {
                            fadeIn(animationSpec = tween(400)) + slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(400)
                            )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        val haptic = LocalHapticFeedback.current
                        LaunchedEffect(Unit) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                            onSetHome = {
                                path?.let { stagingPath ->
                                    val activePath = promoteStagingToActive(this@MainActivity, stagingPath, type)
                                    val activeFile = File(activePath)
                                    if (!activeFile.exists() || !activeFile.canRead()) {
                                        Toast.makeText(this@MainActivity, "Error: Media file not readable", Toast.LENGTH_SHORT).show()
                                        return@let
                                    }
                                    if (type == "image") {
                                        val ok = applyImageWallpaper(this@MainActivity, activePath, WallpaperManager.FLAG_SYSTEM)
                                        Toast.makeText(this@MainActivity, if (ok) "Home wallpaper set!" else "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                                    } else {
                                        saveWallpaperMediaPath(this@MainActivity, activePath, type)
                                        val wm = WallpaperManager.getInstance(this@MainActivity)
                                        val info = wm.wallpaperInfo
                                        val isOurServiceActive = info != null &&
                                                info.packageName == packageName &&
                                                info.serviceName == AuroraWallpaperService::class.java.name

                                        if (isOurServiceActive) {
                                            Toast.makeText(this@MainActivity, "Video changed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            checkedPickerReturn = true
                                            launchLiveWallpaperPicker(this@MainActivity)
                                        }
                                    }
                                }
                                navController.popBackStack("home", false)
                            },
                            onSetLock = {
                                path?.let { stagingPath ->
                                    val activePath = promoteStagingToActive(this@MainActivity, stagingPath, type)
                                    val activeFile = File(activePath)
                                    if (!activeFile.exists() || !activeFile.canRead()) {
                                        Toast.makeText(this@MainActivity, "Error: Media file not readable", Toast.LENGTH_SHORT).show()
                                        return@let
                                    }
                                    if (type == "image") {
                                        val ok = applyImageWallpaper(this@MainActivity, activePath, WallpaperManager.FLAG_LOCK)
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
                                                .setMessage("Your phone may offer a lock screen option on the next screen. If it doesn't, the lock screen will get a still image.")
                                                .setPositiveButton("Continue") { _, _ ->
                                                    saveWallpaperMediaPath(this@MainActivity, activePath, type)
                                                    checkedPickerReturn = true
                                                    pendingLockStillFallback = true
                                                    pendingLockPath = activePath
                                                    launchLiveWallpaperPicker(this@MainActivity)
                                                }
                                                .setNegativeButton("Cancel", null)
                                                .show()
                                        }
                                    }
                                }
                                navController.popBackStack("home", false)
                            },
                            onSetBoth = {
                                path?.let { stagingPath ->
                                    val activePath = promoteStagingToActive(this@MainActivity, stagingPath, type)
                                    val activeFile = File(activePath)
                                    if (!activeFile.exists() || !activeFile.canRead()) {
                                        Toast.makeText(this@MainActivity, "Error: Media file not readable", Toast.LENGTH_SHORT).show()
                                        return@let
                                    }
                                    if (type == "image") {
                                        val ok = applyImageWallpaper(this@MainActivity, activePath, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                                        Toast.makeText(this@MainActivity, if (ok) "Wallpaper applied to Both!" else "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                                    } else {
                                        saveWallpaperMediaPath(this@MainActivity, activePath, type)
                                        checkedPickerReturn = true
                                        launchLiveWallpaperPicker(this@MainActivity)
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

    override fun onResume() {
        super.onResume()
        if (checkedPickerReturn) {
            checkedPickerReturn = false

            // Check 1: at 1500 ms
            mainHandler.postDelayed({
                try {
                    val wm = WallpaperManager.getInstance(this)
                    val info = wm.wallpaperInfo
                    val isOurServiceActive1 = info != null &&
                            info.packageName == packageName &&
                            info.serviceName == AuroraWallpaperService::class.java.name

                    Log.d("MainActivity", "Check 1 (1500ms): isOurServiceActive=$isOurServiceActive1, activeComponent=${info?.component}")

                    if (pendingLockStillFallback) {
                        pendingLockStillFallback = false
                        val fallbackPath = pendingLockPath
                        pendingLockPath = null

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val lockInfo = try {
                                val method = WallpaperManager::class.java.getMethod("getWallpaperInfo", Int::class.javaPrimitiveType)
                                method.invoke(wm, WallpaperManager.FLAG_LOCK) as? WallpaperInfo
                            } catch (e: Throwable) {
                                null
                            }
                            val isOurLockActive = lockInfo != null &&
                                    lockInfo.packageName == packageName &&
                                    lockInfo.serviceName == AuroraWallpaperService::class.java.name

                            if (!isOurLockActive && fallbackPath != null) {
                                val ok = applyLockScreenFromVideo(this, fallbackPath, 1f)
                                Toast.makeText(this, if (ok) "Lock screen set to a still image." else "Failed to set lock screen", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (!isOurServiceActive1 && fallbackPath != null) {
                                AlertDialog.Builder(this)
                                    .setTitle("Lock Screen Wallpaper")
                                    .setMessage("Apply a still image to the lock screen instead?")
                                    .setPositiveButton("Yes") { _, _ ->
                                        val ok = applyLockScreenFromVideo(this, fallbackPath, 1f)
                                        Toast.makeText(this, if (ok) "Lock screen set to a still image." else "Failed to set lock screen", Toast.LENGTH_SHORT).show()
                                    }
                                    .setNegativeButton("No", null)
                                    .show()
                            }
                        }
                    } else if (!isOurServiceActive1) {
                        // Check 2: re-check at 3000 ms
                        mainHandler.postDelayed({
                            try {
                                val info2 = wm.wallpaperInfo
                                val isOurServiceActive2 = info2 != null &&
                                        info2.packageName == packageName &&
                                        info2.serviceName == AuroraWallpaperService::class.java.name

                                Log.d("MainActivity", "Check 2 (3000ms): isOurServiceActive=$isOurServiceActive2, activeComponent=${info2?.component}")
                            } catch (e2: Exception) {
                                Log.e("MainActivity", "Error in Check 2", e2)
                            }
                        }, 1500)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in Check 1", e)
                }
            }, 1500)
        }
    }
}
