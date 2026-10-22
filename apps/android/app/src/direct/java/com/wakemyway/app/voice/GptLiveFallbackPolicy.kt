package com.wakemyway.app.voice

/**
 * Legacy Realtime is Alfred/English-only. Never surprise a sleepy person with
 * another language/character, or begin a backup session too close to the local
 * controller's startup deadline. This is allowed only after explicit
 * pre-provider-admission Live denial.
 */
internal object GptLiveFallbackPolicy {
    private const val CALLBACK_MARGIN_MS = 1_000L

    fun canStart(
        selectedCharacterId: String,
        selectedLocale: GptLiveWebRtcSession.Locale,
        elapsedSinceConnectMillis: Long,
        totalStartupMillis: Long,
        legacyStartupMillis: Long,
    ): Boolean {
        if (selectedCharacterId != "alfred" || selectedLocale != GptLiveWebRtcSession.Locale.EN) {
            return false
        }
        if (elapsedSinceConnectMillis < 0L || legacyStartupMillis < 0L) return false
        return elapsedSinceConnectMillis + legacyStartupMillis + CALLBACK_MARGIN_MS <= totalStartupMillis
    }
}
