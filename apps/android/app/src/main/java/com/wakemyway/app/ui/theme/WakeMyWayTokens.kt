package com.wakemyway.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Canonical WakeMyWay brand tokens.
 *
 * The visual identity is built around a calm sunrise meeting the Wake Line: deep midnight navy,
 * warm sunrise peach, golden morning light, a restrained dawn lavender and clean cloud surfaces.
 * Runtime state may change the amount of light on screen, but the brand hues remain stable.
 */
object WmwColors {
    // Core brand palette from the approved WakeMyWay brand kit.
    val Midnight = Color(0xFF08142F)
    val DeepNavy = Color(0xFF10264C)
    val Sunrise = Color(0xFFFF9F6D)
    val SunriseSoft = Color(0xFFFFB88F)
    val GoldenLight = Color(0xFFFFD699)
    val Dawn = Color(0xFFA5B4FC)
    val DawnDeep = Color(0xFF7188E8)
    val Cloud = Color(0xFFF8F7F4)
    val Paper = Color(0xFFFFFCF8)

    // Compatibility names used by the existing product UI.
    val Ink = Midnight
    val DeepDawn = DeepNavy
    val Clay = Sunrise
    val SoftEmber = SunriseSoft
    val MorningPaper = Color(0xFFFFF7EF)
    val WarmLight = Color(0xFFFFFDFC)
    val Sage = Color(0xFF7F99D9)
    val Success = Color(0xFF3F9B72)

    val NightSurface = Color(0xFF0D1D3C)
    val ElevatedNightSurface = Color(0xFF152B50)
    val LightSurface = Paper
    val LightSurfaceMuted = Color(0xFFF3F1F2)
    val QuietText = Color(0xFFC6D0E2)
    val FaintText = Color(0xFF8290AA)
    val LightQuietText = Color(0xFF66728B)
    val LightFaintText = Color(0xFF9299A8)
    val Hairline = Color(0x2EFFFFFF)
    val DarkHairline = Color(0x1F08142F)
    val SunriseHairline = Color(0x42FF9F6D)
    val EmberGlow = Color(0x8CFFB483)
    val ClayGlow = Color(0x66FF9F6D)
    val DawnGlow = Color(0x55A5B4FC)
    val PaperCard = Color(0xFFFFFFFF)
    val Danger = Color(0xFFD95555)
}

object WmwSpacing {
    val Xxs = 4.dp
    val Xs = 8.dp
    val Sm = 12.dp
    val Md = 16.dp
    val Lg = 20.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 40.dp
    val Huge = 48.dp
    val Hero = 64.dp
    val Monument = 80.dp
}

object WmwSizes {
    val MinimumTouchTarget = 48.dp
    val SleepyTouchTarget = 56.dp
    val PrimaryActionHeight = 58.dp
    val CompactActionHeight = 52.dp
    val HomeWaveHeight = 64.dp
    val WakeWaveHeight = 108.dp
    val MicOrb = 132.dp
    val BrandMarkSmallWidth = 42.dp
    val BrandMarkSmallHeight = 24.dp
    val BrandMarkHeroWidth = 184.dp
    val BrandMarkHeroHeight = 92.dp

    // Compatibility for the legacy presence component while the product uses Wake Line surfaces.
    val PresenceSmall = 72.dp
    val PresenceLarge = 104.dp
}

object WmwMotion {
    const val EmergingAmbientMillis = 1_800
    const val EngagedTransitionMillis = 850
    const val ActiveFeedbackMillis = 420
    const val CompletionSettleMillis = 640
}
