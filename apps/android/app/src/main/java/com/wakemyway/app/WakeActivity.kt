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
import com.wakemyway.core.preparation.PreparedWakePlan
import com.wakemyway.core.schedule.WakeOccurrenceId
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WakeActivity : ComponentActivity() {
    private var occurrenceId: WakeOccurrenceId? = null
    private var preparedPlan by mutableStateOf<PreparedWakePlan?>(null)

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

        setContent {
            WakeMyWayTheme {
                WakeSurface(
                    preparedPlan = preparedPlan,
                    onSnooze = {
                        AlarmPlaybackService.requestSnooze(this, wakeOccurrenceId)
                        finishAndRemoveTask()
                    },
                    onStop = {
                        AlarmPlaybackService.requestStop(this, wakeOccurrenceId)
                        finishAndRemoveTask()
                    },
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
    }

    override fun onPause() {
        // Do not leave private content retained in the Compose state when the wake UI loses focus.
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

@Composable
private fun WakeSurface(
    preparedPlan: PreparedWakePlan?,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
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
                time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
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
    name = "Wake emerging",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
)
@Composable
private fun WakeEmergingPreview() {
    WakeMyWayTheme {
        WakeSurface(
            preparedPlan = null,
            onSnooze = {},
            onStop = {},
        )
    }
}
