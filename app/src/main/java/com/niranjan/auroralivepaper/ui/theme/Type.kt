package com.niranjan.auroralivepaper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.niranjan.auroralivepaper.R

val ManropeFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = ManropeFamily),
    displayMedium = base.displayMedium.copy(fontFamily = ManropeFamily),
    displaySmall = base.displaySmall.copy(fontFamily = ManropeFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = ManropeFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = ManropeFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = ManropeFamily),
    titleLarge = base.titleLarge.copy(fontFamily = ManropeFamily),
    titleMedium = base.titleMedium.copy(fontFamily = ManropeFamily),
    titleSmall = base.titleSmall.copy(fontFamily = ManropeFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = ManropeFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = ManropeFamily),
    bodySmall = base.bodySmall.copy(fontFamily = ManropeFamily),
    labelLarge = base.labelLarge.copy(fontFamily = ManropeFamily),
    labelMedium = base.labelMedium.copy(fontFamily = ManropeFamily),
    labelSmall = base.labelSmall.copy(fontFamily = ManropeFamily)
)
