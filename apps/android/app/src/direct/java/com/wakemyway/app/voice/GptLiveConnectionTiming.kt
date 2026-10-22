package com.wakemyway.app.voice

/**
 * One absolute admission-to-session-start deadline for Direct waking and
 * Voice Lab. Sub-stage timeouts must not extend this total budget.
 * The local alarm stays independent of these clocks.
 */
internal object GptLiveConnectionTiming {
    const val TOTAL_STARTUP_TIMEOUT_MS = 35_000L
    const val MIN_REMAINING_BEFORE_SDP_EXCHANGE_MS = 18_000L

    fun remainingStartupMillis(startedAt: Long, now: Long): Long =
        (TOTAL_STARTUP_TIMEOUT_MS - (now - startedAt).coerceAtLeast(0L)).coerceAtLeast(0L)

    // GPT-Live upstream creation is bounded to 15 seconds on the cloud side.
    const val PROVIDER_DEADLINE_MS = 15_000L

    // Credential refresh happens before WebRTC takes over the audio route.
    const val ACCOUNT_PREFLIGHT_TIMEOUT_MS = 20_000L

    // Prefer COMPLETE. With usable candidates after this grace period, send
    // the candidate-rich offer rather than timing out indefinitely.
    const val ICE_GATHERING_TIMEOUT_MS = 8_000L

    // Cover provider deadline plus authorization, cold-start and transit.
    const val LIVE_BACKEND_TIMEOUT_MS = 28_000L

    // Sub-stage timeouts do not reset the absolute startup budget.
}
