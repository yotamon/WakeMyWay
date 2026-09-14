package com.wakemyway.app

import android.app.KeyguardManager
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakeTimePreparedContent
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwIntentionalStopAction
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.components.WmwTimeDisplay
import com.wakemyway.app.ui.components.WmwWakeLine
import com.wakemyway.app.ui.components.WmwWakeLineState
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.app.voice.WakeVoiceMode
import com.wakemyway.app.voice.WakeVoiceUiState
import com.wakemyway.core.preparation.PreparedWakePlan
import com.wakemyway.core.schedule.WakeOccurrenceId
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
        if (hasFocus && preparedPlan == null) {
            refreshPreparedPlanIfUnlocked()
        }
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
    voiceState: WakeVoiceUiState? = null,
) {
    if (voiceState == null) {
        LegacyWakeEmergingSurface(
            preparedPlan = preparedPlan,
            onSnooze = onSnooze,
            onStop = onStop,
            modifier = modifier,
            displayTime = displayTime,
        )
        return
    }

    VoiceWakeSurface(
        preparedPlan = preparedPlan,
        voiceState = voiceState,
        onSnooze = onSnooze,
        onStop = onStop,
        modifier = modifier,
        displayTime = displayTime,
    )
}

@Composable
private fun VoiceWakeSurface(
    preparedPlan: PreparedWakePlan?,
    voiceState: WakeVoiceUiState,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
    displayTime: String,
) {
    val stage = when (voiceState.mode) {
        WakeVoiceMode.STARTING,
        WakeVoiceMode.SPEAKING,
        WakeVoiceMode.DEGRADED,
        -> WmwCircadianStage.EMERGING

        WakeVoiceMode.LISTENING -> WmwCircadianStage.ENGAGED
        WakeVoiceMode.MOVING -> WmwCircadianStage.ACTIVE
        WakeVoiceMode.ORIENTING -> WmwCircadianStage.ORIENTED
        WakeVoiceMode.COMPLETE -> WmwCircadianStage.COMPLETE
    }
    val lineState = when (voiceState.mode) {
        WakeVoiceMode.STARTING,
        WakeVoiceMode.SPEAKING,
        WakeVoiceMode.DEGRADED,
        -> WmwWakeLineState.QUIET

        WakeVoiceMode.LISTENING -> WmwWakeLineState.LISTENING
        WakeVoiceMode.MOVING -> WmwWakeLineState.MOVING
        WakeVoiceMode.ORIENTING,
        WakeVoiceMode.COMPLETE,
        -> WmwWakeLineState.SETTLED
    }
    val isLightSurface = stage == WmwCircadianStage.ORIENTED || stage == WmwCircadianStage.COMPLETE
    val primaryText = if (isLightSurface) WmwColors.Ink else WmwColors.WarmLight
    val secondaryText = if (isLightSurface) WmwColors.Ink.copy(alpha = 0.66f) else WmwColors.QuietText
    val statusText = when (voiceState.mode) {
        WakeVoiceMode.STARTING -> stringResource(R.string.wake_voice_starting)
        WakeVoiceMode.SPEAKING -> stringResource(R.string.wake_voice_speaking)
        WakeVoiceMode.LISTENING -> stringResource(R.string.wake_voice_listening)
        WakeVoiceMode.MOVING -> stringResource(R.string.wake_voice_moving)
        WakeVoiceMode.ORIENTING -> stringResource(R.string.wake_voice_orienting)
        WakeVoiceMode.DEGRADED -> stringResource(R.string.wake_voice_degraded)
        WakeVoiceMode.COMPLETE -> stringResource(R.string.wake_voice_complete)
    }

    WmwCircadianSurface(
        stage = stage,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Xl, vertical = WmwSpacing.Xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.wake_character_name).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = secondaryText,
            )
            WmwTimeDisplay(
                time = displayTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Sm),
                color = primaryText,
            )

            Spacer(Modifier.height(WmwSpacing.Lg))

            WmwWakeLine(
                state = lineState,
                onLightSurface = isLightSurface,
            )
            Text(
                text = statusText.uppercase(),
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.labelSmall,
                color = if (voiceState.mode == WakeVoiceMode.LISTENING) {
                    WmwColors.SoftEmber
                } else {
                    secondaryText
                },
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(if (isLightSurface) WmwSpacing.Xxl else WmwSpacing.Huge))

            Text(
                text = voiceState.spokenLine
                    ?: preparedPlan?.orientationLeadIn
                    ?: stringResource(R.string.wake_default_greeting),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = primaryText,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (voiceState.mode == WakeVoiceMode.LISTENING) {
                    stringResource(R.string.wake_voice_answer_prompt)
                } else {
                    preparedPlan?.reminderLine
                        ?: stringResource(R.string.wake_default_instruction)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryText,
                textAlign = TextAlign.Center,
            )

            if (isLightSurface) {
                preparedPlan?.firstMoveLine?.let { firstMove ->
                    Spacer(Modifier.height(WmwSpacing.Xxl))
                    Text(
                        text = stringResource(R.string.wake_whats_first).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryText,
                    )
                    Text(
                        text = firstMove,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WmwSpacing.Sm),
                        style = MaterialTheme.typography.titleLarge,
                        color = primaryText,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(WmwSpacing.Huge))

            if (!isLightSurface && voiceState.voiceInputAvailable) {
                Text(
                    text = stringResource(R.string.wake_voice_privacy_note),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    textAlign = TextAlign.Center,
                )
            }
            if (!isLightSurface) {
                Text(
                    text = if (preparedPlan == null) {
                        stringResource(R.string.wake_private_context_locked)
                    } else {
                        stringResource(R.string.wake_private_context_ready)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText,
                    textAlign = TextAlign.Center,
                )
            }

            WmwSecondaryAction(
                label = stringResource(R.string.wake_snooze_five),
                onClick = onSnooze,
                modifier = Modifier.padding(top = WmwSpacing.Lg),
                onLightSurface = isLightSurface,
            )
            WmwIntentionalStopAction(
                label = stringResource(R.string.wake_stop_alarm),
                onClick = onStop,
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                onLightSurface = isLightSurface,
            )

            Spacer(Modifier.height(WmwSpacing.Xl))
        }
    }
}

@Composable
private fun LegacyWakeEmergingSurface(
    preparedPlan: PreparedWakePlan?,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
    displayTime: String,
) {
    WmwCircadianSurface(
        stage = WmwCircadianStage.EMERGING,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Xl, vertical = WmwSpacing.Xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.wake_character_name).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = WmwColors.QuietText,
            )
            WmwTimeDisplay(
                time = displayTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Sm),
            )

            Spacer(Modifier.height(WmwSpacing.Xl))
            WmwWakeLine(state = WmwWakeLineState.QUIET)
            Spacer(Modifier.height(WmwSpacing.Huge))

            Text(
                text = preparedPlan?.orientationLeadIn
                    ?: stringResource(R.string.wake_default_greeting),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = WmwColors.WarmLight,
                textAlign = TextAlign.Center,
            )
            Text(
                text = preparedPlan?.reminderLine
                    ?: stringResource(R.string.wake_default_instruction),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.QuietText,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(WmwSpacing.Hero))

            Text(
                text = if (preparedPlan == null) {
                    stringResource(R.string.wake_private_context_locked)
                } else {
                    stringResource(R.string.wake_private_context_ready)
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.QuietText,
                textAlign = TextAlign.Center,
            )

            WmwSecondaryAction(
                label = stringResource(R.string.wake_snooze_five),
                onClick = onSnooze,
                modifier = Modifier.padding(top = WmwSpacing.Lg),
            )
            WmwIntentionalStopAction(
                label = stringResource(R.string.wake_stop_alarm),
                onClick = onStop,
                modifier = Modifier.padding(top = WmwSpacing.Xs),
            )

            Spacer(Modifier.height(WmwSpacing.Xl))
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
            displayTime = "08:00",
            voiceState = WakeVoiceUiState(
                mode = WakeVoiceMode.LISTENING,
                spokenLine = "Sit up, then tell me when you're sitting.",
                speechAvailable = true,
                voiceInputAvailable = true,
            ),
        )
    }
}
