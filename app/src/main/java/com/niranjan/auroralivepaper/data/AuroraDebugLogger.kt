package com.niranjan.auroralivepaper.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AuroraDebugLogger {
    private const val MAX_SIZE = 200 * 1024L // 200 KB
    private const val TRIM_SIZE = 100 * 1024 // 100 KB
    private const val FILE_NAME = "aurora_debug.log"

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "aurora-debug-logger").apply {
            isDaemon = true
        }
    }

    private val dateFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    fun log(context: Context, event: String) {
        val appContext = context.applicationContext
        val now = Date()
        executor.execute {
            try {
                val file = File(appContext.filesDir, FILE_NAME)
                val formatter = dateFormatThreadLocal.get() ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                val timestamp = formatter.format(now)
                val line = "[$timestamp] $event\n"

                if (file.exists() && file.length() > MAX_SIZE) {
                    trimLogFile(file)
                }

                file.appendText(line)
                Log.d("AuroraDebug", event)
            } catch (t: Throwable) {
                Log.e("AuroraDebugLogger", "Failed to write log", t)
            }
        }
    }

    private fun trimLogFile(file: File) {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val len = raf.length()
                if (len > MAX_SIZE) {
                    val readStart = len - TRIM_SIZE
                    raf.seek(readStart)
                    val buffer = ByteArray(TRIM_SIZE)
                    raf.readFully(buffer)
                    raf.setLength(0)
                    raf.seek(0)
                    raf.write(buffer)
                }
            }
        } catch (t: Throwable) {
            Log.e("AuroraDebugLogger", "Failed to trim log file", t)
        }
    }

    fun getLogFile(context: Context): File {
        val appContext = context.applicationContext
        try {
            val future = executor.submit { /* flush pending writes */ }
            future.get(300, TimeUnit.MILLISECONDS)
        } catch (_: Throwable) {
            // Proceed if flush times out or executor is interrupted
        }

        val file = File(appContext.filesDir, FILE_NAME)
        if (!file.exists()) {
            try {
                file.writeText("=== Aurora Live Paper Debug Log Initialized ===\n")
            } catch (_: Throwable) {}
        }
        return file
    }
}