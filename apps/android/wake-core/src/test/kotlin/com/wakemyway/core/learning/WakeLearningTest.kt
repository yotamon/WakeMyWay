package com.wakemyway.core.learning

import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WakeLearningTest {
    private val policy = WakePolicy()
    private val learner = WakeLearning()
    private val acceptableFriction = WakeFrictionFeedback(
        annoyance = WakeAnnoyance.ACCEPTABLE,
        agency = WakeAgency.ACCEPTABLE,
    )

    @Test
    fun `Activation Completion plus RETURNED_TO_BED tightens activation criterion by one step`() {
        val outcomes = listOf(
            outcome(1, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
            outcome(2, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
            outcome(3, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
            outcome(4, calibration = WakeCalibrationOutcome.GOT_UP),
        )

        val decision = learner.derive(policy, outcomes)

        assertTrue(decision.changed)
        assertEquals(WakeLearningReasonCode.FALSE_POSITIVE_ACTIVATION, decision.explanation.code)
        assertEquals(policy.version + 1, decision.snapshot.policy.version)
        assertEquals(policy.activationThreshold + 1, decision.snapshot.policy.activationThreshold)
        assertEquals(policy.maxEscalationLevel, decision.snapshot.policy.maxEscalationLevel)
        assertIs<WakePolicyChange.ActivationThreshold>(decision.snapshot.changes.single())
    }

    @Test
    fun `missing calibration stays unknown and cannot manufacture Confirmed Wake Success`() {
        val outcomes = (1..4).map { index -> outcome(index, calibration = null) }

        val decision = learner.derive(policy, outcomes)

        assertFalse(decision.changed)
        assertEquals(WakeLearningReasonCode.STABLE_POLICY, decision.explanation.code)
        assertTrue(outcomes.all { wakeOutcome -> wakeOutcome.activationCompleted })
        assertTrue(outcomes.all { wakeOutcome -> wakeOutcome.confirmedWakeSuccess == null })
        assertNull(outcomes.first().calibration)
    }

    @Test
    fun `repeated incomplete activation allows one additional escalation level`() {
        val outcomes = listOf(
            outcome(1, activationCompleted = false),
            outcome(2, activationCompleted = false),
            outcome(3, activationCompleted = false),
            outcome(4, calibration = WakeCalibrationOutcome.GOT_UP),
        )

        val decision = learner.derive(policy, outcomes)

        assertTrue(decision.changed)
        assertEquals(WakeLearningReasonCode.REPEATED_INCOMPLETE_ACTIVATION, decision.explanation.code)
        assertEquals(policy.maxEscalationLevel + 1, decision.snapshot.policy.maxEscalationLevel)
        assertEquals(policy.activationThreshold, decision.snapshot.policy.activationThreshold)
        assertIs<WakePolicyChange.MaxEscalationLevel>(decision.snapshot.changes.single())
    }

    @Test
    fun `annoyance and agency guardrail blocks a more forceful policy`() {
        val highFriction = WakeFrictionFeedback(
            annoyance = WakeAnnoyance.HIGH,
            agency = WakeAgency.LOW,
        )
        val outcomes = listOf(
            outcome(1, activationCompleted = false, friction = highFriction),
            outcome(2, activationCompleted = false, friction = highFriction),
            outcome(3, activationCompleted = false, friction = highFriction),
            outcome(4, friction = acceptableFriction),
        )

        val decision = learner.derive(policy, outcomes)

        assertFalse(decision.changed)
        assertEquals(WakeLearningReasonCode.FRICTION_GUARDRAIL, decision.explanation.code)
        assertEquals(policy, decision.snapshot.policy)
    }

    @Test
    fun `confirmed success with repeated excess friction reduces intervention depth`() {
        val highFriction = WakeFrictionFeedback(
            annoyance = WakeAnnoyance.HIGH,
            agency = WakeAgency.ACCEPTABLE,
        )
        val outcomes = listOf(
            outcome(1, calibration = WakeCalibrationOutcome.GOT_UP, friction = highFriction),
            outcome(2, calibration = WakeCalibrationOutcome.GOT_UP, friction = highFriction),
            outcome(3, calibration = WakeCalibrationOutcome.GOT_UP, friction = highFriction),
            outcome(4, calibration = WakeCalibrationOutcome.GOT_UP, friction = acceptableFriction),
        )

        val decision = learner.derive(policy, outcomes)

        assertTrue(decision.changed)
        assertEquals(WakeLearningReasonCode.SUCCESS_WITH_EXCESS_FRICTION, decision.explanation.code)
        assertEquals(policy.maxEscalationLevel - 1, decision.snapshot.policy.maxEscalationLevel)
        assertEquals(policy.activationThreshold, decision.snapshot.policy.activationThreshold)
    }

    @Test
    fun `learning is deterministic regardless of outcome input ordering`() {
        val outcomes = listOf(
            outcome(4, calibration = WakeCalibrationOutcome.GOT_UP),
            outcome(2, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
            outcome(1, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
            outcome(3, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
        )

        val forward = learner.derive(policy, outcomes)
        val reverse = learner.derive(policy, outcomes.reversed())

        assertEquals(forward, reverse)
        assertEquals(
            listOf("session-1", "session-2", "session-3", "session-4"),
            forward.snapshot.sourceSessionIds.map { sessionId -> sessionId.value },
        )
    }

    @Test
    fun `conflicting duplicate outcomes are rejected instead of becoming order dependent`() {
        val first = outcome(1, calibration = WakeCalibrationOutcome.GOT_UP)
        val conflicting = first.copy(
            calibration = WakeCalibration(WakeCalibrationOutcome.RETURNED_TO_BED),
        )

        assertFailsWith<IllegalArgumentException> {
            learner.derive(policy, listOf(first, conflicting, outcome(2), outcome(3), outcome(4)))
        }
    }

    @Test
    fun `learned policy resolution accepts a valid snapshot and fails closed on invalid data`() {
        val validDecision = learner.derive(
            policy,
            listOf(
                outcome(1, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
                outcome(2, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
                outcome(3, calibration = WakeCalibrationOutcome.RETURNED_TO_BED),
                outcome(4, calibration = WakeCalibrationOutcome.GOT_UP),
            ),
        )

        val accepted = learner.resolveLearnedPolicy(validDecision.snapshot, policy)
        assertTrue(accepted.usedLearnedPolicy)
        assertEquals(LearnedPolicyFallbackReason.ACCEPTED, accepted.reason)
        assertEquals(validDecision.snapshot.policy, accepted.policy)

        val unsupported = learner.resolveLearnedPolicy(
            validDecision.snapshot.copy(algorithmVersion = WAKE_LEARNING_ALGORITHM_VERSION + 1),
            policy,
        )
        assertFalse(unsupported.usedLearnedPolicy)
        assertEquals(LearnedPolicyFallbackReason.UNSUPPORTED_ALGORITHM, unsupported.reason)
        assertEquals(policy, unsupported.policy)

        val explanation = WakeLearningExplanation(
            code = WakeLearningReasonCode.FALSE_POSITIVE_ACTIVATION,
            summary = "invalid two-step fixture",
            relevantSessionCount = 4,
        )
        val twoStepSnapshot = WakePolicySnapshot(
            sourcePolicy = policy,
            policy = policy.copy(version = policy.version + 1, activationThreshold = policy.activationThreshold + 2),
            sourceSessionIds = listOf(WakeSessionId("fixture")),
            changes = listOf(
                WakePolicyChange.ActivationThreshold(
                    from = policy.activationThreshold,
                    to = policy.activationThreshold + 2,
                    explanation = explanation,
                ),
            ),
        )
        val invalidStep = learner.resolveLearnedPolicy(twoStepSnapshot, policy)
        assertFalse(invalidStep.usedLearnedPolicy)
        assertEquals(LearnedPolicyFallbackReason.INVALID_LEARNING_BOUNDS, invalidStep.reason)
        assertEquals(policy, invalidStep.policy)

        val incompatibleSource = policy.copy(meaningfulInteractionWeight = policy.meaningfulInteractionWeight + 1)
        val incompatibleSnapshot = WakePolicySnapshot(
            sourcePolicy = incompatibleSource,
            policy = incompatibleSource.copy(
                version = incompatibleSource.version + 1,
                activationThreshold = incompatibleSource.activationThreshold + 1,
            ),
            sourceSessionIds = listOf(WakeSessionId("incompatible")),
            changes = listOf(
                WakePolicyChange.ActivationThreshold(
                    from = incompatibleSource.activationThreshold,
                    to = incompatibleSource.activationThreshold + 1,
                    explanation = explanation,
                ),
            ),
        )
        val incompatible = learner.resolveLearnedPolicy(incompatibleSnapshot, policy)
        assertFalse(incompatible.usedLearnedPolicy)
        assertEquals(policy, incompatible.policy)
    }

    @Test
    fun `policy snapshot rejects undeclared extra changes`() {
        val explanation = WakeLearningExplanation(
            code = WakeLearningReasonCode.FALSE_POSITIVE_ACTIVATION,
            summary = "coherence fixture",
            relevantSessionCount = 4,
        )

        assertFailsWith<IllegalArgumentException> {
            WakePolicySnapshot(
                sourcePolicy = policy,
                policy = policy.copy(
                    version = policy.version + 1,
                    activationThreshold = policy.activationThreshold + 1,
                    maxEscalationLevel = policy.maxEscalationLevel + 1,
                ),
                sourceSessionIds = listOf(WakeSessionId("coherence")),
                changes = listOf(
                    WakePolicyChange.ActivationThreshold(
                        from = policy.activationThreshold,
                        to = policy.activationThreshold + 1,
                        explanation = explanation,
                    ),
                ),
            )
        }
    }

    @Test
    fun `reset is explicit and restores the stable default policy`() {
        val learned = policy.copy(version = 2, activationThreshold = policy.activationThreshold + 1)

        val reset = learner.resetToDefault(policy)

        assertFalse(reset.usedLearnedPolicy)
        assertEquals(LearnedPolicyFallbackReason.RESET_TO_DEFAULT, reset.reason)
        assertEquals(policy, reset.policy)
        assertFalse(reset.policy == learned)
    }

    private fun outcome(
        index: Int,
        activationCompleted: Boolean = true,
        metActivationWindow: Boolean = activationCompleted,
        calibration: WakeCalibrationOutcome? = null,
        friction: WakeFrictionFeedback? = acceptableFriction,
        policyVersion: Int = policy.version,
    ): WakeOutcomeSummary = WakeOutcomeSummary(
        sessionId = WakeSessionId("session-$index"),
        policyVersion = policyVersion,
        activationCompleted = activationCompleted,
        metActivationWindow = metActivationWindow,
        timeToFirstEngagement = Duration.ofSeconds(5),
        timeToMeaningfulMovement = if (activationCompleted) Duration.ofSeconds(10) else null,
        timeToActivationCompletion = if (activationCompleted) {
            Duration.ofSeconds(if (metActivationWindow) 20 else 60)
        } else {
            null
        },
        snoozeCount = 0,
        maxInterventionDepth = if (activationCompleted) 1 else 3,
        finishReason = if (activationCompleted) WakeOutcome.COMPLETED else WakeOutcome.STOPPED,
        calibration = calibration?.let(::WakeCalibration),
        frictionFeedback = friction,
    )
}
