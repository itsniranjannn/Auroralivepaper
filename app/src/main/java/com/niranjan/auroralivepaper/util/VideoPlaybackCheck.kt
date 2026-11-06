package com.niranjan.auroralivepaper.util

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

/**
 * Checks whether THIS phone's decoders can play a given video smoothly, so users get a
 * clear warning at import time instead of a stuttering wallpaper that also drains battery.
 *
 * Uses platform APIs only (no extra dependency, no decoding).
 */
object VideoPlaybackCheck {

    /**
     * @param mime  video track mime, e.g. "video/avc", "video/hevc", "video/x-vnd.on2.vp9"
     * @param width/height  coded size (orientation does not matter)
     * @param fps   nominal frame rate, 0 if unknown
     */
    fun evaluate(mime: String?, width: Int, height: Int, fps: Float): List<String> {
        if (mime.isNullOrEmpty() || width <= 0 || height <= 0) return emptyList()
        val warnings = mutableListOf<String>()
        try {
            val decoders = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                .filter { !it.isEncoder && it.supportedTypes.any { t -> t.equals(mime, ignoreCase = true) } }

            if (decoders.isEmpty()) {
                warnings.add("This phone has no decoder for this video format ($mime). It will not play as a wallpaper. Re-encode it as H.264 (MP4).")
                return warnings
            }

            val rate = if (fps > 0f) fps.toDouble() else 30.0
            val longSide = maxOf(width, height)
            val shortSide = minOf(width, height)

            val canDecode = decoders.any { info -> supports(info, mime, longSide, shortSide, rate) }
            if (!canDecode) {
                warnings.add("This phone cannot decode ${width}x$height at ${rate.toInt()} FPS smoothly. Use 1080p at 30 FPS or lower.")
                return warnings
            }

            // Software-only decoding burns CPU and battery and often drops frames.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hardwareOk = decoders.any { info ->
                    info.isHardwareAccelerated && supports(info, mime, longSide, shortSide, rate)
                }
                if (!hardwareOk) {
                    warnings.add("Only software decoding is available for this video. It may stutter and use more battery. H.264 is the safest format.")
                }
            }
        } catch (_: Throwable) {
            // Capability lookup is best-effort; never block an import because of it.
        }
        return warnings
    }

    private fun supports(
        info: MediaCodecInfo,
        mime: String,
        longSide: Int,
        shortSide: Int,
        fps: Double
    ): Boolean {
        return try {
            val type = info.supportedTypes.firstOrNull { it.equals(mime, ignoreCase = true) } ?: return false
            val video = info.getCapabilitiesForType(type).videoCapabilities ?: return false
            // Try both orientations: some decoders list limits per orientation.
            video.areSizeAndRateSupported(longSide, shortSide, fps) ||
                video.areSizeAndRateSupported(shortSide, longSide, fps)
        } catch (_: Throwable) {
            false
        }
    }
}
