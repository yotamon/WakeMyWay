package com.wakemyway.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeRuntimeVoiceTurnTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()

    @Test
    fun `conversational opening listens after one prompt instead of speaking twice`() {
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

        val listen = runtime.reduce(snapshot, WakeInput.SpeechFinished(id("initial-done")), policy)
        assertEquals(WakePhase.ENGAGING, listen.snapshot.phase)
        assertTrue(WakeDirective.ListenForVoiceResponse in listen.directives)
        assertTrue(WakeDirective.ObserveMotion in listen.directives)
        assertFalse(listen.directives.any { it is WakeDirective.Speak })

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
    fun `second activating voice reply becomes conversational instead of another physical task`() {
        val conversationalPolicy = WakePolicy(activationThreshold = 8)
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("conversation-loop"),
            policy = conversationalPolicy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )
        snapshot = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("first-reply"), coherent = true),
            conversationalPolicy,
        ).snapshot
        assertEquals(WakePhase.ACTIVATING, snapshot.phase)

        val continued = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("second-reply"), coherent = true),
            conversationalPolicy,
        )

        assertEquals(WakePhase.ACTIVATING, continued.snapshot.phase)
        assertEquals(2, continued.snapshot.activationEvidence.coherentVoiceResponses)
        assertTrue(WakeDirective.Speak(SpeechIntent.HoldEngagement) in continued.directives)
        assertFalse(WakeDirective.Speak(SpeechIntent.ActivateUpperBody) in continued.directives)
        assertTrue(WakeDirective.ObserveMotion in continued.directives)
    }

    @Test
    fun `later conversational turns do not become a physical checklist`() {
        val conversationalPolicy = WakePolicy(activationThreshold = 20)
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("calm-conversation"),
            policy = conversationalPolicy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )

        val first = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("first"), coherent = true),
            conversationalPolicy,
        )
        snapshot = first.snapshot
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in first.directives)

        repeat(3) { index ->
            val continued = runtime.reduce(
                snapshot,
                WakeInput.VoiceResponseObserved(id("continued-$index"), coherent = true),
                conversationalPolicy,
            )
            snapshot = continued.snapshot
            assertTrue(WakeDirective.Speak(SpeechIntent.HoldEngagement) in continued.directives)
            assertFalse(
                continued.directives.any {
                    it == WakeDirective.Speak(SpeechIntent.ActivateUpperBody) ||
                        it == WakeDirective.Speak(SpeechIntent.StandIfSafe) ||
                        it == WakeDirective.Speak(SpeechIntent.KeepEngaging)
                },
            )
        }
    }

    @Test
    fun `verbal engagement alone never escalates to standing`() {
        val conversationalPolicy = WakePolicy(activationThreshold = 20)
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("no-motion-standing-guard"),
            policy = conversationalPolicy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )

        repeat(4) { index ->
            val transition = runtime.reduce(
                snapshot,
                WakeInput.VoiceResponseObserved(id("reply-$index"), coherent = true),
                conversationalPolicy,
            )
            snapshot = transition.snapshot
            assertFalse(WakeDirective.Speak(SpeechIntent.StandIfSafe) in transition.directives)
        }

        assertEquals(0, snapshot.activationEvidence.devicePickups)
        assertEquals(0, snapshot.activationEvidence.orientationChanges)
        assertEquals(0, snapshot.activationEvidence.sustainedMovements)
    }

    @Test
    fun `device pickup alone does not unlock standing`() {
        val conversationalPolicy = WakePolicy(activationThreshold = 20)
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("pickup-not-standing"),
            policy = conversationalPolicy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )

        repeat(2) { index ->
            snapshot = runtime.reduce(
                snapshot,
                WakeInput.VoiceResponseObserved(id("reply-$index"), coherent = true),
                conversationalPolicy,
            ).snapshot
        }
        snapshot = runtime.reduce(
            snapshot,
            WakeInput.MotionObserved(id("pickup"), MotionEvidenceKind.DEVICE_PICKUP),
            conversationalPolicy,
        ).snapshot

        val third = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("reply-2"), coherent = true),
            conversationalPolicy,
        )

        assertFalse(WakeDirective.Speak(SpeechIntent.StandIfSafe) in third.directives)
        assertTrue(WakeDirective.Speak(SpeechIntent.HoldEngagement) in third.directives)
        assertFalse(WakeDirective.Speak(SpeechIntent.ActivateUpperBody) in third.directives)
    }

    @Test
    fun `usable replies without phone motion never loop the same physical action`() {
        val conversationalPolicy = WakePolicy(activationThreshold = 20)
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("no-physical-loop"),
            policy = conversationalPolicy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )

        val first = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("first"), coherent = true),
            conversationalPolicy,
        )
        snapshot = first.snapshot
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in first.directives)

        val second = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("second"), coherent = true),
            conversationalPolicy,
        )
        snapshot = second.snapshot
        assertTrue(WakeDirective.Speak(SpeechIntent.HoldEngagement) in second.directives)
        assertFalse(WakeDirective.Speak(SpeechIntent.ActivateUpperBody) in second.directives)

        val third = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("third"), coherent = true),
            conversationalPolicy,
        )
        snapshot = third.snapshot
        assertTrue(WakeDirective.Speak(SpeechIntent.HoldEngagement) in third.directives)
        assertFalse(third.directives.any {
            it == WakeDirective.Speak(SpeechIntent.AskToSitUp) ||
                it == WakeDirective.Speak(SpeechIntent.AskToMove) ||
                it == WakeDirective.Speak(SpeechIntent.ActivateUpperBody) ||
                it == WakeDirective.Speak(SpeechIntent.StandIfSafe)
        })

        val fourth = runtime.reduce(
            snapshot,
            WakeInput.VoiceResponseObserved(id("fourth"), coherent = true),
            conversationalPolicy,
        )
        assertTrue(WakeDirective.Speak(SpeechIntent.HoldEngagement) in fourth.directives)
    }

    @Test
    fun `motion can satisfy activation without forced spoken proof`() {
        var snapshot = runtime.initial(
            sessionId = WakeSessionId("motion-without-speech"),
            policy = policy,
            capabilities = WakeCapabilities(
                speechAvailable = true,
                voiceInputAvailable = true,
                motionAvailable = true,
            ),
        )
        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot
        snapshot = runtime.reduce(snapshot, WakeInput.SpeechFinished(id("initial-done")), policy).snapshot

        val firstMovement = runtime.reduce(
            snapshot,
            WakeInput.MotionObserved(
                id("movement-0"),
                MotionEvidenceKind.SUSTAINED_MOVEMENT,
            ),
            policy,
        )
        assertEquals(WakePhase.ACTIVATING, firstMovement.snapshot.phase)

        val completedActivation = runtime.reduce(
            firstMovement.snapshot,
            WakeInput.MotionObserved(
                id("movement-1"),
                MotionEvidenceKind.SUSTAINED_MOVEMENT,
            ),
            policy,
        )

        assertEquals(policy.activationThreshold, runtime.diagnostics(completedActivation.snapshot, policy).activationScore)
        assertEquals(0, completedActivation.snapshot.activationEvidence.coherentVoiceResponses)
        assertEquals(WakePhase.ORIENTING, completedActivation.snapshot.phase)
        assertTrue(WakeDirective.PresentOrientation in completedActivation.directives)
        assertTrue(WakeDirective.StopListeningForVoiceResponse in completedActivation.directives)
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
