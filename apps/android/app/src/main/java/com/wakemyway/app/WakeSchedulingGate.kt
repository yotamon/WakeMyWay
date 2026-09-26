package com.wakemyway.app

import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmRepairTarget
import com.wakemyway.app.alarm.futureSchedulingRepairTarget
import com.wakemyway.app.ui.home.VoiceWakeReadiness

/**
 * Product-level preflight for creating a Wake Occurrence.
 *
 * Only Android capabilities required to deliver and control the alarm may block scheduling.
 * Microphone, local recognition and Realtime are enrichment capabilities: when they are absent the
 * committed wake remains valid and runs alarm-only rather than deleting or refusing user intent.
 */
enum class WakeSchedulingBlocker {
    ALARM_SYSTEM,
    NONE,
}

@Suppress("UNUSED_PARAMETER")
fun wakeSchedulingBlocker(
    alarmHealth: AlarmHealth,
    voiceReadiness: VoiceWakeReadiness?,
    requiresVoiceReplies: Boolean = true,
): WakeSchedulingBlocker =
    if (alarmHealth.futureSchedulingRepairTarget() != AlarmRepairTarget.NONE) {
        WakeSchedulingBlocker.ALARM_SYSTEM
    } else {
        WakeSchedulingBlocker.NONE
    }
