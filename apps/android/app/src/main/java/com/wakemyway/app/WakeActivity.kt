package com.wakemyway.app

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.alarm.CriticalWakePolicy
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakeTimePreparedContent
import com.wakemyway.app.product.AlarmDefinitionRepository
import com.wakemyway.app.ui.components.WmwActionTone
import com.wakemyway.app.ui.components.WmwBrandLockup
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwTimeDisplay
import com.wakemyway.app.ui.components.WmwWakeLine
import com.wakemyway.app.ui.components.WmwWakeLineState
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSizes
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.app.voice.WakeVoiceMode
import com.wakemyway.app.voice.WakeVoiceUiState
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.preparation.PreparedWakePlan
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WakeActivity : ComponentActivity() {
    private var occurrenceId: WakeOccurrenceId? = null
    private var preparedPlan by mutableStateOf<PreparedWakePlan?>(null)
    private var defaultFirstMove by mutableStateOf<String?>(null)
    private var sessionViewModel: WakeSessionViewModel? = null
    private var wakePolicy: CriticalWakePolicy = CriticalWakePolicy.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val rawId = intent.getStringExtra(AlarmPlaybackService.EXTRA_OCCURRENCE_ID)
        val wakeOccurrenceId = rawId
            ?.let { value -> runCatching { WakeOccurrenceId(value) }.getOrNull() }
            ?: run {
                finish()
                return
            }
        occurrenceId = wakeOccurrenceId
        if (!isOccurrenceStillActive(wakeOccurrenceId)) {
            finishAndRemoveTask()
            return
        }
        wakePolicy = AlarmKernel(applicationContext).activePolicy(wakeOccurrenceId)
            ?: CriticalWakePolicy.DEFAULT
        refreshPrivateWakeContextIfUnlocked()

        val viewModel = ViewModelProvider(
            this,
            WakeSessionViewModel.factory(applicationContext, wakeOccurrenceId),
        ).get(
            "wake-session:${wakeOccurrenceId.value}",
            WakeSessionViewModel::class.java,
        )
        sessionViewModel = viewModel

        setContent {
            val voiceState = viewModel.voiceState
            val completed = viewModel.completed

            LaunchedEffect(completed) {
                if (completed) finishAndRemoveTask()
            }

            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = preparedPlan,
                    defaultFirstMove = defaultFirstMove,
                    onSnooze = if (wakePolicy.snoozeEnabled) {
                        { viewModel.requestSnooze() }
                    } else {
                        null
                    },
                    onStop = { viewModel.requestStop() },
                    onFirstMoveConfirmed = { viewModel.confirmFirstMove() },
                    voiceCheckInEnabled = wakePolicy.voiceCheckInEnabled,
                    snoozeMinutes = wakePolicy.snoozeDuration.toMinutes().coerceAtLeast(1),
                    voiceState = voiceState,
                )
            }
        }

        window.decorView.post {
            if (isOccurrenceStillActive(wakeOccurrenceId)) {
                WakeTimingTrace(this).uiVisible(wakeOccurrenceId)
            } else {
                finishAndRemoveTask()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop redelivery: when a newer occurrence's wake-UI intent lands on a surviving
        // stale surface, silently ignoring it would leave the user looking at (and "stopping")
        // the wrong wake. Finish and relaunch so a fresh surface binds the new occurrence.
        val incomingId = intent.getStringExtra(AlarmPlaybackService.EXTRA_OCCURRENCE_ID)
            ?.let { value -> runCatching { WakeOccurrenceId(value) }.getOrNull() }
        if (incomingId != null && incomingId != occurrenceId) {
            finishAndRemoveTask()
            startActivity(
                Intent(intent).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val id = occurrenceId
        if (id == null || !isOccurrenceStillActive(id)) {
            finishAndRemoveTask()
            return
        }
        refreshPrivateWakeContextIfUnlocked()
        sessionViewModel?.onSurfaceVisible()
    }

    override fun onPause() {
        sessionViewModel?.onSurfaceHidden()
        preparedPlan = null
        defaultFirstMove = null
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && preparedPlan == null && defaultFirstMove == null) {
            refreshPrivateWakeContextIfUnlocked()
        }
    }

    private fun isOccurrenceStillActive(id: WakeOccurrenceId): Boolean =
        AlarmKernel(applicationContext).activeOccurrence()?.id == id

    private fun refreshPrivateWakeContextIfUnlocked() {
        val id = occurrenceId ?: return
        val userManager = getSystemService(UserManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!userManager.isUserUnlocked || keyguard.isDeviceLocked) {
            preparedPlan = null
            defaultFirstMove = null
            return
        }

        val kernel = AlarmKernel(applicationContext)
        val activeScheduleId = kernel.activeOccurrence()
            ?.takeIf { active -> active.id == id }
            ?.wakeScheduleId

        defaultFirstMove = activeScheduleId?.let { scheduleId ->
            runCatching {
                AlarmDefinitionRepository(this)
                    .get(AlarmDefinitionId(scheduleId.value))
                    ?.firstMoveDefault
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }

        preparedPlan = runCatching {
            when (val content = WakePreparationManager(this).loadForWake(id)) {
                is WakeTimePreparedContent.Prepared -> content.plan
                is WakeTimePreparedContent.GenericFallback -> null
            }
        }.getOrNull()

        if (preparedPlan != null || defaultFirstMove != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
internal fun WakeSurface(
    preparedPlan: PreparedWakePlan?,
    onSnooze: (() -> Unit)?,
    onStop: () -> Unit,
    onFirstMoveConfirmed: () -> Unit = {},
    modifier: Modifier = Modifier,
    displayTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    displayDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMM")),
    voiceCheckInEnabled: Boolean = true,
    snoozeMinutes: Long = 5,
    voiceState: WakeVoiceUiState? = null,
    defaultFirstMove: String? = null,
) {
    if (!voiceCheckInEnabled) {
        AlarmOnlyWakeSurface(
            preparedPlan = preparedPlan,
            displayTime = displayTime,
            onSnooze = onSnooze,
            onStop = onStop,
            snoozeMinutes = snoozeMinutes,
            modifier = modifier,
        )
        return
    }

    when (voiceState?.mode) {
        null,
        WakeVoiceMode.STARTING,
        WakeVoiceMode.SPEAKING,
        WakeVoiceMode.DEGRADED,
        -> EmergingWakeSurface(
            preparedPlan = preparedPlan,
            spokenLine = voiceState?.spokenLine,
            displayTime = displayTime,
            onSnooze = onSnooze,
            onStop = onStop,
            snoozeMinutes = snoozeMinutes,
            modifier = modifier,
        )

        WakeVoiceMode.LISTENING -> EngagedWakeSurface(
            spokenLine = voiceState.spokenLine,
            displayTime = displayTime,
            onSnooze = onSnooze,
            onStop = onStop,
            snoozeMinutes = snoozeMinutes,
            modifier = modifier,
        )

        WakeVoiceMode.MOVING -> ActiveWakeSurface(
            spokenLine = voiceState.spokenLine,
            displayTime = displayTime,
            onSnooze = onSnooze,
            onStop = onStop,
            snoozeMinutes = snoozeMinutes,
            modifier = modifier,
        )

        WakeVoiceMode.ORIENTING -> OrientedWakeSurface(
            preparedPlan = preparedPlan,
            defaultFirstMove = defaultFirstMove,
            spokenLine = voiceState.spokenLine,
            displayDate = displayDate,
            onSnooze = onSnooze,
            onStop = onStop,
            onFirstMoveConfirmed = onFirstMoveConfirmed,
            snoozeMinutes = snoozeMinutes,
            modifier = modifier,
        )

        WakeVoiceMode.COMPLETE -> CompleteWakeSurface(
            preparedPlan = preparedPlan,
            defaultFirstMove = defaultFirstMove,
            displayTime = displayTime,
            onStop = onStop,
            modifier = modifier,
        )
    }
}

@Composable
private fun WakeFrame(
    stage: WmwCircadianStage,
    modifier: Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val onLight = stage == WmwCircadianStage.ORIENTED || stage == WmwCircadianStage.COMPLETE
    WmwCircadianSurface(stage = stage, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = WmwSpacing.Lg, vertical = WmwSpacing.Md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WmwBrandLockup(onDark = !onLight, compact = true)
            content()
        }
    }
}

@Composable
private fun adaptiveWakeSpace(referenceDp: Int): Dp {
    val heightDp = LocalConfiguration.current.screenHeightDp
    val scale = (heightDp / 852f).coerceIn(0.72f, 1.06f)
    return (referenceDp * scale).dp
}

@Composable
private fun AlarmOnlyWakeSurface(
    preparedPlan: PreparedWakePlan?,
    displayTime: String,
    onSnooze: (() -> Unit)?,
    onStop: () -> Unit,
    snoozeMinutes: Long,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.EMERGING, modifier) {
        Spacer(Modifier.height(adaptiveWakeSpace(96)))
        Text(
            text = stringResource(R.string.wake_alarm_only_title),
            style = MaterialTheme.typography.headlineMedium,
            color = WmwColors.WarmLight,
            textAlign = TextAlign.Center,
        )
        WmwTimeDisplay(
            time = displayTime,
            modifier = Modifier.padding(top = WmwSpacing.Sm),
            compact = true,
            color = WmwColors.WarmLight,
        )
        WmwWakeLine(
            state = WmwWakeLineState.QUIET,
            modifier = Modifier.padding(top = adaptiveWakeSpace(54)),
            height = WmwSizes.WakeWaveHeight,
            sunrise = true,
        )
        Text(
            text = if (preparedPlan != null) {
                stringResource(R.string.wake_private_context_ready)
            } else {
                stringResource(R.string.wake_alarm_only_detail)
            },
            modifier = Modifier.padding(top = WmwSpacing.Md),
            style = MaterialTheme.typography.bodyMedium,
            color = WmwColors.QuietText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        WakeSafetyFooter(onSnooze, onStop, snoozeMinutes, onLightSurface = false)
    }
}

@Composable
private fun EmergingWakeSurface(
    preparedPlan: PreparedWakePlan?,
    spokenLine: String?,
    displayTime: String,
    onSnooze: (() -> Unit)?,
    onStop: () -> Unit,
    snoozeMinutes: Long,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.EMERGING, modifier) {
        Spacer(Modifier.height(adaptiveWakeSpace(96)))
        Text(
            text = stringResource(R.string.wake_character_name).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.FaintText,
        )
        WmwTimeDisplay(
            time = displayTime,
            modifier = Modifier.padding(top = WmwSpacing.Sm),
            compact = true,
            color = WmwColors.WarmLight.copy(alpha = 0.80f),
        )
        WmwWakeLine(
            state = WmwWakeLineState.QUIET,
            modifier = Modifier.padding(top = adaptiveWakeSpace(54)),
            height = WmwSizes.WakeWaveHeight,
            sunrise = true,
        )
        Text(
            text = spokenLine?.takeIf { it.isNotBlank() }
                ?: if (preparedPlan != null) stringResource(R.string.wake_private_context_ready)
                else stringResource(R.string.wake_voice_starting),
            modifier = Modifier.padding(top = WmwSpacing.Md),
            style = MaterialTheme.typography.bodyMedium,
            color = WmwColors.QuietText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        WakeSafetyFooter(onSnooze, onStop, snoozeMinutes, onLightSurface = false)
    }
}

@Composable
private fun EngagedWakeSurface(
    spokenLine: String?,
    displayTime: String,
    onSnooze: (() -> Unit)?,
    onStop: () -> Unit,
    snoozeMinutes: Long,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.ENGAGED, modifier) {
        Spacer(Modifier.height(adaptiveWakeSpace(82)))
        Text(
            text = spokenLine?.takeIf { it.isNotBlank() } ?: stringResource(R.string.wake_default_greeting),
            style = MaterialTheme.typography.headlineMedium,
            color = WmwColors.WarmLight,
            textAlign = TextAlign.Center,
        )
        WmwTimeDisplay(
            time = displayTime,
            modifier = Modifier.padding(top = WmwSpacing.Xs),
            compact = true,
        )
        WmwWakeLine(
            state = WmwWakeLineState.LISTENING,
            modifier = Modifier.padding(top = adaptiveWakeSpace(58)),
            height = adaptiveWakeSpace(146),
            sunrise = true,
        )
        Text(
            text = stringResource(R.string.wake_voice_listening),
            modifier = Modifier.padding(top = WmwSpacing.Md),
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.GoldenLight,
        )
        Text(
            text = stringResource(R.string.wake_voice_answer_prompt),
            modifier = Modifier.padding(top = WmwSpacing.Sm),
            style = MaterialTheme.typography.bodySmall,
            color = WmwColors.QuietText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        WakeSafetyFooter(onSnooze, onStop, snoozeMinutes, onLightSurface = false)
    }
}

@Composable
private fun ActiveWakeSurface(
    spokenLine: String?,
    displayTime: String,
    onSnooze: (() -> Unit)?,
    onStop: () -> Unit,
    snoozeMinutes: Long,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.ACTIVE, modifier) {
        Spacer(Modifier.height(adaptiveWakeSpace(72)))
        Text(
            text = spokenLine?.takeIf { it.isNotBlank() } ?: stringResource(R.string.wake_default_instruction),
            style = MaterialTheme.typography.headlineLarge,
            color = WmwColors.WarmLight,
            textAlign = TextAlign.Center,
        )
        WmwTimeDisplay(
            time = displayTime,
            modifier = Modifier.padding(top = WmwSpacing.Xs),
            compact = true,
        )
        WmwWakeLine(
            state = WmwWakeLineState.MOVING,
            modifier = Modifier.padding(top = adaptiveWakeSpace(42)),
            height = adaptiveWakeSpace(156),
            sunrise = true,
        )
        Surface(
            modifier = Modifier.padding(top = WmwSpacing.Lg),
            shape = MaterialTheme.shapes.extraLarge,
            color = WmwColors.WarmLight.copy(alpha = 0.08f),
            border = BorderStroke(0.75.dp, WmwColors.Hairline),
        ) {
            Text(
                text = stringResource(R.string.wake_voice_moving),
                modifier = Modifier.padding(horizontal = WmwSpacing.Lg, vertical = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.WarmLight,
            )
        }
        Spacer(Modifier.weight(1f))
        WakeSafetyFooter(onSnooze, onStop, snoozeMinutes, onLightSurface = false)
    }
}

@Composable
private fun OrientedWakeSurface(
    preparedPlan: PreparedWakePlan?,
    defaultFirstMove: String?,
    spokenLine: String?,
    displayDate: String,
    onSnooze: (() -> Unit)?,
    onStop: () -> Unit,
    onFirstMoveConfirmed: () -> Unit,
    snoozeMinutes: Long,
    modifier: Modifier,
) {
    val compactLargeText =
        LocalDensity.current.fontScale >= 1.3f && LocalConfiguration.current.screenHeightDp <= 700

    WakeFrame(WmwCircadianStage.ORIENTED, modifier) {
        if (compactLargeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = WmwSpacing.Md),
                horizontalAlignment = Alignment.Start,
            ) {
                OrientedWakeBody(
                    preparedPlan = preparedPlan,
                    defaultFirstMove = defaultFirstMove,
                    spokenLine = spokenLine,
                    displayDate = displayDate,
                    compactLargeText = true,
                )
            }

            WmwPrimaryAction(
                label = stringResource(R.string.wake_first_move_action),
                onClick = onFirstMoveConfirmed,
                onLightSurface = true,
                tone = WmwActionTone.WARM,
            )
            WakeSafetyFooter(onSnooze, onStop, snoozeMinutes, onLightSurface = true)
        } else {
            OrientedWakeBody(
                preparedPlan = preparedPlan,
                defaultFirstMove = defaultFirstMove,
                spokenLine = spokenLine,
                displayDate = displayDate,
                compactLargeText = false,
            )

            Spacer(Modifier.weight(1f))
            WmwPrimaryAction(
                label = stringResource(R.string.wake_first_move_action),
                onClick = onFirstMoveConfirmed,
                onLightSurface = true,
                tone = WmwActionTone.WARM,
            )
            WakeSafetyFooter(onSnooze, onStop, snoozeMinutes, onLightSurface = true)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.OrientedWakeBody(
    preparedPlan: PreparedWakePlan?,
    defaultFirstMove: String?,
    spokenLine: String?,
    displayDate: String,
    compactLargeText: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (compactLargeText) WmwSpacing.Md else adaptiveWakeSpace(72),
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = spokenLine?.takeIf { it.isNotBlank() } ?: stringResource(R.string.wake_oriented_greeting),
            style = MaterialTheme.typography.headlineLarge,
            color = WmwColors.Midnight,
        )
        Text(
            text = displayDate,
            modifier = Modifier.padding(top = WmwSpacing.Xs),
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.LightQuietText,
        )
        Text(
            text = preparedPlan?.reminderLine ?: stringResource(R.string.wake_oriented_ready),
            modifier = Modifier.padding(top = WmwSpacing.Lg),
            style = MaterialTheme.typography.bodyLarge,
            color = WmwColors.Midnight.copy(alpha = 0.78f),
        )
    }

    WmwWakeLine(
        state = WmwWakeLineState.SETTLED,
        onLightSurface = true,
        modifier = if (compactLargeText) {
            Modifier
                .fillMaxWidth()
                .padding(top = WmwSpacing.Lg)
        } else {
            Modifier.padding(top = adaptiveWakeSpace(58))
        },
        height = if (compactLargeText) 72.dp else adaptiveWakeSpace(100),
        sunrise = true,
    )

    Text(
        text = stringResource(R.string.wake_whats_first),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (compactLargeText) WmwSpacing.Lg else WmwSpacing.Xl),
        style = MaterialTheme.typography.titleLarge,
        color = WmwColors.Midnight,
    )

    FirstMoveTile(
        label = preparedPlan?.firstMoveLine?.removePrefix("First move: ")
            ?: defaultFirstMove
            ?: stringResource(R.string.wake_first_move_fallback),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WmwSpacing.Md),
    )
    Text(
        text = stringResource(R.string.wake_first_move_confirmation_detail),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WmwSpacing.Sm),
        style = MaterialTheme.typography.bodySmall,
        color = WmwColors.LightQuietText,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CompleteWakeSurface(
    preparedPlan: PreparedWakePlan?,
    defaultFirstMove: String?,
    displayTime: String,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.COMPLETE, modifier) {
        Spacer(Modifier.height(adaptiveWakeSpace(74)))
        Text(
            text = stringResource(R.string.wake_voice_complete).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.DawnText,
        )
        WmwTimeDisplay(
            time = displayTime,
            modifier = Modifier.padding(top = WmwSpacing.Md),
            color = WmwColors.Midnight,
            compact = true,
        )
        WmwWakeLine(
            state = WmwWakeLineState.SETTLED,
            onLightSurface = true,
            modifier = Modifier.padding(top = WmwSpacing.Xl),
            height = adaptiveWakeSpace(118),
            sunrise = true,
        )
        Text(
            text = preparedPlan?.firstMoveLine?.removePrefix("First move: ")
                ?: defaultFirstMove
                ?: stringResource(R.string.wake_first_move_fallback),
            modifier = Modifier.padding(top = WmwSpacing.Xl),
            style = MaterialTheme.typography.headlineSmall,
            color = WmwColors.Midnight,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        WmwPrimaryAction(
            label = stringResource(R.string.wake_finish),
            onClick = onStop,
            onLightSurface = true,
            tone = WmwActionTone.WARM,
        )
    }
}

@Composable
private fun FirstMoveTile(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(adaptiveWakeSpace(124)),
        shape = MaterialTheme.shapes.medium,
        color = WmwColors.PaperCard.copy(alpha = 0.92f),
        border = BorderStroke(0.75.dp, WmwColors.DarkHairline),
    ) {
        Column(
            modifier = Modifier.padding(WmwSpacing.Md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = WmwColors.Sunrise.copy(alpha = 0.16f),
            ) {}
            Text(label, style = MaterialTheme.typography.bodyMedium, color = WmwColors.Midnight)
        }
    }
}

@Composable
private fun WakeSafetyFooter(
    onSnooze: (() -> Unit)?,
    onStop: () -> Unit,
    snoozeMinutes: Long,
    onLightSurface: Boolean,
) {
    val textColor = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.72f) else WmwColors.QuietText
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
    ) {
        if (onSnooze != null) {
            TextButton(
                onClick = onSnooze,
                modifier = Modifier
                    .weight(1f)
                    .height(WmwSizes.SleepyTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.wake_snooze_minutes, snoozeMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                )
            }
        }
        TextButton(
            onClick = onStop,
            modifier = Modifier
                .weight(1f)
                .height(WmwSizes.SleepyTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.wake_stop_alarm),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

@Preview(
    name = "Wake listening",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
)
@Composable
private fun WakeListeningPreview() {
    WakeMyWayTheme {
        WakeSurface(
            preparedPlan = null,
            onSnooze = {},
            onStop = {},
            displayTime = "07:30",
            displayDate = "Tuesday · 14 Jan",
            voiceState = WakeVoiceUiState(
                mode = WakeVoiceMode.LISTENING,
                spokenLine = "Morning, Yotam.",
                speechAvailable = true,
                voiceInputAvailable = true,
            ),
        )
    }
}
