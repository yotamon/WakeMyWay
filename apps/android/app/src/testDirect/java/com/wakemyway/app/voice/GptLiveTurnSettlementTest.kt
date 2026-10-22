package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GptLiveTurnSettlementTest {
    @Test fun userEvidenceWaitsForOldAssistantAudioToEnd() {
        val gate = GptLiveTurnSettlement()
        assertNull(gate.observe(false, true))
        assertEquals(false, gate.drain())
        assertNull(gate.drain())
    }

    @Test fun evidenceIsAtMostOnceAndCoherentValueWins() {
        val gate = GptLiveTurnSettlement()
        assertNull(gate.observe(true, true))
        assertNull(gate.observe(false, true))
        assertEquals(true, gate.drain())
        assertNull(gate.drain())
    }

    @Test fun staleObservationsCannotLeakIntoTheNextCue() {
        val gate = GptLiveTurnSettlement()
        assertEquals(true, gate.observe(true, false))
        assertNull(gate.observe(true, true))
        gate.clear()
        assertNull(gate.drain())
    }
}
