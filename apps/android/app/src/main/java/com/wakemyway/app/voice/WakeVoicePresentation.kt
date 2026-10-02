package com.wakemyway.app.voice

import com.wakemyway.core.runtime.WakePhase

enum class WakeVoiceDegradationReason {
    TRANSPORT_UNAVAILABLE,
    STARTUP_TIMEOUT,
    SESSION_FAILURE,
    TURN_FAILURE,
}

internal fun projectPreRuntimeWakeVoiceState(
    startRequested: Boolean,
    alarmOnly: Boolean,
    degradationReason: WakeVoiceDegradationReason?,
    activationThreshold: Int,
): WakeVoiceUiState =
    WakeVoiceUiState(
        mode = when {
            alarmOnly -> WakeVoiceMode.ALARM_ONLY
            startRequested -> WakeVoiceMode.STARTING
            else -> WakeVoiceMode.ALARM_ONLY
        },
        phase = WakePhase.ALERTING,
        activationScore = 0,
        activationThreshold = activationThreshold,
        speechAvailable = false,
        voiceInputAvailable = false,
        conversational = false,
        degradationReason = degradationReason.takeIf { alarmOnly },
    )
