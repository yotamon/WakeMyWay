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
import androidx.compose.ui.res.stringResource
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
import com.wakemyway.app.product.AlarmProductController
import com.wakemyway.app.ui.alarms.AlarmEditorResult
import com.wakemyway.app.ui.alarms.AlarmEditorScreen
import com.wakemyway.app.ui.alarms.AlarmsScreen
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.developer.WakeAlarmLabScreen
import com.wakemyway.app.ui.home.TonightScreen
import com.wakemyway.app.ui.home.TonightUiState
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import com.wakemyway.app.ui.preparation.TomorrowPlanScreen
import com.wakemyway.app.wakeSchedulingBlocker
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.schedule.WakeOccurrence
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
private data object HomeRoute : NavKey

@Serializable
private data object AlarmsRoute : NavKey

@Serializable
private data class AlarmEditorRoute(val alarmId: String? = null) : NavKey

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
    val alarmSetupRequiredCopy = stringResource(R.string.tonight_readiness_attention)
    val voiceSetupRequiredCopy = stringResource(R.string.tonight_voice_wake_setup_detail)
    val voiceUnavailableCopy = stringResource(R.string.tonight_voice_wake_unavailable_detail)
    val alarmKernel = remember { AlarmKernel(context) }
    val alarmController = remember { AlarmProductController(context) }
    val preparationManager = remember { WakePreparationManager(context) }
    val showDeveloperTools = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    var alarmHealth by remember { mutableStateOf(alarmKernel.health()) }
    var alarms by remember { mutableStateOf(alarmController.list()) }
    val backStack = rememberNavBackStack(HomeRoute)

    fun refreshProductState(reconcile: Boolean = true) {
        alarmHealth = if (reconcile) alarmKernel.reconcile() else alarmKernel.health()
        alarms = alarmController.list()
    }

    fun navigateTop(tab: ConsumerTab) {
        backStack.clear()
        backStack.add(
            when (tab) {
                ConsumerTab.HOME -> HomeRoute
                ConsumerTab.ALARMS -> AlarmsRoute
            },
        )
    }

    fun preflight(definition: AlarmDefinition): AlarmEditorResult? {
        if (!definition.enabled) return null
        return when (
            wakeSchedulingBlocker(
                alarmHealth = alarmKernel.health(),
                voiceReadiness = voiceWakeReadiness,
                requiresVoiceReplies = definition.voiceCheckInEnabled,
            )
        ) {
            WakeSchedulingBlocker.ALARM_SYSTEM -> {
                onRepairWakeSystem()
                AlarmEditorResult(saved = false, detail = alarmSetupRequiredCopy)
            }

            WakeSchedulingBlocker.VOICE_PERMISSION -> {
                onRepairWakeSystem()
                AlarmEditorResult(saved = false, detail = voiceSetupRequiredCopy)
            }

            WakeSchedulingBlocker.VOICE_UNAVAILABLE ->
                AlarmEditorResult(saved = false, detail = voiceUnavailableCopy)

            WakeSchedulingBlocker.NONE -> null
        }
    }

    LaunchedEffect(wakeSystemRevision) {
        refreshProductState()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeRoute> {
                val occurrence = alarmHealth.nextOccurrence
                val preparation = occurrence?.let { preparationManager.snapshotFor(it.id) }
                val nextAlarm = occurrence?.let { next ->
                    alarms.firstOrNull { it.id.value == next.wakeScheduleId.value }
                }
                WmwConsumerScaffold(
                    selectedTab = ConsumerTab.HOME,
                    onTabSelected = ::navigateTop,
                ) { contentModifier ->
                    TonightScreen(
                        state = alarmHealth.toTonightUiState(context, preparation),
                        onOpenWakeSetup = {
                            backStack.add(AlarmEditorRoute(nextAlarm?.id?.value))
                        },
                        onOpenTomorrowPlan = {
                            refreshProductState(reconcile = false)
                            backStack.add(TomorrowPlanRoute)
                        },
                        onOpenWakeLab = {
                            refreshProductState()
                            backStack.add(WakeLabRoute)
                        },
                        modifier = contentModifier,
                        showDeveloperTools = showDeveloperTools,
                        voiceWakeReadiness = voiceWakeReadiness,
                        onEnableVoiceReplies = onEnableVoiceReplies,
                        onRepairWakeSystem = onRepairWakeSystem,
                    )
                }
            }

            entry<AlarmsRoute> {
                WmwConsumerScaffold(
                    selectedTab = ConsumerTab.ALARMS,
                    onTabSelected = ::navigateTop,
                ) { contentModifier ->
                    AlarmsScreen(
                        alarms = alarms,
                        healthFor = { alarm -> alarmController.health(alarm.id) },
                        onAddAlarm = { backStack.add(AlarmEditorRoute()) },
                        onEditAlarm = { alarm -> backStack.add(AlarmEditorRoute(alarm.id.value)) },
                        onSetEnabled = { alarm, enabled ->
                            val target = alarm.copy(
                                enabled = enabled,
                                revision = alarm.revision + 1,
                                updatedAt = java.time.Instant.now(),
                            )
                            val blocked = if (enabled) preflight(target) else null
                            if (blocked == null) {
                                val previous = alarmController.health(alarm.id)?.nextOccurrence
                                runCatching { alarmController.setEnabled(alarm.id, enabled) }
                                    .onSuccess {
                                        if (!enabled && previous != null) {
                                            runCatching {
                                                if (preparationManager.snapshotFor(previous.id)?.contract != null) {
                                                    preparationManager.clear()
                                                }
                                            }
                                        }
                                        refreshProductState(reconcile = false)
                                    }
                            }
                        },
                        modifier = contentModifier,
                    )
                }
            }

            entry<AlarmEditorRoute> { route ->
                val existing = route.alarmId?.let { id ->
                    alarmController.get(AlarmDefinitionId(id))
                }
                AlarmEditorScreen(
                    existing = existing,
                    onBack = {
                        refreshProductState(reconcile = false)
                        backStack.removeLastOrNull()
                    },
                    onSave = { definition ->
                        preflight(definition) ?: run {
                            val previousOccurrence = existing
                                ?.let { alarmController.health(it.id)?.nextOccurrence }
                            val previousPreparation = previousOccurrence
                                ?.let { preparationManager.snapshotFor(it.id) }
                            runCatching { alarmController.save(definition) }
                                .fold(
                                    onSuccess = {
                                        val newOccurrence = alarmController.health(definition.id)?.nextOccurrence
                                        reconcilePreparationAfterScheduleChange(
                                            previousOccurrence = previousOccurrence,
                                            newOccurrence = newOccurrence,
                                            previousPreparation = previousPreparation,
                                            preparationManager = preparationManager,
                                        )
                                        refreshProductState(reconcile = false)
                                        AlarmEditorResult(saved = true)
                                    },
                                    onFailure = { error ->
                                        AlarmEditorResult(saved = false, detail = error.message)
                                    },
                                )
                        }
                    },
                    onDelete = existing?.let { alarm ->
                        { _: AlarmDefinition ->
                            val previousOccurrence = alarmController.health(alarm.id)?.nextOccurrence
                            runCatching { alarmController.delete(alarm.id) }
                                .fold(
                                    onSuccess = {
                                        if (previousOccurrence != null) {
                                            runCatching {
                                                if (preparationManager.snapshotFor(previousOccurrence.id)?.contract != null) {
                                                    preparationManager.clear()
                                                }
                                            }
                                        }
                                        refreshProductState(reconcile = false)
                                        AlarmEditorResult(saved = true)
                                    },
                                    onFailure = { error ->
                                        AlarmEditorResult(saved = false, detail = error.message)
                                    },
                                )
                        }
                    },
                )
            }

            entry<TomorrowPlanRoute> {
                TomorrowPlanScreen(
                    wakeOccurrence = alarmKernel.health().nextOccurrence,
                    onBack = {
                        refreshProductState(reconcile = false)
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<WakeLabRoute> {
                WmwCircadianSurface(stage = WmwCircadianStage.EMERGING) {
                    WakeAlarmLabScreen(
                        onBack = {
                            refreshProductState()
                            backStack.removeLastOrNull()
                        },
                        voiceWakeReadiness = voiceWakeReadiness,
                        readinessRevision = wakeSystemRevision,
                        onRepairWakeSystem = onRepairWakeSystem,
                        onEnableVoiceReplies = onEnableVoiceReplies,
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

    if (!rebound) runCatching { preparationManager.clear() }
}

private fun AlarmHealth.toTonightUiState(
    context: Context,
    preparation: WakePreparationSnapshot?,
): TonightUiState {
    val occurrence = nextOccurrence
    val locale = Locale.getDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", locale)
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
        tomorrowContractText = preparation?.contract?.rawText,
        firstMove = preparation?.contract?.firstMove,
    )
}