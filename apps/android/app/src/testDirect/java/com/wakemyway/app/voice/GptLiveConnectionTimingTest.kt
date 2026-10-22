package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveConnectionTimingTest {
    @Test fun preflightAndWebRtcShareOneAbsoluteBudget() {
        val start = 12_000L
        assertEquals(35_000L, GptLiveConnectionTiming.remainingStartupMillis(start, start))
        assertEquals(17_000L, GptLiveConnectionTiming.remainingStartupMillis(start, start + 18_000))
        assertEquals(0L, GptLiveConnectionTiming.remainingStartupMillis(start, start + 35_000))
        assertEquals(0L, GptLiveConnectionTiming.remainingStartupMillis(start, start + 70_000))
    }

    @Test fun aPaidExchangeNeedsEnoughRemainingBudget() {
        val start = 1_000L
        assertTrue(GptLiveConnectionTiming.remainingStartupMillis(start, start + 10_000) >=
            GptLiveConnectionTiming.MIN_REMAINING_BEFORE_SDP_EXCHANGE_MS)
        assertFalse(GptLiveConnectionTiming.remainingStartupMillis(start, start + 20_000) >=
            GptLiveConnectionTiming.MIN_REMAINING_BEFORE_SDP_EXCHANGE_MS)
    }
}
