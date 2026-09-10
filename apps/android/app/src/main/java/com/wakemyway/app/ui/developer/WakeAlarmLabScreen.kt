package com.wakemyway.app.ui.developer

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakemyway.app.WakeSchedulingBlocker
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.TimingSnapshot
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.character.AlfredCharacterLab
import com.wakemyway.app.preparation.TomorrowContractLab
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.wakeSchedulingBlocker
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.ZonedDateTime

@Composable
fun WakeAlarmLabScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    voiceWakeReadiness: VoiceWakeReadiness? = null,
    readinessRevision: Int = 0,
    onRepairWakeSystem: () -> Unit = {},
    onEnableVoiceReplies: () -> Unit = {},
) {
    val context = LocalContext.current
    val kernel = remember { AlarmKernel(context) }
    val timingTrace = remember { WakeTimingTrace(context) }
    var health by remember { mutableStateOf(kernel.health()) }
    var history by remember { mutableStateOf(timingTrace.history(HISTORY_LIMIT)) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(readinessRevision, voiceWakeReadiness) {
        health = kernel.reconcile()
        recordCapabilities(timingTrace, health)
        history = timingTrace.history(HISTORY_LIMIT)
    }

    val blocker = wakeSchedulingBlocker(health, voiceWakeReadiness)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),
    ) {
        WmwSecondaryAction(
            label = "Back to Tonight",
            onClick = onBack,
        )
        Text(
            modifier = Modifier.padding(top = 18.dp),
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
            text = "This lab uses the same preflight as production. It cannot arm an unsafe Voice Wake.",
            style = MaterialTheme.typography.bodySmall,
            color = WmwColors.QuietText,
        )

        Text(
            modifier = Modifier.padding(top = 28.dp),
            text = if (blocker == WakeSchedulingBlocker.NONE) {
                "Voice Wake preflight ready"
            } else {
                "Voice Wake preflight blocked"
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        HealthFacts(health, voiceWakeReadiness)

        when (blocker) {
            WakeSchedulingBlocker.ALARM_SYSTEM -> {
                OutlinedButton(
                    modifier = Modifier.padding(top = 14.dp),
                    onClick = onRepairWakeSystem,
                ) {
                    Text("Repair next alarm prerequisite")
                }
            }

            WakeSchedulingBlocker.VOICE_PERMISSION -> {
                OutlinedButton(
                    modifier = Modifier.padding(top = 14.dp),
                    onClick = onEnableVoiceReplies,
                ) {
                    Text("Enable microphone for voice replies")
                }
            }

            WakeSchedulingBlocker.VOICE_UNAVAILABLE -> {
                Text(
                    modifier = Modifier.padding(top = 14.dp),
                    text = "On-device speech recognition is unavailable. This build will not arm a Voice Wake silently without it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            WakeSchedulingBlocker.NONE -> Unit
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 18.dp),
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent().setClassName(
                            context.packageName,
                            "com.wakemyway.app.voice.VoiceSpikeActivity",
                        ),
                    )
                }.onFailure {
                    message = "Live conversation setup is available in founder/debug builds only."
                }
            },
        ) {
            Text("Configure live conversation")
        }

        Button(
            modifier = Modifier.padding(top = 12.dp),
            onClick = {
                // Re-evaluate immediately before commit. UI state is never authority for safety.
                val currentHealth = kernel.health()
                if (wakeSchedulingBlocker(currentHealth, voiceWakeReadiness) != WakeSchedulingBlocker.NONE) {
                    health = currentHealth
                    message = "Preflight changed. Repair required before the lab can schedule."
                    return@Button
                }

                runCatching { kernel.commitSchedule(founderTestSchedule()) }
                    .onSuccess { committedHealth ->
                        health = committedHealth
                        committedHealth.nextOccurrence?.let { occurrence ->
                            timingTrace.expected(
                                occurrence = occurrence,
                                scenario = WakeTimingTrace.SCENARIO_NORMAL_T_PLUS_2M,
                                expectFullScreen = committedHealth.fullScreenIntentAllowed,
                            )
                        }
                        recordCapabilities(timingTrace, committedHealth)
                        history = timingTrace.history(HISTORY_LIMIT)
                        message = "One-shot production-path wake scheduled for about 2 minutes from now. Lock the phone."
                    }
                    .onFailure {
                        health = kernel.health()
                        message = "Could not schedule: ${it.message ?: it::class.simpleName}"
                    }
            },
            enabled = blocker == WakeSchedulingBlocker.NONE,
        ) {
            Text("Run one-shot T+2m wake")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 10.dp),
            onClick = {
                health = kernel.reconcile()
                recordCapabilities(timingTrace, health)
                history = timingTrace.history(HISTORY_LIMIT)
                message = health.detail
            },
        ) {
            Text("Refresh evidence")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 10.dp),
            onClick = {
                val report = timingTrace.reportText()
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Wake My Way reliability report")
                            putExtra(Intent.EXTRA_TEXT, report)
                        },
                        "Share reliability report",
                    ),
                )
            },
            enabled = history.isNotEmpty(),
        ) {
            Text("Share reliability report")
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

        message?.let {
            Text(
                modifier = Modifier.padding(top = 18.dp),
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        TomorrowContractLab(
            wakeOccurrence = health.nextOccurrence,
            modifier = Modifier.padding(top = 32.dp),
        )

        AlfredCharacterLab(modifier = Modifier.padding(top = 32.dp))

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
private fun HealthFacts(
    health: AlarmHealth,
    voiceWakeReadiness: VoiceWakeReadiness?,
) {
    Text(
        modifier = Modifier.padding(top = 10.dp),
        text = health.detail,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.secondary,
    )
    Text(
        modifier = Modifier.padding(top = 8.dp),
        text = buildString {
            append("Exact alarm: ${yesNo(health.exactAlarmAllowed)}")
            append("  ·  Notifications: ${yesNo(health.notificationsAllowed)}")
            append("  ·  HIGH channel: ${yesNo(health.notificationChannelHighImportance)}")
            append("  ·  Full screen: ${yesNo(health.fullScreenIntentAllowed)}")
        },
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = "Voice replies: ${voiceWakeReadiness?.name?.lowercase() ?: "unknown"}",
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
    if (timing.events.isNotEmpty()) {
        Text(
            modifier = Modifier.padding(top = 3.dp),
            text = timing.events.takeLast(5).joinToString(" → ") { it.type },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
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

private fun recordCapabilities(
    timingTrace: WakeTimingTrace,
    health: AlarmHealth,
) {
    val occurrence = health.nextOccurrence ?: health.activeOccurrence ?: return
    timingTrace.capabilities(
        occurrenceId = occurrence.id,
        exactAlarmAllowed = health.exactAlarmAllowed,
        notificationsAllowed = health.notificationsAllowed,
        fullScreenIntentAllowed = health.fullScreenIntentAllowed,
    )
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun formatMillis(value: Long?): String = value?.let { "${it}ms" } ?: "—"

private const val HISTORY_LIMIT = 6
