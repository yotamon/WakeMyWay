package com.wakemyway.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.alarm.WakeInteractiveDiagnosticEvent
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.alarm.WakeTerminalActions
import com.wakemyway.app.alarm.WakeTerminalReason
import com.wakemyway.app.motion.AndroidMotionObserver
import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.personalization.WakeAllowedContext
import com.wakemyway.core.personalization.WakePreferences
import com.wakemyway.core.personalization.WakeSessionPlan
import com.wakemyway.core.personalization.WakeSessionStrategyResolver
import com.wakemyway.core.runtime.SpeechIntent
import com.wakemyway.core.runtime.WakeCapabilities
import com.wakemyway.core.runtime.WakeDirective
import com.wakemyway.core.runtime.WakeInput
import com.wakemyway.core.runtime.WakeInputId
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakePhase
import com.wakemyway.core.runtime.WakePolicy
import com.wakemyway.core.runtime.WakeRuntime
import com.wakemyway.core.runtime.WakeSessionId
import com.wakemyway.core.runtime.WakeSessionSnapshot
import com.wakemyway.core.schedule.WakeOccurrenceId

interface WakeSessionController : AutoCloseable {
    fun onSurfaceVisible()
    fun onSurfaceHidden()
    fun confirmOrientation() = Unit
    fun closeForTerminalAction()
}

class WakeVoiceSessionController(
    context: Context,
    private val occurrenceId: WakeOccurrenceId,
    private val onUiState: (WakeVoiceUiState) -> Unit,
    private val onCompleted: () -> Unit,
    private val voiceStyle: VoiceStyle = VoiceStyle.DEFAULT,
    private val policy: WakePolicy = WakePolicy(),
    wakePreferences: WakePreferences = WakePreferences(),
    characterId: CharacterId = CharacterId.ALFRED,
    allowedContext: WakeAllowedContext = WakeAllowedContext(),
    private val terminalActions: WakeTerminalActions = WakeTerminalActions(context.applicationContext),
    runtimeTransitionObserver: WakeRuntimeTransitionObserver = WakeRuntimeTransitionObserver.NONE,
    elapsedRealtimeMillis: () -> Long = { SystemClock.elapsedRealtime() },
) : WakeSessionController {
    private val appContext = context.applicationContext
    private val timingTrace = WakeTimingTrace(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionPlan: WakeSessionPlan = WakeSessionStrategyResolver.resolve(
        preferences = wakePreferences,
        characterId = characterId,
        voiceStyle = voiceStyle,
        wakePolicy = policy,
        allowedContext = allowedContext,
    )
    private val runtime = WakeRuntime()
    private val runtimeObservation = WakeRuntimeObservationBridge(
        runtime = runtime,
        policy = policy,
        elapsedRealtimeMillis = elapsedRealtimeMillis,
        observer = runtimeTransitionObserver,
    )
    private val motionObserver = AndroidMotionObserver(context) { emission ->
        mainHandler.post {
            if (!closed && started && surfaceVisible) {
                dispatch(
                    WakeInput.MotionObserved(
                        id = nextInputId("motion-${emission.kind.name.lowercase()}"),
                        kind = emission.kind,
                    ),
                )
            }
        }
    }
    private val conversation: WakeConversationEnrichment? = WakeConversationEnrichmentFactory.create(
        context,
        object : WakeConversationEnrichment.Listener {
            override fun onConversationReady() {
                mainHandler.post {
                    if (closed) return@post
                    if (alarmOnly) return@post
                    timingTrace.interactive(occurrenceId, WakeInteractiveDiagnosticEvent.REALTIME_READY)
                    conversationLive = true
                    mainHandler.removeCallbacks(startFallback)
                    when {
                        startRequested && !started -> beginRuntime()
                        started -> syncSurfaceBoundResources()
                        else -> publish()
                    }
                }
            }

            override fun onAssistantSpeechStarted() {
                mainHandler.post {
                    if (closed || alarmOnly || !realtimeTurnInFlight) return@post
                    timingTrace.interactive(occurrenceId, WakeInteractiveDiagnosticEvent.REALTIME_SPEAKING)
                    speaking = true
                    listening = false
                    mode = if (::snapshot.isInitialized && snapshot.phase == WakePhase.ORIENTING) {
                        WakeVoiceMode.ORIENTING
                    } else {
                        WakeVoiceMode.SPEAKING
                    }
                    AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)
                    publish()
                }
            }

            override fun onAssistantSpeechFinished(interrupted: Boolean) {
                mainHandler.post {
                    if (closed || alarmOnly || !realtimeTurnInFlight || !started) return@post
                    val userSpeechAlreadyStarted = listening
                    speaking = false
                    if (interrupted) {
                        // Barge-in means the assistant turn is still waiting for the user's reply.
                        // Make that an actual bounded listening state. If speech_started arrived
                        // first, its commit watchdog owns the turn; otherwise arm the normal
                        // listening timeout so a lost/delayed VAD start event cannot hang forever.
                        voiceResponseRequested = true
                        listening = true
                        mode = WakeVoiceMode.LISTENING
                        if (!userSpeechAlreadyStarted) {
                            mainHandler.removeCallbacks(realtimeSilenceTimeout)
                            mainHandler.postDelayed(
                                realtimeSilenceTimeout,
                                sessionPlan.conversationPacing.listenTimeout.toMillis(),
                            )
                        }
                        publish()
                        return@post
                    }

                    realtimeTurnInFlight = false
                    realtimeIntent = null
                    dispatch(WakeInput.SpeechFinished(nextInputId("realtime-speech-finished")))
                }
            }

            override fun onUserSpeechStarted() {
                mainHandler.post {
                    if (closed || alarmOnly || !started || !surfaceVisible) return@post
                    mainHandler.removeCallbacks(realtimeSilenceTimeout)
                    timingTrace.interactive(occurrenceId, WakeInteractiveDiagnosticEvent.REALTIME_LISTENING)
                    listening = true
                    mode = WakeVoiceMode.LISTENING
                    AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)
                    publish()
                }
            }

            override fun onUserTurnObserved(coherent: Boolean) {
                mainHandler.post {
                    if (closed || alarmOnly || !started || !surfaceVisible) return@post
                    // A late classifier result from an already-consumed audio item must not create
                    // another Wake Runtime input after the conversation has moved on.
                    if (!listening && !realtimeTurnInFlight) return@post
                    mainHandler.removeCallbacks(realtimeSilenceTimeout)
                    voiceResponseRequested = false
                    listening = false
                    speaking = false
                    realtimeTurnInFlight = false
                    realtimeIntent = null
                    dispatch(
                        WakeInput.VoiceResponseObserved(
                            id = nextInputId(
                                if (coherent) "realtime-voice-response" else "realtime-voice-unclear",
                            ),
                            coherent = coherent,
                        ),
                    )
                }
            }

            override fun onConversationFailure(stage: String) {
                mainHandler.post {
                    if (closed || alarmOnly) return@post
                    timingTrace.interactive(
                        occurrenceId,
                        diagnosticEventForConversationFailure(stage),
                    )
                    enterAlarmOnly(degradationReasonForConversationFailure(stage))
                }
            }
        },
    )

    private lateinit var snapshot: WakeSessionSnapshot
    private var started = false
    private var startRequested = false
    private var surfaceVisible = false
    private var closed = false
    private var listening = false
    private var speaking = false
    private var voiceResponseRequested = false
    private var motionObservationRequested = false
    private var motionObserving = false
    private var inputSequence = 0L
    private var currentLine: String? = null
    private var mode: WakeVoiceMode = WakeVoiceMode.STARTING
    private var conversationLive = false
    private var alarmOnly = false
    private var degradationReason: WakeVoiceDegradationReason? = null
    private var realtimeTurnInFlight = false
    private var realtimeIntent: SpeechIntent? = null

    private val startFallback = Runnable {
        if (!started && startRequested && !closed && !alarmOnly) {
            timingTrace.interactive(
                occurrenceId,
                WakeInteractiveDiagnosticEvent.REALTIME_FAILURE_STARTUP_TIMEOUT,
            )
            enterAlarmOnly(WakeVoiceDegradationReason.STARTUP_TIMEOUT)
        }
    }
    private val silenceWatchdog = Runnable {
        if (!closed && !alarmOnly && started && surfaceVisible && !speaking && !listening && snapshot.phase != WakePhase.FINISHED) {
            dispatch(
                WakeInput.SilenceElapsed(
                    id = nextInputId("silence"),
                    interval = sessionPlan.conversationPacing.idleReengageDelay,
                ),
            )
        }
    }
    private val realtimeSilenceTimeout = Runnable {
        if (
            !closed &&
            !alarmOnly &&
            started &&
            surfaceVisible &&
            conversationLive &&
            listening &&
            snapshot.phase != WakePhase.FINISHED
        ) {
            voiceResponseRequested = false
            listening = false
            conversation?.setInputEnabled(false)
            dispatch(
                WakeInput.SilenceElapsed(
                    id = nextInputId("realtime-silence"),
                    interval = sessionPlan.conversationPacing.listenTimeout,
                ),
            )
        }
    }

    override fun onSurfaceVisible() {
        if (closed) return
        surfaceVisible = true

        if (!startRequested) {
            startRequested = true
            timingTrace.interactive(occurrenceId, WakeInteractiveDiagnosticEvent.REALTIME_CONNECTING)
            publish()
            if (conversation != null) {
                conversation.connect()
                mainHandler.postDelayed(startFallback, REALTIME_START_BUDGET_MILLIS)
            } else {
                enterAlarmOnly(WakeVoiceDegradationReason.TRANSPORT_UNAVAILABLE)
            }
        } else if (!alarmOnly) {
            conversation?.connect()
            if (started) {
                dispatch(WakeInput.WakeSurfacePresented(nextInputId("surface-visible")))
            }
        }

        if (started && !alarmOnly) syncSurfaceBoundResources()
    }

    override fun confirmOrientation() {
        if (!closed && started && snapshot.phase == WakePhase.ORIENTING) {
            dispatch(WakeInput.OrientationCompleted(nextInputId("first-move-confirmed")))
        }
    }

    override fun onSurfaceHidden() {
        surfaceVisible = false
        mainHandler.removeCallbacks(silenceWatchdog)
        suspendListeningForHiddenSurface()
        suspendMotionForHiddenSurface()
        AlarmPlaybackService.requestCriticalVolume(appContext, occurrenceId)
    }

    override fun closeForTerminalAction() {
        closeInternal(restoreCriticalAudio = false)
    }

    override fun close() {
        closeInternal(restoreCriticalAudio = true)
    }

    private fun closeInternal(restoreCriticalAudio: Boolean) {
        if (closed) return
        closed = true
        voiceResponseRequested = false
        motionObservationRequested = false
        mainHandler.removeCallbacks(startFallback)
        mainHandler.removeCallbacks(silenceWatchdog)
        mainHandler.removeCallbacks(realtimeSilenceTimeout)
        listening = false
        speaking = false
        conversation?.close()
        motionObserver.stop()
        motionObserving = false
        if (restoreCriticalAudio) {
            AlarmPlaybackService.requestCriticalVolume(appContext, occurrenceId)
        }
    }

    private fun beginRuntime() {
        if (started || closed || alarmOnly || !conversationLive) return
        mainHandler.removeCallbacks(startFallback)
        started = true
        runtimeObservation.begin()
        snapshot = runtime.initial(
            sessionId = WakeSessionId("wake-${occurrenceId.value}"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )
        dispatch(WakeInput.AlarmFired(nextInputId("alarm-fired")))
        syncSurfaceBoundResources()
    }

    private fun dispatch(input: WakeInput) {
        if (closed || alarmOnly || !started || snapshot.phase == WakePhase.FINISHED) return
        mainHandler.removeCallbacks(silenceWatchdog)

        val transition = runtimeObservation.reduce(snapshot, input)
        if (!transition.inputApplied) return
        if (transition.snapshot.phase != snapshot.phase) {
            timingTrace.interactive(occurrenceId, transition.snapshot.phase.toDiagnosticEvent())
        }
        snapshot = transition.snapshot
        updateModeFromSnapshot()
        publish()
        transition.directives.forEach(::execute)
        syncWatchdog()
    }

    private fun execute(directive: WakeDirective) {
        if (alarmOnly) return
        when (directive) {
            WakeDirective.EnsureAlarmAudible -> {
                AlarmPlaybackService.requestCriticalVolume(appContext, occurrenceId)
            }

            is WakeDirective.Speak -> speak(directive.intent)
            WakeDirective.ListenForVoiceResponse -> requestVoiceResponse()
            WakeDirective.StopListeningForVoiceResponse -> stopListening()

            WakeDirective.ObserveMotion -> {
                motionObservationRequested = true
                syncMotionObservation()
            }

            WakeDirective.StopObservingMotion -> {
                motionObservationRequested = false
                stopMotionObservation()
            }

            is WakeDirective.OfferSnooze -> Unit
            is WakeDirective.RequestSnoozeSchedule -> Unit
            WakeDirective.RequestStopExecution -> Unit

            WakeDirective.PresentOrientation -> {
                mode = WakeVoiceMode.ORIENTING
                publish()
            }

            is WakeDirective.CompleteSession -> {
                if (directive.outcome == WakeOutcome.COMPLETED) {
                    mode = WakeVoiceMode.COMPLETE
                    publish()
                    if (terminalActions.stop(occurrenceId, WakeTerminalReason.COMPLETED)) {
                        closeInternal(restoreCriticalAudio = false)
                        onCompleted()
                    }
                }
            }
        }
    }

    private fun speak(intent: SpeechIntent) {
        stopListening()
        val liveConversation = conversation?.takeIf { conversationLive && it.ready }
        if (liveConversation == null) {
            enterAlarmOnly(WakeVoiceDegradationReason.SESSION_FAILURE)
            return
        }

        realtimeIntent = intent
        realtimeTurnInFlight = true
        speaking = true
        listening = false
        currentLine = null
        mode = if (snapshot.phase == WakePhase.ORIENTING) {
            WakeVoiceMode.ORIENTING
        } else {
            WakeVoiceMode.SPEAKING
        }
        liveConversation.setInputEnabled(true)
        AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)
        publish()

        if (!liveConversation.respond(WakeSpeechRequest(intent, sessionPlan))) {
            enterAlarmOnly(WakeVoiceDegradationReason.TURN_FAILURE)
        }
    }

    private fun requestVoiceResponse() {
        voiceResponseRequested = true
        syncVoiceListening()
    }

    private fun syncVoiceListening() {
        if (
            !voiceResponseRequested ||
            !surfaceVisible ||
            closed ||
            alarmOnly ||
            !started ||
            speaking ||
            listening ||
            snapshot.phase == WakePhase.FINISHED
        ) {
            return
        }

        mainHandler.removeCallbacks(silenceWatchdog)
        if (conversationLive && conversation?.ready == true) {
            listening = true
            mode = WakeVoiceMode.LISTENING
            currentLine = null
            conversation.setInputEnabled(true)
            AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)
            mainHandler.removeCallbacks(realtimeSilenceTimeout)
            mainHandler.postDelayed(
                realtimeSilenceTimeout,
                sessionPlan.conversationPacing.listenTimeout.toMillis(),
            )
            publish()
            return
        }

        enterAlarmOnly(WakeVoiceDegradationReason.SESSION_FAILURE)
    }

    private fun stopListening() {
        voiceResponseRequested = false
        cancelListening(preserveAssistantOutput = true)
    }

    private fun suspendListeningForHiddenSurface() {
        cancelListening(preserveAssistantOutput = false)
    }

    private fun cancelListening(preserveAssistantOutput: Boolean) {
        mainHandler.removeCallbacks(realtimeSilenceTimeout)
        if (conversationLive) {
            conversation?.setInputEnabled(
                preserveAssistantOutput && realtimeTurnInFlight && speaking,
            )
        }
        if (!listening) return
        listening = false
    }

    private fun syncMotionObservation() {
        if (
            !motionObservationRequested ||
            !surfaceVisible ||
            closed ||
            !started ||
            !snapshot.capabilities.motionAvailable
        ) {
            return
        }
        if (motionObserving) return

        val availability = motionObserver.start()
        motionObserving = availability.observing
        if (!availability.motionAvailable && snapshot.capabilities.motionAvailable) {
            mainHandler.post {
                if (!closed && started) {
                    dispatch(
                        WakeInput.CapabilitiesChanged(
                            id = nextInputId("motion-unavailable"),
                            capabilities = snapshot.capabilities.copy(motionAvailable = false),
                        ),
                    )
                }
            }
        }
    }

    private fun suspendMotionForHiddenSurface() {
        stopMotionObservation()
    }

    private fun stopMotionObservation() {
        if (!motionObserving) return
        motionObserver.stop()
        motionObserving = false
    }

    private fun syncSurfaceBoundResources() {
        if (!started || closed || alarmOnly || !surfaceVisible) return
        syncMotionObservation()
        syncVoiceListening()
    }

    private fun updateModeFromSnapshot() {
        if (snapshot.phase == WakePhase.ORIENTING) {
            mode = WakeVoiceMode.ORIENTING
        } else if (!speaking && !listening && snapshot.phase == WakePhase.ACTIVATING) {
            mode = WakeVoiceMode.MOVING
        }
    }

    private fun publish() {
        if (alarmOnly || !started) {
            onUiState(
                projectPreRuntimeWakeVoiceState(
                    startRequested = startRequested,
                    alarmOnly = alarmOnly,
                    degradationReason = degradationReason,
                    activationThreshold = policy.activationThreshold,
                ),
            )
            return
        }

        val diagnostics = runtime.diagnostics(snapshot, policy)
        onUiState(
            WakeVoiceUiState(
                mode = mode,
                phase = snapshot.phase,
                spokenLine = currentLine,
                activationScore = diagnostics.activationScore,
                activationThreshold = diagnostics.activationThreshold,
                speechAvailable = snapshot.capabilities.speechAvailable,
                voiceInputAvailable = snapshot.capabilities.voiceInputAvailable,
                conversational = conversationLive,
            ),
        )
    }

    private fun syncWatchdog() {
        mainHandler.removeCallbacks(silenceWatchdog)
        if (
            !closed &&
            !alarmOnly &&
            started &&
            surfaceVisible &&
            snapshot.phase != WakePhase.FINISHED &&
            snapshot.phase != WakePhase.ORIENTING &&
            !speaking &&
            !listening
        ) {
            mainHandler.postDelayed(
                silenceWatchdog,
                sessionPlan.conversationPacing.idleReengageDelay.toMillis(),
            )
        }
    }

    private fun enterAlarmOnly(reason: WakeVoiceDegradationReason) {
        if (closed || alarmOnly) return
        alarmOnly = true
        degradationReason = reason
        timingTrace.interactive(occurrenceId, WakeInteractiveDiagnosticEvent.REALTIME_DEGRADED)
        conversationLive = false
        voiceResponseRequested = false
        motionObservationRequested = false
        speaking = false
        listening = false
        realtimeTurnInFlight = false
        realtimeIntent = null
        currentLine = null
        mainHandler.removeCallbacks(startFallback)
        mainHandler.removeCallbacks(silenceWatchdog)
        mainHandler.removeCallbacks(realtimeSilenceTimeout)
        conversation?.setInputEnabled(false)
        conversation?.close()
        stopMotionObservation()
        AlarmPlaybackService.requestCriticalVolume(appContext, occurrenceId)
        publish()
    }

    private fun nextInputId(kind: String): WakeInputId =
        WakeInputId("${occurrenceId.value}:${inputSequence++}:$kind")

    private companion object {
        // A cold Direct wake may need broker token minting plus WebRTC negotiation before the
        // first session.updated event. Each bounded network call may legitimately take up to 15s,
        // so an 8s global budget could cancel a healthy connection before its own timeout. The
        // alarm remains audible throughout while Realtime gets a realistic startup window.
        const val REALTIME_START_BUDGET_MILLIS = 20_000L
    }
}

private fun WakePhase.toDiagnosticEvent(): WakeInteractiveDiagnosticEvent = when (this) {
    WakePhase.ALERTING -> WakeInteractiveDiagnosticEvent.RUNTIME_ALERTING
    WakePhase.ENGAGING -> WakeInteractiveDiagnosticEvent.RUNTIME_ENGAGING
    WakePhase.ACTIVATING -> WakeInteractiveDiagnosticEvent.RUNTIME_ACTIVATING
    WakePhase.ORIENTING -> WakeInteractiveDiagnosticEvent.RUNTIME_ORIENTING
    WakePhase.FINISHED -> WakeInteractiveDiagnosticEvent.RUNTIME_FINISHED
}

enum class WakeVoiceMode {
    ALARM_ONLY,
    STARTING,
    SPEAKING,
    LISTENING,
    MOVING,
    ORIENTING,
    COMPLETE,
}

data class WakeVoiceUiState(
    val mode: WakeVoiceMode = WakeVoiceMode.ALARM_ONLY,
    val phase: WakePhase = WakePhase.ALERTING,
    val spokenLine: String? = null,
    val activationScore: Int = 0,
    val activationThreshold: Int = WakePolicy().activationThreshold,
    val speechAvailable: Boolean = false,
    val voiceInputAvailable: Boolean = false,
    val conversational: Boolean = false,
    val degradationReason: WakeVoiceDegradationReason? = null,
)
