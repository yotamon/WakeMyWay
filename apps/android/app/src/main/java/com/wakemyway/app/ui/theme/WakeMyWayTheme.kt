package com.wakemyway.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WakeMyWayColors = darkColorScheme(
    primary = WmwColors.SoftEmber,
    onPrimary = WmwColors.Ink,
    primaryContainer = WmwColors.Clay,
    onPrimaryContainer = WmwColors.WarmLight,
    secondary = WmwColors.Sage,
    onSecondary = WmwColors.Ink,
    background = WmwColors.Ink,
    onBackground = WmwColors.WarmLight,
    surface = WmwColors.NightSurface,
    onSurface = WmwColors.WarmLight,
    surfaceVariant = WmwColors.ElevatedNightSurface,
    onSurfaceVariant = WmwColors.QuietText,
    outline = WmwColors.Hairline,
    error = WmwColors.Danger,
)

/*
 * The concept board is built around a thin editorial serif paired with an understated grotesk.
 * Android's bundled serif is used deliberately so wake-time rendering remains local and reliable.
 * Light weights, generous leading and restrained tracking bring it much closer to the approved
 * composition than the previous heavier Material treatment.
 */
private val EditorialSerif = FontFamily.Serif
private val InterfaceSans = FontFamily.SansSerif

private val WakeMyWayTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Light,
        fontSize = 86.sp,
        lineHeight = 88.sp,
        letterSpacing = (-2.1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Light,
        fontSize = 70.sp,
        lineHeight = 74.sp,
        letterSpacing = (-1.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.45).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterfaceSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterfaceSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterfaceSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterfaceSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterfaceSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterfaceSans,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.35.sp,
    ),
)

private val WakeMyWayShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun WakeMyWayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WakeMyWayColors,
        typography = WakeMyWayTypography,
        shapes = WakeMyWayShapes,
        content = content,
    )
}
