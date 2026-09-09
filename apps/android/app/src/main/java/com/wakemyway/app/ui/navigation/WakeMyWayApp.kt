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
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
private data object TonightRoute : NavKey

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
                    onOpenWakeLab = {
                        alarmHealth = alarmKernel.reconcile()
                        backStack.add(WakeLabRoute)
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

    return TonightUiState(
        wakeTime = occurrence?.scheduledAt?.format(timeFormatter) ?: "--:--",
        dateLabel = occurrence?.scheduledAt?.format(dateFormatter)
            ?: context.getString(R.string.tonight_section_tomorrow),
        hasOccurrence = occurrence != null,
        wakeReady = ready,
        readinessDetail = detail,
    )
}
