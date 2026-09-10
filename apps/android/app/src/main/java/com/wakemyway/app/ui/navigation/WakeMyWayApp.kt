package com.wakemyway.app.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.wakemyway.app.R
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.developer.WakeAlarmLabScreen
import com.wakemyway.app.ui.home.TonightScreen
import com.wakemyway.app.ui.home.TonightUiState
import com.wakemyway.app.ui.setup.WakeSetupCommitResult
import com.wakemyway.app.ui.setup.WakeSetupScreen
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
private data object TonightRoute : NavKey

@Serializable
private data object WakeSetupRoute : NavKey

@Serializable
private data object WakeLabRoute : NavKey

@Composable
fun WakeMyWayApp() {
    val context = LocalContext.current
    val alarmKernel = remember { AlarmKernel(context) }
    var alarmHealth by remember { mutableStateOf(alarmKernel.health()) }
    val backStack = rememberNavBackStack(TonightRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<TonightRoute> {
                TonightScreen(
                    state = alarmHealth.toTonightUiState(context),
                    onOpenWakeSetup = {
                        alarmHealth = alarmKernel.reconcile()
                        backStack.add(WakeSetupRoute)
                    },
                    onOpenWakeLab = {
                        alarmHealth = alarmKernel.reconcile()
                        backStack.add(WakeLabRoute)
                    },
                )
            }
            entry<WakeSetupRoute> {
                WakeSetupScreen(
                    existingSchedule = alarmKernel.currentSchedule(),
                    onBack = {
                        alarmHealth = alarmKernel.health()
                        backStack.removeLastOrNull()
                    },
                    onCommit = { schedule ->
                        runCatching { alarmKernel.commitSchedule(schedule) }
                            .fold(
                                onSuccess = { health ->
                                    alarmHealth = health
                                    WakeSetupCommitResult(committed = true)
                                },
                                onFailure = { error ->
                                    WakeSetupCommitResult(
                                        committed = false,
                                        detail = error.message,
                                    )
                                },
                            )
                    },
                    onDisable = {
                        runCatching {
                            alarmKernel.cancelSchedule()
                            alarmKernel.health()
                        }.fold(
                            onSuccess = { health ->
                                alarmHealth = health
                                WakeSetupCommitResult(committed = true)
                            },
                            onFailure = { error ->
                                WakeSetupCommitResult(
                                    committed = false,
                                    detail = error.message,
                                )
                            },
                        )
                    },
                )
            }
            entry<WakeLabRoute> {
                WmwCircadianSurface(stage = WmwCircadianStage.EMERGING) {
                    WakeAlarmLabScreen(
                        onBack = {
                            alarmHealth = alarmKernel.reconcile()
                            backStack.removeLastOrNull()
                        },
                    )
                }
            }
        },
    )
}

private fun AlarmHealth.toTonightUiState(context: Context): TonightUiState {
    val occurrence = nextOccurrence
    val locale = Locale.getDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE · MMM d", locale)
    val readinessCopy = when {
        occurrence == null -> context.getString(R.string.tonight_readiness_empty)
        ready -> context.getString(R.string.tonight_readiness_ready)
        else -> context.getString(R.string.tonight_readiness_attention)
    }

    return TonightUiState(
        wakeTime = occurrence?.scheduledAt?.format(timeFormatter) ?: "--:--",
        dateLabel = occurrence?.scheduledAt?.format(dateFormatter)
            ?: context.getString(R.string.tonight_section_tomorrow),
        hasOccurrence = occurrence != null,
        wakeReady = ready,
        readinessDetail = readinessCopy,
    )
}
