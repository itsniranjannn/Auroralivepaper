package com.niranjan.auroralivepaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.WindowManager
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

    inner class AuroraEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var player: ExoPlayer? = null
        private var gifMovie: Movie? = null
        private var gifStartTime: Long = 0
        private val gifHandler = Handler(Looper.getMainLooper())
        private var gifRunning = false
        private var currentHolder: SurfaceHolder? = null

        private var isVisible = false
        private var isPowerSave = false
        private var isBatteryLow = false

        private val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        checkPowerSaveState()
                        updatePlaybackState()
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        intent.let { checkBatteryLevel(it) }
                        updatePlaybackState()
                    }
                }
            }
        }

        private val gifDrawRunnable = object : Runnable {
            override fun run() {
                if (!gifRunning) return
                drawGifFrame()
                if (gifRunning) {
                    gifHandler.postDelayed(this, 33) // ~30fps
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            val settingsPrefs = applicationContext.getSharedPreferences("aurora_settings_prefs", MODE_PRIVATE)
            settingsPrefs.registerOnSharedPreferenceChangeListener(this)

            val filter = IntentFilter().apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
            applicationContext.registerReceiver(batteryReceiver, filter)

            checkPowerSaveState()
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                applicationContext.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}

            val settingsPrefs = applicationContext.getSharedPreferences("aurora_settings_prefs", MODE_PRIVATE)
            settingsPrefs.unregisterOnSharedPreferenceChangeListener(this)

            releasePlayer()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            applyResolutionCap(holder)
            loadWallpaper(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            currentHolder = holder
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            updatePlaybackState()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            currentHolder = null
            releasePlayer()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            currentHolder?.let { holder ->
                applyResolutionCap(holder)
            }
            player?.let { p ->
                val rememberAudio = SettingsPrefs.rememberAudioChoice(applicationContext)
                p.volume = if (rememberAudio) 1f else 0f
                val loop = SettingsPrefs.loopAutomatically(applicationContext)
                p.repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
            }
            checkPowerSaveState()
            updatePlaybackState()
        }

        private fun checkPowerSaveState() {
            val pm = applicationContext.getSystemService(POWER_SERVICE) as? PowerManager
            isPowerSave = pm?.isPowerSaveMode == true
        }

        private fun checkBatteryLevel(intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                val pct = (level * 100) / scale
                val threshold = SettingsPrefs.batteryThreshold(applicationContext)
                val pauseBelow = SettingsPrefs.pauseBelowThreshold(applicationContext)
                isBatteryLow = pauseBelow && (pct <= threshold)
            } else {
                isBatteryLow = false
            }
        }

        private fun shouldPause(): Boolean {
            if (!isVisible) return true
            val pauseOnSaver = SettingsPrefs.pauseOnBatterySaver(applicationContext)
            if (pauseOnSaver && isPowerSave) return true
            if (isBatteryLow) return true
            return false
        }

        private fun updatePlaybackState() {
            if (shouldPause()) {
                player?.pause()
                gifRunning = false
                gifHandler.removeCallbacks(gifDrawRunnable)
            } else {
                player?.play()
                if (gifMovie != null && !gifRunning) {
                    gifRunning = true
                    gifHandler.post(gifDrawRunnable)
                }
            }
        }

        private fun applyResolutionCap(holder: SurfaceHolder) {
            val mode = SettingsPrefs.resolutionMode(applicationContext)
            val windowManager = applicationContext.getSystemService(WINDOW_SERVICE) as? WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getRealMetrics(metrics)

            val screenW = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
            val screenH = if (metrics.heightPixels > 0) metrics.heightPixels else 2400

            when (mode) {
                "1080p" -> {
                    val maxDim = 1920
                    val scale = minOf(1f, maxDim.toFloat() / maxOf(screenW, screenH))
                    val targetW = (screenW * scale).toInt()
                    val targetH = (screenH * scale).toInt()
                    holder.setFixedSize(targetW, targetH)
                }
                "1440p" -> {
                    val maxDim = 2560
                    val scale = minOf(1f, maxDim.toFloat() / maxOf(screenW, screenH))
                    val targetW = (screenW * scale).toInt()
                    val targetH = (screenH * scale).toInt()
                    holder.setFixedSize(targetW, targetH)
                }
                else -> {
                    holder.setSizeFromLayout()
                }
            }
        }

        private fun loadWallpaper(holder: SurfaceHolder) {
            releasePlayer()

            val prefs = applicationContext.getSharedPreferences("aurora_prefs", MODE_PRIVATE)
            val savedPath = prefs.getString("media_path", null)
            val mediaType = prefs.getString("media_type", "video")

            if (savedPath == null || !File(savedPath).exists()) {
                return
            }

            if (mediaType == "gif") {
                startGif(savedPath, holder)
            } else {
                startVideo(Uri.fromFile(File(savedPath)), holder)
            }
        }

        private fun startVideo(uri: Uri, holder: SurfaceHolder) {
            val rememberAudio = SettingsPrefs.rememberAudioChoice(applicationContext)
            val loop = SettingsPrefs.loopAutomatically(applicationContext)

            player = ExoPlayer.Builder(applicationContext).build().apply {
                setVideoSurface(holder.surface)
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
                volume = if (rememberAudio) 1f else 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                prepare()
            }
            if (!shouldPause()) {
                player?.play()
            }
        }

        private fun startGif(path: String, holder: SurfaceHolder) {
            try {
                gifMovie = Movie.decodeStream(FileInputStream(path))
                gifStartTime = 0
                if (!shouldPause()) {
                    gifRunning = true
                    gifHandler.post(gifDrawRunnable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    val scaledWidth = movie.width() * scale
                    val scaledHeight = movie.height() * scale
                    val dx = (canvas.width - scaledWidth) / 2f
                    val dy = (canvas.height - scaledHeight) / 2f
                    canvas.translate(dx, dy)
                    canvas.scale(scale, scale)
                    movie.draw(canvas, 0f, 0f)
                    canvas.restore()
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
