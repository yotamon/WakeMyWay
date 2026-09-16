package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeInputId
import com.wakemyway.core.runtime.WakePhase
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.runtime.WakeSessionSnapshot
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WakeBehaviorEvidenceTrackerTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()

    @Test
    fun `tracks only real engagement movement and activation transitions`() {
        val tracker = WakeBehaviorEvidenceTracker()
        var snapshot = runtime.initial(WakeSessionId("session"), policy)

        snapshot = apply(tracker, snapshot, WakeInput.AlarmFired(id("alarm")), 0)
        snapshot = apply(
            tracker,
            snapshot,
            WakeInput.VoiceResponseObserved(id("incoherent"), coherent = false),
            1,
        )
        snapshot = apply(tracker, snapshot, WakeInput.UserInteracted(id("tap")), 2)
        snapshot = apply(
            tracker,
            snapshot,
            WakeInput.VoiceResponseObserved(id("voice"), coherent = true),
            4,
        )
        snapshot = apply(
            tracker,
            snapshot,
            WakeInput.MotionObserved(id("move"), MotionEvidenceKind.SUSTAINED_MOVEMENT),
            6,
        )

        val observation = requireNotNull(tracker.snapshot())
        assertEquals(policy.version, observation.policyVersion)
        assertEquals(Duration.ofSeconds(2), observation.timeToFirstEngagement)
        assertEquals(Duration.ofSeconds(6), observation.timeToMeaningfulMovement)
        assertEquals(Duration.ofSeconds(6), observation.timeToActivationCompletion)
        assertEquals(1, observation.maxInterventionDepth)
        assertEquals(WakePhase.ORIENTING, snapshot.phase)
    }

    @Test
    fun `missing evidence stays unknown instead of becoming zero`() {
        val tracker = WakeBehaviorEvidenceTracker()
        var snapshot = runtime.initial(WakeSessionId("session"), policy)

        snapshot = apply(tracker, snapshot, WakeInput.AlarmFired(id("alarm")), 0)
        snapshot = apply(tracker, snapshot, WakeInput.SpeechFinished(id("speech")), 1)

        val observation = requireNotNull(tracker.snapshot())
        assertNull(observation.timeToFirstEngagement)
        assertNull(observation.timeToMeaningfulMovement)
        assertNull(observation.timeToActivationCompletion)
        assertEquals(0, observation.maxInterventionDepth)
        assertEquals(WakePhase.ENGAGING, snapshot.phase)
    }

    @Test
    fun `ignored duplicate runtime input does not change evidence`() {
        val tracker = WakeBehaviorEvidenceTracker()
        var snapshot = runtime.initial(WakeSessionId("session"), policy)
        val interaction = WakeInput.UserInteracted(id("same"))

        snapshot = apply(tracker, snapshot, interaction, 2)
        snapshot = apply(tracker, snapshot, interaction, 3)

        val observation = requireNotNull(tracker.snapshot())
        assertEquals(Duration.ofSeconds(2), observation.timeToFirstEngagement)
        assertEquals(0, observation.maxInterventionDepth)
    }

    @Test
    fun `no applied runtime input produces no behavioral observation`() {
        val tracker = WakeBehaviorEvidenceTracker()

        assertNull(tracker.snapshot())
    }

    private fun apply(
        tracker: WakeBehaviorEvidenceTracker,
        before: WakeSessionSnapshot,
        input: WakeInput,
        elapsedSeconds: Long,
    ): WakeSessionSnapshot {
        val transition = runtime.reduce(before, input, policy)
        tracker.observe(before, input, transition, Duration.ofSeconds(elapsedSeconds))
        return transition.snapshot
    }

    private fun id(value: String) = WakeInputId(value)
}
