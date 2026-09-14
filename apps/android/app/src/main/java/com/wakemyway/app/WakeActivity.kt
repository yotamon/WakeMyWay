package com.wakemyway.app

import android.app.KeyguardManager
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakeTimePreparedContent
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
import com.wakemyway.core.preparation.PreparedWakePlan
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WakeActivity : ComponentActivity() {
    private var occurrenceId: WakeOccurrenceId? = null
    private var preparedPlan by mutableStateOf<PreparedWakePlan?>(null)
    private var sessionViewModel: WakeSessionViewModel? = null

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
        refreshPreparedPlanIfUnlocked()

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
                    onSnooze = {
                        viewModel.closeForTerminalAction()
                        AlarmPlaybackService.requestSnooze(this, wakeOccurrenceId)
                        finishAndRemoveTask()
                    },
                    onStop = {
                        viewModel.closeForTerminalAction()
                        AlarmPlaybackService.requestStop(this, wakeOccurrenceId)
                        finishAndRemoveTask()
                    },
                    voiceState = voiceState,
                )
            }
        }

        window.decorView.post {
            WakeTimingTrace(this).uiVisible(wakeOccurrenceId)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPreparedPlanIfUnlocked()
        sessionViewModel?.onSurfaceVisible()
    }

    override fun onPause() {
        sessionViewModel?.onSurfaceHidden()
        preparedPlan = null
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && preparedPlan == null) refreshPreparedPlanIfUnlocked()
    }

    private fun refreshPreparedPlanIfUnlocked() {
        val id = occurrenceId ?: return
        val userManager = getSystemService(UserManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!userManager.isUserUnlocked || keyguard.isDeviceLocked) {
            preparedPlan = null
            return
        }

        preparedPlan = runCatching {
            when (val content = WakePreparationManager(this).loadForWake(id)) {
                is WakeTimePreparedContent.Prepared -> content.plan
                is WakeTimePreparedContent.GenericFallback -> null
            }
        }.getOrNull()

        if (preparedPlan != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
internal fun WakeSurface(
    preparedPlan: PreparedWakePlan?,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    displayTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    displayDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMM")),
    voiceState: WakeVoiceUiState? = null,
) {
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
            modifier = modifier,
        )

        WakeVoiceMode.LISTENING -> EngagedWakeSurface(
            spokenLine = voiceState.spokenLine,
            displayTime = displayTime,
            onSnooze = onSnooze,
            onStop = onStop,
            modifier = modifier,
        )

        WakeVoiceMode.MOVING -> ActiveWakeSurface(
            spokenLine = voiceState.spokenLine,
            displayTime = displayTime,
            onSnooze = onSnooze,
            onStop = onStop,
            modifier = modifier,
        )

        WakeVoiceMode.ORIENTING -> OrientedWakeSurface(
            preparedPlan = preparedPlan,
            spokenLine = voiceState.spokenLine,
            displayDate = displayDate,
            onSnooze = onSnooze,
            onStop = onStop,
            modifier = modifier,
        )

        WakeVoiceMode.COMPLETE -> CompleteWakeSurface(
            preparedPlan = preparedPlan,
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
private fun EmergingWakeSurface(
    preparedPlan: PreparedWakePlan?,
    spokenLine: String?,
    displayTime: String,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.EMERGING, modifier) {
        Spacer(Modifier.height(96.dp))
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
            modifier = Modifier.padding(top = 54.dp),
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
        WakeSafetyFooter(onSnooze, onStop, onLightSurface = false)
    }
}

@Composable
private fun EngagedWakeSurface(
    spokenLine: String?,
    displayTime: String,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.ENGAGED, modifier) {
        Spacer(Modifier.height(82.dp))
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
            modifier = Modifier.padding(top = 58.dp),
            height = 146.dp,
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
        WakeSafetyFooter(onSnooze, onStop, onLightSurface = false)
    }
}

@Composable
private fun ActiveWakeSurface(
    spokenLine: String?,
    displayTime: String,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.ACTIVE, modifier) {
        Spacer(Modifier.height(72.dp))
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
            modifier = Modifier.padding(top = 42.dp),
            height = 156.dp,
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
        WakeSafetyFooter(onSnooze, onStop, onLightSurface = false)
    }
}

@Composable
private fun OrientedWakeSurface(
    preparedPlan: PreparedWakePlan?,
    spokenLine: String?,
    displayDate: String,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.ORIENTED, modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 72.dp),
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
            modifier = Modifier.padding(top = 58.dp),
            height = 100.dp,
            sunrise = true,
        )

        Text(
            text = stringResource(R.string.wake_whats_first),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WmwSpacing.Xl),
            style = MaterialTheme.typography.titleLarge,
            color = WmwColors.Midnight,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WmwSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
        ) {
            FirstMoveTile(
                label = preparedPlan?.firstMoveLine?.removePrefix("First move: ")
                    ?: stringResource(R.string.wake_first_move_fallback),
                modifier = Modifier.weight(1f),
            )
            FirstMoveTile(
                label = stringResource(R.string.wake_keep_moving),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.weight(1f))
        WakeSafetyFooter(onSnooze, onStop, onLightSurface = true)
    }
}

@Composable
private fun CompleteWakeSurface(
    preparedPlan: PreparedWakePlan?,
    displayTime: String,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    WakeFrame(WmwCircadianStage.COMPLETE, modifier) {
        Spacer(Modifier.height(74.dp))
        Text(
            text = stringResource(R.string.wake_voice_complete).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.DawnDeep,
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
            height = 118.dp,
            sunrise = true,
        )
        Text(
            text = preparedPlan?.firstMoveLine?.removePrefix("First move: ")
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
        modifier = modifier.height(124.dp),
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
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    onLightSurface: Boolean,
) {
    val textColor = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.72f) else WmwColors.QuietText
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
    ) {
        TextButton(
            onClick = onSnooze,
            modifier = Modifier
                .weight(1f)
                .height(WmwSizes.SleepyTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.wake_snooze_short),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
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
