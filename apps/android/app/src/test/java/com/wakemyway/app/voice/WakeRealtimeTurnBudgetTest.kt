package com.wakemyway.app.voice

import com.wakemyway.core.runtime.WakePolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeRealtimeTurnBudgetTest {
    @Test
    fun `default policy preserves the existing eight turn floor`() {
        assertEquals(8, WakeRealtimeTurnBudget.resolve(WakePolicy()))
    }

    @Test
    fun `maximum learned activation threshold has enough room to reach orientation`() {
        val policy = WakePolicy(
            activationThreshold = 8,
            maxEscalationLevel = 3,
        )

        assertEquals(12, WakeRealtimeTurnBudget.resolve(policy))
    }

    @Test
    fun `turn budget remains hard capped for unsupported extreme policies`() {
        val policy = WakePolicy(
            activationThreshold = 100,
            maxEscalationLevel = 100,
        )

        assertEquals(12, WakeRealtimeTurnBudget.resolve(policy))
    }
}
