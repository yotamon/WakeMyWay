package com.wakemyway.app.voice

import com.wakemyway.core.runtime.WakePhase
import com.wakemyway.core.runtime.WakePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakeVoicePresentationTest {
    @Test
    fun `realtime startup stays starting until a real degradation occurs`() {
        val state = projectPreRuntimeWakeVoiceState(
            startRequested = true,
            alarmOnly = false,
            degradationReason = null,
            activationThreshold = WakePolicy().activationThreshold,
        )

        assertEquals(WakeVoiceMode.STARTING, state.mode)
        assertEquals(WakePhase.ALERTING, state.phase)
        assertNull(state.degradationReason)
    }

    @Test
    fun `degraded realtime is stable alarm only with a bounded reason`() {
        val state = projectPreRuntimeWakeVoiceState(
            startRequested = true,
            alarmOnly = true,
            degradationReason = WakeVoiceDegradationReason.STARTUP_TIMEOUT,
            activationThreshold = 4,
        )

        assertEquals(WakeVoiceMode.ALARM_ONLY, state.mode)
        assertEquals(WakeVoiceDegradationReason.STARTUP_TIMEOUT, state.degradationReason)
        assertEquals(4, state.activationThreshold)
    }
    @Test
    fun `transport failures stay distinguishable from session failures`() {
        assertEquals(
            WakeVoiceDegradationReason.TRANSPORT_UNAVAILABLE,
            degradationReasonForConversationFailure("sdp-exchange"),
        )
        assertEquals(
            WakeVoiceDegradationReason.TRANSPORT_UNAVAILABLE,
            degradationReasonForConversationFailure("transport-disconnected"),
        )
    }

    @Test
    fun `provider and response failures remain session failures`() {
        assertEquals(
            WakeVoiceDegradationReason.SESSION_FAILURE,
            degradationReasonForConversationFailure("provider"),
        )
        assertEquals(
            WakeVoiceDegradationReason.SESSION_FAILURE,
            degradationReasonForConversationFailure("response-timeout"),
        )
    }

    @Test
    fun `turn budget failures map to turn failure`() {
        assertEquals(
            WakeVoiceDegradationReason.TURN_FAILURE,
            degradationReasonForConversationFailure("turn-budget"),
        )
    }

}
