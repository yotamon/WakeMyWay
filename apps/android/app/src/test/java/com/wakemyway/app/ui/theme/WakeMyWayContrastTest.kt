package com.wakemyway.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeMyWayContrastTest {
    @Test
    fun `canonical normal text tones meet WCAG AA on light surfaces`() {
        assertNormalText(WmwColors.DawnText, WmwColors.Paper, "DawnText on Paper")
        assertNormalText(WmwColors.DawnText, WmwColors.PaperCard, "DawnText on white card")
        assertNormalText(WmwColors.LightQuietText, WmwColors.Paper, "LightQuietText on Paper")
        assertNormalText(WmwColors.LightFaintText, WmwColors.Paper, "LightFaintText on Paper")
        assertNormalText(WmwColors.LightFaintText, WmwColors.MorningPaper, "LightFaintText on MorningPaper")
        assertNormalText(WmwColors.DangerText, WmwColors.Paper, "DangerText on Paper")
        assertNormalText(WmwColors.SuccessText, WmwColors.Paper, "SuccessText on Paper")
        assertNormalText(WmwColors.Midnight, WmwColors.Sunrise, "Midnight on Sunrise")
    }

    @Test
    fun `canonical dark surface text tones meet WCAG AA`() {
        assertNormalText(WmwColors.WarmLight, WmwColors.Midnight, "WarmLight on Midnight")
        assertNormalText(WmwColors.WarmLight, WmwColors.DeepNavy, "WarmLight on DeepNavy")
        assertNormalText(WmwColors.QuietText, WmwColors.Midnight, "QuietText on Midnight")
        assertNormalText(WmwColors.QuietText, WmwColors.NightSurface, "QuietText on NightSurface")
        assertNormalText(WmwColors.FaintText, WmwColors.Midnight, "FaintText on Midnight")
    }

    private fun assertNormalText(
        foreground: Color,
        background: Color,
        label: String,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$label contrast was $ratio; expected at least $NORMAL_TEXT_AA",
            ratio >= NORMAL_TEXT_AA,
        )
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        val lighter = maxOf(firstLuminance, secondLuminance)
        val darker = minOf(firstLuminance, secondLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private companion object {
        const val NORMAL_TEXT_AA = 4.5f
    }
}
