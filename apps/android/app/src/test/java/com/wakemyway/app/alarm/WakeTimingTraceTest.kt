package com.wakemyway.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WakeTimingTraceTest {
    @Test
    fun `instant functional test with audio is active without fabricating receiver timing`() {
        val now = 1_000_000L
        val snapshot = TimingSnapshot(
            occurrenceId = "instant",
            scheduleId = "lab-immediate",
            occurrenceKind = "PRIMARY",
            scenario = WakeTimingTrace.SCENARIO_INSTANT_FUNCTIONAL_TEST,
            expectFullScreen = false,
            targetWallMillis = now + 300_000L,
            expectedWallMillis = now,
            receiverWallMillis = null,
            receiverElapsedMillis = null,
            foregroundWallMillis = now + 10L,
            foregroundElapsedMillis = 10L,
            audioWallMillis = now + 20L,
            audioElapsedMillis = 20L,
            uiWallMillis = now + 30L,
            uiElapsedMillis = 30L,
            terminalAction = null,
            terminalWallMillis = null,
            terminalElapsedMillis = null,
            serviceRecoveryCount = 0,
            lastRecoveryWallMillis = null,
            events = emptyList(),
        )

        assertEquals(ReliabilityState.ACTIVE, snapshot.state(nowWallMillis = now + 30L))
        assertEquals(null, snapshot.triggerDelayMillis)
    }

    @Test
    fun `realtime startup latencies derive from monotonic semantic events`() {
        val snapshot = TimingSnapshot(
            occurrenceId = "voice",
            scheduleId = "voice-schedule",
            occurrenceKind = "PRIMARY",
            scenario = null,
            expectFullScreen = false,
            targetWallMillis = 0L,
            expectedWallMillis = null,
            receiverWallMillis = null,
            receiverElapsedMillis = null,
            foregroundWallMillis = null,
            foregroundElapsedMillis = null,
            audioWallMillis = null,
            audioElapsedMillis = null,
            uiWallMillis = null,
            uiElapsedMillis = null,
            terminalAction = null,
            terminalWallMillis = null,
            terminalElapsedMillis = null,
            serviceRecoveryCount = 0,
            lastRecoveryWallMillis = null,
            events = listOf(
                TimingEventSnapshot("REALTIME_CONNECTING", 10L, 1_000L, null),
                TimingEventSnapshot("REALTIME_READY", 20L, 2_250L, null),
                TimingEventSnapshot("REALTIME_SPEAKING", 30L, 2_600L, null),
            ),
        )

        assertEquals(1_250L, snapshot.realtimeConnectToReadyMillis)
        assertEquals(350L, snapshot.realtimeReadyToFirstSpeakingMillis)
    }

    @Test
    fun `interactive diagnostics expose only bounded semantic storage types`() {
        val stored = WakeInteractiveDiagnosticEvent.entries.map(::diagnosticStorageType)

        assertEquals(
            listOf(
                "REALTIME_CONNECTING",
                "REALTIME_READY",
                "REALTIME_SPEAKING",
                "REALTIME_LISTENING",
                "REALTIME_DEGRADED",
                "REALTIME_FAILURE_STARTUP_TIMEOUT",
                "REALTIME_FAILURE_CREDENTIAL",
                "REALTIME_FAILURE_NEGOTIATION",
                "REALTIME_FAILURE_TRANSPORT",
                "REALTIME_FAILURE_SESSION",
                "REALTIME_FAILURE_TURN",
                "RUNTIME_ALERTING",
                "RUNTIME_ENGAGING",
                "RUNTIME_ACTIVATING",
                "RUNTIME_ORIENTING",
                "RUNTIME_FINISHED",
            ),
            stored,
        )
        assertFalse(stored.any { it.contains("spoken", ignoreCase = true) })
        assertFalse(stored.any { it.contains("transcript", ignoreCase = true) })
        assertFalse(stored.any { it.contains("context", ignoreCase = true) })
    }
}
