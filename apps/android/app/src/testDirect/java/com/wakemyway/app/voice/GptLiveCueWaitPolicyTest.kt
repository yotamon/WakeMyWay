package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveCueWaitPolicyTest {
    @Test fun queuedOrientationBehindAudibleWakeSpeechCannotFailAsNoAudio() {
        assertFalse(
            GptLiveCueWaitPolicy.isSentCueStillSilent(
                responsePending = true,
                hasQueuedCue = true,
                assistantAudioActive = true,
            ),
        )
        assertTrue(
            GptLiveCueWaitPolicy.isQueuedCueStalled(
                hasQueuedCue = true,
                assistantAudioActive = true,
            ),
        )
    }

    @Test fun afterOldPcmEndsNewCueReceivesFullFirstAudioBudget() {
        // On quiet, the adapter drains the queue and only then calls sendCue.
        assertTrue(
            GptLiveCueWaitPolicy.isSentCueStillSilent(
                responsePending = true,
                hasQueuedCue = false,
                assistantAudioActive = false,
            ),
        )
        assertFalse(
            GptLiveCueWaitPolicy.isQueuedCueStalled(
                hasQueuedCue = false,
                assistantAudioActive = false,
            ),
        )
    }

    @Test fun firstRemotePcmCancelsNoAudioFailure() {
        assertFalse(
            GptLiveCueWaitPolicy.isSentCueStillSilent(
                responsePending = false,
                hasQueuedCue = false,
                assistantAudioActive = true,
            ),
        )
        assertFalse(
            GptLiveCueWaitPolicy.isSentCueStillSilent(
                responsePending = false,
                hasQueuedCue = false,
                assistantAudioActive = false,
            ),
        )
    }

    @Test fun stuckPendingWithoutAnyAudioStillFailsSafely() {
        assertTrue(
            GptLiveCueWaitPolicy.isSentCueStillSilent(
                responsePending = true,
                hasQueuedCue = false,
                assistantAudioActive = false,
            ),
        )
        assertFalse(
            GptLiveCueWaitPolicy.isQueuedCueStalled(
                hasQueuedCue = false,
                assistantAudioActive = false,
            ),
        )
    }

    @Test fun queueTimeoutIsBoundedSeparatelyFromProviderFirstAudio() {
        assertTrue(GptLiveCueWaitPolicy.FIRST_AUDIO_TIMEOUT_MS > 0)
        assertTrue(
            GptLiveCueWaitPolicy.QUEUED_CUE_DRAIN_TIMEOUT_MS >
                GptLiveCueWaitPolicy.FIRST_AUDIO_TIMEOUT_MS,
        )
    }
}
