package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveUserTurnGateTest {
    @Test fun shortIntentionalSpeechCanBeEvidence() {
        assertTrue(GptLiveUserTurnGate.isPlausible("yes", false, 10_000L))
        assertTrue(GptLiveUserTurnGate.isPlausible("כן", false, 10_000L))
        assertTrue(GptLiveUserTurnGate.isPlausible("Ja klar", false, 10_000L))
    }

    @Test fun audioEchoOrIncompleteFragmentsNeverCount() {
        assertFalse(GptLiveUserTurnGate.isPlausible("Good morning", true, 10_000L))
        assertFalse(GptLiveUserTurnGate.isPlausible("...", false, 10_000L))
        assertFalse(GptLiveUserTurnGate.isPlausible("uh", false, 10_000L))
    }

    @Test fun repeatedTranscriptBurstsHaveABoundedCooldown() {
        assertFalse(GptLiveUserTurnGate.isPlausible("yes", false, 1_100L))
        assertTrue(GptLiveUserTurnGate.isPlausible("yes", false, 4_500L))
    }
}
