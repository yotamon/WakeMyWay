package com.wakemyway.app.ui.navigation

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.wakemyway.app.WakeSchedulingBlocker
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.AlarmRepairTarget
import com.wakemyway.app.alarm.repairTarget
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakePreparationSnapshot
import com.wakemyway.app.preparation.WakePreparationStatus
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.developer.WakeAlarmLabScreen
import com.wakemyway.app.ui.home.TonightScreen
import com.wakemyway.app.ui.home.TonightUiState
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import com.wakemyway.app.ui.preparation.TomorrowPlanScreen
import com.wakemyway.app.ui.setup.WakeSetupCommitResult
import com.wakemyway.app.ui.setup.WakeSetupScreen
import com.wakemyway.app.wakeSchedulingBlocker
import com.wakemyway.core.schedule.WakeOccurrence
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
private data object TonightRoute : NavKey

@Serializable
private data object WakeSetupRoute : NavKey

@Serializable
private data object TomorrowPlanRoute : NavKey

@Serializable
private data object WakeLabRoute : NavKey

@Composable
fun WakeMyWayApp(
    voiceWakeReadiness: VoiceWakeReadiness? = null,
    onEnableVoiceReplies: () -> Unit = {},
    wakeSystemRevision: Int = 0,
    onRepairWakeSystem: () -> Unit = {},
) {
    val context = LocalContext.current
    val alarmKernel = remember { AlarmKernel(context) }
    val preparationManager = remember { WakePreparationManager(context) }
    val showDeveloperTools = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    var alarmHealth by remember { mutableStateOf(alarmKernel.health()) }
    val backStack = rememberNavBackStack(TonightRoute)

    // Returning from Android permission/special-access settings must immediately refresh the
    // product truth. MainActivity bumps this revision on resume and after runtime permission results.
    LaunchedEffect(wakeSystemRevision) {
        alarmHealth = alarmKernel.reconcile()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<TonightRoute> {
                val preparation = alarmHealth.nextOccurrence
                    ?.let { preparationManager.snapshotFor(it.id) }
                TonightScreen(
                    state = alarmHealth.toTonightUiState(context, preparation),
                    onOpenWakeSetup = {
                        alarmHealth = alarmKernel.reconcile()
                        backStack.add(WakeSetupRoute)
                    },
                    onOpenTomorrowPlan = {
                        alarmHealth = alarmKernel.health()
                        backStack.add(TomorrowPlanRoute)
                    },
                    onOpenWakeLab = {
                        alarmHealth = alarmKernel.reconcile()
                        backStack.add(WakeLabRoute)
                    },
                    showDeveloperTools = showDeveloperTools,
                    voiceWakeReadiness = voiceWakeReadiness,
                    onEnableVoiceReplies = onEnableVoiceReplies,
                    onRepairWakeSystem = onRepairWakeSystem,
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
                        val preflightHealth = alarmKernel.health()
                        when (wakeSchedulingBlocker(preflightHealth, voiceWakeReadiness)) {
                            WakeSchedulingBlocker.ALARM_SYSTEM -> {
                                // Nothing is persisted before the user explicitly repairs the next
                                // required Android capability.
                                onRepairWakeSystem()
                                WakeSetupCommitResult(
                                    committed = false,
                                    detail = context.getString(R.string.tonight_readiness_attention),
                                    wakeReady = false,
                                )
                            }

                            WakeSchedulingBlocker.VOICE_PERMISSION -> {
                                onRepairWakeSystem()
                                WakeSetupCommitResult(
                                    committed = false,
                                    detail = context.getString(R.string.tonight_voice_wake_setup_detail),
                                    wakeReady = false,
                                )
                            }

                            WakeSchedulingBlocker.VOICE_UNAVAILABLE -> {
                                WakeSetupCommitResult(
                                    committed = false,
                                    detail = context.getString(R.string.tonight_voice_wake_unavailable_detail),
                                    wakeReady = false,
                                )
                            }

                            WakeSchedulingBlocker.NONE -> {
                                val previousOccurrence = preflightHealth.nextOccurrence
                                val previousPreparation = previousOccurrence
                                    ?.let { preparationManager.snapshotFor(it.id) }

                                runCatching { alarmKernel.commitSchedule(schedule) }
                                    .fold(
                                        onSuccess = { health ->
                                            reconcilePreparationAfterScheduleChange(
                                                previousOccurrence = previousOccurrence,
                                                newOccurrence = health.nextOccurrence,
                                                previousPreparation = previousPreparation,
                                                preparationManager = preparationManager,
                                            )
                                            alarmHealth = health
                                            WakeSetupCommitResult(
                                                committed = true,
                                                wakeReady = health.ready,
                                            )
                                        },
                                        onFailure = { error ->
                                            WakeSetupCommitResult(
                                                committed = false,
                                                detail = error.message,
                                            )
                                        },
                                    )
                            }
                        }
                    },
                    onDisable = {
                        runCatching {
                            alarmKernel.cancelSchedule()
                            runCatching { preparationManager.clear() }
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
                    onWakeAccessRequired = onRepairWakeSystem,
                )
            }
            entry<TomorrowPlanRoute> {
                TomorrowPlanScreen(
                    wakeOccurrence = alarmKernel.health().nextOccurrence,
                    onBack = {
                        alarmHealth = alarmKernel.health()
                        backStack.removeLastOrNull()
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

private fun reconcilePreparationAfterScheduleChange(
    previousOccurrence: WakeOccurrence?,
    newOccurrence: WakeOccurrence?,
    previousPreparation: WakePreparationSnapshot?,
    preparationManager: WakePreparationManager,
) {
    val contract = previousPreparation?.contract ?: return
    if (newOccurrence == null) {
        runCatching { preparationManager.clear() }
        return
    }

    val sameWakeDate = previousOccurrence?.scheduledAt?.toLocalDate() ==
        newOccurrence.scheduledAt.toLocalDate()
    if (!sameWakeDate) {
        runCatching { preparationManager.clear() }
        return
    }

    val rebound = runCatching {
        preparationManager.saveAndPrepare(
            wakeOccurrenceId = newOccurrence.id,
            rawText = contract.rawText,
            firstMove = contract.firstMove,
        )
    }.isSuccess

    if (!rebound) {
        runCatching { preparationManager.clear() }
    }
}

private fun AlarmHealth.toTonightUiState(
    context: Context,
    preparation: WakePreparationSnapshot?,
): TonightUiState {
    val occurrence = nextOccurrence
    val locale = Locale.getDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE · MMM d", locale)
    val target = if (occurrence != null && !ready) repairTarget() else AlarmRepairTarget.NONE
    val readinessCopy = when {
        occurrence == null -> context.getString(R.string.tonight_readiness_empty)
        ready -> context.getString(R.string.tonight_readiness_ready)
        target == AlarmRepairTarget.EXACT_ALARM -> context.getString(R.string.tonight_readiness_exact_alarm)
        target == AlarmRepairTarget.NOTIFICATIONS -> context.getString(R.string.tonight_readiness_notifications)
        target == AlarmRepairTarget.ACTIVE_WAKE_CHANNEL -> context.getString(R.string.tonight_readiness_channel)
        target == AlarmRepairTarget.FULL_SCREEN_INTENT -> context.getString(R.string.tonight_readiness_full_screen)
        else -> context.getString(R.string.tonight_readiness_reconcile)
    }
    val repairLabel = when (target) {
        AlarmRepairTarget.EXACT_ALARM -> context.getString(R.string.tonight_repair_alarm_access)
        AlarmRepairTarget.NOTIFICATIONS -> context.getString(R.string.tonight_repair_notifications)
        AlarmRepairTarget.ACTIVE_WAKE_CHANNEL -> context.getString(R.string.tonight_repair_channel)
        AlarmRepairTarget.FULL_SCREEN_INTENT -> context.getString(R.string.tonight_repair_full_screen)
        AlarmRepairTarget.NONE -> null
    }

    return TonightUiState(
        wakeTime = occurrence?.scheduledAt?.format(timeFormatter) ?: "--:--",
        dateLabel = occurrence?.scheduledAt?.format(dateFormatter)
            ?: context.getString(R.string.tonight_section_tomorrow),
        hasOccurrence = occurrence != null,
        wakeReady = ready,
        readinessDetail = readinessCopy,
        wakeRepairActionLabel = repairLabel,
        hasTomorrowContract = preparation?.contract != null,
        tomorrowContractPrepared = preparation?.status == WakePreparationStatus.READY,
    )
}
