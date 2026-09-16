package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakePhase
import com.wakemyway.core.runtime.WakeSessionSnapshot
import com.wakemyway.core.runtime.WakeTransition
import java.time.Duration

/**
 * Compact incremental observation of facts already produced by WakeRuntime.
 *
 * This deliberately mirrors the semantics used by [WakeOutcomeDeriver] without retaining the raw
 * input timeline. It is suitable for private local history where we need truthful product metrics
 * but do not want to persist transcripts, raw motion, or every runtime event.
 */
data class WakeBehaviorObservation(
    val policyVersion: Int,
    val timeToFirstEngagement: Duration?,
    val timeToMeaningfulMovement: Duration?,
    val timeToActivationCompletion: Duration?,
    val maxInterventionDepth: Int,
) {
    init {
        require(policyVersion > 0) { "Observed Wake behavior policy version must be positive" }
        listOfNotNull(
            timeToFirstEngagement,
            timeToMeaningfulMovement,
            timeToActivationCompletion,
        ).forEach { duration ->
            require(!duration.isNegative) { "Observed Wake behavior timings must be non-negative" }
        }
        require(maxInterventionDepth >= 0) {
            "Observed Wake behavior intervention depth must be non-negative"
        }
    }
}

class WakeBehaviorEvidenceTracker {
    private var policyVersion: Int? = null
    private var lastElapsed: Duration? = null
    private var firstEngagement: Duration? = null
    private var firstMeaningfulMovement: Duration? = null
    private var activationCompletion: Duration? = null
    private var maxInterventionDepth: Int = 0
    private var observedAppliedInput = false

    fun observe(
        before: WakeSessionSnapshot,
        input: WakeInput,
        transition: WakeTransition,
        elapsedSinceAlarm: Duration,
    ) {
        require(!elapsedSinceAlarm.isNegative) { "Wake behavior elapsed time must be non-negative" }
        lastElapsed?.let { previous ->
            require(elapsedSinceAlarm >= previous) { "Wake behavior observations must be monotonic" }
        }
        lastElapsed = elapsedSinceAlarm

        if (!transition.inputApplied) return

        val existingPolicyVersion = policyVersion
        if (existingPolicyVersion == null) {
            policyVersion = before.policyVersion
        } else {
            require(existingPolicyVersion == before.policyVersion) {
                "Wake behavior observations cannot mix runtime policy versions"
            }
        }

        observedAppliedInput = true
        maxInterventionDepth = maxOf(maxInterventionDepth, transition.snapshot.escalationLevel)

        if (firstEngagement == null && input.isMeaningfulEngagement()) {
            firstEngagement = elapsedSinceAlarm
        }
        if (
            firstMeaningfulMovement == null &&
            input is WakeInput.MotionObserved &&
            input.kind == MotionEvidenceKind.SUSTAINED_MOVEMENT
        ) {
            firstMeaningfulMovement = elapsedSinceAlarm
        }
        if (
            activationCompletion == null &&
            before.phase != WakePhase.ORIENTING &&
            transition.snapshot.phase == WakePhase.ORIENTING
        ) {
            activationCompletion = elapsedSinceAlarm
        }
    }

    fun snapshot(): WakeBehaviorObservation? {
        if (!observedAppliedInput) return null
        return WakeBehaviorObservation(
            policyVersion = requireNotNull(policyVersion),
            timeToFirstEngagement = firstEngagement,
            timeToMeaningfulMovement = firstMeaningfulMovement,
            timeToActivationCompletion = activationCompletion,
            maxInterventionDepth = maxInterventionDepth,
        )
    }

    private fun WakeInput.isMeaningfulEngagement(): Boolean = when (this) {
        is WakeInput.UserInteracted -> true
        is WakeInput.VoiceResponseObserved -> coherent
        else -> false
    }
}
