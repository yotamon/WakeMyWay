package com.wakemyway.core.runtime

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WakeRuntimeNoResponseTest {
    private val runtime = WakeRuntime()
    private val policy = WakePolicy()

    @Test
    fun `initial speech completion starts engagement timer before movement prompt`() {
        var snapshot = runtime.initial(WakeSessionId("no-response-speech"), policy)
        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot

        val afterInitialSpeech = runtime.reduce(
            snapshot,
            WakeInput.SpeechFinished(id("initial-speech-finished")),
            policy,
        )

        assertEquals(WakePhase.ENGAGING, afterInitialSpeech.snapshot.phase)
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToSitUp) in afterInitialSpeech.directives)
        assertTrue(WakeDirective.ObserveMotion in afterInitialSpeech.directives)

        val afterSilence = runtime.reduce(
            afterInitialSpeech.snapshot,
            WakeInput.SilenceElapsed(id("silence"), policy.movementPromptDelay),
            policy,
        )

        assertEquals(WakePhase.ACTIVATING, afterSilence.snapshot.phase)
        assertEquals(1, afterSilence.snapshot.escalationLevel)
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in afterSilence.directives)
        assertTrue(WakeDirective.ObserveMotion in afterSilence.directives)
    }

    @Test
    fun `speech-unavailable session still progresses through silence without user response`() {
        var snapshot = runtime.initial(
            WakeSessionId("no-response-no-speech"),
            policy,
            capabilities = WakeCapabilities(speechAvailable = false, motionAvailable = true),
        )
        snapshot = runtime.reduce(snapshot, WakeInput.AlarmFired(id("alarm")), policy).snapshot

        val firstSilence = runtime.reduce(
            snapshot,
            WakeInput.SilenceElapsed(id("silence-1"), Duration.ofSeconds(15)),
            policy,
        )
        assertEquals(WakePhase.ENGAGING, firstSilence.snapshot.phase)
        assertTrue(firstSilence.directives.none { it is WakeDirective.Speak })
        assertTrue(WakeDirective.ObserveMotion in firstSilence.directives)

        val secondSilence = runtime.reduce(
            firstSilence.snapshot,
            WakeInput.SilenceElapsed(id("silence-2"), policy.movementPromptDelay),
            policy,
        )
        assertEquals(WakePhase.ACTIVATING, secondSilence.snapshot.phase)
        assertEquals(2, secondSilence.snapshot.escalationLevel)
        assertTrue(secondSilence.directives.none { it is WakeDirective.Speak })
        assertTrue(WakeDirective.ObserveMotion in secondSilence.directives)
    }

    @Test
    fun `learned earlier movement delay changes directive timing without changing activation threshold`() {
        val defaultPolicy = WakePolicy(movementPromptDelay = Duration.ofSeconds(15))
        val learnedPolicy = defaultPolicy.copy(version = 2, movementPromptDelay = Duration.ofSeconds(10))

        var defaultSession = runtime.initial(WakeSessionId("default-timing"), defaultPolicy)
        defaultSession = runtime.reduce(defaultSession, WakeInput.UserInteracted(id("default-touch")), defaultPolicy).snapshot
        val defaultAfterTen = runtime.reduce(
            defaultSession,
            WakeInput.SilenceElapsed(id("default-10s"), Duration.ofSeconds(10)),
            defaultPolicy,
        )
        assertEquals(WakePhase.ENGAGING, defaultAfterTen.snapshot.phase)
        assertTrue(WakeDirective.Speak(SpeechIntent.ReEngage(1)) in defaultAfterTen.directives)

        var learnedSession = runtime.initial(WakeSessionId("learned-timing"), learnedPolicy)
        learnedSession = runtime.reduce(learnedSession, WakeInput.UserInteracted(id("learned-touch")), learnedPolicy).snapshot
        val learnedAfterTen = runtime.reduce(
            learnedSession,
            WakeInput.SilenceElapsed(id("learned-10s"), Duration.ofSeconds(10)),
            learnedPolicy,
        )
        assertEquals(WakePhase.ACTIVATING, learnedAfterTen.snapshot.phase)
        assertTrue(WakeDirective.Speak(SpeechIntent.AskToMove) in learnedAfterTen.directives)
        assertEquals(defaultPolicy.activationThreshold, learnedPolicy.activationThreshold)
    }

    private fun id(value: String) = WakeInputId(value)
}
