package com.wakemyway.core.runtime

import java.time.Duration

class WakeRuntime {
    fun initial(
        sessionId: WakeSessionId,
        policy: WakePolicy,
        capabilities: WakeCapabilities = WakeCapabilities(),
    ): WakeSessionSnapshot = WakeSessionSnapshot(
        id = sessionId,
        policyVersion = policy.version,
        capabilities = capabilities,
    )

    fun reduce(
        current: WakeSessionSnapshot,
        input: WakeInput,
        policy: WakePolicy,
    ): WakeTransition {
        require(current.policyVersion == policy.version) {
            "Wake Session policy ${current.policyVersion} does not match reducer policy ${policy.version}"
        }

        if (current.phase == WakePhase.FINISHED) {
            return WakeTransition(current, emptyList(), inputApplied = false)
        }
        if (input.id in current.processedInputIds) {
            return WakeTransition(current, emptyList(), inputApplied = false)
        }

        val remembered = remember(current, input.id, policy)
        if (
            current.stopState == StopState.STOPPING &&
            input !is WakeInput.StopCompleted &&
            input !is WakeInput.StopFailed &&
            input !is WakeInput.UnrecoverableFailure
        ) {
            return transition(remembered)
        }
        if (
            current.snoozeState == SnoozeState.SCHEDULING &&
            input !is WakeInput.SnoozeScheduled &&
            input !is WakeInput.SnoozeSchedulingFailed &&
            input !is WakeInput.CapabilitiesChanged &&
            input !is WakeInput.UnrecoverableFailure
        ) {
            return transition(remembered)
        }
        if (current.phase == WakePhase.ORIENTING && input.isStaleActivationCallback()) {
            return transition(remembered)
        }

        return when (input) {
            is WakeInput.AlarmFired -> transition(
                remembered,
                WakeDirective.EnsureAlarmAudible,
                WakeDirective.Speak(SpeechIntent.InitialWake),
            )

            is WakeInput.WakeSurfacePresented -> transition(remembered)

            is WakeInput.UserInteracted -> {
                val next = remembered.copy(
                    phase = if (remembered.phase == WakePhase.ALERTING) WakePhase.ENGAGING else remembered.phase,
                    activationEvidence = remembered.activationEvidence.copy(
                        meaningfulInteractions = remembered.activationEvidence.meaningfulInteractions + 1,
                    ),
                    engagementSilenceElapsed = Duration.ZERO,
                )
                advanceOr(
                    next,
                    policy,
                    WakeDirective.Speak(SpeechIntent.AskToSitUp),
                    WakeDirective.ObserveMotion,
                )
            }

            is WakeInput.VoiceResponseObserved -> {
                val evidence = if (input.coherent) {
                    remembered.activationEvidence.copy(
                        coherentVoiceResponses = remembered.activationEvidence.coherentVoiceResponses + 1,
                    )
                } else {
                    remembered.activationEvidence
                }
                val next = remembered.copy(
                    phase = if (remembered.phase == WakePhase.ALERTING) WakePhase.ENGAGING else remembered.phase,
                    activationEvidence = evidence,
                    engagementSilenceElapsed = Duration.ZERO,
                )
                advanceOr(next, policy, WakeDirective.ObserveMotion)
            }

            is WakeInput.MotionObserved -> {
                val evidence = when (input.kind) {
                    MotionEvidenceKind.DEVICE_PICKUP -> remembered.activationEvidence.copy(
                        devicePickups = remembered.activationEvidence.devicePickups + 1,
                    )
                    MotionEvidenceKind.ORIENTATION_CHANGE -> remembered.activationEvidence.copy(
                        orientationChanges = remembered.activationEvidence.orientationChanges + 1,
                    )
                    MotionEvidenceKind.SUSTAINED_MOVEMENT -> remembered.activationEvidence.copy(
                        sustainedMovements = remembered.activationEvidence.sustainedMovements + 1,
                    )
                }
                val next = remembered.copy(
                    phase = when (remembered.phase) {
                        WakePhase.ALERTING,
                        WakePhase.ENGAGING,
                        -> WakePhase.ACTIVATING
                        else -> remembered.phase
                    },
                    activationEvidence = evidence,
                    engagementSilenceElapsed = Duration.ZERO,
                )
                advanceOr(next, policy, WakeDirective.ObserveMotion)
            }

            is WakeInput.SilenceElapsed -> {
                val escalation = (remembered.escalationLevel + 1).coerceAtMost(policy.maxEscalationLevel)
                val accumulatedSilence = remembered.engagementSilenceElapsed.plus(input.interval)
                val movementPromptDue =
                    remembered.phase in setOf(WakePhase.ALERTING, WakePhase.ENGAGING) &&
                        accumulatedSilence >= policy.movementPromptDelay
                val next = remembered.copy(
                    phase = when {
                        movementPromptDue -> WakePhase.ACTIVATING
                        remembered.phase == WakePhase.ALERTING -> WakePhase.ENGAGING
                        else -> remembered.phase
                    },
                    escalationLevel = escalation,
                    engagementSilenceElapsed = accumulatedSilence,
                )
                val directives = buildList {
                    add(WakeDirective.EnsureAlarmAudible)
                    add(
                        WakeDirective.Speak(
                            if (movementPromptDue) SpeechIntent.AskToMove
                            else SpeechIntent.ReEngage(escalation),
                        ),
                    )
                    if (next.phase in setOf(WakePhase.ENGAGING, WakePhase.ACTIVATING)) {
                        add(WakeDirective.ObserveMotion)
                    }
                }
                transition(next, *directives.toTypedArray())
            }

            is WakeInput.SpeechFinished -> when (remembered.phase) {
                WakePhase.ALERTING -> transition(
                    remembered.copy(
                        phase = WakePhase.ENGAGING,
                        engagementSilenceElapsed = Duration.ZERO,
                    ),
                    WakeDirective.Speak(SpeechIntent.AskToSitUp),
                    WakeDirective.ObserveMotion,
                )

                WakePhase.ENGAGING -> transition(
                    remembered,
                    WakeDirective.ObserveMotion,
                )

                else -> transition(remembered)
            }

            is WakeInput.SpeechFailed -> {
                val escalation = (remembered.escalationLevel + 1).coerceAtMost(policy.maxEscalationLevel)
                val next = remembered.copy(
                    phase = when (remembered.phase) {
                        WakePhase.ALERTING -> WakePhase.ENGAGING
                        WakePhase.ENGAGING -> WakePhase.ACTIVATING
                        else -> remembered.phase
                    },
                    escalationLevel = escalation,
                )
                transition(
                    next,
                    WakeDirective.EnsureAlarmAudible,
                    WakeDirective.ObserveMotion,
                )
            }

            is WakeInput.SnoozeRequested -> when (remembered.snoozeState) {
                SnoozeState.NONE -> transition(
                    remembered.copy(snoozeState = SnoozeState.OFFERED),
                    WakeDirective.Speak(SpeechIntent.SnoozeConfirmation),
                    WakeDirective.OfferSnooze(policy.defaultSnoozeDuration),
                )
                SnoozeState.OFFERED,
                SnoozeState.SCHEDULING,
                -> transition(remembered)
            }

            is WakeInput.SnoozeConfirmed -> when (remembered.snoozeState) {
                SnoozeState.OFFERED -> transition(
                    remembered.copy(snoozeState = SnoozeState.SCHEDULING),
                    WakeDirective.RequestSnoozeSchedule(policy.defaultSnoozeDuration),
                )
                SnoozeState.NONE,
                SnoozeState.SCHEDULING,
                -> transition(remembered)
            }

            is WakeInput.SnoozeScheduled -> {
                if (remembered.snoozeState != SnoozeState.SCHEDULING) {
                    transition(remembered)
                } else {
                    finish(remembered, WakeOutcome.SNOOZED)
                }
            }

            is WakeInput.SnoozeSchedulingFailed -> {
                if (remembered.snoozeState != SnoozeState.SCHEDULING) {
                    transition(remembered)
                } else {
                    transition(
                        remembered.copy(snoozeState = SnoozeState.NONE),
                        WakeDirective.EnsureAlarmAudible,
                        WakeDirective.Speak(SpeechIntent.SnoozeFailed),
                    )
                }
            }

            is WakeInput.CapabilitiesChanged -> {
                val previous = remembered.capabilities
                val next = remembered.copy(capabilities = input.capabilities)
                val directives = buildList {
                    add(WakeDirective.EnsureAlarmAudible)
                    if (previous.motionAvailable && !input.capabilities.motionAvailable) {
                        add(WakeDirective.StopObservingMotion)
                    } else if (
                        !previous.motionAvailable &&
                        input.capabilities.motionAvailable &&
                        next.phase in setOf(WakePhase.ENGAGING, WakePhase.ACTIVATING)
                    ) {
                        add(WakeDirective.ObserveMotion)
                    }
                }
                transition(next, *directives.toTypedArray())
            }

            is WakeInput.OrientationCompleted -> {
                if (remembered.phase == WakePhase.ORIENTING) finish(remembered, WakeOutcome.COMPLETED)
                else transition(remembered)
            }

            is WakeInput.StopRequested -> transition(
                remembered.copy(
                    snoozeState = SnoozeState.NONE,
                    stopState = StopState.STOPPING,
                ),
                WakeDirective.RequestStopExecution,
            )

            is WakeInput.StopCompleted -> {
                if (remembered.stopState == StopState.STOPPING) finish(remembered, WakeOutcome.STOPPED)
                else transition(remembered)
            }

            is WakeInput.StopFailed -> {
                if (remembered.stopState != StopState.STOPPING) {
                    transition(remembered)
                } else {
                    transition(
                        remembered.copy(stopState = StopState.NONE),
                        WakeDirective.EnsureAlarmAudible,
                    )
                }
            }

            is WakeInput.UnrecoverableFailure -> finish(remembered, WakeOutcome.UNRECOVERABLE)
        }
    }

    fun diagnostics(snapshot: WakeSessionSnapshot, policy: WakePolicy): WakeRuntimeDiagnostics {
        require(snapshot.policyVersion == policy.version) {
            "Wake Session policy ${snapshot.policyVersion} does not match diagnostics policy ${policy.version}"
        }
        return WakeRuntimeDiagnostics(
            activationScore = snapshot.activationEvidence.score(policy),
            activationThreshold = policy.activationThreshold,
            escalationLevel = snapshot.escalationLevel,
            processedInputCount = snapshot.processedInputIds.size,
        )
    }

    fun replay(
        initial: WakeSessionSnapshot,
        inputs: Iterable<WakeInput>,
        policy: WakePolicy,
    ): WakeReplayResult {
        var snapshot = initial
        val steps = inputs.map { input ->
            val transition = reduce(snapshot, input, policy)
            snapshot = transition.snapshot
            WakeReplayStep(input, transition)
        }
        return WakeReplayResult(snapshot, steps)
    }

    private fun advanceOr(
        snapshot: WakeSessionSnapshot,
        policy: WakePolicy,
        vararg otherwise: WakeDirective,
    ): WakeTransition {
        if (snapshot.phase == WakePhase.ORIENTING) return transition(snapshot)
        if (snapshot.activationEvidence.score(policy) < policy.activationThreshold) {
            return transition(snapshot, *otherwise)
        }
        return transition(
            snapshot.copy(
                phase = WakePhase.ORIENTING,
                snoozeState = SnoozeState.NONE,
                engagementSilenceElapsed = Duration.ZERO,
            ),
            WakeDirective.StopObservingMotion,
            WakeDirective.PresentOrientation,
            WakeDirective.Speak(SpeechIntent.Orientation),
        )
    }

    private fun finish(snapshot: WakeSessionSnapshot, outcome: WakeOutcome): WakeTransition = transition(
        snapshot.copy(
            phase = WakePhase.FINISHED,
            outcome = outcome,
            snoozeState = SnoozeState.NONE,
            stopState = StopState.NONE,
            engagementSilenceElapsed = Duration.ZERO,
        ),
        WakeDirective.StopObservingMotion,
        WakeDirective.CompleteSession(outcome),
    )

    private fun transition(
        snapshot: WakeSessionSnapshot,
        vararg directives: WakeDirective,
    ): WakeTransition = WakeTransition(
        snapshot = snapshot,
        directives = directives.filter { directive ->
            when (directive) {
                is WakeDirective.Speak -> snapshot.capabilities.speechAvailable
                WakeDirective.ObserveMotion -> snapshot.capabilities.motionAvailable
                else -> true
            }
        },
        inputApplied = true,
    )

    private fun remember(
        snapshot: WakeSessionSnapshot,
        inputId: WakeInputId,
        policy: WakePolicy,
    ): WakeSessionSnapshot {
        val remembered = (snapshot.processedInputIds + inputId)
            .takeLast(policy.rememberedInputLimit)
        return snapshot.copy(processedInputIds = remembered)
    }

    private fun WakeInput.isStaleActivationCallback(): Boolean = when (this) {
        is WakeInput.AlarmFired,
        is WakeInput.WakeSurfacePresented,
        is WakeInput.UserInteracted,
        is WakeInput.VoiceResponseObserved,
        is WakeInput.MotionObserved,
        is WakeInput.SilenceElapsed,
        is WakeInput.SpeechFinished,
        is WakeInput.SpeechFailed,
        -> true

        else -> false
    }
}
