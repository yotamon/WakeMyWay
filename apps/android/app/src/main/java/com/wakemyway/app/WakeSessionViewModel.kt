package com.wakemyway.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wakemyway.app.voice.WakeSessionController
import com.wakemyway.app.voice.WakeVoiceSessionController
import com.wakemyway.app.voice.WakeVoiceUiState
import com.wakemyway.core.schedule.WakeOccurrenceId

typealias WakeSessionControllerFactory = (
    onUiState: (WakeVoiceUiState) -> Unit,
    onCompleted: () -> Unit,
) -> WakeSessionController

/**
 * Retained owner for one behavioral Wake Session.
 *
 * The Alarm Kernel remains the durable execution authority. This ViewModel only keeps the
 * in-memory WakeRuntime/controller alive across Activity configuration recreation so activation
 * evidence, escalation, speech sequencing, and optional Realtime state are not reset by UI churn.
 * Process death intentionally falls back to a fresh behavioral session while the local alarm
 * continues under AlarmPlaybackService.
 */
class WakeSessionViewModel internal constructor(
    controllerFactory: WakeSessionControllerFactory,
) : ViewModel() {
    var voiceState by mutableStateOf(WakeVoiceUiState())
        private set

    var completed by mutableStateOf(false)
        private set

    private var terminal = false
    private val controller = controllerFactory(
        { state -> voiceState = state },
        {
            terminal = true
            completed = true
        },
    )

    fun onSurfaceVisible() {
        if (!terminal) controller.onSurfaceVisible()
    }

    fun onSurfaceHidden() {
        // Activity.onPause() still runs after Stop/Snooze or automatic completion. Once terminal,
        // do not send resource/audio lifecycle commands into a controller that already handed
        // execution teardown back to AlarmPlaybackService.
        if (!terminal) controller.onSurfaceHidden()
    }

    fun closeForTerminalAction() {
        if (terminal) return
        terminal = true
        controller.closeForTerminalAction()
    }

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }

    companion object {
        fun factory(
            context: Context,
            occurrenceId: WakeOccurrenceId,
        ): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(WakeSessionViewModel::class.java)) {
                        "Unsupported ViewModel class: ${modelClass.name}"
                    }
                    return WakeSessionViewModel { onUiState, onCompleted ->
                        WakeVoiceSessionController(
                            context = appContext,
                            occurrenceId = occurrenceId,
                            onUiState = onUiState,
                            onCompleted = onCompleted,
                        )
                    } as T
                }
            }
        }
    }
}
