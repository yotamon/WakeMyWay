package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeAssistantTurnStateTest {
    @Test
    fun `completed response without audio closes after terminal grace`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("completed"),
        )
        assertTrue(state.active)
        assertEquals(
            RealtimeAssistantTurnState.Signal.FINISHED,
            state.onTerminalGraceExpired(),
        )
        assertFalse(state.active)
    }

    @Test
    fun `response done never finishes while buffered audio is still playing`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertEquals(RealtimeAssistantTurnState.Signal.STARTED, state.onAudioStarted())
        assertNull(state.onResponseDone("completed"))
        assertTrue(state.active)
        assertNull(state.onTerminalGraceExpired())
        assertEquals(
            RealtimeAssistantTurnState.Signal.FINISHED,
            state.onAudioStopped(interrupted = false),
        )
        assertFalse(state.active)
    }

    @Test
    fun `cancelled response without audible output reports interruption once`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("cancelled"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onTerminalGraceExpired(),
        )
        assertNull(state.onTerminalGraceExpired())
        assertFalse(state.active)
    }

    @Test
    fun `cleared output reports interruption and ignores later response terminal event`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertEquals(RealtimeAssistantTurnState.Signal.STARTED, state.onAudioStarted())
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onAudioStopped(interrupted = true),
        )
        assertNull(state.onResponseDone("cancelled"))
        assertFalse(state.active)
    }

    @Test
    fun `failed response ends the turn immediately`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertEquals(
            RealtimeAssistantTurnState.Signal.FAILED,
            state.onResponseDone("failed"),
        )
        assertFalse(state.active)
    }

    @Test
    fun `overlapping user facing responses are rejected`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertFalse(state.begin())
    }
}
