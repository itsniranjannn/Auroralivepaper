package com.niranjan.auroralivepaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.niranjan.auroralivepaper.ui.picker.WallpaperPickerScreen
import com.niranjan.auroralivepaper.ui.preview.PreviewScreen
import com.niranjan.auroralivepaper.ui.splash.AuroraIntroScreen
import com.niranjan.auroralivepaper.ui.theme.AuroraLivePaperTheme
import android.net.Uri
import com.niranjan.auroralivepaper.ui.picker.applyLockScreenFromGif
import com.niranjan.auroralivepaper.ui.picker.applyLockScreenFromVideo
import com.niranjan.auroralivepaper.ui.picker.launchLiveWallpaperPicker
import com.niranjan.auroralivepaper.ui.picker.saveWallpaperMediaPath
import androidx.media3.common.util.UnstableApi

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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

                    composable("home") {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            Box(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                WallpaperPickerScreen(
                                    onPreview = { path, type ->
                                        navController.navigate("preview/${type}?path=${Uri.encode(path)}")
                                    }
                                )
                            }
                        }
                    }

                    composable(
                        route = "preview/{type}?path={path}",
                        arguments = listOf(
                            navArgument("type") { type = NavType.StringType },
                            navArgument("path") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        val type = backStackEntry.arguments?.getString("type") ?: "video"
                        val path = backStackEntry.arguments?.getString("path")

                        PreviewScreen(
                            mediaPath = path,
                            mediaType = type,
                            onSetHome = {
                                path?.let { saveWallpaperMediaPath(this@MainActivity, it, type) }
                                launchLiveWallpaperPicker(this@MainActivity)
                                navController.popBackStack()
                            },
                            onSetLock = {
                                path?.let {
                                    if (type == "gif") applyLockScreenFromGif(this@MainActivity, it)
                                    else applyLockScreenFromVideo(this@MainActivity, it, 1f)
                                }
                                navController.popBackStack()
                            },
                            onSetBoth = {
                                path?.let {
                                    saveWallpaperMediaPath(this@MainActivity, it, type)
                                    if (type == "gif") applyLockScreenFromGif(this@MainActivity, it)
                                    else applyLockScreenFromVideo(this@MainActivity, it, 1f)
                                }
                                launchLiveWallpaperPicker(this@MainActivity)
                                navController.popBackStack()
                            },
                            onDismiss = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}