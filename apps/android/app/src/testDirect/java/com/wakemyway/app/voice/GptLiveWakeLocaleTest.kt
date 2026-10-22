package com.wakemyway.app.voice

import com.wakemyway.core.personalization.WakeSpokenLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class GptLiveWakeLocaleTest {
    @Test fun explicitSelectionWinsOverAndroidSystemLanguage() {
        assertEquals(GptLiveWebRtcSession.Locale.HE,
            resolveWakeLiveLocale(WakeSpokenLanguage.HEBREW, "de"))
        assertEquals(GptLiveWebRtcSession.Locale.DE,
            resolveWakeLiveLocale(WakeSpokenLanguage.GERMAN, "en"))
        assertEquals(GptLiveWebRtcSession.Locale.EN,
            resolveWakeLiveLocale(WakeSpokenLanguage.ENGLISH, "he"))
    }

    @Test fun followDeviceSupportsHebrewAliasesAndUnknownLanguages() {
        assertEquals(GptLiveWebRtcSession.Locale.HE,
            resolveWakeLiveLocale(WakeSpokenLanguage.SYSTEM, "iw"))
        assertEquals(GptLiveWebRtcSession.Locale.HE,
            resolveWakeLiveLocale(WakeSpokenLanguage.SYSTEM, "he"))
        assertEquals(GptLiveWebRtcSession.Locale.DE,
            resolveWakeLiveLocale(WakeSpokenLanguage.SYSTEM, "de"))
        assertEquals(GptLiveWebRtcSession.Locale.EN,
            resolveWakeLiveLocale(WakeSpokenLanguage.SYSTEM, "es"))
    }
}
