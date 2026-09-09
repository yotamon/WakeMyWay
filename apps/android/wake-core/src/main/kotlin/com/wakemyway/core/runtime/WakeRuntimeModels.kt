package com.wakemyway.core.runtime

import java.time.Duration

@JvmInline
value class WakeSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Wake session id must not be blank" }
    }
}

@JvmInline
value class WakeInputId(val value: String) {
    init {
        require(value.isNotBlank()) { "Wake input id must not be blank" }
    }
}

enum class WakePhase {
    ALERTING,
    ENGAGING,
    ACTIVATING,
    ORIENTING,
    FINISHED,
}

enum class WakeOutcome {
    COMPLETED,
    SNOOZED,
    STOPPED,
    UNRECOVERABLE,
}

enum class SnoozeState {
    NONE,
    OFFERED,
    SCHEDULING,
}

enum class MotionEvidenceKind {
    DEVICE_PICKUP,
    ORIENTATION_CHANGE,
    SUSTAINED_MOVEMENT,
}

data class WakeCapabilities(
    val speechAvailable: Boolean = true,
    val motionAvailable: Boolean = true,
)

data class ActivationEvidence(
    val meaningfulInteractions: Int = 0,
    val coherentVoiceResponses: Int = 0,
    val devicePickups: Int = 0,
    val orientationChanges: Int = 0,
    val sustainedMovements: Int = 0,
) {
    init {
        require(
            meaningfulInteractions >= 0 &&
                coherentVoiceResponses >= 0 &&
                devicePickups >= 0 &&
                orientationChanges >= 0 &&
                sustainedMovements >= 0,
        ) { "Activation evidence counts must be non-negative" }
    }

    internal fun score(policy: WakePolicy): Int =
        meaningfulInteractions * policy.meaningfulInteractionWeight +
            coherentVoiceResponses * policy.coherentVoiceWeight +
            devicePickups * policy.devicePickupWeight +
            orientationChanges * policy.orientationChangeWeight +
            sustainedMovements * policy.sustainedMovementWeight
}

data class WakePolicy(
    val version: Int = 1,
    val activationThreshold: Int = 4,
    val meaningfulInteractionWeight: Int = 1,
    val coherentVoiceWeight: Int = 1,
    val devicePickupWeight: Int = 1,
    val orientationChangeWeight: Int = 1,
    val sustainedMovementWeight: Int = 2,
    val maxEscalationLevel: Int = 3,
    val defaultSnoozeDuration: Duration = Duration.ofMinutes(5),
    val rememberedInputLimit: Int = 128,
) {
    init {
        require(version > 0) { "Wake policy version must be positive" }
        require(activationThreshold > 0) { "Activation threshold must be positive" }
        require(
            meaningfulInteractionWeight >= 0 &&
                coherentVoiceWeight >= 0 &&
                devicePickupWeight >= 0 &&
                orientationChangeWeight >= 0 &&
                sustainedMovementWeight >= 0,
        ) { "Evidence weights must be non-negative" }
        require(maxEscalationLevel >= 0) { "Max escalation level must be non-negative" }
        require(!defaultSnoozeDuration.isNegative && !defaultSnoozeDuration.isZero) {
            "Default snooze duration must be positive"
        }
        require(rememberedInputLimit > 0) { "Remembered input limit must be positive" }
    }
}

data class WakeSessionSnapshot(
    val id: WakeSessionId,
    val policyVersion: Int,
    val phase: WakePhase = WakePhase.ALERTING,
    val outcome: WakeOutcome? = null,
    val activationEvidence: ActivationEvidence = ActivationEvidence(),
    val escalationLevel: Int = 0,
    val snoozeState: SnoozeState = SnoozeState.NONE,
    val capabilities: WakeCapabilities = WakeCapabilities(),
    val processedInputIds: List<WakeInputId> = emptyList(),
) {
    init {
        require(policyVersion > 0) { "Wake session policy version must be positive" }
        require(escalationLevel >= 0) { "Escalation level must be non-negative" }
        require(processedInputIds.distinct().size == processedInputIds.size) {
            "Processed wake input ids must be unique"
        }
        require((phase == WakePhase.FINISHED) == (outcome != null)) {
            "Only a finished Wake Session may have an outcome"
        }
        require(phase != WakePhase.FINISHED || snoozeState != SnoozeState.SCHEDULING) {
            "A finished Wake Session cannot still be scheduling snooze"
        }
    }

    val activationScore: Int
        get() = error("Activation score is policy-dependent; ask WakeRuntime diagnostics instead")
}

sealed interface WakeInput {
    val id: WakeInputId

    data class AlarmFired(override val id: WakeInputId) : WakeInput
    data class WakeSurfacePresented(override val id: WakeInputId) : WakeInput
    data class UserInteracted(override val id: WakeInputId) : WakeInput

    data class VoiceResponseObserved(
        override val id: WakeInputId,
        val coherent: Boolean,
    ) : WakeInput

    data class MotionObserved(
        override val id: WakeInputId,
        val kind: MotionEvidenceKind,
    ) : WakeInput

    data class SilenceElapsed(
        override val id: WakeInputId,
        val interval: Duration,
    ) : WakeInput {
        init {
            require(!interval.isNegative && !interval.isZero) { "Silence interval must be positive" }
        }
    }

    data class SpeechFinished(override val id: WakeInputId) : WakeInput
    data class SpeechFailed(override val id: WakeInputId) : WakeInput
    data class SnoozeRequested(override val id: WakeInputId) : WakeInput
    data class SnoozeConfirmed(override val id: WakeInputId) : WakeInput

    data class SnoozeScheduled(
        override val id: WakeInputId,
        val replacementOccurrenceId: String,
    ) : WakeInput {
        init {
            require(replacementOccurrenceId.isNotBlank()) { "Replacement occurrence id must not be blank" }
        }
    }

    data class SnoozeSchedulingFailed(
        override val id: WakeInputId,
        val reasonCode: String,
    ) : WakeInput {
        init {
            require(reasonCode.isNotBlank()) { "Snooze failure reason must not be blank" }
        }
    }

    data class CapabilitiesChanged(
        override val id: WakeInputId,
        val capabilities: WakeCapabilities,
    ) : WakeInput

    data class OrientationCompleted(override val id: WakeInputId) : WakeInput
    data class StopRequested(override val id: WakeInputId) : WakeInput
    data class UnrecoverableFailure(override val id: WakeInputId) : WakeInput
}

sealed interface SpeechIntent {
    data object InitialWake : SpeechIntent
    data object AskToSitUp : SpeechIntent
    data object AskToMove : SpeechIntent
    data class ReEngage(val escalationLevel: Int) : SpeechIntent
    data object SnoozeConfirmation : SpeechIntent
    data object SnoozeFailed : SpeechIntent
    data object Orientation : SpeechIntent
}

sealed interface WakeDirective {
    data object EnsureAlarmAudible : WakeDirective
    data class Speak(val intent: SpeechIntent) : WakeDirective
    data object ObserveMotion : WakeDirective
    data object StopObservingMotion : WakeDirective
    data class OfferSnooze(val duration: Duration) : WakeDirective
    data class RequestSnoozeSchedule(val duration: Duration) : WakeDirective
    data object PresentOrientation : WakeDirective
    data class CompleteSession(val outcome: WakeOutcome) : WakeDirective
}

data class WakeTransition(
    val snapshot: WakeSessionSnapshot,
    val directives: List<WakeDirective>,
    val inputApplied: Boolean,
)

data class WakeRuntimeDiagnostics(
    val activationScore: Int,
    val activationThreshold: Int,
    val escalationLevel: Int,
    val processedInputCount: Int,
)

data class WakeReplayStep(
    val input: WakeInput,
    val transition: WakeTransition,
)

data class WakeReplayResult(
    val finalSnapshot: WakeSessionSnapshot,
    val steps: List<WakeReplayStep>,
)
