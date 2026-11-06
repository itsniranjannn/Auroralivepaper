package com.niranjan.auroralivepaper.ui.theme

import androidx.compose.ui.graphics.Color

val AuroraDeepPurple = Color(0xFF531253)
val AuroraRoyalViolet = Color(0xFF7F2CCB)
val AuroraStormyTeal = Color(0xFF2C666E)
val AuroraFrostedBlue = Color(0xFF90DDF0)
val AuroraBackgroundDark = Color(0xFF0A090C)
val AuroraSurfaceIndigo = Color(0xFF2A2D43)
val AuroraGlow = Color(0xFF97D8B2)
val AuroraOnBackground = Color(0xFFEDEDF2)

// Legacy names kept as aliases so existing code doesn't break
val AuroraViolet = AuroraRoyalViolet
val AuroraTeal = AuroraStormyTeal
val AuroraCoral = AuroraFrostedBlue
val AuroraBackground = AuroraBackgroundDark
val AuroraSurface = AuroraSurfaceIndigo

/**
 * Single source of truth for UI colors. Screens should use these instead of raw Color(0x...) values.
 * Text tiers are tuned for readable contrast (>= 4.5:1) on the near-black Aurora background.
 */
object AuroraTokens {
    val Mint = Color(0xFF7CE7D7)       // success / active / accent
    val Danger = Color(0xFFFF7675)     // destructive actions
    val Warning = Color(0xFFFFB74D)    // paused / not applied / caution

    val TextPrimary = Color.White.copy(alpha = 0.95f)
    val TextSecondary = Color.White.copy(alpha = 0.75f)
    val TextTertiary = Color.White.copy(alpha = 0.62f)
}
