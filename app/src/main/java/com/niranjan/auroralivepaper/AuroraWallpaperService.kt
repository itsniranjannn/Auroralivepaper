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
import android.graphics.LinearGradient
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.niranjan.auroralivepaper.data.SettingsPrefs
import java.io.File
import java.io.FileInputStream

class AuroraWallpaperService : WallpaperService() {

    companion object {
        const val ACTION_RELOAD_WALLPAPER = "com.niranjan.auroralivepaper.RELOAD_WALLPAPER"
    }

    override fun onCreateEngine(): Engine {
        Log.d("AuroraEngine", "Service.onCreateEngine called")
        return AuroraEngine()
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
        private var receiverRegistered = false

        private var currentLoadedPath: String? = null
        private var currentLoadedVersion: Long = 0L
        private var retryCount = 0
        private val MAX_RETRIES = 3

        private val settingsPrefs by lazy {
            applicationContext.getSharedPreferences("aurora_settings_prefs", MODE_PRIVATE)
        }
        private val mediaPrefs by lazy {
            applicationContext.getSharedPreferences("aurora_prefs", MODE_PRIVATE)
        }

        private val unlockPollerRunnable = object : Runnable {
            override fun run() {
                try {
                    if (isVisibleState && !isPreview) {
                        updatePlayerSettings()
                        if (isDeviceLocked()) {
                            gifHandler.postDelayed(this, 800)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuroraEngine", "Error in unlockPollerRunnable", e)
                }
            }
        }

        private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            try {
                updatePlayerSettings()
                refreshPowerState()
                updatePlaybackState()
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in settingsListener", e)
            }
        }

        private val mediaListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            try {
                if (key == "media_version" || key == "media_path") {
                    val savedPath = mediaPrefs.getString("media_path", null)
                    Log.d("AuroraEngine", "media preference changed: reload media from path=$savedPath")
                    retryCount = 0
                    currentHolder?.let { holder ->
                        if (holder.surface.isValid) {
                            loadWallpaper(holder)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in mediaListener", e)
            }
        }

        private val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    if (intent == null) return
                    Log.d("AuroraEngine", "Broadcast received: ${intent.action}")
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
                            // Instantly mute on screen off so subsequent wake-up never leaks audio
                            try {
                                mediaPlayer?.setVolume(0f, 0f)
                            } catch (_: Exception) {}
                            updatePlaybackState()
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            // Screen turned on at lockscreen -> strictly mute
                            updatePlayerSettings()
                            updatePlaybackState()
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            // Device unlocked to Home screen -> restore audio if enabled
                            Log.d("AuroraEngine", "ACTION_USER_PRESENT: Unlocked to Home screen")
                            updatePlayerSettings()
                            gifHandler.postDelayed({ updatePlayerSettings() }, 150)
                            gifHandler.postDelayed({ updatePlayerSettings() }, 400)
                            gifHandler.postDelayed({ updatePlayerSettings() }, 800)
                            updatePlaybackState()
                        }
                        ACTION_RELOAD_WALLPAPER -> {
                            Log.d("AuroraEngine", "ACTION_RELOAD_WALLPAPER broadcast received")
                            retryCount = 0
                            currentHolder?.let { holder ->
                                if (holder.surface.isValid) {
                                    loadWallpaper(holder)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuroraEngine", "Error in powerReceiver.onReceive", e)
                }
            }
        }

        private val gifDrawRunnable = object : Runnable {
            override fun run() {
                try {
                    if (!gifRunning) return
                    drawGifFrame()
                    if (gifRunning) gifHandler.postDelayed(this, 33) // ~30fps
                } catch (e: Exception) {
                    Log.e("AuroraEngine", "Error in gifDrawRunnable", e)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            try {
                super.onCreate(surfaceHolder)
                Log.d("AuroraEngine", "Engine.onCreate: isPreview=$isPreview")
                registerPowerReceiver()
                settingsPrefs.registerOnSharedPreferenceChangeListener(settingsListener)
                mediaPrefs.registerOnSharedPreferenceChangeListener(mediaListener)
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in Engine.onCreate", e)
            }
        }

        override fun onDestroy() {
            try {
                super.onDestroy()
                Log.d("AuroraEngine", "Engine.onDestroy: isPreview=$isPreview")
                gifHandler.removeCallbacks(unlockPollerRunnable)
                unregisterPowerReceiver()
                settingsPrefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
                mediaPrefs.unregisterOnSharedPreferenceChangeListener(mediaListener)
                releasePlayer()
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in Engine.onDestroy", e)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            try {
                super.onSurfaceCreated(holder)
                Log.d("AuroraEngine", "onSurfaceCreated: surfaceValid=${holder.surface.isValid}, isPreview=$isPreview")
                currentHolder = holder
                val savedPath = mediaPrefs.getString("media_path", null)
                val savedVersion = mediaPrefs.getLong("media_version", 0L)
                val needsReload = (savedPath != currentLoadedPath) ||
                        (savedVersion != currentLoadedVersion) ||
                        (mediaPlayer == null && gifMovie == null)

                if (needsReload && !shouldPause()) {
                    loadWallpaper(holder)
                }
            } catch (e: Exception) {
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
                        } catch (e: Exception) {
                            Log.e("AuroraEngine", "Error setting surface in onSurfaceChanged", e)
                        }
                    } else if (gifMovie == null && !shouldPause()) {
                        loadWallpaper(holder)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in onSurfaceChanged", e)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            try {
                super.onVisibilityChanged(visible)
                Log.d("AuroraEngine", "onVisibilityChanged: visible=$visible, isPreview=$isPreview")
                isVisibleState = visible
                if (visible) {
                    refreshPowerState()
                    gifHandler.removeCallbacks(unlockPollerRunnable)
                    gifHandler.post(unlockPollerRunnable)
                    updatePlayerSettings()

                    val savedPath = mediaPrefs.getString("media_path", null)
                    val savedVersion = mediaPrefs.getLong("media_version", 0L)
                    val needsReload = (savedPath != currentLoadedPath) ||
                            (savedVersion != currentLoadedVersion) ||
                            (mediaPlayer == null && gifMovie == null)

                    if (needsReload) {
                        retryCount = 0
                        currentHolder?.let { holder ->
                            if (holder.surface.isValid && !shouldPause()) {
                                loadWallpaper(holder)
                            }
                        }
                    }
                } else {
                    gifHandler.removeCallbacks(unlockPollerRunnable)
                    try {
                        mediaPlayer?.setVolume(0f, 0f)
                    } catch (_: Exception) {}
                }
                updatePlaybackState()
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in onVisibilityChanged", e)
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            if (isVisibleState && !isPreview) {
                updatePlayerSettings()
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (isVisibleState && !isPreview && event?.action == MotionEvent.ACTION_DOWN) {
                updatePlayerSettings()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            try {
                super.onSurfaceDestroyed(holder)
                Log.d("AuroraEngine", "onSurfaceDestroyed: isPreview=$isPreview")
                currentHolder = null
                releasePlayer()
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in onSurfaceDestroyed", e)
            }
        }

        private fun registerPowerReceiver() {
            if (receiverRegistered) return
            try {
                val filter = IntentFilter().apply {
                    addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(ACTION_RELOAD_WALLPAPER)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.registerReceiver(
                        applicationContext, powerReceiver, filter, ContextCompat.RECEIVER_EXPORTED
                    )
                } else {
                    applicationContext.registerReceiver(powerReceiver, filter)
                }
                receiverRegistered = true
                Log.d("AuroraEngine", "powerReceiver successfully registered")
            } catch (e: Exception) {
                Log.w("AuroraEngine", "Failed to register powerReceiver", e)
            }
        }

        private fun unregisterPowerReceiver() {
            if (!receiverRegistered) return
            try {
                applicationContext.unregisterReceiver(powerReceiver)
            } catch (_: Exception) {
            }
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
            } catch (e: Exception) {
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
                    val threshold = SettingsPrefs.batteryThreshold(applicationContext)
                    val pauseBelow = SettingsPrefs.pauseBelowThreshold(applicationContext)
                    isBatteryLow = pauseBelow && !charging && pct <= threshold
                } else {
                    isBatteryLow = false
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in updateBatteryLow", e)
            }
        }

        private fun isDeviceLocked(): Boolean {
            val km = applicationContext.getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            return km?.isKeyguardLocked == true
        }

        private fun updatePlayerSettings() {
            mediaPlayer?.let { mp ->
                try {
                    val isLocked = isDeviceLocked()
                    val soundEnabled = SettingsPrefs.playWallpaperSound(applicationContext)
                    // STRICT ISOLATION:
                    // Sound ONLY plays on Home Screen when completely unlocked!
                    // If device is locked (Keyguard on screen), in preview, or disabled: strictly 0f.
                    val canPlaySound = soundEnabled && !isLocked && !isPreview
                    val vol = SettingsPrefs.wallpaperVolume(applicationContext)
                    val actualVol = if (canPlaySound) vol else 0f
                    mp.setVolume(actualVol, actualVol)
                    val loop = SettingsPrefs.loopAutomatically(applicationContext)
                    mp.isLooping = loop
                } catch (e: Exception) {
                    Log.e("AuroraEngine", "Failed to update player settings", e)
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
                    if (isPrepared && mediaPlayer != null) {
                        try {
                            mediaPlayer?.setVolume(0f, 0f)
                            if (mediaPlayer?.isPlaying == true) {
                                mediaPlayer?.pause()
                            }
                        } catch (e: Exception) {
                            Log.w("AuroraEngine", "Error pausing MediaPlayer", e)
                        }
                    }
                    gifRunning = false
                    gifHandler.removeCallbacks(gifDrawRunnable)

                    currentHolder?.let { holder ->
                        if (holder.surface.isValid && isVisibleState && !isPreview && isPowerSave && SettingsPrefs.pauseOnBatterySaver(applicationContext)) {
                            drawPosterFrame(holder)
                        }
                    }
                } else {
                    val savedPath = mediaPrefs.getString("media_path", null)
                    val savedVersion = mediaPrefs.getLong("media_version", 0L)
                    val needsReload = (savedPath != currentLoadedPath) ||
                            (savedVersion != currentLoadedVersion) ||
                            (mediaPlayer == null && gifMovie == null)

                    if (needsReload && currentHolder?.surface?.isValid == true && !gifRunning) {
                        currentHolder?.let { loadWallpaper(it) }
                    } else if (isPrepared && mediaPlayer != null) {
                        try {
                            // CRITICAL: Ensure volume is strictly adjusted before calling start()
                            updatePlayerSettings()
                            if (mediaPlayer?.isPlaying == false) {
                                mediaPlayer?.start()
                            }
                        } catch (e: Exception) {
                            Log.w("AuroraEngine", "Error starting MediaPlayer", e)
                        }
                    }
                    if (gifMovie != null && !gifRunning) {
                        gifRunning = true
                        gifHandler.post(gifDrawRunnable)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in updatePlaybackState", e)
            }
        }

        private fun loadWallpaper(holder: SurfaceHolder) {
            try {
                val savedPath = mediaPrefs.getString("media_path", null)
                val mediaType = mediaPrefs.getString("media_type", "video")
                val savedVersion = mediaPrefs.getLong("media_version", 0L)
                val fileExists = savedPath != null && File(savedPath).exists()
                val fileSize = if (fileExists && savedPath != null) File(savedPath).length() else 0L

                Log.d(
                    "AuroraEngine",
                    "loadWallpaper: path=$savedPath, version=$savedVersion, type=$mediaType, exists=$fileExists, size=$fileSize, isPreview=$isPreview"
                )

                currentLoadedPath = savedPath
                currentLoadedVersion = savedVersion

                if (!fileExists || savedPath == null) {
                    releasePlayer()
                    drawAuroraFallbackTheme(holder)
                    return
                }

                if (mediaType == "gif") {
                    releasePlayer()
                    startGif(savedPath)
                } else {
                    startVideo(savedPath, holder)
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error in loadWallpaper", e)
            }
        }

        private fun drawPosterFrame(holder: SurfaceHolder) {
            val posterFile = File(applicationContext.filesDir, "current_wallpaper_poster.jpg")
            var canvas: Canvas? = null
            try {
                if (!holder.surface.isValid) return
                if (posterFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(posterFile.absolutePath)
                    if (bitmap != null) {
                        canvas = holder.lockCanvas()
                        if (canvas != null) {
                            val w = canvas.width.toFloat()
                            val h = canvas.height.toFloat()
                            val scale = maxOf(w / bitmap.width, h / bitmap.height)
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
                    } else {
                        drawAuroraFallbackTheme(holder)
                    }
                } else {
                    drawAuroraFallbackTheme(holder)
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Error drawing poster frame", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("AuroraEngine", "Error unlocking canvas in poster frame", e)
                    }
                }
            }
        }

        private fun drawAuroraFallbackTheme(holder: SurfaceHolder) {
            var canvas: Canvas? = null
            try {
                if (!holder.surface.isValid) return
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()

                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, 0f, h,
                            intArrayOf(
                                Color.parseColor("#07090F"),
                                Color.parseColor("#121829"),
                                Color.parseColor("#07090F")
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, w, h, bgPaint)

                    val wavePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 16f
                        strokeCap = Paint.Cap.ROUND
                        shader = LinearGradient(
                            0f, 0f, w, h,
                            intArrayOf(
                                Color.parseColor("#4ECCA3"),
                                Color.parseColor("#7CE7D7"),
                                Color.parseColor("#B38CFF")
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    val path1 = Path().apply {
                        moveTo(w * 0.1f, h * 0.65f)
                        cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, h * 0.75f, w * 0.9f, h * 0.45f)
                    }
                    canvas.drawPath(path1, wavePaint1)

                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 48f
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#B3C0D6")
                        textSize = 28f
                        textAlign = Paint.Align.CENTER
                    }

                    canvas.drawText("Aurora Live Paper", w / 2f, h * 0.85f, textPaint)
                    canvas.drawText("Bring your screen to life", w / 2f, h * 0.85f + 40f, subTextPaint)
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Failed to draw fallback theme canvas", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("AuroraEngine", "Error unlocking canvas in fallback theme", e)
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

                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    setOnPreparedListener { mp ->
                        Log.d("AuroraEngine", "MediaPlayer prepared successfully: duration=${mp.duration}ms, isPreview=$isPreview")
                        try {
                            isPrepared = true
                            retryCount = 0
                            updatePlayerSettings()
                            updatePlaybackState()
                        } catch (e: Exception) {
                            Log.e("AuroraEngine", "Error handling onPrepared", e)
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("AuroraEngine", "MediaPlayer onError: what=$what, extra=$extra (retryCount=$retryCount)")
                        isPrepared = false
                        releasePlayer()
                        currentHolder?.let { h ->
                            if (h.surface.isValid) drawPosterFrame(h)
                        }
                        if (retryCount < MAX_RETRIES) {
                            retryCount++
                            val delayMs = retryCount * 1200L
                            Log.d("AuroraEngine", "Scheduling auto-recovery attempt $retryCount in ${delayMs}ms")
                            gifHandler.postDelayed({
                                if (isVisibleState && !shouldPause()) {
                                    currentHolder?.let { h ->
                                        if (h.surface.isValid) {
                                            Log.d("AuroraEngine", "Executing auto-recovery reload...")
                                            loadWallpaper(h)
                                        }
                                    }
                                }
                            }, delayMs)
                        }
                        true
                    }
                    prepareAsync()
                }

                refreshPowerState()
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Failed to startVideo for $filePath", e)
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
            } catch (e: Exception) {
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
                    val scale = maxOf(
                        canvas.width.toFloat() / movie.width(),
                        canvas.height.toFloat() / movie.height()
                    )
                    canvas.save()
                    val dx = (canvas.width - movie.width() * scale) / 2f
                    val dy = (canvas.height - movie.height() * scale) / 2f
                    canvas.translate(dx, dy)
                    canvas.scale(scale, scale)
                    movie.draw(canvas, 0f, 0f)
                    canvas.restore()
                }
            } catch (e: Exception) {
                Log.e("AuroraEngine", "Failed to draw GIF frame", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("AuroraEngine", "Error unlocking canvas in GIF frame", e)
                    }
                }
            }
        }

        private fun releasePlayer() {
            isPrepared = false
            try {
                mediaPlayer?.let { mp ->
                    try { mp.setSurface(null) } catch (_: Exception) {}
                    try { mp.stop() } catch (_: Exception) {}
                    try { mp.reset() } catch (_: Exception) {}
                    try { mp.release() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w("AuroraEngine", "Error releasing MediaPlayer", e)
            }
            mediaPlayer = null
            gifRunning = false
            gifHandler.removeCallbacks(gifDrawRunnable)
            gifMovie = null
        }
    }
}
