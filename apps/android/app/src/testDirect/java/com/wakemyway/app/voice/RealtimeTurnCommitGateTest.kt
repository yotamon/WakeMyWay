package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeTurnCommitGateTest {
    private val gate = RealtimeTurnCommitGate(minimumDurationMs = 160L)

    @Test
    fun `qualifying speech is exposed only after commit`() {
        gate.onSpeechStarted(1_000L)
        gate.onSpeechStopped(1_700L)

        assertTrue(gate.onCommitted())
        assertFalse(gate.onCommitted())
    }

    @Test
    fun `short one-word reply is allowed through to semantic validation`() {
        gate.onSpeechStarted(1_000L)
        gate.onSpeechStopped(1_200L)

        assertTrue(gate.onCommitted())
    }

    @Test
    fun `very short noise is not promoted to semantic validation`() {
        gate.onSpeechStarted(1_000L)
        gate.onSpeechStopped(1_080L)

        assertFalse(gate.onCommitted())
    }

    @Test
    fun `missing timing metadata fails closed`() {
        gate.onSpeechStarted(null)
        gate.onSpeechStopped(1_700L)

        assertFalse(gate.onCommitted())
    }

    @Test
    fun `reset clears an awaiting committed turn`() {
        gate.onSpeechStarted(1_000L)
        gate.onSpeechStopped(1_700L)
        gate.reset()

        assertFalse(gate.onCommitted())
    }
}
