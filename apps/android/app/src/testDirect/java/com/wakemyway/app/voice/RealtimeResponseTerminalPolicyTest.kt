package com.wakemyway.app.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeResponseTerminalPolicyTest {
    @Test
    fun completedResponseKeepsSessionAlive() {
        assertFalse(RealtimeResponseTerminalPolicy.shouldFailSession("completed"))
    }

    @Test
    fun incompleteResponseKeepsSessionAlive() {
        assertFalse(RealtimeResponseTerminalPolicy.shouldFailSession("incomplete"))
    }

    @Test
    fun cancelledResponseKeepsSessionAlive() {
        assertFalse(RealtimeResponseTerminalPolicy.shouldFailSession("cancelled"))
    }

    @Test
    fun failedResponseDegradesSession() {
        assertTrue(RealtimeResponseTerminalPolicy.shouldFailSession("failed"))
    }
}
