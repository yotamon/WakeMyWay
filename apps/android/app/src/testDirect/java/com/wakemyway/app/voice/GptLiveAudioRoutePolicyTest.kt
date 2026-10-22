package com.wakemyway.app.voice

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GptLiveAudioRoutePolicyTest {
    @Test fun activeCallsMustNotHaveTheirAudioModeStolen() {
        assertTrue(GptLiveAudioRoutePolicy.mayAcquire(AudioManager.MODE_NORMAL))
        assertFalse(GptLiveAudioRoutePolicy.mayAcquire(AudioManager.MODE_IN_CALL))
        assertFalse(GptLiveAudioRoutePolicy.mayAcquire(AudioManager.MODE_IN_COMMUNICATION))
        assertFalse(GptLiveAudioRoutePolicy.mayAcquire(AudioManager.MODE_RINGTONE))
    }

    @Test fun speakerRouteChangesInvalidateVoiceWithoutStoppingLocalAlarm() {
        assertTrue(GptLiveAudioRoutePolicy.remainsOnSelectedSpeaker(12, 12))
        assertFalse(GptLiveAudioRoutePolicy.remainsOnSelectedSpeaker(12, 9))
        assertFalse(GptLiveAudioRoutePolicy.remainsOnSelectedSpeaker(12, null))
        assertTrue(GptLiveAudioRoutePolicy.remainsOnSelectedSpeaker(null, 9))
    }
}
