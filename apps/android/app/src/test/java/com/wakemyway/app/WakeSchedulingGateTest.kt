package com.wakemyway.app

import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeSchedulingGateTest {
    @Test
    fun `alarm system access blocks scheduling before voice`() {
        assertEquals(
            WakeSchedulingBlocker.ALARM_SYSTEM,
            wakeSchedulingBlocker(
                alarmHealth = health(notificationsAllowed = false),
                voiceReadiness = VoiceWakeReadiness.SETUP_REQUIRED,
            ),
        )
    }

    @Test
    fun `microphone setup blocks scheduling when alarm system is controllable`() {
        assertEquals(
            WakeSchedulingBlocker.VOICE_PERMISSION,
            wakeSchedulingBlocker(
                alarmHealth = health(),
                voiceReadiness = VoiceWakeReadiness.SETUP_REQUIRED,
            ),
        )
    }

    @Test
    fun `missing on-device recognition blocks a voice wake`() {
        assertEquals(
            WakeSchedulingBlocker.VOICE_UNAVAILABLE,
            wakeSchedulingBlocker(
                alarmHealth = health(),
                voiceReadiness = VoiceWakeReadiness.UNAVAILABLE,
            ),
        )
    }

    @Test
    fun `scheduling opens only after alarm and voice prerequisites are ready`() {
        assertEquals(
            WakeSchedulingBlocker.NONE,
            wakeSchedulingBlocker(
                alarmHealth = health(),
                voiceReadiness = VoiceWakeReadiness.READY,
            ),
        )
    }

    private fun health(
        exactAlarmAllowed: Boolean = true,
        notificationsAllowed: Boolean = true,
        channelHigh: Boolean = true,
        fullScreenAllowed: Boolean = true,
    ) = AlarmHealth(
        ready = false,
        exactAlarmAllowed = exactAlarmAllowed,
        notificationsAllowed = notificationsAllowed,
        notificationChannelHighImportance = channelHigh,
        fullScreenIntentAllowed = fullScreenAllowed,
        nextOccurrence = null,
        activeOccurrence = null,
        detail = "test",
    )
}
