package com.niranjan.auroralivepaper.util

object MediaValidator {
    fun evaluate(
        width: Int = 0,
        height: Int = 0,
        fps: Float = 0f,
        durationMs: Long = 0L,
        sizeBytes: Long = 0L
    ): List<String> {
        val warnings = mutableListOf<String>()

        val longSide = maxOf(width, height)
        if (longSide > 1920) {
            warnings.add("Resolution is high ($longSide px). May increase GPU and battery usage.")
        }

        if (fps > 30f) {
            warnings.add("Frame rate is high (${fps.toInt()} FPS). May increase battery usage.")
        }

        if (durationMs > 60_000L) {
            val seconds = durationMs / 1000L
            warnings.add("Duration is long (${seconds}s). May use more memory.")
        }

        val sizeMb = sizeBytes / (1024L * 1024L)
        if (sizeBytes > 150L * 1024L * 1024L) {
            warnings.add("File size is large (${sizeMb} MB). May slow down wallpaper loading.")
        }

        return warnings
    }
}
