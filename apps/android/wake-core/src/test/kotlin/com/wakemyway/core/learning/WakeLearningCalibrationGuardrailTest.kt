package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WakeLearningCalibrationGuardrailTest {
    @Test
    fun `unknown calibration is not positive evidence for relaxing friction`() {
        val policy = WakePolicy()
        val outcomes = List(4) { index ->
            WakeOutcomeDeriver.derive(
                timeline = WakeTimeline(
                    sessionId = WakeSessionId("unknown-$index"),
                    policyVersion = policy.version,
                    startedAtEpochMillis = 1_800_000_000_000L + index,
                    terminalOutcome = WakeOutcome.COMPLETED,
                    facts = listOf(
                        WakeTimelineFact.EngagementObserved(Duration.ofSeconds(8)),
                        WakeTimelineFact.MotionObserved(
                            Duration.ofSeconds(30),
                            MotionEvidenceKind.SUSTAINED_MOVEMENT,
                        ),
                        WakeTimelineFact.ActivationCompleted(Duration.ofSeconds(50)),
                    ),
                ),
                calibration = null,
                annoyance = WakeFeedbackRating(if (index < 2) 5 else 2),
                agency = WakeFeedbackRating(4),
            )
        }

        val decision = assertIs<WakeLearningDecision.NoChange>(WakeLearning.evaluate(policy, outcomes))

        assertEquals(WakeLearningNoChangeReason.MIXED_EVIDENCE, decision.reason)
    }
}
