package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WakeLearningDerivationVersionTest {
    @Test
    fun `unsupported outcome derivation versions cannot change policy`() {
        val policy = WakePolicy()
        val unsupported = List(4) { index ->
            WakeOutcomeDeriver.derive(
                timeline = WakeTimeline(
                    sessionId = WakeSessionId("legacy-$index"),
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
            ).copy(derivationVersion = WakeOutcomeDeriver.DERIVATION_VERSION + 1)
        }

        val decision = assertIs<WakeLearningDecision.NoChange>(WakeLearning.evaluate(policy, unsupported))

        assertEquals(WakeLearningNoChangeReason.INSUFFICIENT_EVIDENCE, decision.reason)
        assertEquals(emptyList(), decision.consideredOutcomeIds)
    }
}
