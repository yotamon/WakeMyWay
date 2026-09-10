package com.wakemyway.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.character.LocalCharacterSpeaker
import com.wakemyway.app.character.LocalSpeechResult
import com.wakemyway.app.character.LocalSpeechState
import com.wakemyway.app.motion.AndroidMotionObserver
import com.wakemyway.core.character.AlfredCharacter
import com.wakemyway.core.character.WakeLineKey
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
import java.time.Duration

/**
 * Android adapter around the pure Wake Runtime.
 *
 * Wake Runtime remains the only behavioral authority. This controller may choose an optional
 * conversational speech renderer when available, but model/network state can never Stop/Snooze,
 * complete a session, or replace typed activation evidence. Local TTS/STT remains the fallback.
 */
class WakeVoiceSessionController(
    context: Context,
    private val occurrenceId: WakeOccurrenceId,
    private val onUiState: (WakeVoiceUiState) -> Unit,
    private val onCompleted: () -> Unit,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()
    private val voiceListener = LocalVoiceListener(
        context = context,
        languageTag = AlfredCharacter.spec.voiceLocaleTag,
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
    private val speaker = LocalCharacterSpeaker(
        context = context,
        character = AlfredCharacter.spec,
        onStateChanged = ::onSpeechStateChanged,
    )
    private val conversation: WakeConversationEnrichment? = WakeConversationEnrichmentFactory.create(
        context,
        object : WakeConversationEnrichment.Listener {
            override fun onConversationReady() {
                mainHandler.post {
                    if (closed) return@post
                    conversationLive = true
                    publish()
                }
            }

            override fun onAssistantSpeechStarted() {
                mainHandler.post {
                    if (closed || !realtimeTurnInFlight) return@post
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
                    if (closed || !realtimeTurnInFlight || !started) return@post
                    speaking = false
                    if (interrupted) {
                        // VAD will shortly emit the user's completed turn. That VoiceResponseObserved
                        // becomes the sole runtime transition, preventing a double response.
                        mode = WakeVoiceMode.LISTENING
                        publish()
                        return@post
                    }

                    val completedIntent = realtimeIntent
                    realtimeTurnInFlight = false
                    realtimeIntent = null
                    dispatch(WakeInput.SpeechFinished(nextInputId("realtime-speech-finished")))
                    if (
                        completedIntent == SpeechIntent.Orientation &&
                        snapshot.phase == WakePhase.ORIENTING
                    ) {
                        dispatch(WakeInput.OrientationCompleted(nextInputId("realtime-orientation-complete")))
                    }
                }
            }

            override fun onUserSpeechStarted() {
                mainHandler.post {
                    if (closed || !started || !surfaceVisible) return@post
                    mainHandler.removeCallbacks(realtimeSilenceTimeout)
                    listening = true
                    mode = WakeVoiceMode.LISTENING
                    AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)
                    publish()
                }
            }

            override fun onUserTurnObserved() {
                mainHandler.post {
                    if (closed || !started || !surfaceVisible) return@post
                    mainHandler.removeCallbacks(realtimeSilenceTimeout)
                    listening = false
                    speaking = false
                    realtimeTurnInFlight = false
                    realtimeIntent = null
                    dispatch(
                        WakeInput.VoiceResponseObserved(
                            id = nextInputId("realtime-voice-response"),
                            coherent = true,
                        ),
                    )
                }
            }

            override fun onConversationFailure(stage: String) {
                mainHandler.post {
                    if (closed) return@post
                    conversationLive = false
                    conversation?.setInputEnabled(false)
                    mainHandler.removeCallbacks(realtimeSilenceTimeout)

                    val failedIntent = realtimeIntent
                    val failedDuringTurn = realtimeTurnInFlight && failedIntent != null
                    realtimeTurnInFlight = false
                    realtimeIntent = null
                    speaking = false
                    listening = false
                    publish()

                    // Preserve the exact typed runtime intent. A network/provider failure changes
                    // rendering only; it must not create a parallel behavioral transition.
                    if (failedDuringTurn && started && snapshot.phase != WakePhase.FINISHED) {
                        speakLocally(failedIntent)
                    } else if (started) {
                        syncWatchdog()
                    }
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
    private var motionObserving = false
    private var inputSequence = 0L
    private var speechSequence = 0L
    private var currentLine: String? = null
    private var mode: WakeVoiceMode = WakeVoiceMode.STARTING
    private var conversationLive = false
    private var realtimeTurnInFlight = false
    private var realtimeIntent: SpeechIntent? = null

    private val startFallback = Runnable {
        if (!started && startRequested && !closed) {
            beginRuntime(speaker.state() is LocalSpeechState.Ready)
        }
    }
    private val silenceWatchdog = Runnable {
        if (!closed && started && surfaceVisible && !speaking && !listening && snapshot.phase != WakePhase.FINISHED) {
            dispatch(
                WakeInput.SilenceElapsed(
                    id = nextInputId("silence"),
                    interval = SILENCE_INTERVAL,
                ),
            )
        }
    }
    private val realtimeSilenceTimeout = Runnable {
        if (
            !closed &&
            started &&
            surfaceVisible &&
            conversationLive &&
            listening &&
            snapshot.phase != WakePhase.FINISHED
        ) {
            listening = false
            conversation?.setInputEnabled(false)
            dispatch(
                WakeInput.SilenceElapsed(
                    id = nextInputId("realtime-silence"),
                    interval = REALTIME_LISTEN_INTERVAL,
                ),
            )
        }
    }

    fun onSurfaceVisible() {
        if (closed) return
        surfaceVisible = true
        conversation?.connect()
        if (!startRequested) {
            startRequested = true
            publish()
            when (speaker.state()) {
                is LocalSpeechState.Ready -> beginRuntime(speechAvailable = true)
                is LocalSpeechState.Unavailable -> beginRuntime(speechAvailable = false)
                LocalSpeechState.Initializing -> mainHandler.postDelayed(startFallback, TTS_START_BUDGET_MILLIS)
            }
        } else if (started) {
            dispatch(WakeInput.WakeSurfacePresented(nextInputId("surface-visible")))
        }
    }

    fun onSurfaceHidden() {
        surfaceVisible = false
        mainHandler.removeCallbacks(silenceWatchdog)
        mainHandler.removeCallbacks(realtimeSilenceTimeout)
        conversation?.setInputEnabled(false)
        if (listening) {
            listening = false
            voiceListener.cancel(deliverCancellation = false)
        }
        AlarmPlaybackService.requestCriticalVolume(appContext, occurrenceId)
    }

    /**
     * Use immediately before a terminal AlarmPlaybackService command such as Stop or Snooze.
     * No restore-volume command is sent because the terminal command itself owns playback teardown.
     */
    fun closeForTerminalAction() {
        closeInternal(restoreCriticalAudio = false)
    }

    override fun close() {
        closeInternal(restoreCriticalAudio = true)
    }

    private fun closeInternal(restoreCriticalAudio: Boolean) {
        if (closed) return
        closed = true
        mainHandler.removeCallbacks(startFallback)
        mainHandler.removeCallbacks(silenceWatchdog)
        mainHandler.removeCallbacks(realtimeSilenceTimeout)
        listening = false
        speaking = false
        voiceListener.close()
        conversation?.close()
        speaker.close()
        motionObserver.stop()
        if (restoreCriticalAudio) {
            AlarmPlaybackService.requestCriticalVolume(appContext, occurrenceId)
        }
    }

    private fun beginRuntime(speechAvailable: Boolean) {
        if (started || closed) return
        mainHandler.removeCallbacks(startFallback)
        started = true
        snapshot = runtime.initial(
            sessionId = WakeSessionId("wake-${occurrenceId.value}"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = speechAvailable || conversationLive,
                voiceInputAvailable = voiceListener.availability() is LocalVoiceAvailability.Ready,
                motionAvailable = true,
            ),
        )
        dispatch(WakeInput.AlarmFired(nextInputId("alarm-fired")))
    }

    private fun onSpeechStateChanged(state: LocalSpeechState) {
        if (closed || !startRequested) return
        if (!started) {
            when (state) {
                is LocalSpeechState.Ready -> beginRuntime(speechAvailable = true)
                is LocalSpeechState.Unavailable -> beginRuntime(speechAvailable = conversationLive)
                LocalSpeechState.Initializing -> Unit
            }
            return
        }

        val available = state is LocalSpeechState.Ready || conversationLive
        if (snapshot.capabilities.speechAvailable != available) {
            dispatch(
                WakeInput.CapabilitiesChanged(
                    id = nextInputId("speech-capability"),
                    capabilities = snapshot.capabilities.copy(speechAvailable = available),
                ),
            )
        }
    }

    private fun dispatch(input: WakeInput) {
        if (closed || !started || snapshot.phase == WakePhase.FINISHED) return
        mainHandler.removeCallbacks(silenceWatchdog)

        val transition = runtime.reduce(snapshot, input, policy)
        if (!transition.inputApplied) return
        snapshot = transition.snapshot
        updateModeFromSnapshot()
        publish()
        transition.directives.forEach(::execute)
        syncWatchdog()
    }

    private fun execute(directive: WakeDirective) {
        when (directive) {
            WakeDirective.EnsureAlarmAudible -> {
                AlarmPlaybackService.requestCriticalVolume(appContext, occurrenceId)
            }

            is WakeDirective.Speak -> speak(directive.intent)
            WakeDirective.ListenForVoiceResponse -> listenForVoiceResponse()
            WakeDirective.StopListeningForVoiceResponse -> stopListening()

            WakeDirective.ObserveMotion -> {
                if (!motionObserving && surfaceVisible) {
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
            }

            WakeDirective.StopObservingMotion -> {
                motionObserver.stop()
                motionObserving = false
            }

            is WakeDirective.OfferSnooze -> Unit
            is WakeDirective.RequestSnoozeSchedule -> Unit
            WakeDirective.RequestStopExecution -> Unit

            WakeDirective.PresentOrientation -> {
                mode = WakeVoiceMode.ORIENTING
                publish()
                if (!snapshot.capabilities.speechAvailable) {
                    mainHandler.post {
                        if (!closed && started && snapshot.phase == WakePhase.ORIENTING) {
                            dispatch(WakeInput.OrientationCompleted(nextInputId("orientation-without-speech")))
                        }
                    }
                }
            }

            is WakeDirective.CompleteSession -> {
                if (directive.outcome == WakeOutcome.COMPLETED) {
                    mode = WakeVoiceMode.COMPLETE
                    publish()
                    closeInternal(restoreCriticalAudio = false)
                    AlarmPlaybackService.requestStop(appContext, occurrenceId)
                    onCompleted()
                }
            }
        }
    }

    private fun speak(intent: SpeechIntent) {
        stopListening()
        val liveConversation = conversation?.takeIf { conversationLive && it.ready }
        if (liveConversation != null) {
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
            if (liveConversation.respond(intent)) return

            realtimeTurnInFlight = false
            realtimeIntent = null
            liveConversation.setInputEnabled(false)
        }
        speakLocally(intent)
    }

    private fun speakLocally(intent: SpeechIntent) {
        stopListening()
        speaking = true
        mode = if (snapshot.phase == WakePhase.ORIENTING) {
            WakeVoiceMode.ORIENTING
        } else {
            WakeVoiceMode.SPEAKING
        }
        AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)

        val line = AlfredCharacter.render(
            intent = intent,
            key = WakeLineKey("${occurrenceId.value}:${speechSequence++}"),
        )
        currentLine = line.text
        publish()

        speaker.speak(
            line = line,
            utteranceId = "wake-${occurrenceId.value}-${speechSequence}",
        ) { result ->
            mainHandler.post {
                if (closed || !started) return@post
                speaking = false
                when (result) {
                    LocalSpeechResult.Completed -> {
                        dispatch(WakeInput.SpeechFinished(nextInputId("speech-finished")))
                        if (intent == SpeechIntent.Orientation && snapshot.phase == WakePhase.ORIENTING) {
                            dispatch(WakeInput.OrientationCompleted(nextInputId("orientation-complete")))
                        }
                    }

                    is LocalSpeechResult.Failed -> {
                        if (intent == SpeechIntent.Orientation && snapshot.phase == WakePhase.ORIENTING) {
                            dispatch(WakeInput.OrientationCompleted(nextInputId("orientation-speech-failed")))
                        } else {
                            dispatch(WakeInput.SpeechFailed(nextInputId("speech-failed")))
                        }
                    }
                }
            }
        }
    }

    private fun listenForVoiceResponse() {
        if (!surfaceVisible || closed) return
        if (conversationLive && conversation?.ready == true) {
            speaking = false
            listening = true
            mode = WakeVoiceMode.LISTENING
            currentLine = null
            conversation.setInputEnabled(true)
            AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)
            mainHandler.removeCallbacks(realtimeSilenceTimeout)
            mainHandler.postDelayed(realtimeSilenceTimeout, REALTIME_LISTEN_INTERVAL.toMillis())
            publish()
            return
        }

        if (voiceListener.availability() !is LocalVoiceAvailability.Ready) {
            degradeVoiceInput()
            return
        }

        speaking = false
        listening = true
        mode = WakeVoiceMode.LISTENING
        AlarmPlaybackService.requestVoiceWindow(appContext, occurrenceId)
        publish()

        voiceListener.listen { result ->
            mainHandler.post {
                if (closed || !started || !listening) return@post
                listening = false
                when (result) {
                    is LocalVoiceResult.Recognized -> dispatch(
                        WakeInput.VoiceResponseObserved(
                            id = nextInputId("voice-response"),
                            coherent = result.coherent,
                        ),
                    )

                    LocalVoiceResult.NoResponse -> dispatch(
                        WakeInput.SilenceElapsed(
                            id = nextInputId("voice-silence"),
                            interval = SILENCE_INTERVAL,
                        ),
                    )

                    is LocalVoiceResult.Failed -> degradeVoiceInput()
                    LocalVoiceResult.Cancelled -> syncWatchdog()
                }
            }
        }
    }

    private fun stopListening() {
        mainHandler.removeCallbacks(realtimeSilenceTimeout)
        if (conversationLive) {
            conversation?.setInputEnabled(realtimeTurnInFlight && speaking)
        }
        if (!listening) return
        listening = false
        voiceListener.cancel(deliverCancellation = false)
    }

    private fun degradeVoiceInput() {
        if (!snapshot.capabilities.voiceInputAvailable) {
            syncWatchdog()
            return
        }
        mode = WakeVoiceMode.DEGRADED
        publish()
        dispatch(
            WakeInput.CapabilitiesChanged(
                id = nextInputId("voice-input-unavailable"),
                capabilities = snapshot.capabilities.copy(voiceInputAvailable = false),
            ),
        )
    }

    private fun updateModeFromSnapshot() {
        if (snapshot.phase == WakePhase.ORIENTING) {
            mode = WakeVoiceMode.ORIENTING
        } else if (!snapshot.capabilities.voiceInputAvailable && snapshot.phase != WakePhase.ALERTING) {
            mode = WakeVoiceMode.DEGRADED
        } else if (!speaking && !listening && snapshot.phase == WakePhase.ACTIVATING) {
            mode = WakeVoiceMode.MOVING
        }
    }

    private fun publish() {
        if (!started) {
            onUiState(
                WakeVoiceUiState(
                    mode = WakeVoiceMode.STARTING,
                    phase = WakePhase.ALERTING,
                    spokenLine = currentLine,
                    activationScore = 0,
                    activationThreshold = policy.activationThreshold,
                    speechAvailable = speaker.state() is LocalSpeechState.Ready || conversationLive,
                    voiceInputAvailable = voiceListener.availability() is LocalVoiceAvailability.Ready,
                    conversational = conversationLive,
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
            started &&
            surfaceVisible &&
            snapshot.phase != WakePhase.FINISHED &&
            !speaking &&
            !listening
        ) {
            mainHandler.postDelayed(silenceWatchdog, SILENCE_INTERVAL.toMillis())
        }
    }

    private fun nextInputId(kind: String): WakeInputId =
        WakeInputId("${occurrenceId.value}:${inputSequence++}:$kind")

    private companion object {
        val SILENCE_INTERVAL: Duration = Duration.ofSeconds(12)
        val REALTIME_LISTEN_INTERVAL: Duration = Duration.ofSeconds(10)
        const val TTS_START_BUDGET_MILLIS = 1_200L
    }
}

enum class WakeVoiceMode {
    STARTING,
    SPEAKING,
    LISTENING,
    MOVING,
    ORIENTING,
    DEGRADED,
    COMPLETE,
}

data class WakeVoiceUiState(
    val mode: WakeVoiceMode = WakeVoiceMode.STARTING,
    val phase: WakePhase = WakePhase.ALERTING,
    val spokenLine: String? = null,
    val activationScore: Int = 0,
    val activationThreshold: Int = WakePolicy().activationThreshold,
    val speechAvailable: Boolean = false,
    val voiceInputAvailable: Boolean = false,
    val conversational: Boolean = false,
)
