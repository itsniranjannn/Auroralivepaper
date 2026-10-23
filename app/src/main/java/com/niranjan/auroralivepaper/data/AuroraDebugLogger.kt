package com.niranjan.auroralivepaper.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AuroraDebugLogger {
    private const val MAX_SIZE = 200 * 1024L // 200 KB
    private const val FILE_NAME = "aurora_debug.log"
    private val lock = Any()

    fun log(context: Context, event: String) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val line = "[$timestamp] $event\n"
            synchronized(lock) {
                if (file.exists() && file.length() > MAX_SIZE) {
                    val bytes = file.readBytes()
                    val keepStart = bytes.size - (100 * 1024)
                    if (keepStart > 0) {
                        val trimmed = bytes.copyOfRange(keepStart, bytes.size)
                        file.writeBytes(trimmed)
                    }
                }
                file.appendText(line)
            }
            Log.d("AuroraDebug", event)
        } catch (e: Exception) {
            Log.e("AuroraDebugLogger", "Failed to write log", e)
        }
    }

    fun getLogFile(context: Context): File {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            file.writeText("=== Aurora Live Paper Debug Log Initialized ===\n")
        }
        return file
    }
}

