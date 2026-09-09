package com.wakemyway.core.runtime

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeRuntimeTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()

    @Test
    fun `happy path advances only after enough activation evidence`() {
        var snapshot = runtime.initial(WakeSessionId("session-1"), policy)

        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot
        assertEquals(WakePhase.ALERTING, snapshot.phase)

        snapshot = runtime.reduce(snapshot, WakeInput.UserInteracted(id("touch")), policy).snapshot
        assertEquals(WakePhase.ENGAGING, snapshot.phase)
        assertEquals(1, runtime.diagnostics(snapshot, policy).activationScore)

        snapshot = runtime.reduce(snapshot, WakeInput.SpeechFinished(id("speech-done")), policy).snapshot
        assertEquals(WakePhase.ENGAGING, snapshot.phase)

        val movementPrompt = runtime.reduce(
            snapshot,
            WakeInput.SilenceElapsed(id("movement-delay"), policy.movementPromptDelay),
            policy,
        )
        snapshot = movementPrompt.snapshot
        assertEquals(WakePhase.ACTIVATING, snapshot.phase)
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in movementPrompt.directives)

        snapshot = runtime.reduce(
            snapshot,
            WakeInput.MotionObserved(id("move"), MotionEvidenceKind.SUSTAINED_MOVEMENT),
            policy,
        ).snapshot
        assertEquals(WakePhase.ACTIVATING, snapshot.phase)
        assertEquals(3, runtime.diagnostics(snapshot, policy).activationScore)

        val orienting = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("voice"), coherent = true),
            policy,
        )
        assertEquals(WakePhase.ORIENTING, orienting.snapshot.phase)
        assertTrue(WakeDirective.PresentOrientation in orienting.directives)
        assertTrue(WakeDirective.Speak(SpeechIntent.Orientation) in orienting.directives)

        val finished = runtime.reduce(
            orienting.snapshot,
            WakeInput.OrientationCompleted(id("orientation-done")),
            policy,
        )
        assertEquals(WakePhase.FINISHED, finished.snapshot.phase)
        assertEquals(WakeOutcome.COMPLETED, finished.snapshot.outcome)
        assertTrue(WakeDirective.CompleteSession(WakeOutcome.COMPLETED) in finished.directives)
    }

    @Test
    fun `duplicate input is ignored without duplicate directives`() {
        val initial = runtime.initial(WakeSessionId("session-2"), policy)
        val input = WakeInput.UserInteracted(id("same-touch"))
        val first = runtime.reduce(initial, input, policy)
        val duplicate = runtime.reduce(first.snapshot, input, policy)

        assertTrue(first.inputApplied)
        assertFalse(duplicate.inputApplied)
        assertEquals(first.snapshot, duplicate.snapshot)
        assertTrue(duplicate.directives.isEmpty())
    }

    @Test
    fun `snooze does not finish session until replacement occurrence is confirmed`() {
        var snapshot = runtime.initial(WakeSessionId("session-3"), policy)

        val offered = runtime.reduce(snapshot, WakeInput.SnoozeRequested(id("snooze-request")), policy)
        snapshot = offered.snapshot
        assertEquals(SnoozeState.OFFERED, snapshot.snoozeState)
        assertEquals(WakePhase.ALERTING, snapshot.phase)
        assertTrue(WakeDirective.OfferSnooze(Duration.ofMinutes(5)) in offered.directives)

        val scheduling = runtime.reduce(snapshot, WakeInput.SnoozeConfirmed(id("snooze-confirm")), policy)
        snapshot = scheduling.snapshot
        assertEquals(SnoozeState.SCHEDULING, snapshot.snoozeState)
        assertEquals(WakePhase.ALERTING, snapshot.phase)
        assertTrue(WakeDirective.RequestSnoozeSchedule(Duration.ofMinutes(5)) in scheduling.directives)

        val scheduled = runtime.reduce(
            snapshot,
            WakeInput.SnoozeScheduled(id("snooze-scheduled"), "replacement-1"),
            policy,
        )
        assertEquals(WakePhase.FINISHED, scheduled.snapshot.phase)
        assertEquals(WakeOutcome.SNOOZED, scheduled.snapshot.outcome)
    }

    @Test
    fun `out of order snooze scheduled input cannot finish the wake`() {
        val initial = runtime.initial(WakeSessionId("session-3b"), policy)
        val spoofed = runtime.reduce(
            initial,
            WakeInput.SnoozeScheduled(id("unexpected-scheduled"), "replacement-1"),
            policy,
        )

        assertEquals(WakePhase.ALERTING, spoofed.snapshot.phase)
        assertEquals(SnoozeState.NONE, spoofed.snapshot.snoozeState)
        assertEquals(null, spoofed.snapshot.outcome)
        assertTrue(spoofed.directives.isEmpty())
    }

    @Test
    fun `failed snooze scheduling keeps wake active and audible`() {
        var snapshot = runtime.initial(WakeSessionId("session-4"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.SnoozeRequested(id("request")), policy).snapshot
        snapshot = runtime.reduce(snapshot, WakeInput.SnoozeConfirmed(id("confirm")), policy).snapshot

        val failed = runtime.reduce(
            snapshot,
            WakeInput.SnoozeSchedulingFailed(id("failed"), "exact_alarm_unavailable"),
            policy,
        )

        assertEquals(WakePhase.ALERTING, failed.snapshot.phase)
        assertEquals(SnoozeState.NONE, failed.snapshot.snoozeState)
        assertEquals(null, failed.snapshot.outcome)
        assertTrue(WakeDirective.EnsureAlarmAudible in failed.directives)
        assertTrue(WakeDirective.Speak(SpeechIntent.SnoozeFailed) in failed.directives)
    }

    @Test
    fun `speech capability loss removes speech directives without changing lifecycle`() {
        var snapshot = runtime.initial(WakeSessionId("session-5"), policy)
        snapshot = runtime.reduce(
            snapshot,
            WakeInput.CapabilitiesChanged(
                id("capability"),
                WakeCapabilities(speechAvailable = false, motionAvailable = true),
            ),
            policy,
        ).snapshot

        val wake = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy)
        assertEquals(WakePhase.ALERTING, wake.snapshot.phase)
        assertTrue(WakeDirective.EnsureAlarmAudible in wake.directives)
        assertTrue(wake.directives.none { it is WakeDirective.Speak })
    }

    @Test
    fun `motion capability loss keeps timed movement prompt while observation stays degraded`() {
        var snapshot = runtime.initial(WakeSessionId("session-5b"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.UserInteracted(id("touch")), policy).snapshot

        val degraded = runtime.reduce(
            snapshot,
            WakeInput.CapabilitiesChanged(
                id("motion-off"),
                WakeCapabilities(speechAvailable = true, motionAvailable = false),
            ),
            policy,
        )
        assertTrue(WakeDirective.StopObservingMotion in degraded.directives)

        val afterSpeech = runtime.reduce(
            degraded.snapshot,
            WakeInput.SpeechFinished(id("speech-finished")),
            policy,
        )
        assertEquals(WakePhase.ENGAGING, afterSpeech.snapshot.phase)
        assertTrue(afterSpeech.directives.none { it == WakeDirective.ObserveMotion })

        val activating = runtime.reduce(
            afterSpeech.snapshot,
            WakeInput.SilenceElapsed(id("movement-delay"), policy.movementPromptDelay),
            policy,
        )
        assertEquals(WakePhase.ACTIVATING, activating.snapshot.phase)
        assertTrue(activating.directives.none { it == WakeDirective.ObserveMotion })
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in activating.directives)
    }

    @Test
    fun `speech failure advances safely without making speech authoritative`() {
        var snapshot = runtime.initial(WakeSessionId("session-5c"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.UserInteracted(id("touch")), policy).snapshot

        val failed = runtime.reduce(snapshot, WakeInput.SpeechFailed(id("speech-failed")), policy)

        assertEquals(WakePhase.ACTIVATING, failed.snapshot.phase)
        assertEquals(1, failed.snapshot.escalationLevel)
        assertTrue(WakeDirective.EnsureAlarmAudible in failed.directives)
        assertTrue(failed.directives.none { it is WakeDirective.Speak })
    }

    @Test
    fun `silence reaches movement prompt deterministically without a separate escalation phase`() {
        var snapshot = runtime.initial(WakeSessionId("session-6"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.UserInteracted(id("touch")), policy).snapshot
        assertEquals(WakePhase.ENGAGING, snapshot.phase)

        val firstSilence = runtime.reduce(
            snapshot,
            WakeInput.SilenceElapsed(id("silence-1"), policy.movementPromptDelay),
            policy,
        )
        assertEquals(WakePhase.ACTIVATING, firstSilence.snapshot.phase)
        assertEquals(1, firstSilence.snapshot.escalationLevel)
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in firstSilence.directives)

        snapshot = firstSilence.snapshot
        repeat(5) { index ->
            snapshot = runtime.reduce(
                snapshot,
                WakeInput.SilenceElapsed(id("silence-${index + 2}"), Duration.ofSeconds(15)),
                policy,
            ).snapshot
        }
        assertEquals(policy.maxEscalationLevel, snapshot.escalationLevel)
        assertEquals(WakePhase.ACTIVATING, snapshot.phase)
    }

    @Test
    fun `orientation directives are emitted once even if more evidence arrives`() {
        var snapshot = runtime.initial(WakeSessionId("session-6b"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.UserInteracted(id("touch")), policy).snapshot
        snapshot = runtime.reduce(
            snapshot,
            WakeInput.MotionObserved(id("move"), MotionEvidenceKind.SUSTAINED_MOVEMENT),
            policy,
        ).snapshot
        val orienting = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("voice"), coherent = true),
            policy,
        )
        assertEquals(WakePhase.ORIENTING, orienting.snapshot.phase)
        assertTrue(WakeDirective.PresentOrientation in orienting.directives)

        val extraEvidence = runtime.reduce(
            orienting.snapshot,
            WakeInput.MotionObserved(id("move-after-orient"), MotionEvidenceKind.DEVICE_PICKUP),
            policy,
        )
        assertEquals(WakePhase.ORIENTING, extraEvidence.snapshot.phase)
        assertTrue(extraEvidence.directives.isEmpty())
    }

    @Test
    fun `stop waits for durable execution confirmation before finishing`() {
        val initial = runtime.initial(WakeSessionId("session-7"), policy)
        val requested = runtime.reduce(initial, WakeInput.StopRequested(id("stop-request")), policy)

        assertEquals(WakePhase.ALERTING, requested.snapshot.phase)
        assertEquals(StopState.STOPPING, requested.snapshot.stopState)
        assertEquals(null, requested.snapshot.outcome)
        assertTrue(WakeDirective.RequestStopExecution in requested.directives)

        val unrelated = runtime.reduce(
            requested.snapshot,
            WakeInput.UserInteracted(id("touch-while-stopping")),
            policy,
        )
        assertEquals(StopState.STOPPING, unrelated.snapshot.stopState)
        assertTrue(unrelated.directives.isEmpty())

        val completed = runtime.reduce(
            unrelated.snapshot,
            WakeInput.StopCompleted(id("stop-completed")),
            policy,
        )
        assertEquals(WakePhase.FINISHED, completed.snapshot.phase)
        assertEquals(WakeOutcome.STOPPED, completed.snapshot.outcome)
        assertEquals(StopState.NONE, completed.snapshot.stopState)
    }

    @Test
    fun `out of order stop completion cannot finish the wake`() {
        val initial = runtime.initial(WakeSessionId("session-7a"), policy)
        val completed = runtime.reduce(initial, WakeInput.StopCompleted(id("unexpected-stop")), policy)

        assertEquals(WakePhase.ALERTING, completed.snapshot.phase)
        assertEquals(null, completed.snapshot.outcome)
        assertTrue(completed.directives.isEmpty())
    }

    @Test
    fun `failed stop keeps wake active and audible`() {
        val initial = runtime.initial(WakeSessionId("session-7b"), policy)
        val requested = runtime.reduce(initial, WakeInput.StopRequested(id("request")), policy)
        val failed = runtime.reduce(
            requested.snapshot,
            WakeInput.StopFailed(id("failed"), "kernel_rejected"),
            policy,
        )

        assertEquals(WakePhase.ALERTING, failed.snapshot.phase)
        assertEquals(StopState.NONE, failed.snapshot.stopState)
        assertEquals(null, failed.snapshot.outcome)
        assertTrue(WakeDirective.EnsureAlarmAudible in failed.directives)
    }

    @Test
    fun `finished session is terminal`() {
        val initial = runtime.initial(WakeSessionId("session-7c"), policy)
        val finished = runtime.reduce(initial, WakeInput.UnrecoverableFailure(id("fatal")), policy)
        val afterFinish = runtime.reduce(
            finished.snapshot,
            WakeInput.UserInteracted(id("late-touch")),
            policy,
        )

        assertEquals(WakePhase.FINISHED, finished.snapshot.phase)
        assertEquals(WakeOutcome.UNRECOVERABLE, finished.snapshot.outcome)
        assertFalse(afterFinish.inputApplied)
        assertEquals(finished.snapshot, afterFinish.snapshot)
        assertTrue(afterFinish.directives.isEmpty())
    }

    @Test
    fun `policy version mismatch is rejected instead of silently changing behavior`() {
        val initial = runtime.initial(WakeSessionId("session-7d"), policy)
        val newerPolicy = policy.copy(version = 2)

        assertFailsWith<IllegalArgumentException> {
            runtime.reduce(initial, WakeInput.AlarmFired(id("alarm")), newerPolicy)
        }
        assertFailsWith<IllegalArgumentException> {
            runtime.diagnostics(initial, newerPolicy)
        }
    }

    @Test
    fun `replay is deterministic`() {
        val initial = runtime.initial(WakeSessionId("session-8"), policy)
        val inputs = listOf(
            WakeInput.AlarmFired(id("1")),
            WakeInput.UserInteracted(id("2")),
            WakeInput.SpeechFinished(id("3")),
            WakeInput.MotionObserved(id("4"), MotionEvidenceKind.SUSTAINED_MOVEMENT),
            WakeInput.VoiceResponseObserved(id("5"), coherent = true),
            WakeInput.OrientationCompleted(id("6")),
        )

        val first = runtime.replay(initial, inputs, policy)
        val second = runtime.replay(initial, inputs, policy)

        assertEquals(first, second)
        assertEquals(WakePhase.FINISHED, first.finalSnapshot.phase)
        assertEquals(WakeOutcome.COMPLETED, first.finalSnapshot.outcome)
    }

    private fun id(value: String) = WakeInputId(value)
}
