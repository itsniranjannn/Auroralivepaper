package com.niranjan.auroralivepaper

import android.graphics.Canvas
import android.graphics.Movie
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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

        private val gifDrawRunnable = object : Runnable {
            override fun run() {
                drawGifFrame()
                if (gifRunning) {
                    gifHandler.postDelayed(this, 33) // ~30fps
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder

            val prefs = applicationContext.getSharedPreferences("aurora_prefs", MODE_PRIVATE)
            val savedPath = prefs.getString("media_path", null)
            val mediaType = prefs.getString("media_type", "video")

            if (mediaType == "gif" && savedPath != null) {
                startGif(savedPath, holder)
            } else if (savedPath != null) {
                startVideo(Uri.fromFile(File(savedPath)), holder)
            }
            // No saved wallpaper yet -> leave the surface blank instead of
            // falling back to a bundled sample video. The user sets a
            // wallpaper via the app's picker before this engine has
            // anything real to play.
        }

        private fun startVideo(uri: Uri, holder: SurfaceHolder) {
            player = ExoPlayer.Builder(applicationContext).build().apply {
                setVideoSurface(holder.surface)
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = ExoPlayer.REPEAT_MODE_ALL
                volume = 0f
                videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                prepare()
            }
        }


        private fun startGif(path: String, holder: SurfaceHolder) {
            gifMovie = Movie.decodeStream(FileInputStream(path))
            gifStartTime = 0
            gifRunning = true
            gifHandler.post(gifDrawRunnable)
        }

        private fun drawGifFrame() {
            val movie = gifMovie ?: return
            val holder = currentHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    if (gifStartTime == 0L) gifStartTime = android.os.SystemClock.uptimeMillis()
                    val duration = if (movie.duration() == 0) 1000 else movie.duration()
                    val elapsed = ((android.os.SystemClock.uptimeMillis() - gifStartTime) % duration).toInt()
                    movie.setTime(elapsed)
                    canvas.drawColor(android.graphics.Color.BLACK)
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
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                player?.play()
                if (gifMovie != null && !gifRunning) {
                    gifRunning = true
                    gifHandler.post(gifDrawRunnable)
                }
            } else {
                player?.pause()
                gifRunning = false
                gifHandler.removeCallbacks(gifDrawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            player?.release()
            player = null
            gifRunning = false
            gifHandler.removeCallbacks(gifDrawRunnable)
            gifMovie = null
        }
    }
}