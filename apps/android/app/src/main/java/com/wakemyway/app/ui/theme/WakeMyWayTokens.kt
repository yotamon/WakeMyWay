package com.wakemyway.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object WmwColors {
    val Ink = Color(0xFF101115)
    val DeepDawn = Color(0xFF242129)
    val Clay = Color(0xFFB9755A)
    val SoftEmber = Color(0xFFE4A17F)
    val MorningPaper = Color(0xFFF1E8DC)
    val WarmLight = Color(0xFFF7F3EC)
    val Sage = Color(0xFF8EA18B)

    // Night surfaces stay neutral. Warmth belongs to bounded dawn illumination and actions,
    // not to a full-screen brown wash.
    val NightSurface = Color(0xFF15161A)
    val ElevatedNightSurface = Color(0xFF1D1D22)
    val QuietSurface = Color(0xFF18191E)
    val RaisedSurface = Color(0xFF222228)
    val QuietText = Color(0xFFA8A5A2)
    val Hairline = Color(0x26F1E8DC)
    val StrongHairline = Color(0x40F1E8DC)
    val EmberGlow = Color(0x33E4A17F)
    val ClayGlow = Color(0x2EB9755A)
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
}

object WmwSizes {
    val MinimumTouchTarget = 48.dp
    val SleepyTouchTarget = 56.dp
    val PrimaryActionHeight = 60.dp
    val PresenceCompact = 52.dp
    val PresenceSmall = 72.dp
    val PresenceLarge = 104.dp
}

object WmwMotion {
    const val EmergingAmbientMillis = 1_500
    const val EngagedTransitionMillis = 750
    const val ActiveFeedbackMillis = 360
    const val CompletionSettleMillis = 560
}
