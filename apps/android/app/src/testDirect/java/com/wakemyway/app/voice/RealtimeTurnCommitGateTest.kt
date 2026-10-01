package com.wakemyway.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeTurnCommitGateTest {
    private val gate = RealtimeTurnCommitGate(minimumDurationMs = 160L)

    @Test
    fun `qualifying speech is exposed only after matching commit`() {
        gate.onSpeechStarted("item-1", 1_000L)
        gate.onSpeechStopped("item-1", 1_700L)

        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.QUALIFIED,
            gate.onCommitted("item-1"),
        )
        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.STALE,
            gate.onCommitted("item-1"),
        )
    }

    @Test
    fun `short one-word reply is allowed through to semantic validation`() {
        gate.onSpeechStarted("item-1", 1_000L)
        gate.onSpeechStopped("item-1", 1_200L)

        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.QUALIFIED,
            gate.onCommitted("item-1"),
        )
    }

    @Test
    fun `very short noise is not promoted to semantic validation`() {
        gate.onSpeechStarted("item-1", 1_000L)
        gate.onSpeechStopped("item-1", 1_080L)

        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.UNQUALIFIED,
            gate.onCommitted("item-1"),
        )
    }

    @Test
    fun `missing timing metadata fails closed for the matching item`() {
        gate.onSpeechStarted("item-1", null)
        gate.onSpeechStopped("item-1", 1_700L)

        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.UNQUALIFIED,
            gate.onCommitted("item-1"),
        )
    }

    @Test
    fun `late commit after timeout is stale rather than a second bad reply`() {
        gate.onSpeechStarted("item-old", 1_000L)
        gate.onSpeechStopped("item-old", 1_700L)

        assertTrue(gate.onTimedOut())
        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.STALE,
            gate.onCommitted("item-old"),
        )
        assertFalse(gate.onTimedOut())
    }

    @Test
    fun `old commit cannot consume a newer active turn`() {
        gate.onSpeechStarted("item-old", 1_000L)
        assertTrue(gate.onTimedOut())

        gate.onSpeechStarted("item-new", 2_000L)
        gate.onSpeechStopped("item-new", 2_500L)

        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.STALE,
            gate.onCommitted("item-old"),
        )
        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.QUALIFIED,
            gate.onCommitted("item-new"),
        )
    }

    @Test
    fun `mismatched stop cannot alter active turn qualification`() {
        gate.onSpeechStarted("item-new", 1_000L)
        assertFalse(gate.onSpeechStopped("item-old", 2_000L))

        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.UNQUALIFIED,
            gate.onCommitted("item-new"),
        )
    }

    @Test
    fun `reset clears an awaiting committed turn`() {
        gate.onSpeechStarted("item-1", 1_000L)
        gate.onSpeechStopped("item-1", 1_700L)
        gate.reset()

        assertEquals(
            RealtimeTurnCommitGate.CommitDecision.STALE,
            gate.onCommitted("item-1"),
        )
    }
}
