package org.helix.survix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.helix.survix.R

/** Titles: Bricolage Grotesque 600 with negative tracking (Dusk display styles). */
val Bricolage = FontFamily(Font(R.font.bricolage_semibold, FontWeight.SemiBold))

/** Everything else: Geist. */
val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

private fun display(size: Int, lineHeight: Double, tracking: Double) = TextStyle(
    fontFamily = Bricolage,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.em,
)

private fun sans(size: Double, weight: FontWeight, lineHeight: Double, tracking: Double = 0.0) = TextStyle(
    fontFamily = Geist,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.em,
)

/** The Dusk type scale mapped onto Material roles. */
val SurvixTypography = Typography(
    displayLarge = display(48, 1.04, -0.035),
    displayMedium = display(40, 1.06, -0.03),
    displaySmall = display(34, 1.08, -0.03),
    headlineLarge = display(32, 1.1, -0.02),
    headlineMedium = display(28, 1.15, -0.02),
    headlineSmall = display(26, 1.2, -0.02),
    titleLarge = display(22, 1.25, -0.02),
    titleMedium = sans(17.0, FontWeight.SemiBold, 1.4, -0.01),
    titleSmall = sans(15.0, FontWeight.Medium, 1.45),
    bodyLarge = sans(16.0, FontWeight.Normal, 1.5),
    bodyMedium = sans(15.0, FontWeight.Normal, 1.6),
    bodySmall = sans(13.0, FontWeight.Normal, 1.55),
    labelLarge = sans(15.0, FontWeight.Medium, 1.5),
    labelMedium = sans(13.0, FontWeight.Medium, 1.45),
    labelSmall = sans(12.0, FontWeight.Medium, 1.35),
)
