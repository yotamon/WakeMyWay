package com.wakemyway.core.learning

import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration

@JvmInline
value class WakeOutcomeId(val value: String) {
    init {
        require(value.isNotBlank()) { "Wake Outcome id must not be blank" }
    }
}

@JvmInline
value class WakeFeedbackRating(val value: Int) {
    init {
        require(value in 1..5) { "Wake feedback rating must be between 1 and 5" }
    }
}

enum class WakeCalibration {
    GOT_UP,
    RETURNED_TO_BED,
    GOT_UP_LATER,
}

enum class ConfirmedWakeSuccess {
    CONFIRMED,
    NOT_CONFIRMED,
    UNKNOWN,
}

data class WakeTimeline(
    val sessionId: WakeSessionId,
    val policyVersion: Int,
    val startedAtEpochMillis: Long,
    val terminalOutcome: WakeOutcome,
    val facts: List<WakeTimelineFact>,
) {
    init {
        require(policyVersion > 0) { "Wake timeline policy version must be positive" }
        require(startedAtEpochMillis > 0) { "Wake timeline start time must be positive" }
        require(facts.all { !it.elapsed.isNegative }) { "Wake timeline facts cannot be negative" }
        require(facts.zipWithNext().all { (left, right) -> left.elapsed <= right.elapsed }) {
            "Wake timeline facts must be ordered by elapsed time"
        }
    }
}

sealed interface WakeTimelineFact {
    val elapsed: Duration

    data class EngagementObserved(override val elapsed: Duration) : WakeTimelineFact

    data class MotionObserved(
        override val elapsed: Duration,
        val kind: MotionEvidenceKind,
    ) : WakeTimelineFact

    data class ActivationCompleted(override val elapsed: Duration) : WakeTimelineFact

    data class SnoozeScheduled(override val elapsed: Duration) : WakeTimelineFact

    data class InterventionDepth(
        override val elapsed: Duration,
        val depth: Int,
    ) : WakeTimelineFact {
        init {
            require(depth >= 0) { "Intervention depth must be non-negative" }
        }
    }
}

data class WakeOutcomeRecord(
    val id: WakeOutcomeId,
    val derivationVersion: Int,
    val sessionId: WakeSessionId,
    val policyVersion: Int,
    val startedAtEpochMillis: Long,
    val firstEngagementAfter: Duration?,
    val meaningfulMovementAfter: Duration?,
    val activationCompletedAfter: Duration?,
    val terminalOutcome: WakeOutcome,
    val snoozeCount: Int,
    val maxInterventionDepth: Int,
    val calibration: WakeCalibration? = null,
    val annoyance: WakeFeedbackRating? = null,
    val agency: WakeFeedbackRating? = null,
) {
    init {
        require(derivationVersion > 0) { "Wake Outcome derivation version must be positive" }
        require(policyVersion > 0) { "Wake Outcome policy version must be positive" }
        require(startedAtEpochMillis > 0) { "Wake Outcome start time must be positive" }
        require(firstEngagementAfter == null || !firstEngagementAfter.isNegative) {
            "First engagement time cannot be negative"
        }
        require(meaningfulMovementAfter == null || !meaningfulMovementAfter.isNegative) {
            "Meaningful movement time cannot be negative"
        }
        require(activationCompletedAfter == null || !activationCompletedAfter.isNegative) {
            "Activation Completion time cannot be negative"
        }
        require(snoozeCount >= 0) { "Snooze count must be non-negative" }
        require(maxInterventionDepth >= 0) { "Max intervention depth must be non-negative" }
    }

    val activationCompleted: Boolean
        get() = activationCompletedAfter != null

    val confirmedWakeSuccess: ConfirmedWakeSuccess
        get() = when (calibration) {
            WakeCalibration.GOT_UP -> ConfirmedWakeSuccess.CONFIRMED
            WakeCalibration.RETURNED_TO_BED,
            WakeCalibration.GOT_UP_LATER,
            -> ConfirmedWakeSuccess.NOT_CONFIRMED
            null -> ConfirmedWakeSuccess.UNKNOWN
        }
}

object WakeOutcomeDeriver {
    const val DERIVATION_VERSION = 1

    fun derive(
        timeline: WakeTimeline,
        calibration: WakeCalibration? = null,
        annoyance: WakeFeedbackRating? = null,
        agency: WakeFeedbackRating? = null,
    ): WakeOutcomeRecord {
        val firstEngagement = timeline.facts
            .filterIsInstance<WakeTimelineFact.EngagementObserved>()
            .firstOrNull()
            ?.elapsed
        val meaningfulMovement = timeline.facts
            .filterIsInstance<WakeTimelineFact.MotionObserved>()
            .firstOrNull { it.kind == MotionEvidenceKind.SUSTAINED_MOVEMENT }
            ?.elapsed
        val activationCompleted = timeline.facts
            .filterIsInstance<WakeTimelineFact.ActivationCompleted>()
            .firstOrNull()
            ?.elapsed
        val snoozeCount = timeline.facts.count { it is WakeTimelineFact.SnoozeScheduled }
        val maxInterventionDepth = timeline.facts
            .filterIsInstance<WakeTimelineFact.InterventionDepth>()
            .maxOfOrNull { it.depth }
            ?: 0

        return WakeOutcomeRecord(
            id = WakeOutcomeId("outcome:${timeline.sessionId.value}"),
            derivationVersion = DERIVATION_VERSION,
            sessionId = timeline.sessionId,
            policyVersion = timeline.policyVersion,
            startedAtEpochMillis = timeline.startedAtEpochMillis,
            firstEngagementAfter = firstEngagement,
            meaningfulMovementAfter = meaningfulMovement,
            activationCompletedAfter = activationCompleted,
            terminalOutcome = timeline.terminalOutcome,
            snoozeCount = snoozeCount,
            maxInterventionDepth = maxInterventionDepth,
            calibration = calibration,
            annoyance = annoyance,
            agency = agency,
        )
    }
}
