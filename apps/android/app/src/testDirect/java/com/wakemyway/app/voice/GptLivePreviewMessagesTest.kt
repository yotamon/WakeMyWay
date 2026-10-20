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

    @Test fun missingCandidatesProduceActionableOfflineGuidance() {
        val text = GptLivePreviewMessages.failure("ice-no-candidates")
        assertTrue(text.contains("audio route"))
        assertTrue(text.contains("mobile data"))
        assertFalse(text.contains("billing"))
    }

    @Test fun statusStagesAreHumanReadable() {
        assertTrue(GptLivePreviewMessages.progress("server-authentication").contains("Authorizing"))
        assertTrue(GptLivePreviewMessages.progress("ice-gathering").contains("network"))
    }
}
