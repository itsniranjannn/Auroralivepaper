package com.niranjan.auroralivepaper.util

import com.niranjan.auroralivepaper.data.SettingsPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PausePolicyTest {

    private fun eval(
        preview: Boolean = false,
        visible: Boolean = true,
        saverRule: Boolean = false,
        powerSave: Boolean = false,
        low: Boolean = false
    ) = PausePolicy.evaluate(preview, visible, saverRule, powerSave, low)

    @Test fun playsWhenVisibleAndNoRuleTriggers() = assertNull(eval())

    @Test fun pausesWhenNotVisible() = assertEquals(PausePolicy.Reason.NOT_VISIBLE, eval(visible = false))

    @Test fun batterySaverRuleOnAndSaverActive_pauses() =
        assertEquals(PausePolicy.Reason.BATTERY_SAVER, eval(saverRule = true, powerSave = true))

    /** Regression: Battery Saver active but the user did NOT opt in -> must keep playing. */
    @Test fun batterySaverActiveButRuleOff_keepsPlaying() = assertNull(eval(saverRule = false, powerSave = true))

    @Test fun batterySaverRuleOnButSaverInactive_keepsPlaying() = assertNull(eval(saverRule = true, powerSave = false))

    @Test fun lowBatteryPauses() = assertEquals(PausePolicy.Reason.LOW_BATTERY, eval(low = true))

    @Test fun previewNeverPauses() =
        assertNull(eval(preview = true, visible = false, saverRule = true, powerSave = true, low = true))

    @Test fun notVisibleWinsOverOtherReasons() =
        assertEquals(PausePolicy.Reason.NOT_VISIBLE, eval(visible = false, saverRule = true, powerSave = true, low = true))

    @Test fun lowBatteryVerdict_respectsOptionChargingAndThreshold() {
        assertTrue(PausePolicy.isBatteryLow(true, false, 20, 20))
        assertTrue(PausePolicy.isBatteryLow(true, false, 5, 20))
        assertFalse(PausePolicy.isBatteryLow(true, false, 21, 20))
        assertFalse("charging must never count as low", PausePolicy.isBatteryLow(true, true, 3, 20))
        assertFalse("option off must never pause", PausePolicy.isBatteryLow(false, false, 3, 20))
    }

    /** Locks the defaults that caused the "wallpaper stuck on first frame" regression. */
    @Test fun pauseRulesAreOffByDefault() {
        assertFalse(SettingsPrefs.DEFAULT_PAUSE_ON_BATTERY_SAVER)
        assertFalse(SettingsPrefs.DEFAULT_PAUSE_BELOW_THRESHOLD)
        assertEquals(20, SettingsPrefs.DEFAULT_BATTERY_THRESHOLD)
    }

    @Test fun playbackDefaults() {
        assertFalse("sound must be off by default", SettingsPrefs.DEFAULT_PLAY_SOUND)
        assertTrue("loop must be on by default", SettingsPrefs.DEFAULT_LOOP)
    }
}
