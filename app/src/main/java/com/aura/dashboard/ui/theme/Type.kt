package com.aura.dashboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aura.dashboard.R

/** Manrope (SIL OFL 1.1), bundled so Latin and Cyrillic text render consistently offline. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
)

val AuraTypography = Typography(
    displayLarge = style(44, FontWeight.Bold, 50, -0.02),
    displayMedium = style(36, FontWeight.Bold, 42, -0.02),
    displaySmall = style(32, FontWeight.Bold, 38, -0.02),
    headlineLarge = style(30, FontWeight.Bold, 36, -0.015),
    headlineMedium = style(26, FontWeight.Bold, 32, -0.01),
    headlineSmall = style(22, FontWeight.SemiBold, 28, -0.01),
    titleLarge = style(20, FontWeight.SemiBold, 26, -0.005),
    titleMedium = style(17, FontWeight.SemiBold, 23),
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 23),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(13, FontWeight.Normal, 18),
    labelLarge = style(14, FontWeight.SemiBold, 20),
    labelMedium = style(13, FontWeight.Medium, 18),
    labelSmall = style(11, FontWeight.SemiBold, 16, 0.02),
)
