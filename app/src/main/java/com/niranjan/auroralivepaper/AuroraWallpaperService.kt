package com.niranjan.auroralivepaper

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.niranjan.auroralivepaper.data.AuroraDebugLogger
import com.niranjan.auroralivepaper.data.SettingsPrefs
import com.niranjan.auroralivepaper.util.RecoveryLimiter
import java.io.File
import java.io.FileInputStream

class AuroraWallpaperService : WallpaperService() {

    companion object {
        const val ACTION_RELOAD_WALLPAPER = "com.niranjan.auroralivepaper.RELOAD_WALLPAPER"
    }

    override fun onCreateEngine(): Engine {
        return try {
            Log.d("AuroraEngine", "Service.onCreateEngine called")
            AuroraEngine()
        } catch (t: Throwable) {
            Log.e("AuroraEngine", "Error in onCreateEngine", t)
            AuroraEngine()
        }
    }

    inner class AuroraEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isPrepared = false
        private var gifMovie: Movie? = null
        private var gifStartTime: Long = 0
        private val gifHandler = Handler(Looper.getMainLooper())
        private var gifRunning = false
        private var currentHolder: SurfaceHolder? = null

        private var isVisibleState = false
        private var isPowerSave = false
        private var isBatteryLow = false
        private var currentBatteryPct = -1
        private var receiverRegistered = false
        private var isUserUnlockedToHome = false

        private var currentLoadedPath: String? = null
        private var currentLoadedVersion: Long = 0L
        private var currentLoadedScalingMode: String? = null
        private var retryCount = 0
        private val MAX_RETRIES = 3

        private var reloadOnNextVisible = false
        private var lastPauseReason: String? = null

        private val recoveryLimiter = RecoveryLimiter()
        private var lastUnlockPollerErrorTime = 0L
        private var pendingReloadRunnable: Runnable? = null

        private val settingsPrefs by lazy {
            applicationContext.getSharedPreferences("aurora_settings_prefs", MODE_PRIVATE)
        }
        private val mediaPrefs by lazy {
            applicationContext.getSharedPreferences("aurora_prefs", MODE_PRIVATE)
        }

        private fun recoverPlayer(reason: String) {
            when (val res = recoveryLimiter.attemptRecovery(SystemClock.uptimeMillis())) {
                is RecoveryLimiter.Result.AlreadyScheduled -> {
                    Log.d("AuroraEngine", "recoverPlayer skipped: recovery already scheduled")
                    return
                }
                is RecoveryLimiter.Result.Suppressed -> {
                    AuroraDebugLogger.log(applicationContext, "RECOVERY: suppressed")
                    return
                }
                is RecoveryLimiter.Result.Scheduled -> {
                    val delayMs = res.delayMs
                    AuroraDebugLogger.log(applicationContext, "RECOVERY: recoverPlayer (reason=$reason, delay=${delayMs}ms)")
                    releasePlayer()
                    retryCount = 0

                    gifHandler.postDelayed({
                        recoveryLimiter.onRecoveryDispatched()
                        currentHolder?.let { holder ->
                            if (holder.surface.isValid) {
                                loadWallpaper(holder)
                            }
                        }
                    }, delayMs)
                }
            }
        }

        private fun scheduleReload(reason: String) {
            pendingReloadRunnable?.let { gifHandler.removeCallbacks(it) }
            val runnable = Runnable {
                try {
                    pendingReloadRunnable = null
                    AuroraDebugLogger.log(applicationContext, "RELOAD: scheduled reload firing (reason=$reason)")
                    recoveryLimiter.reset()
                    currentLoadedPath = null
                    currentLoadedVersion = -1L
                    currentLoadedScalingMode = null
                    retryCount = 0
                    releasePlayer()
                    currentHolder?.let { holder ->
                        if (holder.surface.isValid) {
                            loadWallpaper(holder)
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("AuroraEngine", "Error in scheduleReload runnable", e)
                }
            }
            pendingReloadRunnable = runnable
            gifHandler.postDelayed(runnable, 300L)
        }

        private val reload60sRunnable = Runnable {
            if (isVisibleState && !shouldPause()) {
                recoverPlayer("60s timer post-MAX_RETRIES while visible")
            }
        }

        private val unlockPollerRunnable = object : Runnable {
            override fun run() {
                try {
                    if (isVisibleState && !isPreview) {
                        updatePlayerSettings(allowRecovery = false)
                        if (isDeviceLocked()) {
                            gifHandler.postDelayed(this, 800)
                        }
                    }
                } catch (e: Throwable) {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastUnlockPollerErrorTime > 30_000L) {
                        lastUnlockPollerErrorTime = now
                        AuroraDebugLogger.log(applicationContext, "ERROR: unlockPoller updatePlayerSettings error: ${e.message}")
                        Log.e("AuroraEngine", "Error in unlockPollerRunnable", e)
                    }
                }
            }
        }

        private fun getScalingMode(): String {
            return SettingsPrefs.scalingMode(applicationContext)
        }

        private fun applyVideoScalingMode(mp: MediaPlayer) {
            try {
                val mode = getScalingMode()
                if (mode == "fit") {
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                } else {
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                }
                Log.d("AuroraEngine", "Applied video scaling mode: $mode")
            } catch (e: Throwable) {
                Log.w("AuroraEngine", "Failed to apply video scaling mode: ${e.message}")
            }
        }

        private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            try {
                updatePlayerSettings()
                refreshPowerState()
                updatePlaybackState()
                if (key == "scaling_mode") {
                    val newScaling = getScalingMode()
                    if (newScaling != currentLoadedScalingMode) {
                        currentLoadedScalingMode = newScaling
                        if (mediaPlayer != null && isPrepared) {
                            mediaPlayer?.let { applyVideoScalingMode(it) }
                        }
                    }
                }
            } catch (e: Throwable) {
                recoverPlayer("settingsListener error: ${e.message}")
            }
        }

        private val mediaListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            try {
                if (key == "media_version" || key == "media_path" || key == "media_type") {
                    val savedPath = mediaPrefs.getString("media_path", null)
                    Log.d("AuroraEngine", "media preference changed ($key): reload media from path=$savedPath")
                    scheduleReload("mediaListener key=$key")
                }
            } catch (e: Throwable) {
                recoverPlayer("mediaListener error: ${e.message}")
            }
        }

        private val systemReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    Log.d("AuroraEngine", "System broadcast received: ${intent.action}")
                    when (intent.action) {
                        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                            val pm = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager
                            isPowerSave = pm?.isPowerSaveMode == true
                            updatePlaybackState()
                        }
                        Intent.ACTION_BATTERY_CHANGED -> {
                            updateBatteryLow(intent)
                            updatePlaybackState()
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            // User locked or screen turned off -> strictly mute and flag as not unlocked
                            isUserUnlockedToHome = false
                            try {
                                mediaPlayer?.setVolume(0f, 0f)
                            } catch (_: Throwable) {}
                            AuroraDebugLogger.log(applicationContext, "PAUSE: screen off")
                            updatePlaybackState()
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            // Screen woke up at lock screen -> strictly mute, user is not present yet
                            isUserUnlockedToHome = false
                            try {
                                mediaPlayer?.setVolume(0f, 0f)
                            } catch (_: Throwable) {}
                            updatePlayerSettings()
                            updatePlaybackState()
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            // Device unlocked through keyguard to Home screen -> restore audio if enabled
                            Log.d("AuroraEngine", "ACTION_USER_PRESENT: Unlocked to Home screen")
                            isUserUnlockedToHome = true
                            updatePlayerSettings()
                            gifHandler.postDelayed({ updatePlayerSettings() }, 200)
                            gifHandler.postDelayed({ updatePlayerSettings() }, 500)
                            updatePlaybackState()
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("AuroraEngine", "Error in systemReceiver.onReceive", e)
                }
            }
        }

        private val reloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    if (intent.action == ACTION_RELOAD_WALLPAPER) {
                        if (!intent.hasExtra("media_path")) {
                            // Scaling-only broadcast from SettingsPrefs.setScalingMode
                            Log.d("AuroraEngine", "ACTION_RELOAD_WALLPAPER: scaling-only")
                            val newScaling = getScalingMode()
                            currentLoadedScalingMode = newScaling
                            if (mediaPlayer != null && isPrepared) {
                                mediaPlayer?.let { applyVideoScalingMode(it) }
                            }
                            if (gifMovie == null && mediaPlayer == null) {
                                currentHolder?.let { holder ->
                                    if (holder.surface.isValid) {
                                        val savedPath = mediaPrefs.getString("media_path", null)
                                        if (savedPath != null && File(savedPath).exists()) {
                                            val mediaType = mediaPrefs.getString("media_type", "video")
                                            if (mediaType == "image") {
                                                drawImage(savedPath, holder)
                                            }
                                        }
                                    }
                                }
                            }
                            AuroraDebugLogger.log(applicationContext, "SCALING: applied scaling-only change to $newScaling")
                        } else {
                            // Full reload broadcast with media_path
                            Log.d("AuroraEngine", "ACTION_RELOAD_WALLPAPER: full reload")
                            scheduleReload("reloadReceiver full broadcast")
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("AuroraEngine", "Error in reloadReceiver.onReceive", e)
                }
            }
        }

        private val gifDrawRunnable = object : Runnable {
            override fun run() {
                try {
                    if (!gifRunning) return
                    drawGifFrame()
                    if (gifRunning) gifHandler.postDelayed(this, 33) // ~30fps
                } catch (e: Throwable) {
                    Log.e("AuroraEngine", "Error in gifDrawRunnable", e)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            try {
                super.onCreate(surfaceHolder)
                Log.d("AuroraEngine", "Engine.onCreate: isPreview=$isPreview")
                val km = applicationContext.getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
                val pm = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager
                val isLocked = km?.isKeyguardLocked == true || km?.inKeyguardRestrictedInputMode() == true
                val isScreenInteractive = pm?.isInteractive == true
                isUserUnlockedToHome = !isLocked && isScreenInteractive

                registerReceivers()
                settingsPrefs.registerOnSharedPreferenceChangeListener(settingsListener)
                mediaPrefs.registerOnSharedPreferenceChangeListener(mediaListener)
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in Engine.onCreate", e)
            }
        }

        override fun onDestroy() {
            try {
                super.onDestroy()
                Log.d("AuroraEngine", "Engine.onDestroy: isPreview=$isPreview")
                gifHandler.removeCallbacksAndMessages(null)
                recoveryLimiter.reset()
                unregisterReceivers()
                settingsPrefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
                mediaPrefs.unregisterOnSharedPreferenceChangeListener(mediaListener)
                releasePlayer()
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in Engine.onDestroy", e)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            try {
                super.onSurfaceCreated(holder)
                Log.d("AuroraEngine", "onSurfaceCreated: surfaceValid=${holder.surface.isValid}, isPreview=$isPreview")
                currentHolder = holder
                AuroraDebugLogger.log(applicationContext, "SURFACE: onSurfaceCreated (surfaceValid=${holder.surface.isValid}, isPreview=$isPreview)")

                val savedPath = mediaPrefs.getString("media_path", null)
                val savedVersion = mediaPrefs.getLong("media_version", 0L)
                val savedScaling = getScalingMode()
                val needsReload = (savedPath != currentLoadedPath) ||
                        (savedVersion != currentLoadedVersion) ||
                        (savedScaling != currentLoadedScalingMode) ||
                        (mediaPlayer == null && gifMovie == null)

                if (needsReload) {
                    loadWallpaper(holder)
                }
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in onSurfaceCreated", e)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            try {
                super.onSurfaceChanged(holder, format, width, height)
                Log.d("AuroraEngine", "onSurfaceChanged: ${width}x${height}, isPreview=$isPreview")
                currentHolder = holder
                if (holder.surface.isValid) {
                    if (mediaPlayer != null) {
                        try {
                            mediaPlayer?.setSurface(holder.surface)
                            mediaPlayer?.let { applyVideoScalingMode(it) }
                        } catch (e: Throwable) {
                            recoverPlayer("onSurfaceChanged setSurface: ${e.message}")
                        }
                    } else if (gifMovie == null) {
                        loadWallpaper(holder)
                    }
                }
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in onSurfaceChanged", e)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            try {
                super.onVisibilityChanged(visible)
                Log.d("AuroraEngine", "onVisibilityChanged: visible=$visible, isPreview=$isPreview")
                isVisibleState = visible
                if (visible) {
                    recoveryLimiter.reset()

                    val km = applicationContext.getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
                    val isLocked = km?.isKeyguardLocked == true || km?.inKeyguardRestrictedInputMode() == true
                    if (isLocked) {
                        // Keyguard is up -> screen turned on at Lock screen. Keep audio strictly muted!
                        isUserUnlockedToHome = false
                        try { mediaPlayer?.setVolume(0f, 0f) } catch (_: Throwable) {}
                    }

                    refreshPowerState()
                    retryCount = 0 // Always reset retry count when visibility resumes

                    gifHandler.removeCallbacks(unlockPollerRunnable)
                    gifHandler.post(unlockPollerRunnable)
                    updatePlayerSettings()

                    // Recovery for dead players when returning to visibility:
                    // If mediaPlayer != null && isPrepared && !shouldPause() && !isPlaying, call start() and, if it fails, recoverPlayer
                    if (mediaPlayer != null && isPrepared && !shouldPause()) {
                        try {
                            val playing = mediaPlayer?.isPlaying == true
                            if (!playing) {
                                mediaPlayer?.start()
                                AuroraDebugLogger.log(applicationContext, "RESUME: playback resumed on visibility changed")
                            }
                        } catch (e: Throwable) {
                            recoverPlayer("onVisibilityChanged start failed: ${e.message}")
                        }
                    }

                    // After MAX_RETRIES in onError, do not stop forever: schedule one more reload when screen next becomes visible
                    if (reloadOnNextVisible) {
                        reloadOnNextVisible = false
                        gifHandler.removeCallbacks(reload60sRunnable)
                        recoverPlayer("screen became visible post-MAX_RETRIES")
                    } else {
                        val savedPath = mediaPrefs.getString("media_path", null)
                        val savedVersion = mediaPrefs.getLong("media_version", 0L)
                        val savedScaling = getScalingMode()
                        val needsReload = (savedPath != currentLoadedPath) ||
                                (savedVersion != currentLoadedVersion) ||
                                (savedScaling != currentLoadedScalingMode) ||
                                (mediaPlayer == null && gifMovie == null)

                        if (needsReload) {
                            currentHolder?.let { holder ->
                                if (holder.surface.isValid) {
                                    loadWallpaper(holder)
                                }
                            }
                        }
                    }
                } else {
                    gifHandler.removeCallbacks(unlockPollerRunnable)
                    try {
                        mediaPlayer?.setVolume(0f, 0f)
                    } catch (_: Throwable) {}
                }
                updatePlaybackState()
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in onVisibilityChanged", e)
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            try {
                super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in onOffsetsChanged", e)
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            try {
                super.onTouchEvent(event)
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in onTouchEvent", e)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            try {
                super.onSurfaceDestroyed(holder)
                Log.d("AuroraEngine", "onSurfaceDestroyed: isPreview=$isPreview")
                AuroraDebugLogger.log(applicationContext, "SURFACE: onSurfaceDestroyed (isPreview=$isPreview)")
                currentHolder = null
                releasePlayer()
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in onSurfaceDestroyed", e)
            }
        }

        private fun registerReceivers() {
            if (receiverRegistered) return
            try {
                val sysFilter = IntentFilter().apply {
                    addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
                applicationContext.registerReceiver(systemReceiver, sysFilter)

                val reloadFilter = IntentFilter(ACTION_RELOAD_WALLPAPER)
                ContextCompat.registerReceiver(
                    applicationContext, reloadReceiver, reloadFilter, ContextCompat.RECEIVER_NOT_EXPORTED
                )
                receiverRegistered = true
                Log.d("AuroraEngine", "Receivers successfully registered")
            } catch (e: Throwable) {
                Log.w("AuroraEngine", "Failed to register receivers", e)
            }
        }

        private fun unregisterReceivers() {
            if (!receiverRegistered) return
            try {
                applicationContext.unregisterReceiver(systemReceiver)
            } catch (_: Throwable) {}
            try {
                applicationContext.unregisterReceiver(reloadReceiver)
            } catch (_: Throwable) {}
            receiverRegistered = false
        }

        private fun refreshPowerState() {
            try {
                val pm = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager
                isPowerSave = pm?.isPowerSaveMode == true
                val sticky = applicationContext.registerReceiver(
                    null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                if (sticky != null) updateBatteryLow(sticky) else isBatteryLow = false
            } catch (e: Throwable) {
                Log.w("AuroraEngine", "Failed to get sticky battery intent", e)
                isBatteryLow = false
            }
        }

        private fun updateBatteryLow(intent: Intent) {
            try {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                if (level >= 0 && scale > 0) {
                    val pct = (level * 100) / scale
                    currentBatteryPct = pct
                    val threshold = SettingsPrefs.batteryThreshold(applicationContext)
                    val pauseBelow = SettingsPrefs.pauseBelowThreshold(applicationContext)
                    isBatteryLow = pauseBelow && !charging && pct <= threshold
                } else {
                    isBatteryLow = false
                }
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Error in updateBatteryLow", e)
            }
        }

        private fun isDeviceLocked(): Boolean {
            val km = applicationContext.getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            val isKmLocked = km?.isKeyguardLocked == true || km?.inKeyguardRestrictedInputMode() == true
            return isKmLocked || !isUserUnlockedToHome
        }

        private fun isAudioAllowed(): Boolean {
            if (isPreview) return false
            if (!isVisibleState) return false
            if (!SettingsPrefs.playWallpaperSound(applicationContext)) return false
            if (!isUserUnlockedToHome) return false

            val km = applicationContext.getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardLocked == true || km?.inKeyguardRestrictedInputMode() == true) {
                return false
            }
            return true
        }

        private fun updatePlayerSettings(allowRecovery: Boolean = true) {
            mediaPlayer?.let { mp ->
                try {
                    val canPlaySound = isAudioAllowed()
                    val vol = SettingsPrefs.wallpaperVolume(applicationContext)
                    val actualVol = if (canPlaySound) vol else 0f
                    mp.setVolume(actualVol, actualVol)
                    val loop = SettingsPrefs.loopAutomatically(applicationContext)
                    mp.isLooping = loop
                    applyVideoScalingMode(mp)
                } catch (e: Throwable) {
                    if (allowRecovery) {
                        recoverPlayer("updatePlayerSettings failed: ${e.message}")
                    } else {
                        throw e
                    }
                }
            }
        }

        private fun shouldPause(): Boolean {
            if (isPreview) return false
            if (!isVisibleState) return true
            if (SettingsPrefs.pauseOnBatterySaver(applicationContext) && isPowerSave) return true
            if (isBatteryLow) return true
            return false
        }

        private fun updatePlaybackState() {
            try {
                val pause = shouldPause()
                Log.d("AuroraEngine", "updatePlaybackState: pause=$pause, isVisible=$isVisibleState, isPreview=$isPreview, isPrepared=$isPrepared, isPowerSave=$isPowerSave")
                if (pause) {
                    val pauseReason = when {
                        !isVisibleState -> "not visible"
                        SettingsPrefs.pauseOnBatterySaver(applicationContext) && isPowerSave -> "battery saver"
                        isBatteryLow -> "battery low ($currentBatteryPct%)"
                        else -> "condition met"
                    }
                    if (lastPauseReason != pauseReason) {
                        AuroraDebugLogger.log(applicationContext, "PAUSE: $pauseReason")
                        lastPauseReason = pauseReason
                    }

                    if (isPrepared && mediaPlayer != null) {
                        try {
                            mediaPlayer?.setVolume(0f, 0f)
                            if (mediaPlayer?.isPlaying == true) {
                                mediaPlayer?.pause()
                            }
                        } catch (e: Throwable) {
                            recoverPlayer("updatePlaybackState pause failed: ${e.message}")
                        }
                    }
                    gifRunning = false
                    gifHandler.removeCallbacks(gifDrawRunnable)
                    // Note: MediaPlayer keeps the last rendered frame on the surface naturally.
                } else {
                    if (lastPauseReason != null) {
                        AuroraDebugLogger.log(applicationContext, "RESUME: playback resumed")
                        lastPauseReason = null
                    }

                    val savedPath = mediaPrefs.getString("media_path", null)
                    val savedVersion = mediaPrefs.getLong("media_version", 0L)
                    val savedScaling = getScalingMode()
                    val needsReload = (savedPath != currentLoadedPath) ||
                            (savedVersion != currentLoadedVersion) ||
                            (savedScaling != currentLoadedScalingMode) ||
                            (mediaPlayer == null && gifMovie == null)

                    if (needsReload && currentHolder?.surface?.isValid == true && !gifRunning) {
                        currentHolder?.let { loadWallpaper(it) }
                    } else if (isPrepared && mediaPlayer != null) {
                        try {
                            updatePlayerSettings()
                            val playing = mediaPlayer?.isPlaying == true
                            if (!playing) {
                                mediaPlayer?.start()
                            }
                        } catch (e: Throwable) {
                            recoverPlayer("updatePlaybackState start failed: ${e.message}")
                        }
                    }
                    if (gifMovie != null && !gifRunning) {
                        gifRunning = true
                        gifHandler.post(gifDrawRunnable)
                    }
                }
            } catch (e: Throwable) {
                recoverPlayer("updatePlaybackState top-level error: ${e.message}")
            }
        }

        private fun loadWallpaper(holder: SurfaceHolder) {
            try {
                val savedPath = mediaPrefs.getString("media_path", null)
                val mediaType = mediaPrefs.getString("media_type", "video")
                val savedVersion = mediaPrefs.getLong("media_version", 0L)
                val fileExists = savedPath != null && File(savedPath).exists()
                val fileSize = if (fileExists && savedPath != null) File(savedPath).length() else 0L

                AuroraDebugLogger.log(
                    applicationContext,
                    "RELOAD: loadWallpaper (path=$savedPath, version=$savedVersion, type=$mediaType, exists=$fileExists, size=$fileSize, isPreview=$isPreview)"
                )

                val savedScaling = getScalingMode()

                currentLoadedPath = savedPath
                currentLoadedVersion = savedVersion
                currentLoadedScalingMode = savedScaling

                // Requirement 1: If the media file is missing, call releasePlayer() and draw nothing. No app-made static images anywhere in the service.
                if (!fileExists || savedPath == null) {
                    releasePlayer()
                    return
                }

                if (mediaType == "gif") {
                    releasePlayer()
                    startGif(savedPath)
                } else if (mediaType == "image") {
                    releasePlayer()
                    drawImage(savedPath, holder)
                } else {
                    startVideo(savedPath, holder)
                }
            } catch (e: Throwable) {
                AuroraDebugLogger.log(applicationContext, "ERROR: loadWallpaper failed: ${e.message}")
                releasePlayer()
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    val delayMs = retryCount * 1000L
                    gifHandler.postDelayed({
                        currentHolder?.let { h ->
                            if (h.surface.isValid) {
                                loadWallpaper(h)
                            }
                        }
                    }, delayMs)
                }
            }
        }

        private fun drawImage(path: String, holder: SurfaceHolder) {
            if (!holder.surface.isValid) return
            var canvas: Canvas? = null
            try {
                val bitmap = BitmapFactory.decodeFile(path) ?: return
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()
                    val scalingMode = getScalingMode()
                    val scale = if (scalingMode == "fit") {
                        minOf(w / bitmap.width, h / bitmap.height)
                    } else {
                        maxOf(w / bitmap.width, h / bitmap.height)
                    }
                    val dx = (w - bitmap.width * scale) / 2f
                    val dy = (h - bitmap.height * scale) / 2f
                    canvas.drawColor(Color.BLACK)
                    canvas.save()
                    canvas.translate(dx, dy)
                    canvas.scale(scale, scale)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    canvas.restore()
                }
                bitmap.recycle()
            } catch (e: Throwable) {
                AuroraDebugLogger.log(applicationContext, "ERROR: drawImage failed: ${e.message}")
                Log.e("AuroraEngine", "Failed to draw image $path", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Throwable) {
                        Log.e("AuroraEngine", "Error unlocking canvas in drawImage", e)
                    }
                }
            }
        }

        private fun startVideo(filePath: String, holder: SurfaceHolder) {
            try {
                releasePlayer()
                isPrepared = false

                Log.d("AuroraEngine", "startVideo: preparing MediaPlayer for $filePath")
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(filePath)

                    val audioAttr = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                    setAudioAttributes(audioAttr)
                    // Always initialize volume at 0f to guarantee no audio leak on startup or wake
                    setVolume(0f, 0f)

                    if (holder.surface.isValid) {
                        setSurface(holder.surface)
                    }

                    applyVideoScalingMode(this)
                    setOnPreparedListener { mp ->
                        Log.d("AuroraEngine", "MediaPlayer prepared successfully: duration=${mp.duration}ms, isPreview=$isPreview")
                        try {
                            isPrepared = true
                            retryCount = 0
                            recoveryLimiter.resetBackoff()
                            applyVideoScalingMode(mp)
                            updatePlayerSettings()
                            updatePlaybackState()
                        } catch (e: Throwable) {
                            recoverPlayer("onPrepared error: ${e.message}")
                        }
                    }
                    setOnCompletionListener { mp ->
                        val loop = SettingsPrefs.loopAutomatically(applicationContext)
                        if (loop) {
                            try {
                                mp.seekTo(0)
                                mp.start()
                                updatePlayerSettings()
                            } catch (e: Throwable) {
                                recoverPlayer("onCompletion seek/start failed: ${e.message}")
                            }
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        AuroraDebugLogger.log(applicationContext, "ERROR: MediaPlayer error (what=$what, extra=$extra)")
                        isPrepared = false
                        releasePlayer()
                        if (retryCount < MAX_RETRIES) {
                            retryCount++
                            val delayMs = retryCount * 1000L
                            AuroraDebugLogger.log(applicationContext, "RECOVERY: scheduling auto-recovery attempt $retryCount in ${delayMs}ms")
                            gifHandler.postDelayed({
                                if (isVisibleState && !shouldPause()) {
                                    currentHolder?.let { h ->
                                        if (h.surface.isValid) {
                                            loadWallpaper(h)
                                        }
                                    }
                                }
                            }, delayMs)
                        } else {
                            AuroraDebugLogger.log(applicationContext, "RECOVERY: MAX_RETRIES reached, scheduling reload on next visible & 60s")
                            reloadOnNextVisible = true
                            gifHandler.removeCallbacks(reload60sRunnable)
                            gifHandler.postDelayed(reload60sRunnable, 60_000L)
                        }
                        true
                    }
                    prepareAsync()
                }

                refreshPowerState()
            } catch (e: Throwable) {
                AuroraDebugLogger.log(applicationContext, "ERROR: startVideo failed: ${e.message}")
                releasePlayer()
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    val delayMs = retryCount * 1000L
                    gifHandler.postDelayed({
                        currentHolder?.let { h ->
                            if (h.surface.isValid) {
                                loadWallpaper(h)
                            }
                        }
                    }, delayMs)
                }
            }
        }

        private fun startGif(path: String) {
            try {
                gifMovie = FileInputStream(path).use { Movie.decodeStream(it) }
                gifStartTime = 0
                refreshPowerState()
                if (!shouldPause()) {
                    gifRunning = true
                    gifHandler.post(gifDrawRunnable)
                }
            } catch (e: Throwable) {
                AuroraDebugLogger.log(applicationContext, "ERROR: startGif failed: ${e.message}")
                Log.e("AuroraEngine", "Failed to decode GIF stream for $path", e)
            }
        }

        private fun drawGifFrame() {
            val movie = gifMovie ?: return
            val holder = currentHolder ?: return
            if (!holder.surface.isValid) return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    if (gifStartTime == 0L) gifStartTime = SystemClock.uptimeMillis()
                    val duration = if (movie.duration() == 0) 1000 else movie.duration()
                    val loop = SettingsPrefs.loopAutomatically(applicationContext)
                    val rawElapsed = SystemClock.uptimeMillis() - gifStartTime
                    val elapsed = if (loop) {
                        (rawElapsed % duration).toInt()
                    } else {
                        rawElapsed.coerceAtMost(duration.toLong()).toInt()
                    }
                    movie.setTime(elapsed)
                    canvas.drawColor(Color.BLACK)
                    val scalingMode = getScalingMode()
                    val scale = if (scalingMode == "fit") {
                        minOf(
                            canvas.width.toFloat() / movie.width(),
                            canvas.height.toFloat() / movie.height()
                        )
                    } else {
                        maxOf(
                            canvas.width.toFloat() / movie.width(),
                            canvas.height.toFloat() / movie.height()
                        )
                    }
                    canvas.save()
                    val dx = (canvas.width - movie.width() * scale) / 2f
                    val dy = (canvas.height - movie.height() * scale) / 2f
                    canvas.translate(dx, dy)
                    canvas.scale(scale, scale)
                    movie.draw(canvas, 0f, 0f)
                    canvas.restore()
                }
            } catch (e: Throwable) {
                Log.e("AuroraEngine", "Failed to draw GIF frame", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Throwable) {
                        Log.e("AuroraEngine", "Error unlocking canvas in GIF frame", e)
                    }
                }
            }
        }

        private fun releasePlayer() {
            isPrepared = false
            try {
                mediaPlayer?.let { mp ->
                    try { mp.setSurface(null) } catch (_: Throwable) {}
                    try { mp.stop() } catch (_: Throwable) {}
                    try { mp.reset() } catch (_: Throwable) {}
                    try { mp.release() } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                Log.w("AuroraEngine", "Error releasing MediaPlayer", e)
            }
            mediaPlayer = null
            gifRunning = false
            gifHandler.removeCallbacks(gifDrawRunnable)
            gifMovie = null
        }
    }
}
