package com.wakemyway.app

import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmRepairTarget
import com.wakemyway.app.alarm.repairTarget
import com.wakemyway.app.ui.home.VoiceWakeReadiness

/**
 * Product-level preflight for creating a Wake Occurrence.
 *
 * A critical alarm must never be persisted first and repaired later. The user explicitly grants
 * every capability needed for a controllable two-way Wake before scheduling is allowed.
 */
enum class WakeSchedulingBlocker {
    ALARM_SYSTEM,
    VOICE_PERMISSION,
    VOICE_UNAVAILABLE,
    NONE,
}

fun wakeSchedulingBlocker(
    alarmHealth: AlarmHealth,
    voiceReadiness: VoiceWakeReadiness?,
): WakeSchedulingBlocker = when {
    alarmHealth.repairTarget() != AlarmRepairTarget.NONE -> WakeSchedulingBlocker.ALARM_SYSTEM
    voiceReadiness == VoiceWakeReadiness.SETUP_REQUIRED -> WakeSchedulingBlocker.VOICE_PERMISSION
    voiceReadiness == VoiceWakeReadiness.UNAVAILABLE || voiceReadiness == null ->
        WakeSchedulingBlocker.VOICE_UNAVAILABLE
    else -> WakeSchedulingBlocker.NONE
}
