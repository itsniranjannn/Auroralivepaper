package com.niranjan.auroralivepaper.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.niranjan.auroralivepaper.data.SettingsPrefs

/** Reads live device state and asks [PausePolicy] why the wallpaper would be paused right now. */
object PauseStatus {
    fun current(context: Context): PausePolicy.Reason? {
        return try {
            val app = context.applicationContext
            val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val sticky = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            var low = false
            if (sticky != null) {
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                if (level >= 0 && scale > 0) {
                    low = PausePolicy.isBatteryLow(
                        SettingsPrefs.pauseBelowThreshold(app),
                        charging,
                        (level * 100) / scale,
                        SettingsPrefs.batteryThreshold(app)
                    )
                }
            }
            PausePolicy.evaluate(
                isPreview = false,
                isVisible = true,
                pauseOnBatterySaver = SettingsPrefs.pauseOnBatterySaver(app),
                isPowerSave = pm?.isPowerSaveMode == true,
                isBatteryLow = low
            )
        } catch (_: Throwable) {
            null
        }
    }
}
