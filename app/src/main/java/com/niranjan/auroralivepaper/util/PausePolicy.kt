package com.niranjan.auroralivepaper.util

/**
 * Pure (Android-free) pause rules, shared by the wallpaper service and the app UI so the
 * "paused: <reason>" notice can never disagree with what the engine actually does.
 */
object PausePolicy {

    enum class Reason(val label: String) {
        NOT_VISIBLE("Not visible"),
        BATTERY_SAVER("Battery Saver is on"),
        LOW_BATTERY("Battery is low")
    }

    /** @return why playback must be paused, or null if it should play. */
    fun evaluate(
        isPreview: Boolean,
        isVisible: Boolean,
        pauseOnBatterySaver: Boolean,
        isPowerSave: Boolean,
        isBatteryLow: Boolean
    ): Reason? {
        if (isPreview) return null
        if (!isVisible) return Reason.NOT_VISIBLE
        if (pauseOnBatterySaver && isPowerSave) return Reason.BATTERY_SAVER
        if (isBatteryLow) return Reason.LOW_BATTERY
        return null
    }

    /** Low-battery verdict: only when the option is on, not charging, and at/below the threshold. */
    fun isBatteryLow(
        pauseBelowThreshold: Boolean,
        charging: Boolean,
        percent: Int,
        threshold: Int
    ): Boolean = pauseBelowThreshold && !charging && percent <= threshold
}
