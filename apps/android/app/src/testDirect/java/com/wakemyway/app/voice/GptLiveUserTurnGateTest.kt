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
        assertFalse(GptLiveUserTurnGate.isPlausible("yes", false, 2_000L, true))
        assertTrue(GptLiveUserTurnGate.isPlausible("yes", false, 4_500L, true))
    }

    @Test fun distinctFastHumanReplyIsNotIgnored() {
        assertTrue(GptLiveUserTurnGate.isPlausible("I am tired", false, 2_000L, false))
        assertTrue(GptLiveUserTurnGate.isPlausible("כן, רגע", false, 2_000L, false))
        assertFalse(GptLiveUserTurnGate.isPlausible("I am tired", true, 2_000L, false))
    }

    @Test fun duplicateFingerprintNormalizesCaseAndWhitespaceWithoutRetainingText() {
        assertTrue(
            GptLiveUserTurnGate.fingerprint(" Ja  Klar ") ==
                GptLiveUserTurnGate.fingerprint("ja klar"),
        )
        assertFalse(
            GptLiveUserTurnGate.fingerprint("Ja klar") ==
                GptLiveUserTurnGate.fingerprint("Nein"),
        )
    }
}
