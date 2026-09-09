package com.wakemyway.core.runtime

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WakeRuntimeOrderingTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()

    @Test
    fun `late silence callback cannot re-engage after orienting`() {
        var snapshot = runtime.initial(WakeSessionId("ordering-orient"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.UserInteracted(id("touch")), policy).snapshot
        snapshot = runtime.reduce(
            snapshot,
            WakeInput.MotionObserved(id("move"), MotionEvidenceKind.SUSTAINED_MOVEMENT),
            policy,
        ).snapshot
        snapshot = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("voice"), coherent = true),
            policy,
        ).snapshot
        assertEquals(WakePhase.ORIENTING, snapshot.phase)

        val lateSilence = runtime.reduce(
            snapshot,
            WakeInput.SilenceElapsed(id("late-silence"), Duration.ofSeconds(15)),
            policy,
        )

        assertEquals(WakePhase.ORIENTING, lateSilence.snapshot.phase)
        assertEquals(snapshot.escalationLevel, lateSilence.snapshot.escalationLevel)
        assertTrue(lateSilence.directives.isEmpty())
    }

    @Test
    fun `out of order snooze confirmation is a no-op`() {
        val initial = runtime.initial(WakeSessionId("ordering-snooze"), policy)
        val confirmation = runtime.reduce(
            initial,
            WakeInput.SnoozeConfirmed(id("confirm-without-offer")),
            policy,
        )

        assertEquals(SnoozeState.NONE, confirmation.snapshot.snoozeState)
        assertEquals(WakePhase.ALERTING, confirmation.snapshot.phase)
        assertTrue(confirmation.directives.isEmpty())
    }

    @Test
    fun `stop cannot race an in-flight snooze scheduling transaction`() {
        var snapshot = runtime.initial(WakeSessionId("ordering-snooze-stop"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.SnoozeRequested(id("request")), policy).snapshot
        snapshot = runtime.reduce(snapshot, WakeInput.SnoozeConfirmed(id("confirm")), policy).snapshot
        assertEquals(SnoozeState.SCHEDULING, snapshot.snoozeState)

        val stopDuringScheduling = runtime.reduce(
            snapshot,
            WakeInput.StopRequested(id("stop-during-scheduling")),
            policy,
        )

        assertEquals(SnoozeState.SCHEDULING, stopDuringScheduling.snapshot.snoozeState)
        assertEquals(StopState.NONE, stopDuringScheduling.snapshot.stopState)
        assertTrue(WakeDirective.RequestStopExecution !in stopDuringScheduling.directives)

        val schedulingFailed = runtime.reduce(
            stopDuringScheduling.snapshot,
            WakeInput.SnoozeSchedulingFailed(id("schedule-failed"), "timeout"),
            policy,
        )
        assertEquals(SnoozeState.NONE, schedulingFailed.snapshot.snoozeState)

        val stopAfterResolution = runtime.reduce(
            schedulingFailed.snapshot,
            WakeInput.StopRequested(id("stop-after-resolution")),
            policy,
        )
        assertEquals(StopState.STOPPING, stopAfterResolution.snapshot.stopState)
        assertTrue(WakeDirective.RequestStopExecution in stopAfterResolution.directives)
    }

    private fun id(value: String) = WakeInputId(value)
}
