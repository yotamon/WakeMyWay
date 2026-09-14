package com.wakemyway.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wakemyway.app.alarm.WakeTerminalActions
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
 * AlarmKernel remains the durable authority for terminal Stop/Snooze. WakeRuntime owns
 * activation/orientation behavior, while this ViewModel guarantees the Wake Surface disappears
 * only after the terminal Alarm Kernel transaction has actually succeeded.
 */
class WakeSessionViewModel internal constructor(
    controllerFactory: WakeSessionControllerFactory,
    private val requestStopExecution: () -> Boolean = { false },
    private val requestSnoozeExecution: () -> Boolean = { false },
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
        // execution teardown back to the Alarm Kernel/playback layer.
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

    private fun requestTerminal(command: () -> Boolean): Boolean {
        if (terminal) return false

        // Do not release the Wake Surface merely because a command was queued. Stop/Snooze is
        // terminal only after the Alarm Kernel has committed the durable transition. In particular,
        // a failed Snooze replacement must leave the current audible/controllable wake intact.
        val committed = runCatching(command).getOrDefault(false)
        if (!committed) return false

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
            val terminalActions = WakeTerminalActions(appContext)
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
                            terminalActions.stop(occurrenceId)
                        },
                        requestSnoozeExecution = {
                            terminalActions.snooze(occurrenceId)
                        },
                    ) as T
                }
            }
        }
    }
}
