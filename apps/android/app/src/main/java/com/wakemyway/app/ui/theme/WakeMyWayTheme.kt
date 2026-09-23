package com.wakemyway.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakemyway.app.product.AppAppearance

data class WmwPlanningAtmosphere(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val sunriseGlow: Color,
    val sunriseGlowAlpha: Float,
    val dawnGlow: Color,
    val dawnGlowAlpha: Float,
)

private val DaylightAtmosphere = WmwPlanningAtmosphere(
    top = WmwColors.Paper,
    middle = WmwColors.Cloud,
    bottom = Color(0xFFF3F1F7),
    sunriseGlow = WmwColors.Sunrise,
    sunriseGlowAlpha = 0.12f,
    dawnGlow = WmwColors.Dawn,
    dawnGlowAlpha = 0.10f,
)

private val WarmSunriseAtmosphere = WmwPlanningAtmosphere(
    top = WmwColors.Paper,
    middle = WmwColors.MorningPaper,
    bottom = Color(0xFFFFEFE4),
    sunriseGlow = WmwColors.SunriseSoft,
    sunriseGlowAlpha = 0.18f,
    dawnGlow = WmwColors.GoldenLight,
    dawnGlowAlpha = 0.09f,
)

private val SoftDawnAtmosphere = WmwPlanningAtmosphere(
    top = WmwColors.Paper,
    middle = Color(0xFFF6F5FA),
    bottom = Color(0xFFECEFFA),
    sunriseGlow = WmwColors.Sunrise,
    sunriseGlowAlpha = 0.08f,
    dawnGlow = WmwColors.Dawn,
    dawnGlowAlpha = 0.18f,
)

val LocalWmwPlanningAtmosphere = staticCompositionLocalOf { DaylightAtmosphere }

private val DaylightColors = lightColorScheme(
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

private val WarmSunriseColors = lightColorScheme(
    primary = WmwColors.Sunrise,
    onPrimary = WmwColors.Midnight,
    primaryContainer = Color(0xFFFFD7C2),
    onPrimaryContainer = WmwColors.Midnight,
    secondary = Color(0xFF8A74C9),
    onSecondary = WmwColors.WarmLight,
    secondaryContainer = Color(0xFFF1E9FF),
    onSecondaryContainer = WmwColors.Midnight,
    background = Color(0xFFFFF6EE),
    onBackground = WmwColors.Midnight,
    surface = WmwColors.Paper,
    onSurface = WmwColors.Midnight,
    surfaceVariant = Color(0xFFFFEEE4),
    onSurfaceVariant = WmwColors.LightQuietText,
    outline = WmwColors.DarkHairline,
    error = WmwColors.Danger,
)

private val SoftDawnColors = lightColorScheme(
    primary = WmwColors.DawnDeep,
    onPrimary = WmwColors.WarmLight,
    primaryContainer = Color(0xFFE3E7FF),
    onPrimaryContainer = WmwColors.Midnight,
    secondary = WmwColors.Sunrise,
    onSecondary = WmwColors.Midnight,
    secondaryContainer = Color(0xFFFFE8DC),
    onSecondaryContainer = WmwColors.Midnight,
    background = Color(0xFFF3F4FA),
    onBackground = WmwColors.Midnight,
    surface = WmwColors.Paper,
    onSurface = WmwColors.Midnight,
    surfaceVariant = Color(0xFFEEEFFA),
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
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.Light,
        fontSize = 76.sp,
        lineHeight = 80.sp,
        letterSpacing = (-2.0).sp,
    ),
    displayMedium = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.Light,
        fontSize = 62.sp,
        lineHeight = 66.sp,
        letterSpacing = (-1.6).sp,
    ),
    headlineLarge = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 39.sp,
        letterSpacing = (-0.9).sp,
    ),
    headlineMedium = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyLarge = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
        fontFamily = BrandSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.2.sp,
    ),
    labelSmall = TextStyle(
        textDirection = TextDirection.ContentOrLtr,
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
fun WakeMyWayTheme(
    appearance: AppAppearance = AppAppearance.DAYLIGHT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appearance) {
        AppAppearance.DAYLIGHT -> DaylightColors
        AppAppearance.WARM_SUNRISE -> WarmSunriseColors
        AppAppearance.SOFT_DAWN -> SoftDawnColors
    }
    val atmosphere = when (appearance) {
        AppAppearance.DAYLIGHT -> DaylightAtmosphere
        AppAppearance.WARM_SUNRISE -> WarmSunriseAtmosphere
        AppAppearance.SOFT_DAWN -> SoftDawnAtmosphere
    }

    CompositionLocalProvider(LocalWmwPlanningAtmosphere provides atmosphere) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WakeMyWayTypography,
            shapes = WakeMyWayShapes,
            content = content,
        )
    }
}
