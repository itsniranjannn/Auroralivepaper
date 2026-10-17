package com.niranjan.auroralivepaper.data

import android.content.Context

/**
 * Lightweight SharedPreferences-backed settings store for Aurora Live Paper.
 * Uses a dedicated prefs file so it never collides with the wallpaper-state
 * prefs ("aurora_prefs") used by AuroraWallpaperService / SettingsScreen.
 */
object SettingsPrefs {

    private const val PREFS_NAME = "aurora_settings_prefs"

    // Keys
    private const val KEY_PAUSE_ON_BATTERY_SAVER = "pause_on_battery_saver"
    private const val KEY_PAUSE_BELOW_THRESHOLD = "pause_below_threshold"
    private const val KEY_BATTERY_THRESHOLD = "battery_threshold"

    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    private const val KEY_RICH_MOTION = "rich_motion"

    private const val KEY_REMEMBER_AUDIO_CHOICE = "remember_audio_choice"
    private const val KEY_RESOLUTION_MODE = "resolution_mode"
    private const val KEY_LOOP_AUTOMATICALLY = "loop_automatically"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Battery & Performance ----------

    fun pauseOnBatterySaver(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAUSE_ON_BATTERY_SAVER, true)

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

    fun rememberAudioChoice(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMEMBER_AUDIO_CHOICE, false)

    fun setRememberAudioChoice(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMEMBER_AUDIO_CHOICE, value).apply()
    }

    /** One of: "auto", "1080p", "1440p", "original" */
    fun resolutionMode(context: Context): String =
        prefs(context).getString(KEY_RESOLUTION_MODE, "auto") ?: "auto"

    fun setResolutionMode(context: Context, value: String) {
        prefs(context).edit().putString(KEY_RESOLUTION_MODE, value).apply()
    }

    fun loopAutomatically(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOOP_AUTOMATICALLY, true)

    fun setLoopAutomatically(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOOP_AUTOMATICALLY, value).apply()
    }
}
