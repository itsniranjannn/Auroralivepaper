package com.niranjan.auroralivepaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.niranjan.auroralivepaper.data.SettingsPrefs
import java.io.File
import java.io.FileInputStream

@UnstableApi
class AuroraWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AuroraEngine()

    inner class AuroraEngine : Engine() {

        private var player: ExoPlayer? = null
        private var gifMovie: Movie? = null
        private var gifStartTime: Long = 0
        private val gifHandler = Handler(Looper.getMainLooper())
        private var gifRunning = false
        private var currentHolder: SurfaceHolder? = null

        private var isVisible = false
        private var isPowerSave = false
        private var isBatteryLow = false
        private var receiverRegistered = false

        private val settingsPrefs by lazy {
            applicationContext.getSharedPreferences("aurora_settings_prefs", MODE_PRIVATE)
        }
        private val mediaPrefs by lazy {
            applicationContext.getSharedPreferences("aurora_prefs", MODE_PRIVATE)
        }

        // Strong references so the listeners are not garbage collected
        private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            currentHolder?.let { applyResolutionCap(it) }
            updatePlayerSettings()
            refreshPowerState()
            updatePlaybackState()
        }

        // media_version is written on every save, so reacting to it alone avoids triple reloads
        private val mediaListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "media_version") {
                currentHolder?.let { loadWallpaper(it) }
            }
        }

        private val powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        val pm = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager
                        isPowerSave = pm?.isPowerSaveMode == true
                    }
                    Intent.ACTION_BATTERY_CHANGED -> updateBatteryLow(intent)
                }
                updatePlaybackState()
            }
        }

        private val gifDrawRunnable = object : Runnable {
            override fun run() {
                if (!gifRunning) return
                drawGifFrame()
                if (gifRunning) gifHandler.postDelayed(this, 33) // ~30fps
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            settingsPrefs.registerOnSharedPreferenceChangeListener(settingsListener)
            mediaPrefs.registerOnSharedPreferenceChangeListener(mediaListener)
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterPowerReceiver()
            settingsPrefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
            mediaPrefs.unregisterOnSharedPreferenceChangeListener(mediaListener)
            releasePlayer()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            Log.d("AuroraEngine", "onSurfaceCreated: isValid=${holder.surface.isValid}")
            currentHolder = holder
            applyResolutionCap(holder)
            loadWallpaper(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d("AuroraEngine", "onSurfaceChanged: ${width}x${height}")
            currentHolder = holder
            player?.let { p ->
                if (holder.surface.isValid) {
                    p.setVideoSurfaceHolder(holder)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d("AuroraEngine", "onVisibilityChanged: visible=$visible")
            isVisible = visible
            if (visible) {
                registerPowerReceiver()
                refreshPowerState()
                currentHolder?.let { holder ->
                    if (holder.surface.isValid) {
                        player?.setVideoSurfaceHolder(holder)
                    }
                }
            } else {
                unregisterPowerReceiver()
            }
            updatePlaybackState()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            Log.d("AuroraEngine", "onSurfaceDestroyed")
            currentHolder = null
            player?.clearVideoSurfaceHolder(holder)
            player?.pause()
            gifRunning = false
            gifHandler.removeCallbacks(gifDrawRunnable)
        }

        // Receivers only exist while the wallpaper is visible, so there are no wakeups in the background
        private fun registerPowerReceiver() {
            if (receiverRegistered) return
            try {
                val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                ContextCompat.registerReceiver(
                    applicationContext, powerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
                )
                receiverRegistered = true
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

        // Re-read current power state, including the sticky battery intent
        private fun refreshPowerState() {
            val pm = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager
            isPowerSave = pm?.isPowerSaveMode == true
            try {
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
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0) {
                val pct = (level * 100) / scale
                val threshold = SettingsPrefs.batteryThreshold(applicationContext)
                val pauseBelow = SettingsPrefs.pauseBelowThreshold(applicationContext)
                // never pause just because the battery is low while it is charging
                isBatteryLow = pauseBelow && !charging && pct <= threshold
            } else {
                isBatteryLow = false
            }
        }

        private fun updatePlayerSettings() {
            player?.let { p ->
                val playSound = SettingsPrefs.playWallpaperSound(applicationContext)
                val vol = SettingsPrefs.wallpaperVolume(applicationContext)
                p.volume = if (playSound) vol else 0f
                val loop = SettingsPrefs.loopAutomatically(applicationContext)
                p.repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
            }
        }

        private fun shouldPause(): Boolean {
            if (!isVisible) return true
            if (SettingsPrefs.pauseOnBatterySaver(applicationContext) && isPowerSave) return true
            if (isBatteryLow) return true
            return false
        }

        private fun updatePlaybackState() {
            val pause = shouldPause()
            Log.d("AuroraEngine", "updatePlaybackState: shouldPause=$pause (isVisible=$isVisible, isPowerSave=$isPowerSave, isBatteryLow=$isBatteryLow)")
            if (pause) {
                player?.pause()
                gifRunning = false
                gifHandler.removeCallbacks(gifDrawRunnable)
            } else {
                player?.let { p ->
                    p.playWhenReady = true
                    p.play()
                }
                if (gifMovie != null && !gifRunning) {
                    gifRunning = true
                    gifHandler.post(gifDrawRunnable)
                }
            }
        }

        private fun applyResolutionCap(holder: SurfaceHolder) {
            try {
                holder.setSizeFromLayout()
            } catch (e: Exception) {
                Log.w("Aurora", "Error configuring surface size", e)
            }
        }

        private fun loadWallpaper(holder: SurfaceHolder) {
            val savedPath = mediaPrefs.getString("media_path", null)
            val mediaType = mediaPrefs.getString("media_type", "video")
            Log.d("AuroraEngine", "loadWallpaper: path=$savedPath, type=$mediaType")

            if (savedPath == null || !File(savedPath).exists()) {
                releasePlayer()
                drawAuroraFallbackTheme(holder)
                return
            }

            if (mediaType == "gif") {
                releasePlayer()
                startGif(savedPath)
            } else {
                startVideo(Uri.fromFile(File(savedPath)), holder)
            }
        }

        private fun drawAuroraFallbackTheme(holder: SurfaceHolder) {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()

                    // Background: Dark Cosmic Gradient
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

                    // Aurora Wave 1
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

                    // Aurora Wave 2
                    val wavePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 10f
                        strokeCap = Paint.Cap.ROUND
                        shader = LinearGradient(
                            0f, 0f, w, h,
                            intArrayOf(
                                Color.parseColor("#B38CFF"),
                                Color.parseColor("#7CE7D7")
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    val path2 = Path().apply {
                        moveTo(w * 0.15f, h * 0.72f)
                        cubicTo(w * 0.4f, h * 0.42f, w * 0.7f, h * 0.82f, w * 0.95f, h * 0.52f)
                    }
                    canvas.drawPath(path2, wavePaint2)

                    // Centered Glass Badge Text
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
                Log.w("Aurora", "Failed to draw fallback theme canvas", e)
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun startVideo(uri: Uri, holder: SurfaceHolder) {
            val playSound = SettingsPrefs.playWallpaperSound(applicationContext)
            val vol = SettingsPrefs.wallpaperVolume(applicationContext)
            val loop = SettingsPrefs.loopAutomatically(applicationContext)

            releasePlayer()

            player = ExoPlayer.Builder(applicationContext).build().apply {
                setVideoSurfaceHolder(holder)
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
                volume = if (playSound) vol else 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                playWhenReady = true
                prepare()
            }

            refreshPowerState()
            updatePlaybackState()
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
                Log.w("Aurora", "Failed to decode GIF stream", e)
            }
        }

        private fun drawGifFrame() {
            val movie = gifMovie ?: return
            val holder = currentHolder ?: return
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
                Log.w("Aurora", "Failed to draw GIF frame", e)
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun releasePlayer() {
            player?.release()
            player = null
            gifRunning = false
            gifHandler.removeCallbacks(gifDrawRunnable)
            gifMovie = null
        }
    }
}
