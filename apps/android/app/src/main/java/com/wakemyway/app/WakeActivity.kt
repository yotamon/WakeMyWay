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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakeTimePreparedContent
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwIntentionalStopAction
import com.wakemyway.app.ui.components.WmwPresence
import com.wakemyway.app.ui.components.WmwPresenceState
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.components.WmwTimeDisplay
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.app.voice.WakeVoiceMode
import com.wakemyway.app.voice.WakeVoiceSessionController
import com.wakemyway.app.voice.WakeVoiceUiState
import com.wakemyway.core.preparation.PreparedWakePlan
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WakeActivity : ComponentActivity() {
    private var occurrenceId: WakeOccurrenceId? = null
    private var preparedPlan by mutableStateOf<PreparedWakePlan?>(null)
    private var voiceState by mutableStateOf(WakeVoiceUiState())
    private var voiceController: WakeVoiceSessionController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val rawId = intent.getStringExtra(AlarmPlaybackService.EXTRA_OCCURRENCE_ID)
        if (rawId == null) {
            finish()
            return
        }
        val wakeOccurrenceId = WakeOccurrenceId(rawId)
        occurrenceId = wakeOccurrenceId
        refreshPreparedPlanIfUnlocked()

        voiceController = WakeVoiceSessionController(
            context = this,
            occurrenceId = wakeOccurrenceId,
            onUiState = { voiceState = it },
            onCompleted = { finishAndRemoveTask() },
        )

        setContent {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = preparedPlan,
                    onSnooze = {
                        voiceController?.closeForTerminalAction()
                        AlarmPlaybackService.requestSnooze(this, wakeOccurrenceId)
                        finishAndRemoveTask()
                    },
                    onStop = {
                        voiceController?.closeForTerminalAction()
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
        voiceController?.onSurfaceVisible()
    }

    override fun onPause() {
        // Microphone capture is scoped to a visible Wake Surface. The critical alarm continues
        // under AlarmPlaybackService if this activity loses focus.
        voiceController?.onSurfaceHidden()
        // Do not leave private content retained in the Compose state when the wake UI loses focus.
        preparedPlan = null
        super.onPause()
    }

    override fun onDestroy() {
        voiceController?.close()
        voiceController = null
        super.onDestroy()
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
            // Never read/render private Tomorrow Contract-derived content during Direct Boot or lock.
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
            // Prevent OS screenshots/recents thumbnails while private prepared text is visible.
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/**
 * `voiceState == null` intentionally renders the previously reviewed synthetic Wake Emerging
 * fixture. Production WakeActivity always supplies a live voice state. This preserves the curated
 * visual baseline until the new dynamic states receive their own explicit visual-review gate.
 */
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
    val presenceState = when (voiceState.mode) {
        WakeVoiceMode.LISTENING -> WmwPresenceState.LISTENING
        WakeVoiceMode.MOVING -> WmwPresenceState.MOVING
        WakeVoiceMode.ORIENTING,
        WakeVoiceMode.COMPLETE,
        -> WmwPresenceState.COMPLETE
        else -> WmwPresenceState.QUIET
    }
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
            WmwTimeDisplay(
                time = displayTime,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.wake_character_name),
                style = MaterialTheme.typography.labelMedium,
                color = WmwColors.QuietText,
            )

            Spacer(Modifier.height(WmwSpacing.Huge))

            WmwPresence(
                state = presenceState,
                contentDescription = stringResource(R.string.wake_presence_description),
            )
            Text(
                text = statusText,
                modifier = Modifier.padding(top = WmwSpacing.Md),
                style = MaterialTheme.typography.labelMedium,
                color = if (voiceState.mode == WakeVoiceMode.LISTENING) {
                    WmwColors.SoftEmber
                } else {
                    WmwColors.QuietText
                },
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(WmwSpacing.Xl))

            Text(
                text = voiceState.spokenLine
                    ?: preparedPlan?.orientationLeadIn
                    ?: stringResource(R.string.wake_default_greeting),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = WmwColors.WarmLight,
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
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.MorningPaper,
                textAlign = TextAlign.Center,
            )
            preparedPlan?.firstMoveLine?.let { firstMove ->
                Text(
                    text = firstMove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Md),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WmwColors.QuietText,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(WmwSpacing.Hero))

            if (voiceState.voiceInputAvailable) {
                Text(
                    text = stringResource(R.string.wake_voice_privacy_note),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.QuietText,
                    textAlign = TextAlign.Center,
                )
            }
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
            WmwTimeDisplay(
                time = displayTime,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.wake_character_name),
                style = MaterialTheme.typography.labelMedium,
                color = WmwColors.QuietText,
            )

            Spacer(Modifier.height(WmwSpacing.Huge))

            WmwPresence(
                state = WmwPresenceState.QUIET,
                contentDescription = stringResource(R.string.wake_presence_description),
            )

            Spacer(Modifier.height(WmwSpacing.Xxl))

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
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.MorningPaper,
                textAlign = TextAlign.Center,
            )
            preparedPlan?.firstMoveLine?.let { firstMove ->
                Text(
                    text = firstMove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Md),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WmwColors.QuietText,
                    textAlign = TextAlign.Center,
                )
            }

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
