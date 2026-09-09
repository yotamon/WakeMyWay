package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WakeLearningPolicyBoundsTest {
    @Test
    fun `repeated earlier pattern at minimum bound does not advance policy version`() {
        val policy = WakePolicy(
            version = 4,
            movementPromptDelay = Duration.ofSeconds(5),
        )
        val outcomes = List(4) { index ->
            WakeOutcomeDeriver.derive(
                timeline = WakeTimeline(
                    sessionId = WakeSessionId("bound-$index"),
                    policyVersion = policy.version,
                    startedAtEpochMillis = 1_800_000_000_000L + index,
                    terminalOutcome = WakeOutcome.COMPLETED,
                    facts = listOf(
                        WakeTimelineFact.EngagementObserved(Duration.ofSeconds(10)),
                        WakeTimelineFact.MotionObserved(
                            Duration.ofSeconds(90),
                            MotionEvidenceKind.SUSTAINED_MOVEMENT,
                        ),
                        WakeTimelineFact.ActivationCompleted(Duration.ofSeconds(110)),
                    ),
                ),
                calibration = WakeCalibration.GOT_UP,
                annoyance = WakeFeedbackRating(2),
                agency = WakeFeedbackRating(4),
            )
        }

        val decision = assertIs<WakeLearningDecision.NoChange>(WakeLearning.evaluate(policy, outcomes))

        assertEquals(WakeLearningNoChangeReason.AT_POLICY_BOUND, decision.reason)
        assertEquals(4, policy.version)
        assertEquals(Duration.ofSeconds(5), policy.movementPromptDelay)
    }
}
