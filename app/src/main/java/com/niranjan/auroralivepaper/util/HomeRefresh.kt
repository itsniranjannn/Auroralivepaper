package com.niranjan.auroralivepaper.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Bumped by MainActivity.onResume(). The home screen re-reads the active wallpaper and the recents
 * list whenever this changes, so it is never stale after returning from the system wallpaper chooser.
 */
object HomeRefresh {
    val tick = MutableStateFlow(0)
    fun bump() { tick.value = tick.value + 1 }
}
