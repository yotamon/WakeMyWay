package com.wakemyway.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WakeMyWayColors = lightColorScheme(
    primary = WmwColors.Sunrise,
    onPrimary = WmwColors.Midnight,
    primaryContainer = WmwColors.SunriseSoft,
    onPrimaryContainer = WmwColors.Midnight,
    secondary = WmwColors.DawnDeep,
    onSecondary = WmwColors.WarmLight,
    secondaryContainer = WmwColors.Dawn.copy(alpha = 0.28f),
    onSecondaryContainer = WmwColors.Midnight,
    background = WmwColors.Cloud,
    onBackground = WmwColors.Midnight,
    surface = WmwColors.Paper,
    onSurface = WmwColors.Midnight,
    surfaceVariant = WmwColors.LightSurfaceMuted,
    onSurfaceVariant = WmwColors.LightQuietText,
    outline = WmwColors.DarkHairline,
    error = WmwColors.Danger,
)

/*
 * Plus Jakarta Sans is the approved brand direction. Production wake surfaces must remain fully
 * local/offline, so the system sans family is used until the licensed font asset is bundled in the
 * application package. The hierarchy, weight and spacing intentionally mirror the approved kit.
 */
private val BrandSans = FontFamily.SansSerif

private val WakeMyWayTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Light,
        fontSize = 76.sp,
        lineHeight = 80.sp,
        letterSpacing = (-2.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Light,
        fontSize = 62.sp,
        lineHeight = 66.sp,
        letterSpacing = (-1.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 39.sp,
        letterSpacing = (-0.9).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.65.sp,
    ),
)

private val WakeMyWayShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
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
