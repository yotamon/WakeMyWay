package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveAudioActivityGateTest {
    @Test fun naturalPausesDoNotCompleteTheAssistantTurn() {
        val gate = GptLiveAudioActivityGate()
        assertTrue(gate.onAudiblePcm(1_000))
        assertFalse(gate.onAudiblePcm(1_200))
        assertFalse(gate.finishIfQuiet(3_400))
        assertFalse(gate.onAudiblePcm(3_500))
        assertFalse(gate.finishIfQuiet(5_900))
        assertTrue(gate.finishIfQuiet(6_300))
        assertNull(gate.remainingQuietMillis(7_000))
    }

    @Test fun lateTranscriptExtendsTheQuietWindow() {
        val gate = GptLiveAudioActivityGate()
        gate.onAudiblePcm(2_000)
        gate.onOutputTranscript(4_000)
        assertEquals(1_300L, gate.remainingQuietMillis(4_300))
        assertFalse(gate.finishIfQuiet(5_000))
        assertTrue(gate.finishIfQuiet(5_600))
    }

    @Test fun aClosedOrInterruptedSessionCannotLeakOldQuietEvidence() {
        val gate = GptLiveAudioActivityGate()
        gate.onOutputTranscript(0)
        gate.onAudiblePcm(100)
        gate.reset()
        assertFalse(gate.finishIfQuiet(20_000))
        assertNull(gate.remainingQuietMillis(20_000))
        assertTrue(gate.onAudiblePcm(21_000))
        assertFalse(gate.finishIfQuiet(21_500))
    }
}
