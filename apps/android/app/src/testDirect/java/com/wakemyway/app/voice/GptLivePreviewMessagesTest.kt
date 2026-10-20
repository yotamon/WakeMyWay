package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLivePreviewMessagesTest {
    @Test fun authenticationErrorsOfferAnActionableFix() {
        val text = GptLivePreviewMessages.failure("sign-in-required")
        assertTrue(text.contains("sign in"))
        assertTrue(text.contains("Regular alarms are unaffected"))
    }

    @Test fun iceGatheringFailureDoesNotPretendTheServerWasCalled() {
        val text = GptLivePreviewMessages.failure("ice-gathering-timeout")
        assertTrue(text.contains("audio route"))
        assertFalse(text.contains("billing"))
    }

    @Test fun providerDenialIsDistinguishableFromLocalAudioErrors() {
        assertTrue(GptLivePreviewMessages.failure("provider-or-server-unavailable").contains("OpenAI"))
        assertTrue(GptLivePreviewMessages.failure("offer-rejected").contains("WebRTC"))
    }

    @Test fun accountPreflightHasItsOwnErrorBeforeStartingAudio() {
        val text = GptLivePreviewMessages.failure("account-preflight-timeout")
        assertTrue(text.contains("Sign-in verification"))
        assertFalse(text.contains("OpenAI session is unavailable"))
        assertTrue(GptLivePreviewMessages.progress("account-check").contains("sign-in"))
    }

    @Test fun missingCandidatesProduceActionableOfflineGuidance() {
        val text = GptLivePreviewMessages.failure("ice-no-candidates")
        assertTrue(text.contains("audio route"))
        assertTrue(text.contains("mobile data"))
        assertFalse(text.contains("billing"))
    }

    @Test fun noAudioReportDoesNotClaimTheProviderFailed() {
        val text = GptLivePreviewMessages.audioNotHeard()
        assertTrue(text.contains("no-audible-playback"))
        assertTrue(text.contains("does not prove"))
        assertFalse(text.contains("provider unavailable"))
    }

    @Test fun statusStagesAreHumanReadable() {
        assertTrue(GptLivePreviewMessages.progress("server-authentication").contains("Authorizing"))
        assertTrue(GptLivePreviewMessages.progress("ice-gathering").contains("network"))
    }
}
