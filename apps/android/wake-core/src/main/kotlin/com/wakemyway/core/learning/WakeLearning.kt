package com.wakemyway.core.learning

import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePolicy
import java.time.Duration

/**
 * Versioned tuning policy for deterministic Wake Learning v0.
 *
 * This is intentionally tiny. V0 may only move the movement-prompt timing and may never rewrite
 * activation criteria, evidence weights, alarm behavior, Stop/Snooze authority, privacy, or safety.
 */
data class WakeLearningPolicy(
    val version: Int = 1,
    val observationWindow: Int = 6,
    val minimumRelevantOutcomes: Int = 4,
    val requiredPatternCount: Int = 3,
    val quickEngagementThreshold: Duration = Duration.ofSeconds(30),
    val slowMovementThreshold: Duration = Duration.ofSeconds(75),
    val fastMovementThreshold: Duration = Duration.ofSeconds(45),
    val movementPromptStep: Duration = Duration.ofSeconds(5),
    val minimumMovementPromptDelay: Duration = Duration.ofSeconds(5),
    val maximumMovementPromptDelay: Duration = Duration.ofSeconds(45),
    val highAnnoyanceRating: Int = 4,
    val lowAgencyRating: Int = 2,
    val maxGuardrailViolationsForEarlierPrompt: Int = 1,
    val requiredGuardrailViolationsForLaterPrompt: Int = 2,
) {
    init {
        require(version > 0) { "Wake Learning version must be positive" }
        require(observationWindow >= minimumRelevantOutcomes) {
            "Observation window must cover minimum relevant outcomes"
        }
        require(minimumRelevantOutcomes >= 2) { "Wake Learning needs repeated evidence" }
        require(requiredPatternCount in 2..minimumRelevantOutcomes) {
            "Pattern count must require repeated evidence within the minimum sample"
        }
        require(quickEngagementThreshold > Duration.ZERO) { "Quick-engagement threshold must be positive" }
        require(slowMovementThreshold > quickEngagementThreshold) {
            "Slow-movement threshold must be later than quick engagement"
        }
        require(fastMovementThreshold > Duration.ZERO && fastMovementThreshold < slowMovementThreshold) {
            "Fast-movement threshold must be positive and below slow movement"
        }
        require(movementPromptStep > Duration.ZERO) { "Movement-prompt step must be positive" }
        require(minimumMovementPromptDelay > Duration.ZERO) { "Minimum movement-prompt delay must be positive" }
        require(maximumMovementPromptDelay > minimumMovementPromptDelay) {
            "Maximum movement-prompt delay must exceed the minimum"
        }
        require(highAnnoyanceRating in 1..5) { "High-annoyance rating must be on the 1..5 scale" }
        require(lowAgencyRating in 1..5) { "Low-agency rating must be on the 1..5 scale" }
        require(maxGuardrailViolationsForEarlierPrompt >= 0) {
            "Earlier-prompt guardrail allowance cannot be negative"
        }
        require(requiredGuardrailViolationsForLaterPrompt > 0) {
            "Later-prompt guardrail requirement must be positive"
        }
    }
}

enum class WakeLearningNoChangeReason {
    INSUFFICIENT_EVIDENCE,
    MIXED_EVIDENCE,
    GUARDRAIL_BLOCKED,
    AT_POLICY_BOUND,
}

sealed interface WakePolicyChange {
    data class MovementPromptDelay(
        val from: Duration,
        val to: Duration,
    ) : WakePolicyChange
}

sealed interface WakeLearningDecision {
    val consideredOutcomeIds: List<WakeOutcomeId>
    val explanation: String

    data class NoChange(
        val reason: WakeLearningNoChangeReason,
        override val consideredOutcomeIds: List<WakeOutcomeId>,
        override val explanation: String,
    ) : WakeLearningDecision

    data class PolicyUpdated(
        val previousPolicy: WakePolicy,
        val nextPolicy: WakePolicy,
        val change: WakePolicyChange,
        override val consideredOutcomeIds: List<WakeOutcomeId>,
        override val explanation: String,
    ) : WakeLearningDecision
}

data class LearnedWakeProfile(
    val profileVersion: Int = 1,
    val learningAlgorithmVersion: Int,
    val activePolicy: WakePolicy,
    val sourceOutcomeIds: List<WakeOutcomeId>,
    val explanation: String,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(profileVersion > 0) { "Wake profile version must be positive" }
        require(learningAlgorithmVersion > 0) { "Learning algorithm version must be positive" }
        require(sourceOutcomeIds.isNotEmpty()) { "A learned profile needs source outcomes" }
        require(explanation.isNotBlank()) { "A learned profile needs an explanation" }
        require(updatedAtEpochMillis > 0) { "Wake profile update time must be positive" }
    }
}

object WakeLearning {
    fun evaluate(
        currentPolicy: WakePolicy,
        outcomes: List<WakeOutcomeRecord>,
        learningPolicy: WakeLearningPolicy = WakeLearningPolicy(),
    ): WakeLearningDecision {
        val considered = outcomes
            .asSequence()
            .filter { it.policyVersion == currentPolicy.version }
            .filter { it.terminalOutcome != WakeOutcome.UNRECOVERABLE }
            .sortedWith(compareBy<WakeOutcomeRecord> { it.startedAtEpochMillis }.thenBy { it.id.value })
            .toList()
            .takeLast(learningPolicy.observationWindow)
        val ids = considered.map { it.id }

        if (considered.size < learningPolicy.minimumRelevantOutcomes) {
            return WakeLearningDecision.NoChange(
                reason = WakeLearningNoChangeReason.INSUFFICIENT_EVIDENCE,
                consideredOutcomeIds = ids,
                explanation = "Need at least ${learningPolicy.minimumRelevantOutcomes} relevant wakes on policy ${currentPolicy.version}; have ${considered.size}.",
            )
        }

        val guardrailViolations = considered.count { outcome ->
            (outcome.annoyance?.value ?: 0) >= learningPolicy.highAnnoyanceRating ||
                (outcome.agency?.value ?: 5) <= learningPolicy.lowAgencyRating
        }
        val quickEngagementSlowMovement = considered.count { outcome ->
            val engagement = outcome.firstEngagementAfter
            val movement = outcome.meaningfulMovementAfter
            engagement != null &&
                engagement <= learningPolicy.quickEngagementThreshold &&
                (movement == null || movement >= learningPolicy.slowMovementThreshold)
        }

        if (quickEngagementSlowMovement >= learningPolicy.requiredPatternCount) {
            if (guardrailViolations > learningPolicy.maxGuardrailViolationsForEarlierPrompt) {
                return WakeLearningDecision.NoChange(
                    reason = WakeLearningNoChangeReason.GUARDRAIL_BLOCKED,
                    consideredOutcomeIds = ids,
                    explanation = "Movement was repeatedly slow, but $guardrailViolations/${considered.size} recent wakes reported high annoyance or low agency; do not add friction yet.",
                )
            }

            val nextDelay = currentPolicy.movementPromptDelay
                .minus(learningPolicy.movementPromptStep)
                .coerceAtLeast(learningPolicy.minimumMovementPromptDelay)
            if (nextDelay == currentPolicy.movementPromptDelay) {
                return atBound(ids, currentPolicy, "earlier")
            }
            return updated(
                currentPolicy = currentPolicy,
                nextDelay = nextDelay,
                consideredOutcomeIds = ids,
                learningPolicy = learningPolicy,
                explanation = "$quickEngagementSlowMovement/${considered.size} recent wakes engaged within ${learningPolicy.quickEngagementThreshold.seconds}s but meaningful movement was missing or took at least ${learningPolicy.slowMovementThreshold.seconds}s. Ask for movement ${learningPolicy.movementPromptStep.seconds}s earlier.",
            )
        }

        val fastMovementWithNoKnownFailure = considered.count { outcome ->
            val movement = outcome.meaningfulMovementAfter
            movement != null &&
                movement <= learningPolicy.fastMovementThreshold &&
                outcome.confirmedWakeSuccess != ConfirmedWakeSuccess.NOT_CONFIRMED
        }
        if (
            fastMovementWithNoKnownFailure >= learningPolicy.requiredPatternCount &&
            guardrailViolations >= learningPolicy.requiredGuardrailViolationsForLaterPrompt
        ) {
            val nextDelay = currentPolicy.movementPromptDelay
                .plus(learningPolicy.movementPromptStep)
                .coerceAtMost(learningPolicy.maximumMovementPromptDelay)
            if (nextDelay == currentPolicy.movementPromptDelay) {
                return atBound(ids, currentPolicy, "later")
            }
            return updated(
                currentPolicy = currentPolicy,
                nextDelay = nextDelay,
                consideredOutcomeIds = ids,
                learningPolicy = learningPolicy,
                explanation = "$fastMovementWithNoKnownFailure/${considered.size} recent wakes reached meaningful movement within ${learningPolicy.fastMovementThreshold.seconds}s while $guardrailViolations reported high annoyance or low agency. Reduce friction by asking for movement ${learningPolicy.movementPromptStep.seconds}s later.",
            )
        }

        return WakeLearningDecision.NoChange(
            reason = WakeLearningNoChangeReason.MIXED_EVIDENCE,
            consideredOutcomeIds = ids,
            explanation = "Recent wakes do not show one repeated movement-timing pattern strong enough to change policy ${currentPolicy.version}.",
        )
    }

    private fun updated(
        currentPolicy: WakePolicy,
        nextDelay: Duration,
        consideredOutcomeIds: List<WakeOutcomeId>,
        learningPolicy: WakeLearningPolicy,
        explanation: String,
    ): WakeLearningDecision.PolicyUpdated {
        require(currentPolicy.version < Int.MAX_VALUE) { "Wake policy version exhausted" }
        val nextPolicy = currentPolicy.copy(
            version = currentPolicy.version + 1,
            movementPromptDelay = nextDelay,
        )
        return WakeLearningDecision.PolicyUpdated(
            previousPolicy = currentPolicy,
            nextPolicy = nextPolicy,
            change = WakePolicyChange.MovementPromptDelay(
                from = currentPolicy.movementPromptDelay,
                to = nextDelay,
            ),
            consideredOutcomeIds = consideredOutcomeIds,
            explanation = explanation,
        )
    }

    private fun atBound(
        consideredOutcomeIds: List<WakeOutcomeId>,
        currentPolicy: WakePolicy,
        direction: String,
    ): WakeLearningDecision.NoChange = WakeLearningDecision.NoChange(
        reason = WakeLearningNoChangeReason.AT_POLICY_BOUND,
        consideredOutcomeIds = consideredOutcomeIds,
        explanation = "The repeated pattern points $direction, but movement prompt timing is already at its safe v0 bound.",
    )

    private fun Duration.coerceAtLeast(minimum: Duration): Duration = if (this < minimum) minimum else this

    private fun Duration.coerceAtMost(maximum: Duration): Duration = if (this > maximum) maximum else this
}
