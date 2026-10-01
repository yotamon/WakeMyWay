package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeAssistantTurnStateTest {
    @Test
    fun `completed response without audio is treated as silent failure after grace`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-1"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("resp-1", "completed"),
        )
        assertTrue(state.active)
        assertEquals(
            RealtimeAssistantTurnState.Signal.SILENT,
            state.onTerminalGraceExpired(),
        )
        assertFalse(state.active)
    }

    @Test
    fun `response done never finishes while buffered audio is still playing`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-1"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.STARTED,
            state.onAudioStarted("resp-1"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("resp-1", "completed"),
        )
        assertTrue(state.active)
        assertNull(state.onTerminalGraceExpired())
        assertEquals(
            RealtimeAssistantTurnState.Signal.FINISHED,
            state.onAudioStopped("resp-1", interrupted = false),
        )
        assertFalse(state.active)
    }

    @Test
    fun `cancelled response without audible output reports interruption once`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-1"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("resp-1", "cancelled"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onTerminalGraceExpired(),
        )
        assertNull(state.onTerminalGraceExpired())
        assertFalse(state.active)
    }

    @Test
    fun `incomplete response without audible output reports interruption`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-1"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("resp-1", "incomplete"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onTerminalGraceExpired(),
        )
        assertFalse(state.active)
    }

    @Test
    fun `cancelled audible response closes after grace when buffer clear is missing`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-1"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.STARTED,
            state.onAudioStarted("resp-1"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("resp-1", "cancelled"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onTerminalGraceExpired(),
        )
        assertFalse(state.active)
    }

    @Test
    fun `cleared output reports interruption and ignores later response terminal event`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-1"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.STARTED,
            state.onAudioStarted("resp-1"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onAudioStopped("resp-1", interrupted = true),
        )
        assertNull(state.onResponseDone("resp-1", "cancelled"))
        assertFalse(state.active)
    }

    @Test
    fun `failed response ends the turn immediately`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-1"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.FAILED,
            state.onResponseDone("resp-1", "failed"),
        )
        assertFalse(state.active)
    }

    @Test
    fun `late old buffer clear cannot interrupt a newer unbound response`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-old"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.STARTED,
            state.onAudioStarted("resp-old"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.NEEDS_TERMINAL_GRACE,
            state.onResponseDone("resp-old", "cancelled"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onTerminalGraceExpired(),
        )

        assertTrue(state.begin())
        assertNull(state.onAudioStopped("resp-old", interrupted = true))
        assertTrue(state.active)
        assertTrue(state.onResponseCreated("resp-new"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.STARTED,
            state.onAudioStarted("resp-new"),
        )
    }

    @Test
    fun `late old terminal event cannot terminate a newer bound response`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-old"))
        assertEquals(
            RealtimeAssistantTurnState.Signal.STARTED,
            state.onAudioStarted("resp-old"),
        )
        assertEquals(
            RealtimeAssistantTurnState.Signal.INTERRUPTED,
            state.onAudioStopped("resp-old", interrupted = true),
        )

        assertTrue(state.begin())
        assertTrue(state.onResponseCreated("resp-new"))
        assertNull(state.onResponseDone("resp-old", "cancelled"))
        assertTrue(state.active)
        assertEquals(
            RealtimeAssistantTurnState.Signal.STARTED,
            state.onAudioStarted("resp-new"),
        )
    }

    @Test
    fun `overlapping user facing responses are rejected`() {
        val state = RealtimeAssistantTurnState()

        assertTrue(state.begin())
        assertFalse(state.begin())
    }
}
