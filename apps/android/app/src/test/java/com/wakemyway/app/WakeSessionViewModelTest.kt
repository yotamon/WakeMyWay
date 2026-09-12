package com.wakemyway.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.wakemyway.app.voice.WakeSessionController
import com.wakemyway.app.voice.WakeVoiceMode
import com.wakemyway.app.voice.WakeVoiceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeSessionViewModelTest {
    @Test
    fun `ViewModelStore retains one controller and closes it when the session owner clears`() {
        val fake = FakeWakeSessionController()
        var creations = 0
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                creations += 1
                return WakeSessionViewModel { _, _ -> fake } as T
            }
        }

        val first = ViewModelProvider(store, factory).get("wake-session", WakeSessionViewModel::class.java)
        first.onSurfaceVisible()
        first.onSurfaceHidden()

        // A recreated Activity receives another provider backed by the same retained store.
        val recreated = ViewModelProvider(store, factory).get("wake-session", WakeSessionViewModel::class.java)
        recreated.onSurfaceVisible()

        assertSame(first, recreated)
        assertEquals(1, creations)
        assertEquals(2, fake.visibleCalls)
        assertEquals(1, fake.hiddenCalls)
        assertFalse(recreated.completed)

        store.clear()
        assertEquals(1, fake.closeCalls)
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
        var closeCalls = 0

        override fun onSurfaceVisible() {
            visibleCalls += 1
        }

        override fun onSurfaceHidden() {
            hiddenCalls += 1
        }

        override fun closeForTerminalAction() {
            terminalCloseCalls += 1
        }

        override fun close() {
            closeCalls += 1
        }
    }
}
