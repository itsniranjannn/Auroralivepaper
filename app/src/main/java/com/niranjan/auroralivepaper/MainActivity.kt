package com.niranjan.auroralivepaper

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.niranjan.auroralivepaper.ui.picker.WallpaperPickerScreen
import com.niranjan.auroralivepaper.ui.picker.applyLockScreenFromGif
import com.niranjan.auroralivepaper.ui.picker.applyLockScreenFromVideo
import com.niranjan.auroralivepaper.ui.picker.launchLiveWallpaperPicker
import com.niranjan.auroralivepaper.ui.picker.saveWallpaperMediaPath
import com.niranjan.auroralivepaper.ui.preview.PreviewScreen
import com.niranjan.auroralivepaper.ui.splash.AuroraIntroScreen
import com.niranjan.auroralivepaper.ui.theme.AuroraLivePaperTheme
import com.niranjan.auroralivepaper.ui.settings.SettingsScreen
import com.niranjan.auroralivepaper.ui.settings.WallpaperSettingsScreen
import com.niranjan.auroralivepaper.ui.settings.BatterySettingsScreen
import com.niranjan.auroralivepaper.ui.settings.StorageSettingsScreen
import com.niranjan.auroralivepaper.ui.settings.ExperienceSettingsScreen
import com.niranjan.auroralivepaper.ui.settings.AboutScreen

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
