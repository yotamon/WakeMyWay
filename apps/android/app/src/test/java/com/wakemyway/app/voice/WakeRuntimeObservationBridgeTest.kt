package com.wakemyway.app.voice

import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeInputId
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeRuntimeObservationBridgeTest {
    @Test
    fun `reports only applied transitions from the monotonic runtime start`() {
        val runtime = WakeRuntime()
        val policy = WakePolicy()
        var nowMillis = 1_000L
        val observations = mutableListOf<Pair<String, Duration>>()
        val bridge = WakeRuntimeObservationBridge(
            runtime = runtime,
            policy = policy,
            elapsedRealtimeMillis = { nowMillis },
            observer = WakeRuntimeTransitionObserver { _, input, _, elapsed ->
                observations += input.id.value to elapsed
            },
        )
        var snapshot = runtime.initial(WakeSessionId("observed"), policy)
        bridge.begin()

        val alarm = WakeInput.AlarmFired(WakeInputId("alarm"))
        snapshot = bridge.reduce(snapshot, alarm).snapshot
        nowMillis += 3_000L
        val interaction = WakeInput.UserInteracted(WakeInputId("interaction"))
        snapshot = bridge.reduce(snapshot, interaction).snapshot

        // Duplicate input ids are not applied by WakeRuntime and must not be observed twice.
        bridge.reduce(snapshot, interaction)

        assertEquals(
            listOf(
                "alarm" to Duration.ZERO,
                "interaction" to Duration.ofSeconds(3),
            ),
            observations,
        )
    }

    @Test
    fun `observer failure never changes the runtime transition`() {
        val runtime = WakeRuntime()
        val policy = WakePolicy()
        val bridge = WakeRuntimeObservationBridge(
            runtime = runtime,
            policy = policy,
            elapsedRealtimeMillis = { 42L },
            observer = WakeRuntimeTransitionObserver { _, _, _, _ -> error("history unavailable") },
        )
        val snapshot = runtime.initial(WakeSessionId("failure-isolated"), policy)
        bridge.begin()

        val transition = bridge.reduce(
            snapshot,
            WakeInput.AlarmFired(WakeInputId("alarm")),
        )

        assertTrue(transition.inputApplied)
        assertEquals(WakeInputId("alarm"), transition.snapshot.processedInputIds.single())
    }

    @Test
    fun `clock rollback is clamped to zero rather than producing invalid evidence`() {
        val runtime = WakeRuntime()
        val policy = WakePolicy()
        var nowMillis = 10_000L
        var observedElapsed: Duration? = null
        val bridge = WakeRuntimeObservationBridge(
            runtime = runtime,
            policy = policy,
            elapsedRealtimeMillis = { nowMillis },
            observer = WakeRuntimeTransitionObserver { _, _, _, elapsed ->
                observedElapsed = elapsed
            },
        )
        val snapshot = runtime.initial(WakeSessionId("clamped"), policy)
        bridge.begin()
        nowMillis = 9_000L

        bridge.reduce(snapshot, WakeInput.AlarmFired(WakeInputId("alarm")))

        assertEquals(Duration.ZERO, observedElapsed)
    }
}
