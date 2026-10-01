package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeResponseBudgetTest {
    @Test
    fun `user facing audio keeps proven headroom from truncation regression`() {
        // PR #174 proved that ~120 Realtime output tokens can clip a normal spoken wake turn.
        assertEquals(1_024, REALTIME_USER_RESPONSE_MAX_OUTPUT_TOKENS)
        assertTrue(REALTIME_USER_RESPONSE_MAX_OUTPUT_TOKENS > 128)
    }
}
