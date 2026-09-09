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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WakeOutcomeDeriverTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()
    private val deriver = WakeOutcomeDeriver(runtime)

    @Test
    fun `derives compact outcome from the real Wake Runtime replay`() {
        val initial = runtime.initial(WakeSessionId("outcome-1"), policy)
        val timeline = listOf(
            at(0, WakeInput.AlarmFired(id("alarm"))),
            at(5, WakeInput.UserInteracted(id("touch"))),
            at(10, WakeInput.MotionObserved(id("movement"), MotionEvidenceKind.SUSTAINED_MOVEMENT)),
            at(12, WakeInput.VoiceResponseObserved(id("voice"), coherent = true)),
            at(15, WakeInput.MotionObserved(id("late-movement"), MotionEvidenceKind.DEVICE_PICKUP)),
            at(20, WakeInput.OrientationCompleted(id("orientation-complete"))),
        )

        val outcome = deriver.derive(
            initial = initial,
            timeline = timeline,
            policy = policy,
            activationWindow = Duration.ofSeconds(30),
            calibration = WakeCalibration(WakeCalibrationOutcome.RETURNED_TO_BED),
            frictionFeedback = WakeFrictionFeedback(
                annoyance = WakeAnnoyance.ACCEPTABLE,
                agency = WakeAgency.ACCEPTABLE,
            ),
        )

        assertTrue(outcome.activationCompleted)
        assertTrue(outcome.metActivationWindow)
        assertEquals(Duration.ofSeconds(5), outcome.timeToFirstEngagement)
        assertEquals(Duration.ofSeconds(10), outcome.timeToMeaningfulMovement)
        assertEquals(Duration.ofSeconds(12), outcome.timeToActivationCompletion)
        assertEquals(0, outcome.snoozeCount)
        assertEquals(0, outcome.maxInterventionDepth)
        assertEquals(WakeOutcome.COMPLETED, outcome.finishReason)
        assertFalse(outcome.confirmedWakeSuccess!!)
    }

    @Test
    fun `missing calibration remains unknown instead of becoming Wake Success`() {
        val initial = runtime.initial(WakeSessionId("outcome-2"), policy)
        val timeline = listOf(
            at(0, WakeInput.AlarmFired(id("alarm"))),
            at(5, WakeInput.UserInteracted(id("touch"))),
            at(10, WakeInput.MotionObserved(id("movement"), MotionEvidenceKind.SUSTAINED_MOVEMENT)),
            at(12, WakeInput.VoiceResponseObserved(id("voice"), coherent = true)),
            at(20, WakeInput.OrientationCompleted(id("orientation-complete"))),
        )

        val outcome = deriver.derive(
            initial = initial,
            timeline = timeline,
            policy = policy,
            activationWindow = Duration.ofSeconds(30),
        )

        assertTrue(outcome.activationCompleted)
        assertNull(outcome.confirmedWakeSuccess)
    }

    @Test
    fun `rejects a timeline that is not ordered by monotonic elapsed time`() {
        val initial = runtime.initial(WakeSessionId("outcome-3"), policy)

        assertFailsWith<IllegalArgumentException> {
            deriver.derive(
                initial = initial,
                timeline = listOf(
                    at(5, WakeInput.AlarmFired(id("alarm"))),
                    at(4, WakeInput.UnrecoverableFailure(id("fatal"))),
                ),
                policy = policy,
                activationWindow = Duration.ofSeconds(30),
            )
        }
    }

    @Test
    fun `rejects an unfinished Wake Session instead of inventing an outcome`() {
        val initial = runtime.initial(WakeSessionId("outcome-4"), policy)

        assertFailsWith<IllegalArgumentException> {
            deriver.derive(
                initial = initial,
                timeline = listOf(at(0, WakeInput.AlarmFired(id("alarm")))),
                policy = policy,
                activationWindow = Duration.ofSeconds(30),
            )
        }
    }

    private fun at(seconds: Long, input: WakeInput): TimedWakeInput = TimedWakeInput(
        elapsedSinceAlarm = Duration.ofSeconds(seconds),
        input = input,
    )

    private fun id(value: String) = WakeInputId(value)
}
