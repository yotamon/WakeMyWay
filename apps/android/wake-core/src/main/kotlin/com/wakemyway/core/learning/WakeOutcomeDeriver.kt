package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakePhase
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeSessionSnapshot
import java.time.Duration

class WakeOutcomeDeriver(
    private val runtime: WakeRuntime = WakeRuntime(),
) {
    fun derive(
        initial: WakeSessionSnapshot,
        timeline: List<TimedWakeInput>,
        policy: WakePolicy,
        activationWindow: Duration,
        calibration: WakeCalibration? = null,
        frictionFeedback: WakeFrictionFeedback? = null,
    ): WakeOutcomeSummary {
        require(initial.policyVersion == policy.version) {
            "Wake Outcome policy ${initial.policyVersion} does not match replay policy ${policy.version}"
        }
        require(initial.phase == WakePhase.ALERTING) {
            "Wake Outcome derivation requires the Wake Session timeline from its Alerting start"
        }
        require(!activationWindow.isNegative && !activationWindow.isZero) {
            "Activation window must be positive"
        }
        require(timeline.zipWithNext().all { (left, right) ->
            left.elapsedSinceAlarm <= right.elapsedSinceAlarm
        }) {
            "Wake timeline must be ordered by monotonic elapsed time"
        }

        val replay = runtime.replay(initial, timeline.map(TimedWakeInput::input), policy)
        require(replay.finalSnapshot.phase == WakePhase.FINISHED) {
            "Wake Outcome can only be derived from a finished Wake Session"
        }
        require(replay.steps.size == timeline.size) {
            "Wake replay must preserve one step per timeline input"
        }

        val appliedIndices = replay.steps.indices.filter { index ->
            replay.steps[index].transition.inputApplied
        }

        val firstEngagement = appliedIndices.firstOrNull { index ->
            when (val input = replay.steps[index].input) {
                is WakeInput.UserInteracted -> true
                is WakeInput.VoiceResponseObserved -> input.coherent
                else -> false
            }
        }?.let { index -> timeline[index].elapsedSinceAlarm }

        val firstMeaningfulMovement = appliedIndices.firstOrNull { index ->
            val input = replay.steps[index].input
            input is WakeInput.MotionObserved && input.kind == MotionEvidenceKind.SUSTAINED_MOVEMENT
        }?.let { index -> timeline[index].elapsedSinceAlarm }

        val activationIndex = replay.steps.indices.firstOrNull { index ->
            val step = replay.steps[index]
            val phaseBefore = if (index == 0) {
                initial.phase
            } else {
                replay.steps[index - 1].transition.snapshot.phase
            }
            step.transition.inputApplied &&
                phaseBefore != WakePhase.ORIENTING &&
                step.transition.snapshot.phase == WakePhase.ORIENTING
        }
        val activationCompletion = activationIndex?.let { index ->
            timeline[index].elapsedSinceAlarm
        }

        val snoozeCount = appliedIndices.count { index ->
            replay.steps[index].input is WakeInput.SnoozeScheduled
        }
        val maxInterventionDepth = replay.steps.maxOfOrNull { step ->
            step.transition.snapshot.escalationLevel
        } ?: initial.escalationLevel

        return WakeOutcomeSummary(
            sessionId = initial.id,
            policyVersion = initial.policyVersion,
            activationCompleted = activationCompletion != null,
            metActivationWindow = activationCompletion != null && activationCompletion <= activationWindow,
            timeToFirstEngagement = firstEngagement,
            timeToMeaningfulMovement = firstMeaningfulMovement,
            timeToActivationCompletion = activationCompletion,
            snoozeCount = snoozeCount,
            maxInterventionDepth = maxInterventionDepth,
            finishReason = requireNotNull(replay.finalSnapshot.outcome) {
                "A finished Wake Session must expose a terminal outcome"
            },
            calibration = calibration,
            frictionFeedback = frictionFeedback,
        )
    }
}
