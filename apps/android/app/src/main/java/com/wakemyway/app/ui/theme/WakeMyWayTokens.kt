package com.wakemyway.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Wake My Way visual tokens are intentionally pulled from the approved concept board rather than
 * Material defaults. Dark surfaces lean slightly blue/green, warm accents stay muted and tactile,
 * and the oriented state moves onto a paper-like morning surface.
 */
object WmwColors {
    val Ink = Color(0xFF080B0D)
    val DeepDawn = Color(0xFF17191C)
    val Clay = Color(0xFFC18463)
    val SoftEmber = Color(0xFFE6BD9A)
    val MorningPaper = Color(0xFFF2E4D8)
    val WarmLight = Color(0xFFF5EFE8)
    val Sage = Color(0xFF747A66)
    val Success = Color(0xFF48D487)

    val NightSurface = Color(0xFF15191B)
    val ElevatedNightSurface = Color(0xFF242728)
    val QuietText = Color(0xFFB8AEA5)
    val FaintText = Color(0xFF817B76)
    val Hairline = Color(0x3DF2E4D8)
    val DarkHairline = Color(0x24101115)
    val EmberGlow = Color(0x80E7A577)
    val ClayGlow = Color(0x55C18463)
    val PaperCard = Color(0xFFE9DCCE)
    val Danger = Color(0xFFE35D45)
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
    val HomeWaveHeight = 58.dp
    val WakeWaveHeight = 92.dp
    val MicOrb = 132.dp
}

object WmwMotion {
    const val EmergingAmbientMillis = 1_800
    const val EngagedTransitionMillis = 850
    const val ActiveFeedbackMillis = 420
    const val CompletionSettleMillis = 640
}
