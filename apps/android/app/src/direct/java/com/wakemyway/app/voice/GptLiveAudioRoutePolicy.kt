package com.wakemyway.app.voice

import android.media.AudioManager

/**
 * Voice is optional: do not commandeer an existing call/audio session, and
 * never silently claim that PCM is speaker-audible after Android reroutes it.
 * The local foreground alarm still owns critical playback and Stop/Snooze.
 */
internal object GptLiveAudioRoutePolicy {
    fun mayAcquire(mode: Int): Boolean = mode != AudioManager.MODE_IN_CALL &&
        mode != AudioManager.MODE_IN_COMMUNICATION &&
        mode != AudioManager.MODE_RINGTONE

    fun remainsOnSelectedSpeaker(expectedDeviceId: Int?, actualDeviceId: Int?): Boolean =
        expectedDeviceId == null || expectedDeviceId == actualDeviceId
}
