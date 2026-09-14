package com.wakemyway.app

import android.app.KeyguardManager
import android.os.Bundle
import android.os.UserManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
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
        // Microphone and motion capture are scoped to a visible Wake Surface. Runtime intent is
        // retained by WakeSessionViewModel and restored when the surface becomes visible again.
        sessionViewModel?.onSurfaceHidden()
        // Private Tomorrow Contract-derived content must not remain in Compose state off-screen.
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
            // Never read or render private prepared content during Direct Boot or while locked.
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
            // Prevent screenshots and recent-app thumbnails while private morning context is shown.
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
    WmwCircadianSurface(stage = stage, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = WmwSpacing.Lg, vertical = WmwSpacing.Md),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
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
        Spacer(Modifier.height(224.dp))
        Text(
            text = stringResource(R.string.wake_character_name).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.FaintText,
        )
        Spacer(Modifier.height(96.dp))
        WmwTimeDisplay(
            time = displayTime,
            compact = true,
            color = WmwColors.WarmLight.copy(alpha = 0.62f),
        )
        Spacer(Modifier.height(98.dp))
        WmwWakeLine(
            state = WmwWakeLineState.QUIET,
            height = WmwSizes.WakeWaveHeight,
        )
        if (!spokenLine.isNullOrBlank()) {
            Text(
                text = spokenLine,
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.FaintText,
                textAlign = TextAlign.Center,
            )
        } else if (preparedPlan != null) {
            Text(
                text = stringResource(R.string.wake_private_context_ready),
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.FaintText,
                textAlign = TextAlign.Center,
            )
        }
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
        Spacer(Modifier.height(248.dp))
        WmwTimeDisplay(time = displayTime, compact = true)
        Text(
            text = spokenLine?.takeIf { it.isNotBlank() } ?: stringResource(R.string.wake_default_greeting),
            modifier = Modifier.padding(top = 104.dp),
            style = MaterialTheme.typography.titleLarge,
            color = WmwColors.WarmLight,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "◌  ${stringResource(R.string.wake_voice_listening)}…",
            modifier = Modifier.padding(top = 66.dp),
            style = MaterialTheme.typography.bodySmall,
            color = WmwColors.QuietText,
        )
        Spacer(Modifier.height(24.dp))
        WmwWakeLine(
            state = WmwWakeLineState.LISTENING,
            height = WmwSizes.WakeWaveHeight,
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
        Spacer(Modifier.height(245.dp))
        WmwTimeDisplay(time = displayTime, compact = true)
        Text(
            text = spokenLine?.takeIf { it.isNotBlank() } ?: stringResource(R.string.wake_default_instruction),
            modifier = Modifier.padding(top = 67.dp),
            style = MaterialTheme.typography.titleLarge,
            color = WmwColors.WarmLight,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(86.dp))
        WmwWakeLine(
            state = WmwWakeLineState.MOVING,
            height = WmwSizes.WakeWaveHeight,
        )
        Row(
            modifier = Modifier.padding(top = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(WmwColors.WarmLight.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", style = MaterialTheme.typography.bodySmall, color = WmwColors.WarmLight)
            }
            Text(
                text = stringResource(R.string.wake_voice_moving),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.WarmLight.copy(alpha = 0.84f),
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
                .padding(top = 146.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = spokenLine?.takeIf { it.isNotBlank() } ?: stringResource(R.string.wake_oriented_greeting),
                style = MaterialTheme.typography.headlineMedium,
                color = WmwColors.Ink,
            )
            Text(
                text = displayDate,
                modifier = Modifier.padding(top = WmwSpacing.Md),
                style = MaterialTheme.typography.labelMedium,
                color = WmwColors.Ink.copy(alpha = 0.54f),
            )
            Text(
                text = preparedPlan?.reminderLine ?: stringResource(R.string.wake_oriented_ready),
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.Ink.copy(alpha = 0.72f),
            )
        }

        Spacer(Modifier.height(204.dp))
        WmwWakeLine(
            state = WmwWakeLineState.SETTLED,
            onLightSurface = true,
            height = 72.dp,
        )

        Text(
            text = stringResource(R.string.wake_whats_first),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 38.dp),
            style = MaterialTheme.typography.titleLarge,
            color = WmwColors.Ink,
            textAlign = TextAlign.Start,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 33.dp),
            horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
        ) {
            FirstMoveTile(
                label = preparedPlan?.firstMoveLine?.removePrefix("First move: ")
                    ?: stringResource(R.string.wake_first_move_fallback),
                glyph = "◯",
                modifier = Modifier.weight(1f),
            )
            FirstMoveTile(
                label = stringResource(R.string.wake_keep_moving),
                glyph = "▰",
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
        Spacer(Modifier.height(92.dp))
        Text(
            text = stringResource(R.string.wake_voice_complete).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.Ink.copy(alpha = 0.7f),
        )
        WmwTimeDisplay(
            time = displayTime,
            modifier = Modifier.padding(top = WmwSpacing.Md),
            color = WmwColors.Ink,
            compact = true,
        )
        WmwWakeLine(
            state = WmwWakeLineState.SETTLED,
            onLightSurface = true,
            modifier = Modifier.padding(top = WmwSpacing.Xxl),
        )
        Text(
            text = preparedPlan?.firstMoveLine?.removePrefix("First move: ")
                ?: stringResource(R.string.wake_first_move_fallback),
            modifier = Modifier.padding(top = WmwSpacing.Xxl),
            style = MaterialTheme.typography.headlineSmall,
            color = WmwColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        WmwPrimaryAction(
            label = stringResource(R.string.wake_finish),
            onClick = onStop,
            onLightSurface = true,
            tone = WmwActionTone.DARK,
        )
    }
}

@Composable
private fun FirstMoveTile(label: String, glyph: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(150.dp),
        shape = MaterialTheme.shapes.medium,
        color = WmwColors.PaperCard.copy(alpha = 0.90f),
        border = BorderStroke(0.75.dp, WmwColors.DarkHairline),
    ) {
        Column(
            modifier = Modifier.padding(WmwSpacing.Md),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(glyph, style = MaterialTheme.typography.headlineMedium, color = WmwColors.Ink)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = WmwColors.Ink)
        }
    }
}

@Composable
private fun WakeSafetyFooter(
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    onLightSurface: Boolean,
) {
    val textColor = if (onLightSurface) WmwColors.Ink.copy(alpha = 0.70f) else WmwColors.QuietText
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
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
                spokenLine = "Morning.",
                speechAvailable = true,
                voiceInputAvailable = true,
            ),
        )
    }
}
