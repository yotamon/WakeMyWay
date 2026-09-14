package com.wakemyway.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wakemyway.app.alarm.AlarmPlaybackService
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
 * The Alarm Kernel / AlarmPlaybackService remains the durable authority for terminal Stop/Snooze.
 * WakeRuntime owns activation/orientation behavior, while this ViewModel ensures the UI has exactly
 * one terminal command path and that Activity recreation cannot duplicate it.
 */
class WakeSessionViewModel internal constructor(
    controllerFactory: WakeSessionControllerFactory,
    private val requestStopExecution: () -> Unit = {},
    private val requestSnoozeExecution: () -> Unit = {},
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

    fun requestStop(): Boolean = requestTerminal(requestStopExecution)

    fun requestSnooze(): Boolean = requestTerminal(requestSnoozeExecution)

    /** Kept for lifecycle tests and non-UI terminal hand-offs. Prefer requestStop/requestSnooze. */
    fun closeForTerminalAction() {
        if (terminal) return
        terminal = true
        controller.closeForTerminalAction()
    }

    private fun requestTerminal(command: () -> Unit): Boolean {
        if (terminal) return false

        // Queue the kernel-authoritative command before releasing behavioral resources. If Android
        // rejects service dispatch synchronously, the Wake Session remains alive and controllable.
        val dispatched = runCatching(command).isSuccess
        if (!dispatched) return false

        terminal = true
        controller.closeForTerminalAction()
        completed = true
        return true
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
                    return WakeSessionViewModel(
                        controllerFactory = { onUiState, onCompleted ->
                            WakeVoiceSessionController(
                                context = appContext,
                                occurrenceId = occurrenceId,
                                onUiState = onUiState,
                                onCompleted = onCompleted,
                            )
                        },
                        requestStopExecution = {
                            AlarmPlaybackService.requestStop(appContext, occurrenceId)
                        },
                        requestSnoozeExecution = {
                            AlarmPlaybackService.requestSnooze(appContext, occurrenceId)
                        },
                    ) as T
                }
            }
        }
    }
}
