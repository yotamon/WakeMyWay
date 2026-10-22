package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveFallbackPolicyTest {
    @Test fun approvedEnglishAlfredCanFallbackWhenThereIsEnoughTime() {
        assertTrue(GptLiveFallbackPolicy.canStart("alfred", GptLiveWebRtcSession.Locale.EN,
            elapsedSinceConnectMillis = 1_000L, totalStartupMillis = 36_000L,
            legacyStartupMillis = 20_000L))
    }

    @Test fun wrongPersonaOrLanguageNeverStartsAlfredFallback() {
        for (character in listOf("luna", "rocco", "sam")) {
            assertFalse(GptLiveFallbackPolicy.canStart(character, GptLiveWebRtcSession.Locale.EN,
                0L, 36_000L, 20_000L))
        }
        assertFalse(GptLiveFallbackPolicy.canStart("alfred", GptLiveWebRtcSession.Locale.DE,
            0L, 36_000L, 20_000L))
        assertFalse(GptLiveFallbackPolicy.canStart("alfred", GptLiveWebRtcSession.Locale.HE,
            0L, 36_000L, 20_000L))
    }

    @Test fun lateDenialCannotInitiateAnOrphanedLegacySession() {
        assertFalse(GptLiveFallbackPolicy.canStart("alfred", GptLiveWebRtcSession.Locale.EN,
            20_000L, 36_000L, 20_000L))
        assertFalse(GptLiveFallbackPolicy.canStart("alfred", GptLiveWebRtcSession.Locale.EN,
            -1L, 36_000L, 20_000L))
    }
}
