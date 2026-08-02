package com.niranjan.auroralivepaper.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

data class ScreenSize(val widthPx: Int, val heightPx: Int)

fun getScreenSize(context: Context): ScreenSize {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    windowManager.defaultDisplay.getRealMetrics(metrics)
    return ScreenSize(metrics.widthPixels, metrics.heightPixels)
}

fun centerCropScale(mediaWidth: Int, mediaHeight: Int, screen: ScreenSize): Float {
    if (mediaWidth == 0 || mediaHeight == 0) return 1f
    val scaleX = screen.widthPx.toFloat() / mediaWidth
    val scaleY = screen.heightPx.toFloat() / mediaHeight
    return maxOf(scaleX, scaleY)
}