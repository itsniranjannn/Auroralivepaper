package com.niranjan.auroralivepaper.data

import android.content.Context

/**
 * Lightweight SharedPreferences-backed settings store for Aurora Live Paper.
 * Uses a dedicated prefs file ("aurora_settings_prefs") so it never collides
 * with the wallpaper-state prefs ("aurora_prefs").
 */
object SettingsPrefs {

    private const val PREFS_NAME = "aurora_settings_prefs"

    // Keys
    private const val KEY_PAUSE_ON_BATTERY_SAVER = "pause_on_battery_saver"
    private const val KEY_PAUSE_BELOW_THRESHOLD = "pause_below_threshold"
    private const val KEY_BATTERY_THRESHOLD = "battery_threshold"

    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_RICH_MOTION = "rich_motion"

    private const val KEY_PLAY_WALLPAPER_SOUND = "play_wallpaper_sound"
    private const val KEY_WALLPAPER_VOLUME = "wallpaper_volume"
    private const val KEY_RESOLUTION_MODE = "resolution_mode"
    private const val KEY_SCALING_MODE = "scaling_mode"
    private const val KEY_LOOP_AUTOMATICALLY = "loop_automatically"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Battery & Performance ----------

    fun pauseOnBatterySaver(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAUSE_ON_BATTERY_SAVER, false)

    fun setPauseOnBatterySaver(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAUSE_ON_BATTERY_SAVER, value).apply()
    }

    fun pauseBelowThreshold(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAUSE_BELOW_THRESHOLD, false)

    fun setPauseBelowThreshold(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAUSE_BELOW_THRESHOLD, value).apply()
    }

    fun batteryThreshold(context: Context): Int =
        prefs(context).getInt(KEY_BATTERY_THRESHOLD, 20)

    fun setBatteryThreshold(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_BATTERY_THRESHOLD, value).apply()
    }

    // ---------- Experience ----------

    fun hapticsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAPTICS_ENABLED, true)

    fun setHapticsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()
    }

    fun richMotion(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RICH_MOTION, true)

    fun setRichMotion(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_RICH_MOTION, value).apply()
    }

    // ---------- Wallpaper Playback ----------

    fun playWallpaperSound(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PLAY_WALLPAPER_SOUND, false)

    fun setPlayWallpaperSound(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PLAY_WALLPAPER_SOUND, value).apply()
    }

    fun wallpaperVolume(context: Context): Float =
        prefs(context).getFloat(KEY_WALLPAPER_VOLUME, 0.5f)

    fun setWallpaperVolume(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_WALLPAPER_VOLUME, value.coerceIn(0f, 1f)).apply()
    }

    /** One of: "auto", "1080p", "1440p", "original" */
    fun resolutionMode(context: Context): String =
        prefs(context).getString(KEY_RESOLUTION_MODE, "auto") ?: "auto"

    fun setResolutionMode(context: Context, value: String) {
        prefs(context).edit().putString(KEY_RESOLUTION_MODE, value).apply()
    }

    /** One of: "fill" (zooms to fill screen), "fit" (preserves entire video with letterboxing) */
    fun scalingMode(context: Context): String =
        prefs(context).getString(KEY_SCALING_MODE, "fill") ?: "fill"

    fun setScalingMode(context: Context, value: String) {
        prefs(context).edit().putString(KEY_SCALING_MODE, value).apply()
    }

    fun loopAutomatically(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOOP_AUTOMATICALLY, true)

    fun setLoopAutomatically(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOOP_AUTOMATICALLY, value).apply()
    }
}
