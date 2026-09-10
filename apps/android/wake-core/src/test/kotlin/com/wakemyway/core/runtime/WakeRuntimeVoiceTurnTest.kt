package com.wakemyway.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
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

    private fun id(value: String) = WakeInputId(value)
}
