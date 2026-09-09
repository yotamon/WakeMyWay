package com.wakemyway.core.learning

import com.wakemyway.core.runtime.WakePolicy
import java.time.Duration
import kotlin.math.abs

/**
 * Deterministic Wake Learning v0.
 *
 * The learner changes at most one already-effective Wake Runtime parameter per derivation.
 * It deliberately avoids timing/snooze tuning until those paths have reliable lineage and
 * execution evidence.
 */
class WakeLearning(
    private val rules: Rules = Rules(),
) {
    data class Rules(
        val minimumRelevantSessions: Int = 4,
        val minimumCalibratedActivationCompletions: Int = 3,
        val minimumRepeatedPatternSessions: Int = 3,
        val minimumActivationThreshold: Int = 3,
        val maximumActivationThreshold: Int = 8,
        val minimumEscalationLevel: Int = 1,
        val maximumEscalationLevel: Int = 4,
    ) {
        init {
            require(minimumRelevantSessions >= 2) { "Wake Learning requires multiple relevant sessions" }
            require(minimumCalibratedActivationCompletions >= 2) {
                "Calibration changes require multiple calibrated Activation Completions"
            }
            require(minimumRepeatedPatternSessions >= 2) {
                "Repeated-pattern threshold must be at least two"
            }
            require(minimumActivationThreshold > 0) {
                "Minimum activation threshold must be positive"
            }
            require(maximumActivationThreshold >= minimumActivationThreshold) {
                "Activation threshold learning bounds are invalid"
            }
            require(minimumEscalationLevel >= 0) {
                "Minimum escalation level must be non-negative"
            }
            require(maximumEscalationLevel >= minimumEscalationLevel) {
                "Escalation learning bounds are invalid"
            }
        }
    }

    fun derive(
        currentPolicy: WakePolicy,
        outcomes: List<WakeOutcomeSummary>,
    ): WakeLearningDecision {
        val relevantGroups = outcomes
            .filter { outcome -> outcome.policyVersion == currentPolicy.version }
            .groupBy(WakeOutcomeSummary::sessionId)

        require(relevantGroups.values.all { duplicates -> duplicates.distinct().size == 1 }) {
            "Wake Learning received conflicting outcomes for the same Wake Session"
        }

        val relevant = relevantGroups
            .values
            .map(List<WakeOutcomeSummary>::first)
            .sortedBy { outcome -> outcome.sessionId.value }

        if (relevant.size < rules.minimumRelevantSessions) {
            return unchanged(
                currentPolicy,
                relevant,
                WakeLearningExplanation(
                    code = WakeLearningReasonCode.INSUFFICIENT_EVIDENCE,
                    summary = "Kept policy ${currentPolicy.version}: ${relevant.size} relevant sessions are not enough for a bounded learning update.",
                    relevantSessionCount = relevant.size,
                ),
            )
        }

        val calibratedActivationCompletions = relevant.filter { outcome ->
            outcome.activationCompleted && outcome.calibration?.confirmedWakeSuccess != null
        }
        val returnedToBedAfterActivation = calibratedActivationCompletions.count { outcome ->
            outcome.calibration?.outcome == WakeCalibrationOutcome.RETURNED_TO_BED
        }
        val falsePositivePattern =
            calibratedActivationCompletions.size >= rules.minimumCalibratedActivationCompletions &&
                returnedToBedAfterActivation * 2 >= calibratedActivationCompletions.size

        if (falsePositivePattern) {
            val explanation = WakeLearningExplanation(
                code = WakeLearningReasonCode.FALSE_POSITIVE_ACTIVATION,
                summary = "Activation Completion overestimated Wake Success: $returnedToBedAfterActivation of ${calibratedActivationCompletions.size} calibrated completions were followed by RETURNED_TO_BED.",
                relevantSessionCount = calibratedActivationCompletions.size,
            )
            if (increasedFrictionIsBlocked(relevant)) {
                return unchanged(
                    currentPolicy,
                    relevant,
                    explanation.copy(
                        code = WakeLearningReasonCode.FRICTION_GUARDRAIL,
                        summary = "Detected false-positive Activation Completion, but did not increase activation friction because recent annoyance/agency feedback blocks a stricter policy.",
                    ),
                )
            }
            if (currentPolicy.activationThreshold >= rules.maximumActivationThreshold) {
                return unchanged(
                    currentPolicy,
                    relevant,
                    explanation.copy(
                        code = WakeLearningReasonCode.PARAMETER_AT_SAFE_BOUND,
                        summary = "Detected false-positive Activation Completion, but activationThreshold is already at the Wake Learning v0 safe maximum ${rules.maximumActivationThreshold}.",
                    ),
                )
            }

            val learned = currentPolicy.copy(
                version = currentPolicy.version + 1,
                activationThreshold = currentPolicy.activationThreshold + 1,
            )
            val change = WakePolicyChange.ActivationThreshold(
                from = currentPolicy.activationThreshold,
                to = learned.activationThreshold,
                explanation = explanation,
            )
            return changed(currentPolicy, learned, relevant, change, explanation)
        }

        val incompleteActivation = relevant.count { outcome ->
            !outcome.activationCompleted || !outcome.metActivationWindow
        }
        if (incompleteActivation >= rules.minimumRepeatedPatternSessions) {
            val explanation = WakeLearningExplanation(
                code = WakeLearningReasonCode.REPEATED_INCOMPLETE_ACTIVATION,
                summary = "$incompleteActivation of ${relevant.size} recent sessions did not complete activation inside the target window; allow one additional bounded escalation level.",
                relevantSessionCount = relevant.size,
            )
            if (increasedFrictionIsBlocked(relevant)) {
                return unchanged(
                    currentPolicy,
                    relevant,
                    explanation.copy(
                        code = WakeLearningReasonCode.FRICTION_GUARDRAIL,
                        summary = "Repeated incomplete activation was observed, but recent annoyance/agency feedback blocks a more forceful policy.",
                    ),
                )
            }
            if (currentPolicy.maxEscalationLevel >= rules.maximumEscalationLevel) {
                return unchanged(
                    currentPolicy,
                    relevant,
                    explanation.copy(
                        code = WakeLearningReasonCode.PARAMETER_AT_SAFE_BOUND,
                        summary = "Repeated incomplete activation was observed, but maxEscalationLevel is already at the Wake Learning v0 safe maximum ${rules.maximumEscalationLevel}.",
                    ),
                )
            }

            val learned = currentPolicy.copy(
                version = currentPolicy.version + 1,
                maxEscalationLevel = currentPolicy.maxEscalationLevel + 1,
            )
            val change = WakePolicyChange.MaxEscalationLevel(
                from = currentPolicy.maxEscalationLevel,
                to = learned.maxEscalationLevel,
                explanation = explanation,
            )
            return changed(currentPolicy, learned, relevant, change, explanation)
        }

        val highFrictionSuccesses = relevant.count { outcome ->
            outcome.confirmedWakeSuccess == true &&
                (outcome.frictionFeedback?.annoyance == WakeAnnoyance.HIGH ||
                    outcome.frictionFeedback?.agency == WakeAgency.LOW)
        }
        if (highFrictionSuccesses >= rules.minimumRepeatedPatternSessions) {
            val explanation = WakeLearningExplanation(
                code = WakeLearningReasonCode.SUCCESS_WITH_EXCESS_FRICTION,
                summary = "$highFrictionSuccesses confirmed-success sessions also reported high annoyance or low agency; reduce maximum intervention depth by one bounded level.",
                relevantSessionCount = relevant.size,
            )
            if (currentPolicy.maxEscalationLevel <= rules.minimumEscalationLevel) {
                return unchanged(
                    currentPolicy,
                    relevant,
                    explanation.copy(
                        code = WakeLearningReasonCode.PARAMETER_AT_SAFE_BOUND,
                        summary = "Confirmed Wake Success has excess friction, but maxEscalationLevel is already at the Wake Learning v0 safe minimum ${rules.minimumEscalationLevel}.",
                    ),
                )
            }

            val learned = currentPolicy.copy(
                version = currentPolicy.version + 1,
                maxEscalationLevel = currentPolicy.maxEscalationLevel - 1,
            )
            val change = WakePolicyChange.MaxEscalationLevel(
                from = currentPolicy.maxEscalationLevel,
                to = learned.maxEscalationLevel,
                explanation = explanation,
            )
            return changed(currentPolicy, learned, relevant, change, explanation)
        }

        return unchanged(
            currentPolicy,
            relevant,
            WakeLearningExplanation(
                code = WakeLearningReasonCode.STABLE_POLICY,
                summary = "Kept policy ${currentPolicy.version}: recent evidence does not justify a bounded Wake Learning v0 change.",
                relevantSessionCount = relevant.size,
            ),
        )
    }

    fun resolveLearnedPolicy(
        candidate: WakePolicySnapshot?,
        defaultPolicy: WakePolicy,
    ): LearnedPolicyResolution {
        if (candidate == null) {
            return LearnedPolicyResolution(
                policy = defaultPolicy,
                usedLearnedPolicy = false,
                reason = LearnedPolicyFallbackReason.MISSING,
            )
        }
        if (candidate.algorithmVersion != WAKE_LEARNING_ALGORITHM_VERSION) {
            return LearnedPolicyResolution(
                policy = defaultPolicy,
                usedLearnedPolicy = false,
                reason = LearnedPolicyFallbackReason.UNSUPPORTED_ALGORITHM,
            )
        }
        if (
            candidate.sourcePolicy.version < defaultPolicy.version ||
            !baselineCompatible(candidate.sourcePolicy, defaultPolicy) ||
            !withinLearningBounds(candidate.sourcePolicy) ||
            !withinLearningBounds(candidate.policy) ||
            !hasBoundedDeclaredChange(candidate)
        ) {
            return LearnedPolicyResolution(
                policy = defaultPolicy,
                usedLearnedPolicy = false,
                reason = LearnedPolicyFallbackReason.INVALID_LEARNING_BOUNDS,
            )
        }
        return LearnedPolicyResolution(
            policy = candidate.policy,
            usedLearnedPolicy = true,
            reason = LearnedPolicyFallbackReason.ACCEPTED,
        )
    }

    fun resetToDefault(defaultPolicy: WakePolicy): LearnedPolicyResolution =
        LearnedPolicyResolution(
            policy = defaultPolicy,
            usedLearnedPolicy = false,
            reason = LearnedPolicyFallbackReason.RESET_TO_DEFAULT,
        )

    private fun increasedFrictionIsBlocked(outcomes: List<WakeOutcomeSummary>): Boolean {
        val rated = outcomes.mapNotNull(WakeOutcomeSummary::frictionFeedback)
        if (rated.size < rules.minimumRepeatedPatternSessions) return false

        val highAnnoyance = rated.count { feedback -> feedback.annoyance == WakeAnnoyance.HIGH }
        val lowAgency = rated.count { feedback -> feedback.agency == WakeAgency.LOW }
        return highAnnoyance * 2 >= rated.size || lowAgency * 2 >= rated.size
    }

    private fun withinLearningBounds(policy: WakePolicy): Boolean =
        policy.activationThreshold in rules.minimumActivationThreshold..rules.maximumActivationThreshold &&
            policy.maxEscalationLevel in rules.minimumEscalationLevel..rules.maximumEscalationLevel &&
            policy.defaultSnoozeDuration in Duration.ofMinutes(1)..Duration.ofMinutes(30)

    private fun baselineCompatible(policy: WakePolicy, defaultPolicy: WakePolicy): Boolean =
        policy.meaningfulInteractionWeight == defaultPolicy.meaningfulInteractionWeight &&
            policy.coherentVoiceWeight == defaultPolicy.coherentVoiceWeight &&
            policy.devicePickupWeight == defaultPolicy.devicePickupWeight &&
            policy.orientationChangeWeight == defaultPolicy.orientationChangeWeight &&
            policy.sustainedMovementWeight == defaultPolicy.sustainedMovementWeight &&
            policy.defaultSnoozeDuration == defaultPolicy.defaultSnoozeDuration &&
            policy.rememberedInputLimit == defaultPolicy.rememberedInputLimit

    private fun hasBoundedDeclaredChange(snapshot: WakePolicySnapshot): Boolean {
        val change = snapshot.changes.singleOrNull() ?: return true
        return when (change) {
            is WakePolicyChange.ActivationThreshold -> abs(change.to - change.from) == 1
            is WakePolicyChange.MaxEscalationLevel -> abs(change.to - change.from) == 1
        }
    }

    private fun unchanged(
        currentPolicy: WakePolicy,
        relevant: List<WakeOutcomeSummary>,
        explanation: WakeLearningExplanation,
    ): WakeLearningDecision = WakeLearningDecision(
        snapshot = WakePolicySnapshot(
            sourcePolicy = currentPolicy,
            policy = currentPolicy,
            sourceSessionIds = relevant.map(WakeOutcomeSummary::sessionId),
            changes = emptyList(),
        ),
        explanation = explanation,
    )

    private fun changed(
        currentPolicy: WakePolicy,
        learnedPolicy: WakePolicy,
        relevant: List<WakeOutcomeSummary>,
        change: WakePolicyChange,
        explanation: WakeLearningExplanation,
    ): WakeLearningDecision = WakeLearningDecision(
        snapshot = WakePolicySnapshot(
            sourcePolicy = currentPolicy,
            policy = learnedPolicy,
            sourceSessionIds = relevant.map(WakeOutcomeSummary::sessionId),
            changes = listOf(change),
        ),
        explanation = explanation,
    )
}
