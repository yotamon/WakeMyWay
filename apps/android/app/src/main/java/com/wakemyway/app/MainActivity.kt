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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
                AlarmFoundationScreen()
            }
        }
    }
}

@Composable
private fun AlarmFoundationScreen() {
    val context = LocalContext.current
    val kernel = remember { AlarmKernel(context) }
    val timingTrace = remember { WakeTimingTrace(context) }
    var health by remember { mutableStateOf(kernel.health()) }
    var timing by remember { mutableStateOf(timingTrace.snapshot()) }
    var message by remember { mutableStateOf<String?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        health = kernel.health()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Wake My Way",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 42.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = "Wake up your way.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = if (health.ready) "Wake Ready" else "Wake not ready",
            style = MaterialTheme.typography.headlineSmall,
        )
        HealthFacts(health)

        Button(
            modifier = Modifier.padding(top = 28.dp),
            onClick = {
                runCatching { kernel.commitSchedule(founderTestSchedule()) }
                    .onSuccess {
                        health = it
                        message = "Test wake scheduled for about 2 minutes from now. Lock the phone."
                    }
                    .onFailure {
                        health = kernel.health()
                        message = "Could not schedule: ${it.message ?: it::class.simpleName}"
                    }
            },
            enabled = health.exactAlarmAllowed,
        ) {
            Text("Schedule test wake in 2 minutes")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 12.dp),
            onClick = {
                health = kernel.reconcile()
                timing = timingTrace.snapshot()
                message = health.detail
            },
        ) {
            Text("Refresh Wake Ready + timing")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !health.notificationsAllowed) {
            OutlinedButton(
                modifier = Modifier.padding(top = 12.dp),
                onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
            ) {
                Text("Allow alarm notifications")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !health.fullScreenIntentAllowed) {
            OutlinedButton(
                modifier = Modifier.padding(top = 12.dp),
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

        timing?.let { TimingFacts(it) }

        message?.let {
            Text(
                modifier = Modifier.padding(top = 20.dp),
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
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
private fun TimingFacts(timing: TimingSnapshot) {
    Text(
        modifier = Modifier.padding(top = 20.dp),
        text = "Last wake timing",
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        modifier = Modifier.padding(top = 6.dp),
        text = buildString {
            append("trigger delay: ${formatMillis(timing.triggerDelayMillis)}")
            append("  ·  audio: ${formatMillis(timing.triggerToAudioMillis)}")
            append("  ·  UI: ${formatMillis(timing.triggerToUiMillis)}")
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

private fun founderTestSchedule(): WakeSchedule {
    val target = ZonedDateTime.now().plusMinutes(2).withNano(0)
    return WakeSchedule(
        id = WakeScheduleId("founder-test"),
        zoneId = target.zone,
        timesByDay = DayOfWeek.values().associateWith { target.toLocalTime() },
        revision = System.currentTimeMillis().coerceAtLeast(1),
    )
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun formatMillis(value: Long?): String = value?.let { "${it}ms" } ?: "—"
