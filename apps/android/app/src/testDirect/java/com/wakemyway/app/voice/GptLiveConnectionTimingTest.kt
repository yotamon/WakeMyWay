package com.wakemyway.app.voice

import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveConnectionTimingTest {
    @Test fun liveHttpDeadlineDoesNotRaceTheServerProviderDeadline() {
        assertTrue(GptLiveConnectionTiming.LIVE_BACKEND_TIMEOUT_MS >
            GptLiveConnectionTiming.PROVIDER_DEADLINE_MS)
    }

    @Test fun startupBudgetIncludesIceAndBoundedBackendWait() {
        assertTrue(GptLiveConnectionTiming.WEBRTC_STARTUP_TIMEOUT_MS >=
            GptLiveConnectionTiming.ICE_GATHERING_TIMEOUT_MS +
                GptLiveConnectionTiming.LIVE_BACKEND_TIMEOUT_MS + 4_000L)
    }

    @Test fun allPreSessionDeadlinesRemainFiniteAndWellBelowTwoMinutes() {
        val deadlines = listOf(
            GptLiveConnectionTiming.ACCOUNT_PREFLIGHT_TIMEOUT_MS,
            GptLiveConnectionTiming.ICE_GATHERING_TIMEOUT_MS,
            GptLiveConnectionTiming.LIVE_BACKEND_TIMEOUT_MS,
            GptLiveConnectionTiming.WEBRTC_STARTUP_TIMEOUT_MS,
        )
        assertTrue(deadlines.all { it > 0 && it < 120_000L })
    }
}
