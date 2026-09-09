package com.wakemyway.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.TimingSnapshot
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmKernel(this).reconcile()
        setContent {
            WakeMyWayTheme {
                WakeAlarmLabScreen()
            }
        }
    }
}

@Composable
private fun WakeAlarmLabScreen() {
    val context = LocalContext.current
    val kernel = remember { AlarmKernel(context) }
    val timingTrace = remember { WakeTimingTrace(context) }
    var health by remember { mutableStateOf(kernel.health()) }
    var history by remember { mutableStateOf(timingTrace.history(HISTORY_LIMIT)) }
    var message by remember { mutableStateOf<String?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        health = kernel.health()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),
    ) {
        Text(
            text = "Wake My Way",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 42.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Wake Alarm Lab",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "Local founder diagnostics. No private wake context is recorded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        Text(
            modifier = Modifier.padding(top = 28.dp),
            text = if (health.ready) "Wake Ready" else "Wake not ready",
            style = MaterialTheme.typography.headlineSmall,
        )
        HealthFacts(health)

        Button(
            modifier = Modifier.padding(top = 24.dp),
            onClick = {
                runCatching { kernel.commitSchedule(founderTestSchedule()) }
                    .onSuccess { committedHealth ->
                        health = committedHealth
                        committedHealth.nextOccurrence?.let { occurrence ->
                            timingTrace.expected(
                                occurrence = occurrence,
                                scenario = WakeTimingTrace.SCENARIO_NORMAL_T_PLUS_2M,
                            )
                        }
                        history = timingTrace.history(HISTORY_LIMIT)
                        message = "One-shot lab wake scheduled for about 2 minutes from now. Lock the phone."
                    }
                    .onFailure {
                        health = kernel.health()
                        message = "Could not schedule: ${it.message ?: it::class.simpleName}"
                    }
            },
            enabled = health.exactAlarmAllowed,
        ) {
            Text("Run one-shot T+2m wake")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 10.dp),
            onClick = {
                health = kernel.reconcile()
                history = timingTrace.history(HISTORY_LIMIT)
                message = health.detail
            },
        ) {
            Text("Refresh evidence")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 10.dp),
            onClick = {
                timingTrace.clearHistory()
                history = emptyList()
                message = "Local reliability history cleared."
            },
        ) {
            Text("Clear lab history")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !health.notificationsAllowed) {
            OutlinedButton(
                modifier = Modifier.padding(top = 10.dp),
                onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
            ) {
                Text("Allow alarm notifications")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !health.fullScreenIntentAllowed) {
            OutlinedButton(
                modifier = Modifier.padding(top = 10.dp),
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            ) {
                Text("Allow full-screen alarms")
            }
        }

        message?.let {
            Text(
                modifier = Modifier.padding(top = 18.dp),
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            modifier = Modifier.padding(top = 30.dp),
            text = "Recent wake evidence",
            style = MaterialTheme.typography.titleMedium,
        )

        if (history.isEmpty()) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "No wake sessions recorded yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            history.forEachIndexed { index, timing ->
                TimingFacts(index + 1, timing)
            }
        }
    }
}

@Composable
private fun HealthFacts(health: AlarmHealth) {
    Text(
        modifier = Modifier.padding(top = 10.dp),
        text = health.detail,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.secondary,
    )
    Text(
        modifier = Modifier.padding(top = 8.dp),
        text = "Exact alarm: ${yesNo(health.exactAlarmAllowed)}  ·  Notifications: ${yesNo(health.notificationsAllowed)}  ·  Full screen: ${yesNo(health.fullScreenIntentAllowed)}",
        style = MaterialTheme.typography.bodySmall,
    )
    health.nextOccurrence?.let {
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Next: ${it.scheduledAt}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TimingFacts(number: Int, timing: TimingSnapshot) {
    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = "#$number · ${timing.scenario ?: timing.occurrenceKind.ifBlank { "unknown" }} · ${timing.state().name}",
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = buildString {
            append("trigger ${formatMillis(timing.triggerDelayMillis)}")
            append("  ·  audio ${formatMillis(timing.triggerToAudioMillis)}")
            append("  ·  UI ${formatMillis(timing.triggerToUiMillis)}")
        },
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        modifier = Modifier.padding(top = 3.dp),
        text = buildString {
            append("terminal ${timing.terminalAction ?: "—"}")
            append("  ·  recoveries ${timing.serviceRecoveryCount}")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

private fun founderTestSchedule(): WakeSchedule {
    val target = ZonedDateTime.now().plusMinutes(2).withNano(0)
    return WakeSchedule(
        id = WakeScheduleId("lab-normal-${System.currentTimeMillis()}"),
        zoneId = target.zone,
        timesByDay = DayOfWeek.values().associateWith { target.toLocalTime() },
        revision = System.currentTimeMillis().coerceAtLeast(1),
        completionPolicy = WakeCompletionPolicy.ONE_SHOT,
    )
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun formatMillis(value: Long?): String = value?.let { "${it}ms" } ?: "—"

private const val HISTORY_LIMIT = 6
