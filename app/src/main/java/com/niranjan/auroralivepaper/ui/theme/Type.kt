package com.niranjan.auroralivepaper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.niranjan.auroralivepaper.R

/**
 * Manrope is bundled as ONE variable font (wght axis). The previous setup loaded the same file
 * five times as "static" weights, so every weight rendered at the font's thin default (~200).
 * Each weight below now sets the variable axis explicitly. minSdk is 26, which supports this.
 */
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val ManropeFamily = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold)
)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal),
    bodySmall = base.bodySmall.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Normal),
    labelLarge = base.labelLarge.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = ManropeFamily, fontWeight = FontWeight.Medium)
)
