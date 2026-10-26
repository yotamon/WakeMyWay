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

    @Test fun queuedLiveCueAwaitingFreshPcmDoesNotSuppressCriticalAlarm() {
        // A queued next cue is not audible evidence. While the old PCM is
        // already quiet, waiting for fresh PCM must restore alarm gain.
        assertFalse(WakeVoiceAudioPolicy.voiceWindowNeeded(
            remotePcmActive = false, listeningForUser = false,
        ))
        // An actual active user-listening window is intentionally different.
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
