package com.wakemyway.app

import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmRepairTarget
import com.wakemyway.app.alarm.repairTarget
import com.wakemyway.app.ui.home.VoiceWakeReadiness

/**
 * Product-level preflight for creating a Wake Occurrence.
 *
 * A critical alarm must never be persisted first and repaired later. The user explicitly grants
 * every capability needed for the alarm being saved. Voice readiness is required only when that
 * alarm has Voice Check-In enabled.
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
    requiresVoiceReplies: Boolean = true,
): WakeSchedulingBlocker = when {
    alarmHealth.repairTarget() != AlarmRepairTarget.NONE -> WakeSchedulingBlocker.ALARM_SYSTEM
    !requiresVoiceReplies -> WakeSchedulingBlocker.NONE
    voiceReadiness == VoiceWakeReadiness.SETUP_REQUIRED -> WakeSchedulingBlocker.VOICE_PERMISSION
    voiceReadiness == VoiceWakeReadiness.UNAVAILABLE || voiceReadiness == null ->
        WakeSchedulingBlocker.VOICE_UNAVAILABLE
    else -> WakeSchedulingBlocker.NONE
}