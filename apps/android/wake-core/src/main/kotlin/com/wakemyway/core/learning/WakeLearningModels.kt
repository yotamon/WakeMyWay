package com.wakemyway.core.learning

import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration

const val WAKE_LEARNING_ALGORITHM_VERSION: Int = 1

enum class WakeCalibrationOutcome {
    GOT_UP,
    RETURNED_TO_BED,
    GOT_UP_LATER,
    SKIPPED,
}

enum class WakeCalibrationSource {
    USER_FEEDBACK,
}

data class WakeCalibration(
    val outcome: WakeCalibrationOutcome,
    val source: WakeCalibrationSource = WakeCalibrationSource.USER_FEEDBACK,
) {
    val confirmedWakeSuccess: Boolean?
        get() = when (outcome) {
            WakeCalibrationOutcome.GOT_UP -> true
            WakeCalibrationOutcome.RETURNED_TO_BED,
            WakeCalibrationOutcome.GOT_UP_LATER,
            -> false
            WakeCalibrationOutcome.SKIPPED -> null
        }
}

enum class WakeAnnoyance {
    LOW,
    ACCEPTABLE,
    HIGH,
}

enum class WakeAgency {
    LOW,
    ACCEPTABLE,
    HIGH,
}

data class WakeFrictionFeedback(
    val annoyance: WakeAnnoyance,
    val agency: WakeAgency,
)

data class TimedWakeInput(
    val elapsedSinceAlarm: Duration,
    val input: WakeInput,
) {
    init {
        require(!elapsedSinceAlarm.isNegative) { "Wake timeline elapsed time must be non-negative" }
    }
}

data class WakeOutcomeSummary(
    val sessionId: WakeSessionId,
    val policyVersion: Int,
    val activationCompleted: Boolean,
    val metActivationWindow: Boolean,
    val timeToFirstEngagement: Duration?,
    val timeToMeaningfulMovement: Duration?,
    val timeToActivationCompletion: Duration?,
    val snoozeCount: Int,
    val maxInterventionDepth: Int,
    val finishReason: WakeOutcome,
    val calibration: WakeCalibration? = null,
    val frictionFeedback: WakeFrictionFeedback? = null,
) {
    init {
        require(policyVersion > 0) { "Wake Outcome policy version must be positive" }
        require(snoozeCount >= 0) { "Wake Outcome snooze count must be non-negative" }
        require(maxInterventionDepth >= 0) { "Wake Outcome intervention depth must be non-negative" }
        require(timeToFirstEngagement?.isNegative != true) { "Engagement duration must be non-negative" }
        require(timeToMeaningfulMovement?.isNegative != true) { "Movement duration must be non-negative" }
        require(timeToActivationCompletion?.isNegative != true) { "Activation duration must be non-negative" }
        require(activationCompleted == (timeToActivationCompletion != null)) {
            "Activation Completion must have exactly one activation-completion duration"
        }
        require(!metActivationWindow || activationCompleted) {
            "Meeting the activation window requires Activation Completion"
        }
    }

    val confirmedWakeSuccess: Boolean?
        get() = calibration?.confirmedWakeSuccess
}

enum class WakeLearningReasonCode {
    INSUFFICIENT_EVIDENCE,
    FALSE_POSITIVE_ACTIVATION,
    REPEATED_INCOMPLETE_ACTIVATION,
    SUCCESS_WITH_EXCESS_FRICTION,
    FRICTION_GUARDRAIL,
    PARAMETER_AT_SAFE_BOUND,
    STABLE_POLICY,
}

data class WakeLearningExplanation(
    val code: WakeLearningReasonCode,
    val summary: String,
    val relevantSessionCount: Int,
) {
    init {
        require(summary.isNotBlank()) { "Wake Learning explanation must not be blank" }
        require(relevantSessionCount >= 0) { "Relevant session count must be non-negative" }
    }
}

sealed interface WakePolicyChange {
    val explanation: WakeLearningExplanation

    data class ActivationThreshold(
        val from: Int,
        val to: Int,
        override val explanation: WakeLearningExplanation,
    ) : WakePolicyChange

    data class MaxEscalationLevel(
        val from: Int,
        val to: Int,
        override val explanation: WakeLearningExplanation,
    ) : WakePolicyChange
}

data class WakePolicySnapshot(
    val algorithmVersion: Int = WAKE_LEARNING_ALGORITHM_VERSION,
    val sourcePolicyVersion: Int,
    val policy: WakePolicy,
    val sourceSessionIds: List<WakeSessionId>,
    val changes: List<WakePolicyChange>,
) {
    init {
        require(algorithmVersion > 0) { "Wake Learning algorithm version must be positive" }
        require(sourcePolicyVersion > 0) { "Source Wake Policy version must be positive" }
        require(sourceSessionIds.distinct().size == sourceSessionIds.size) {
            "Source Wake Session ids must be unique"
        }
        require(changes.size <= 1) { "Wake Learning v0 changes at most one parameter per derivation" }
        require(changes.isEmpty() || policy.version == sourcePolicyVersion + 1) {
            "A learned policy change must advance the Wake Policy version exactly once"
        }
        require(changes.isNotEmpty() || policy.version == sourcePolicyVersion) {
            "An unchanged policy snapshot must retain the current Wake Policy version"
        }
    }
}

data class WakeLearningDecision(
    val snapshot: WakePolicySnapshot,
    val explanation: WakeLearningExplanation,
) {
    val changed: Boolean
        get() = snapshot.changes.isNotEmpty()
}

enum class LearnedPolicyFallbackReason {
    ACCEPTED,
    MISSING,
    UNSUPPORTED_ALGORITHM,
    INVALID_LEARNING_BOUNDS,
}

data class LearnedPolicyResolution(
    val policy: WakePolicy,
    val usedLearnedPolicy: Boolean,
    val reason: LearnedPolicyFallbackReason,
)
