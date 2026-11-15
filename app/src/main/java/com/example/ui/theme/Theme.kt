package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GameColorScheme =
  darkColorScheme(
    primary = RacingOrangeAccent,
    onPrimary = TextPrimary,
    primaryContainer = MidnightBluePrimary,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlueSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = CardNavyElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = GoldenYellowHighlight,
    onTertiary = DarkNavyBg,
    background = DarkNavyBg,
    onBackground = TextPrimary,
    surface = CardNavy,
    onSurface = TextPrimary,
    surfaceVariant = CardNavyElevated,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = TextPrimary,
  )

val GameShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun HillRushIndiaTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = GameColorScheme,
    typography = HillRushTypography,
    shapes = GameShapes,
    content = content,
  )
}
