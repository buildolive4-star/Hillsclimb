package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Gaming Typography Hierarchy: Rajdhani (Headings), Exo 2 (UI Labels), Inter (Supporting Text)
val RajdhaniFontFamily = FontFamily(Font(R.font.rajdhani, FontWeight.Bold))
val Exo2FontFamily = FontFamily(Font(R.font.exo_2, FontWeight.SemiBold))
val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Medium))

val HillRushTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.sp,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.8.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.6.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.5.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = Exo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = Exo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = Exo2FontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = Exo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
      ),
  )
