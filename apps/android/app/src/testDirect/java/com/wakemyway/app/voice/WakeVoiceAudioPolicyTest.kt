package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeVoiceAudioPolicyTest {
    @Test fun waitingForProviderCannotQuietTheLocalAlarm() {
        assertFalse(WakeVoiceAudioPolicy.voiceWindowNeeded(
            remotePcmActive = false, listeningForUser = false,
        ))
    }

    @Test fun assistantPcmCanBrieflyDuckNativeAlarmSound() {
        assertTrue(WakeVoiceAudioPolicy.voiceWindowNeeded(
            remotePcmActive = true, listeningForUser = false,
        ))
    }

    @Test fun spokenBargeInStillKeepsAConservativeListeningWindow() {
        assertTrue(WakeVoiceAudioPolicy.voiceWindowNeeded(
            remotePcmActive = true, listeningForUser = true,
        ))
        assertTrue(WakeVoiceAudioPolicy.voiceWindowNeeded(
            remotePcmActive = false, listeningForUser = true,
        ))
    }

    @Test fun finishedAssistantAndFinishedListeningRestoreCriticalPlayback() {
        assertFalse(WakeVoiceAudioPolicy.voiceWindowNeeded(
            remotePcmActive = false, listeningForUser = false,
        ))
    }
}
