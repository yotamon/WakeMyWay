package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeInputId
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WakeLearningTest {
    @Test
    fun `activation completion and return-to-bed calibration remain different facts`() {
        val outcome = WakeOutcomeDeriver.derive(
            timeline = timeline(
                session = "false-positive",
                policyVersion = 1,
                engagementSeconds = 10,
                movementSeconds = 40,
                activationSeconds = 55,
            ),
            calibration = WakeCalibration.RETURNED_TO_BED,
        )

        assertTrue(outcome.activationCompleted)
        assertEquals(ConfirmedWakeSuccess.NOT_CONFIRMED, outcome.confirmedWakeSuccess)
    }

    @Test
    fun `one unusual morning cannot change policy`() {
        val policy = WakePolicy()
        val decision = WakeLearning.evaluate(
            currentPolicy = policy,
            outcomes = listOf(slowMovementOutcome("one", policy.version, 1_800_000_000_000L)),
        )

        val noChange = assertIs<WakeLearningDecision.NoChange>(decision)
        assertEquals(WakeLearningNoChangeReason.INSUFFICIENT_EVIDENCE, noChange.reason)
        assertEquals(policy.version, 1)
    }

    @Test
    fun `repeated quick engagement and slow movement asks for movement earlier`() {
        val policy = WakePolicy(movementPromptDelay = Duration.ofSeconds(15))
        val outcomes = List(4) { index ->
            slowMovementOutcome(
                id = "slow-$index",
                policyVersion = policy.version,
                startedAt = 1_800_000_000_000L + index * 86_400_000L,
            )
        }

        val decision = assertIs<WakeLearningDecision.PolicyUpdated>(
            WakeLearning.evaluate(policy, outcomes),
        )

        assertEquals(Duration.ofSeconds(15), decision.previousPolicy.movementPromptDelay)
        assertEquals(Duration.ofSeconds(10), decision.nextPolicy.movementPromptDelay)
        assertEquals(2, decision.nextPolicy.version)
        assertEquals(policy.activationThreshold, decision.nextPolicy.activationThreshold)
        assertEquals(policy.defaultSnoozeDuration, decision.nextPolicy.defaultSnoozeDuration)
        assertTrue(decision.explanation.contains("Ask for movement 5s earlier"))
    }

    @Test
    fun `same evidence produces the same learning decision`() {
        val policy = WakePolicy()
        val outcomes = List(4) { index ->
            slowMovementOutcome("det-$index", policy.version, 1_800_000_000_000L + index)
        }

        val first = WakeLearning.evaluate(policy, outcomes)
        val second = WakeLearning.evaluate(policy, outcomes.reversed())

        assertEquals(first, second)
    }

    @Test
    fun `annoyance and low-agency guardrail can block an earlier prompt`() {
        val policy = WakePolicy()
        val outcomes = List(4) { index ->
            slowMovementOutcome(
                id = "guard-$index",
                policyVersion = policy.version,
                startedAt = 1_800_000_000_000L + index,
                annoyance = if (index < 2) 5 else 2,
                agency = if (index < 2) 2 else 4,
            )
        }

        val decision = assertIs<WakeLearningDecision.NoChange>(WakeLearning.evaluate(policy, outcomes))

        assertEquals(WakeLearningNoChangeReason.GUARDRAIL_BLOCKED, decision.reason)
    }

    @Test
    fun `fast movement plus repeated friction can move the prompt later`() {
        val policy = WakePolicy(movementPromptDelay = Duration.ofSeconds(15))
        val outcomes = List(4) { index ->
            outcome(
                id = "fast-$index",
                policyVersion = policy.version,
                startedAt = 1_800_000_000_000L + index,
                engagementSeconds = 8,
                movementSeconds = 30,
                calibration = WakeCalibration.GOT_UP,
                annoyance = if (index < 2) 4 else 2,
                agency = 4,
            )
        }

        val decision = assertIs<WakeLearningDecision.PolicyUpdated>(WakeLearning.evaluate(policy, outcomes))

        assertEquals(Duration.ofSeconds(20), decision.nextPolicy.movementPromptDelay)
        assertEquals(2, decision.nextPolicy.version)
        assertTrue(decision.explanation.contains("Reduce friction"))
    }

    @Test
    fun `return-to-bed calibration is never counted as positive evidence for relaxing friction`() {
        val policy = WakePolicy()
        val outcomes = List(4) { index ->
            outcome(
                id = "false-success-$index",
                policyVersion = policy.version,
                startedAt = 1_800_000_000_000L + index,
                engagementSeconds = 8,
                movementSeconds = 30,
                calibration = if (index < 3) WakeCalibration.RETURNED_TO_BED else WakeCalibration.GOT_UP,
                annoyance = if (index < 2) 5 else 2,
                agency = 4,
            )
        }

        val decision = assertIs<WakeLearningDecision.NoChange>(WakeLearning.evaluate(policy, outcomes))

        assertEquals(WakeLearningNoChangeReason.MIXED_EVIDENCE, decision.reason)
    }

    @Test
    fun `new policy waits for evidence gathered under its own version`() {
        val policyV1 = WakePolicy()
        val learned = assertIs<WakeLearningDecision.PolicyUpdated>(
            WakeLearning.evaluate(
                policyV1,
                List(4) { index -> slowMovementOutcome("v1-$index", 1, 1_800_000_000_000L + index) },
            ),
        )

        val nextDecision = assertIs<WakeLearningDecision.NoChange>(
            WakeLearning.evaluate(learned.nextPolicy, List(4) { index -> slowMovementOutcome("old-$index", 1, 1_800_000_100_000L + index) }),
        )

        assertEquals(WakeLearningNoChangeReason.INSUFFICIENT_EVIDENCE, nextDecision.reason)
    }

    @Test
    fun `learned policy never mutates an already-started wake session`() {
        val runtime = WakeRuntime()
        val policyV1 = WakePolicy()
        val session = runtime.initial(WakeSessionId("immutable-session"), policyV1)
        val learned = assertIs<WakeLearningDecision.PolicyUpdated>(
            WakeLearning.evaluate(
                policyV1,
                List(4) { index -> slowMovementOutcome("immutable-$index", 1, 1_800_000_000_000L + index) },
            ),
        )

        assertNotEquals(session.policyVersion, learned.nextPolicy.version)
        assertFailsWith<IllegalArgumentException> {
            runtime.reduce(session, WakeInput.AlarmFired(WakeInputId("late-policy")), learned.nextPolicy)
        }
    }

    private fun slowMovementOutcome(
        id: String,
        policyVersion: Int,
        startedAt: Long,
        annoyance: Int = 2,
        agency: Int = 4,
    ): WakeOutcomeRecord = outcome(
        id = id,
        policyVersion = policyVersion,
        startedAt = startedAt,
        engagementSeconds = 10,
        movementSeconds = 90,
        calibration = WakeCalibration.GOT_UP,
        annoyance = annoyance,
        agency = agency,
    )

    private fun outcome(
        id: String,
        policyVersion: Int,
        startedAt: Long,
        engagementSeconds: Long,
        movementSeconds: Long,
        calibration: WakeCalibration,
        annoyance: Int,
        agency: Int,
    ): WakeOutcomeRecord = WakeOutcomeDeriver.derive(
        timeline = timeline(
            session = id,
            policyVersion = policyVersion,
            startedAt = startedAt,
            engagementSeconds = engagementSeconds,
            movementSeconds = movementSeconds,
            activationSeconds = movementSeconds + 20,
        ),
        calibration = calibration,
        annoyance = WakeFeedbackRating(annoyance),
        agency = WakeFeedbackRating(agency),
    )

    private fun timeline(
        session: String,
        policyVersion: Int,
        startedAt: Long = 1_800_000_000_000L,
        engagementSeconds: Long,
        movementSeconds: Long,
        activationSeconds: Long,
    ): WakeTimeline = WakeTimeline(
        sessionId = WakeSessionId(session),
        policyVersion = policyVersion,
        startedAtEpochMillis = startedAt,
        terminalOutcome = WakeOutcome.COMPLETED,
        facts = listOf(
            WakeTimelineFact.EngagementObserved(Duration.ofSeconds(engagementSeconds)),
            WakeTimelineFact.InterventionDepth(Duration.ofSeconds(engagementSeconds + 2), 1),
            WakeTimelineFact.MotionObserved(Duration.ofSeconds(movementSeconds), MotionEvidenceKind.SUSTAINED_MOVEMENT),
            WakeTimelineFact.ActivationCompleted(Duration.ofSeconds(activationSeconds)),
        ),
    )
}
