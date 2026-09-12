package com.wakemyway.app

import com.wakemyway.app.voice.WakeSessionController
import com.wakemyway.app.voice.WakeVoiceMode
import com.wakemyway.app.voice.WakeVoiceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeSessionViewModelTest {
    @Test
    fun `retained owner forwards surface lifecycle without recreating controller`() {
        val fake = FakeWakeSessionController()
        var creations = 0
        val viewModel = WakeSessionViewModel { _, _ ->
            creations += 1
            fake
        }

        viewModel.onSurfaceVisible()
        viewModel.onSurfaceHidden()
        viewModel.onSurfaceVisible()

        assertEquals(1, creations)
        assertEquals(2, fake.visibleCalls)
        assertEquals(1, fake.hiddenCalls)
        assertFalse(viewModel.completed)
    }

    @Test
    fun `controller state and completion are surfaced without replacing the session`() {
        val fake = FakeWakeSessionController()
        lateinit var publishState: (WakeVoiceUiState) -> Unit
        lateinit var complete: () -> Unit
        val viewModel = WakeSessionViewModel { onUiState, onCompleted ->
            publishState = onUiState
            complete = onCompleted
            fake
        }
        val state = WakeVoiceUiState(
            mode = WakeVoiceMode.MOVING,
            activationScore = 3,
            activationThreshold = 4,
            speechAvailable = true,
            voiceInputAvailable = true,
        )

        publishState(state)
        complete()
        viewModel.closeForTerminalAction()

        assertEquals(state, viewModel.voiceState)
        assertTrue(viewModel.completed)
        assertEquals(1, fake.terminalCloseCalls)
    }

    private class FakeWakeSessionController : WakeSessionController {
        var visibleCalls = 0
        var hiddenCalls = 0
        var terminalCloseCalls = 0

        override fun onSurfaceVisible() {
            visibleCalls += 1
        }

        override fun onSurfaceHidden() {
            hiddenCalls += 1
        }

        override fun closeForTerminalAction() {
            terminalCloseCalls += 1
        }

        override fun close() = Unit
    }
}
