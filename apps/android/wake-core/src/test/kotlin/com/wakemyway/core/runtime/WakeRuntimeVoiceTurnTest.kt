package com.wakemyway.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeRuntimeVoiceTurnTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()

    @Test
    fun `sit-up prompt waits for a voice response when local input is available`() {
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("voice-turn"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )
        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot

        val askToSit = runtime.reduce(snapshot, WakeInput.SpeechFinished(id("initial-done")), policy)
        assertEquals(WakePhase.ENGAGING, askToSit.snapshot.phase)
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToSitUp) in askToSit.directives)

        val listen = runtime.reduce(
            askToSit.snapshot,
            WakeInput.SpeechFinished(id("sit-prompt-done")),
            policy,
        )
        assertEquals(WakePhase.ENGAGING, listen.snapshot.phase)
        assertTrue(WakeDirective.ListenForVoiceResponse in listen.directives)

        val replied = runtime.reduce(
            listen.snapshot,
            WakeInput.VoiceResponseObserved(id("reply"), coherent = true),
            policy,
        )
        assertEquals(WakePhase.ACTIVATING, replied.snapshot.phase)
        assertEquals(1, replied.snapshot.activationEvidence.coherentVoiceResponses)
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in replied.directives)
    }

    @Test
    fun `motion cannot silently bypass the required spoken reply`() {
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("voice-gate"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )
        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot
        snapshot = runtime.reduce(snapshot, WakeInput.SpeechFinished(id("initial-done")), policy).snapshot

        repeat(2) { index ->
            snapshot = runtime.reduce(
                snapshot,
                WakeInput.MotionObserved(
                    id("movement-$index"),
                    MotionEvidenceKind.SUSTAINED_MOVEMENT,
                ),
                policy,
            ).snapshot
        }

        assertEquals(policy.activationThreshold, runtime.diagnostics(snapshot, policy).activationScore)
        assertEquals(WakePhase.ACTIVATING, snapshot.phase)
        assertEquals(0, snapshot.activationEvidence.coherentVoiceResponses)

        val promptFinished = runtime.reduce(
            snapshot,
            WakeInput.SpeechFinished(id("sit-prompt-done")),
            policy,
        )
        assertEquals(WakePhase.ACTIVATING, promptFinished.snapshot.phase)
        assertTrue(WakeDirective.ListenForVoiceResponse in promptFinished.directives)
        assertFalse(WakeDirective.PresentOrientation in promptFinished.directives)

        val replied = runtime.reduce(
            promptFinished.snapshot,
            WakeInput.VoiceResponseObserved(id("required-reply"), coherent = true),
            policy,
        )
        assertEquals(WakePhase.ORIENTING, replied.snapshot.phase)
        assertTrue(WakeDirective.PresentOrientation in replied.directives)
    }

    @Test
    fun `incoherent voice response earns no evidence and prompts a bounded re-engagement`() {
        val initial = runtime.initial(
            sessionId = WakeSessionId("voice-unclear"),
            policy = policy,
            capabilities = WakeCapabilities(voiceInputAvailable = true),
        )

        val unclear = runtime.reduce(
            initial,
            WakeInput.VoiceResponseObserved(id("unclear"), coherent = false),
            policy,
        )

        assertEquals(0, unclear.snapshot.activationEvidence.coherentVoiceResponses)
        assertEquals(1, unclear.snapshot.escalationLevel)
        assertTrue(WakeDirective.Speak(SpeechIntent.ReEngage(1)) in unclear.directives)
    }

    @Test
    fun `voice-input loss degrades to movement without blocking wake progression`() {
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("voice-degraded"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = false,
                motionAvailable = true,
            ),
        )
        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot
        snapshot = runtime.reduce(snapshot, WakeInput.SpeechFinished(id("initial-done")), policy).snapshot

        val afterSitPrompt = runtime.reduce(
            snapshot,
            WakeInput.SpeechFinished(id("sit-prompt-done")),
            policy,
        )

        assertEquals(WakePhase.ACTIVATING, afterSitPrompt.snapshot.phase)
        assertTrue(afterSitPrompt.directives.none { it == WakeDirective.ListenForVoiceResponse })
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in afterSitPrompt.directives)
        assertTrue(WakeDirective.ObserveMotion in afterSitPrompt.directives)
    }

    @Test
    fun `dropping voice input releases an already-satisfied score without another motion event`() {
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("voice-drop"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )
        repeat(2) { index ->
            snapshot = runtime.reduce(
                snapshot,
                WakeInput.MotionObserved(
                    id("movement-$index"),
                    MotionEvidenceKind.SUSTAINED_MOVEMENT,
                ),
                policy,
            ).snapshot
        }
        assertEquals(WakePhase.ACTIVATING, snapshot.phase)

        val degraded = runtime.reduce(
            snapshot,
            WakeInput.CapabilitiesChanged(
                id("voice-off"),
                snapshot.capabilities.copy(voiceInputAvailable = false),
            ),
            policy,
        )

        assertEquals(WakePhase.ORIENTING, degraded.snapshot.phase)
        assertTrue(WakeDirective.PresentOrientation in degraded.directives)
    }

    @Test
    fun `late local speech availability replays the initial wake prompt immediately`() {
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("late-tts"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = false,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )
        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot

        val restored = runtime.reduce(
            snapshot,
            WakeInput.CapabilitiesChanged(
                id("tts-ready"),
                snapshot.capabilities.copy(speechAvailable = true),
            ),
            policy,
        )

        assertEquals(WakePhase.ALERTING, restored.snapshot.phase)
        assertTrue(WakeDirective.Speak(SpeechIntent.InitialWake) in restored.directives)
    }

    private fun id(value: String) = WakeInputId(value)
}
