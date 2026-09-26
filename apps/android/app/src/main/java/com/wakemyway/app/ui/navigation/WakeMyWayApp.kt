package com.wakemyway.app.ui.navigation

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.wakemyway.app.alarm.futureSchedulingRepairTarget
import com.wakemyway.app.alarm.repairTarget
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakePreparationSnapshot
import com.wakemyway.app.preparation.WakePreparationStatus
import com.wakemyway.app.product.AlarmProductController
import com.wakemyway.app.product.ConsumerPreferences
import com.wakemyway.app.product.ConsumerPreferencesRepository
import com.wakemyway.app.product.account.WakeAccountManager
import com.wakemyway.app.product.followup.WakeSafetyCheckScheduler
import com.wakemyway.app.product.history.WakeHistoryRepository
import com.wakemyway.app.product.insights.WakeInsightsPeriod
import com.wakemyway.app.product.insights.WakeInsightsProjector
import com.wakemyway.app.product.learning.WakeLearningRepository
import com.wakemyway.app.ui.alarms.AlarmEditorDefaults
import com.wakemyway.app.ui.alarms.AlarmEditorResult
import com.wakemyway.app.ui.alarms.AlarmEditorScreen
import com.wakemyway.app.ui.alarms.AlarmsScreen
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.developer.WakeAlarmLabScreen
import com.wakemyway.app.ui.home.TonightScreen
import com.wakemyway.app.ui.home.TonightUiState
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import com.wakemyway.app.ui.insights.InsightsScreen
import com.wakemyway.app.ui.onboarding.OnboardingScreen
import com.wakemyway.app.ui.preparation.TomorrowPlanScreen
import com.wakemyway.app.ui.profile.AboutScreen
import com.wakemyway.app.ui.profile.AccountScreen
import com.wakemyway.app.ui.profile.AppearanceScreen
import com.wakemyway.app.ui.profile.PrivacyScreen
import com.wakemyway.app.ui.profile.ProfileScreen
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.update.UpdateState
import com.wakemyway.app.wakeSchedulingBlocker
import com.wakemyway.app.widget.WakeWidgetDestination
import com.wakemyway.app.widget.WakeWidgetLaunchRequest
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.schedule.WakeOccurrence
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
private data object OnboardingRoute : NavKey

@Serializable
private data object HomeRoute : NavKey

@Serializable
private data object AlarmsRoute : NavKey

@Serializable
private data object InsightsRoute : NavKey

@Serializable
private data object ProfileRoute : NavKey

@Serializable
private data object AccountRoute : NavKey

@Serializable
private data object PrivacyRoute : NavKey

@Serializable
private data object AppearanceRoute : NavKey

@Serializable
private data object AboutRoute : NavKey

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
    onOpenNotificationSettings: () -> Unit = {},
    updateState: UpdateState = UpdateState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onBeginUpdate: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onOpenInstallPermission: () -> Unit = {},
    launchRequest: WakeWidgetLaunchRequest? = null,
    onLaunchRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val alarmSetupRequiredCopy = stringResource(R.string.tonight_readiness_attention)
    val voiceSetupRequiredCopy = stringResource(R.string.tonight_voice_wake_setup_detail)
    val voiceUnavailableCopy = stringResource(R.string.tonight_voice_wake_unavailable_detail)
    val alarmKernel = remember { AlarmKernel(context) }
    val alarmController = remember { AlarmProductController(context) }
    val preparationManager = remember { WakePreparationManager(context) }
    val preferencesRepository = remember { ConsumerPreferencesRepository(context) }
    val historyRepository = remember { WakeHistoryRepository(context) }
    val learningRepository = remember { WakeLearningRepository(context, historyRepository) }
    val accountManager = remember(context) { WakeAccountManager.get(context) }
    val accountState by accountManager.state.collectAsState()
    val accountState by accountManager.state.collectAsState()
    val initialPreferences = remember { preferencesRepository.get() }
    val appVersionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1.0"
        }.getOrDefault("0.1.0")
    }
    val showDeveloperTools = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    var alarmHealth by remember { mutableStateOf(alarmKernel.health()) }
    var alarms by remember { mutableStateOf(alarmController.list()) }
    var preferences by remember { mutableStateOf(initialPreferences) }
    var wakeHistory by remember { mutableStateOf(historyRepository.list()) }
    var wakeLearning by remember { mutableStateOf(learningRepository.state()) }
    val backStack = rememberNavBackStack(
        if (initialPreferences.onboardingCompleted) HomeRoute else OnboardingRoute,
    )

    fun refreshProductState(reconcile: Boolean = true) {
        val currentHealth = alarmKernel.health()
        alarmHealth = if (
            reconcile &&
            (
                currentHealth.nextOccurrence == null ||
                    currentHealth.futureSchedulingRepairTarget() == AlarmRepairTarget.NONE
                )
        ) {
            alarmController.reconcile()
        } else {
            currentHealth
        }
        alarms = alarmController.list()
    }

    fun refreshWakeHistory() {
        wakeHistory = historyRepository.list()
        wakeLearning = learningRepository.state()
    }

    fun calibrateMorning(
        occurrenceId: com.wakemyway.core.schedule.WakeOccurrenceId,
        outcome: com.wakemyway.core.learning.WakeCalibrationOutcome,
    ) {
        runCatching {
            wakeLearning = learningRepository.submitCalibration(occurrenceId, outcome)
            WakeSafetyCheckScheduler.resolve(context, occurrenceId)
            wakeHistory = historyRepository.list()
        }
    }

    fun savePreferences(next: ConsumerPreferences) {
        preferences = preferencesRepository.replace(next)
    }

    fun completeOnboarding() {
        preferences = preferencesRepository.update { it.copy(onboardingCompleted = true) }
        backStack.clear()
        backStack.add(HomeRoute)
    }

    fun navigateTop(tab: ConsumerTab) {
        if (tab == ConsumerTab.INSIGHTS) refreshWakeHistory()
        backStack.clear()
        backStack.add(
            when (tab) {
                ConsumerTab.HOME -> HomeRoute
                ConsumerTab.ALARMS -> AlarmsRoute
                ConsumerTab.INSIGHTS -> InsightsRoute
                ConsumerTab.PROFILE -> ProfileRoute
            },
        )
    }

    LaunchedEffect(launchRequest, preferences.onboardingCompleted) {
        val request = launchRequest ?: return@LaunchedEffect
        if (!preferences.onboardingCompleted) return@LaunchedEffect

        if (request.destination == WakeWidgetDestination.INSIGHTS) {
            refreshWakeHistory()
        }
        backStack.clear()
        backStack.add(
            when (request.destination) {
                WakeWidgetDestination.HOME -> HomeRoute
                WakeWidgetDestination.ALARMS -> AlarmsRoute
                WakeWidgetDestination.ALARM_EDITOR -> AlarmEditorRoute(request.alarmId)
                WakeWidgetDestination.TOMORROW_PLAN -> TomorrowPlanRoute
                WakeWidgetDestination.INSIGHTS -> InsightsRoute
            },
        )
        onLaunchRequestConsumed()
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
        refreshWakeHistory()
    }

    WakeMyWayTheme(appearance = preferences.appearance) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<OnboardingRoute> {
                    OnboardingScreen(
                        onComplete = ::completeOnboarding,
                        onSkip = ::completeOnboarding,
                    )
                }

                entry<HomeRoute> {
                    val occurrence = alarmHealth.nextOccurrence
                    val preparation = occurrence?.let { preparationManager.snapshotFor(it.id) }
                    val nextAlarm = occurrence?.let { next ->
                        alarms.firstOrNull { it.id.value == next.wakeScheduleId.value }
                    }
                    val nextAlarmReady = nextAlarm
                        ?.let { alarmController.health(it.id)?.ready }
                        ?: alarmHealth.ready
                    val hasMorningCheckIn = remember(wakeHistory) {
                        WakeInsightsProjector.project(
                            entries = wakeHistory,
                            period = WakeInsightsPeriod.LAST_7_DAYS,
                            now = Instant.now(),
                        ).pendingCalibration != null
                    }
                    WmwConsumerScaffold(
                        selectedTab = ConsumerTab.HOME,
                        onTabSelected = ::navigateTop,
                    ) { contentModifier ->
                        TonightScreen(
                            state = alarmHealth.toTonightUiState(
                                context = context,
                                preparation = preparation,
                                alarm = nextAlarm,
                                nextAlarmReady = nextAlarmReady,
                            ),
                            onOpenWakeSetup = {
                                backStack.add(AlarmEditorRoute(nextAlarm?.id?.value))
                            },
                            onOpenTomorrowPlan = {
                                if (nextAlarm?.tomorrowContractMode != TomorrowContractMode.DISABLED) {
                                    refreshProductState(reconcile = false)
                                    backStack.add(TomorrowPlanRoute)
                                }
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
                            hasMorningCheckIn = hasMorningCheckIn,
                            onOpenMorningCheckIn = { navigateTop(ConsumerTab.INSIGHTS) },
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

                entry<InsightsRoute> {
                    var period by remember { mutableStateOf(WakeInsightsPeriod.LAST_7_DAYS) }
                    val summary = remember(wakeHistory, period) {
                        WakeInsightsProjector.project(
                            entries = wakeHistory,
                            period = period,
                            now = Instant.now(),
                        )
                    }
                    WmwConsumerScaffold(
                        selectedTab = ConsumerTab.INSIGHTS,
                        onTabSelected = ::navigateTop,
                    ) { contentModifier ->
                        InsightsScreen(
                            summary = summary,
                            learningState = wakeLearning,
                            onPeriodSelected = { period = it },
                            onCalibrateMorning = ::calibrateMorning,
                            modifier = contentModifier,
                        )
                    }
                }

                entry<ProfileRoute> {
                    WmwConsumerScaffold(
                        selectedTab = ConsumerTab.PROFILE,
                        onTabSelected = ::navigateTop,
                    ) { contentModifier ->
                        ProfileScreen(
                            preferences = preferences,
                            onPreferencesChanged = ::savePreferences,
                            showAccount = accountState.configured,
                            onOpenAccount = { backStack.add(AccountRoute) },
                            onOpenNotifications = onOpenNotificationSettings,
                            onOpenPrivacy = { backStack.add(PrivacyRoute) },
                            onOpenAppearance = { backStack.add(AppearanceRoute) },
                            onOpenAbout = { backStack.add(AboutRoute) },
                            modifier = contentModifier,
                        )
                    }
                }

                entry<AccountRoute> {
                    AccountScreen(
                        manager = accountManager,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<PrivacyRoute> {
                    PrivacyScreen(onBack = { backStack.removeLastOrNull() })
                }

                entry<AppearanceRoute> {
                    AppearanceScreen(
                        appearance = preferences.appearance,
                        onAppearanceChanged = { appearance ->
                            savePreferences(preferences.copy(appearance = appearance))
                        },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<AboutRoute> {
                    AboutScreen(
                        versionName = appVersionName,
                        updateState = updateState,
                        onCheckForUpdates = onCheckForUpdates,
                        onBeginUpdate = onBeginUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onOpenInstallPermission = onOpenInstallPermission,
                        onBack = { backStack.removeLastOrNull() },
                    )
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
                                            if (definition.tomorrowContractMode == TomorrowContractMode.DISABLED) {
                                                runCatching {
                                                    if (newOccurrence != null &&
                                                        preparationManager.snapshotFor(newOccurrence.id)?.contract != null
                                                    ) {
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
                        defaults = AlarmEditorDefaults(
                            soundId = preferences.defaultSoundId,
                            voiceCheckInEnabled = preferences.defaultVoiceCheckInEnabled,
                            voiceStyle = preferences.defaultVoiceStyle,
                            snoozeMinutes = preferences.defaultSnoozeMinutes,
                            firstMove = preferences.defaultFirstMove,
                        ),
                    )
                }

                entry<TomorrowPlanRoute> {
                    val occurrence = alarmKernel.health().nextOccurrence
                    val owner = occurrence?.let { next ->
                        alarms.firstOrNull { it.id.value == next.wakeScheduleId.value }
                    }
                    TomorrowPlanScreen(
                        wakeOccurrence = occurrence,
                        defaultFirstMove = owner?.firstMoveDefault,
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
    alarm: AlarmDefinition?,
    nextAlarmReady: Boolean,
): TonightUiState {
    val occurrence = nextOccurrence
    val locale = Locale.getDefault()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", locale)
    val target = if (occurrence != null && !nextAlarmReady) repairTarget() else AlarmRepairTarget.NONE
    val readinessCopy = when {
        occurrence == null -> context.getString(R.string.tonight_readiness_empty)
        nextAlarmReady -> context.getString(R.string.tonight_readiness_ready)
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
    val contractMode = alarm?.tomorrowContractMode ?: TomorrowContractMode.DISABLED

    return TonightUiState(
        wakeTime = occurrence?.scheduledAt?.format(timeFormatter) ?: "--:--",
        dateLabel = occurrence?.scheduledAt?.format(dateFormatter)
            ?: context.getString(R.string.tonight_section_tomorrow),
        hasOccurrence = occurrence != null,
        wakeReady = nextAlarmReady,
        readinessDetail = readinessCopy,
        wakeRepairActionLabel = repairLabel,
        hasTomorrowContract = preparation?.contract != null,
        tomorrowContractPrepared = preparation?.status == WakePreparationStatus.READY,
        tomorrowContractText = preparation?.contract?.rawText,
        firstMove = preparation?.contract?.firstMove ?: alarm?.firstMoveDefault,
        voiceCheckInEnabled = occurrence != null && alarm?.voiceCheckInEnabled == true,
        tomorrowContractAvailable = occurrence != null && contractMode != TomorrowContractMode.DISABLED,
        tomorrowContractPromptRequired = contractMode == TomorrowContractMode.ALWAYS_PROMPT,
    )
}
