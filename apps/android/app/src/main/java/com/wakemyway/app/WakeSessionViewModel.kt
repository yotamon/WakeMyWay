package com.wakemyway.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.CriticalWakePolicy
import com.wakemyway.app.alarm.WakeTerminalActions
import com.wakemyway.app.alarm.WakeTerminalObserver
import com.wakemyway.app.product.history.WakeHistorySessionRecorder
import com.wakemyway.app.voice.AlarmOnlyWakeSessionController
import com.wakemyway.app.voice.WakeRuntimeTransitionObserver
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
 * only after the terminal Alarm Kernel transaction has actually succeeded. The concrete behavioral
 * controller is still selected from the active alarm's device-protected execution policy.
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
        // terminal only after AlarmKernel has committed the durable transition. A failed Snooze
        // replacement therefore leaves the current audible/controllable wake intact.
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
            val kernel = AlarmKernel(appContext)
            val policy = kernel.activePolicy(occurrenceId) ?: CriticalWakePolicy.DEFAULT
            val activeOccurrence = kernel.activeOccurrence()?.takeIf { it.id == occurrenceId }
            val historyRecorder = activeOccurrence?.let { occurrence ->
                WakeHistorySessionRecorder(appContext, occurrence)
            }
            val terminalActions = WakeTerminalActions(
                context = appContext,
                observer = historyRecorder ?: WakeTerminalObserver.NONE,
            )
            val runtimeTransitionObserver = historyRecorder?.let { recorder ->
                WakeRuntimeTransitionObserver { before, input, transition, elapsed ->
                    recorder.observeRuntimeTransition(
                        before = before,
                        input = input,
                        transition = transition,
                        elapsedSinceRuntimeStart = elapsed,
                    )
                }
            } ?: WakeRuntimeTransitionObserver.NONE
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(WakeSessionViewModel::class.java)) {
                        "Unsupported ViewModel class: ${modelClass.name}"
                    }
                    return WakeSessionViewModel(
                        controllerFactory = { onUiState, onCompleted ->
                            if (policy.voiceCheckInEnabled) {
                                WakeVoiceSessionController(
                                    context = appContext,
                                    occurrenceId = occurrenceId,
                                    onUiState = onUiState,
                                    onCompleted = onCompleted,
                                    voiceStyle = policy.voiceStyle,
                                    terminalActions = terminalActions,
                                    runtimeTransitionObserver = runtimeTransitionObserver,
                                )
                            } else {
                                AlarmOnlyWakeSessionController()
                            }
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
