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

    @Test fun statusStagesAreHumanReadable() {
        assertTrue(GptLivePreviewMessages.progress("server-authentication").contains("Authorizing"))
        assertTrue(GptLivePreviewMessages.progress("ice-gathering").contains("network"))
    }
}
