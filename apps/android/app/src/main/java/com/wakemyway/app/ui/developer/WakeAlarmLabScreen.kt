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
import androidx.compose.runtime.mutableIntStateOf
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
import com.wakemyway.app.product.learning.WakeLearningRepository
import com.wakemyway.app.product.learning.WakeLearningState
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.wakeSchedulingBlocker
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.ZonedDateTime

private data class PhysicalReliabilityScenario(
    val id: String,
    val label: String,
    val minutesFromNow: Long,
    val instruction: String,
)

private val PHYSICAL_RELIABILITY_SCENARIOS = listOf(
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_NORMAL_T_PLUS_2M,
        label = "Normal locked wake",
        minutesFromNow = 2,
        instruction = "Lock the phone and leave WakeMyWay in the background until the alarm fires.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_SNOOZE_REPLACEMENT,
        label = "Snooze replacement",
        minutesFromNow = 2,
        instruction = "When the wake fires, Snooze it. Confirm the replacement is durable before the current wake ends.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_STOP_RECREATION,
        label = "Stop resurrection",
        minutesFromNow = 2,
        instruction = "Stop the wake, then kill/recreate the app process and confirm the stopped occurrence never returns.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_SERVICE_RECREATION,
        label = "Active service recreation",
        minutesFromNow = 2,
        instruction = "After audio starts, recreate the app process/service without Force Stop. Confirm the active wake recovers.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_RECONCILE_TIME_CHANGE,
        label = "Clock / timezone change",
        minutesFromNow = 5,
        instruction = "After scheduling, change wall clock or timezone, reopen WakeMyWay, then verify reconciliation stays truthful.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_REBOOT_UNLOCKED,
        label = "Reboot then unlock",
        minutesFromNow = 5,
        instruction = "Reboot after scheduling, unlock before target time, and confirm the future wake is restored.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_DIRECT_BOOT,
        label = "Direct Boot",
        minutesFromNow = 5,
        instruction = "Reboot after scheduling and remain locked until the wake. No private credential-protected data may be required.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_EXACT_ALARM_UNAVAILABLE,
        label = "Exact-alarm capability loss",
        minutesFromNow = 5,
        instruction = "Schedule while ready, then remove exact-alarm access before target and verify readiness/recovery behavior is truthful.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_FULL_SCREEN_UNAVAILABLE,
        label = "Full-screen capability loss",
        minutesFromNow = 3,
        instruction = "Schedule while ready, then remove full-screen alarm access. Audio must remain safe and presentation degradation explicit.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_DOZE_IDLE,
        label = "Doze / idle",
        minutesFromNow = 5,
        instruction = "After scheduling, force or naturally enter idle/Doze. Keep the phone locked through the target.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_BLUETOOTH_ROUTE,
        label = "Bluetooth / audio route",
        minutesFromNow = 3,
        instruction = "Connect Bluetooth before target, then exercise the real wake and voice window while observing audible local fallback.",
    ),
    PhysicalReliabilityScenario(
        id = WakeTimingTrace.SCENARIO_MOTION_CALIBRATION,
        label = "Motion calibration",
        minutesFromNow = 2,
        instruction = "Use controlled pickup, rotation and real movement after the wake fires; note false positives or missed activation evidence.",
    ),
)

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
    val learningRepository = remember { WakeLearningRepository(context) }
    var health by remember { mutableStateOf(kernel.health()) }
    var learningState by remember { mutableStateOf(learningRepository.state()) }
    var history by remember { mutableStateOf(timingTrace.history(HISTORY_LIMIT)) }
    var message by remember { mutableStateOf<String?>(null) }
    var scenarioIndex by remember { mutableIntStateOf(0) }
    val scenario = PHYSICAL_RELIABILITY_SCENARIOS[scenarioIndex]

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

        Text(
            modifier = Modifier.padding(top = 24.dp),
            text = "Physical reliability scenario",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                scenarioIndex = (scenarioIndex + 1) % PHYSICAL_RELIABILITY_SCENARIOS.size
                message = null
            },
        ) {
            Text("${scenario.label} · ${scenario.minutesFromNow} min")
        }
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = scenario.instruction,
            style = MaterialTheme.typography.bodySmall,
            color = WmwColors.QuietText,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = "Scenario ID: ${scenario.id}",
            style = MaterialTheme.typography.labelSmall,
            color = WmwColors.QuietText,
        )

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

                runCatching {
                    kernel.commitSchedule(
                        founderTestSchedule(
                            scenarioId = scenario.id,
                            minutesFromNow = scenario.minutesFromNow,
                        ),
                    )
                }
                    .onSuccess { committedHealth ->
                        health = committedHealth
                        committedHealth.nextOccurrence?.let { occurrence ->
                            timingTrace.expected(
                                occurrence = occurrence,
                                scenario = scenario.id,
                                expectFullScreen = committedHealth.fullScreenIntentAllowed,
                            )
                        }
                        recordCapabilities(timingTrace, committedHealth)
                        history = timingTrace.history(HISTORY_LIMIT)
                        message =
                            "Scenario ${scenario.id} scheduled for about ${scenario.minutesFromNow} minutes from now. " +
                                scenario.instruction
                    }
                    .onFailure {
                        health = kernel.health()
                        message = "Could not schedule: ${it.message ?: it::class.simpleName}"
                    }
            },
            enabled = blocker == WakeSchedulingBlocker.NONE,
        ) {
            Text("Run ${scenario.label}")
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

        WakeLearningLab(
            state = learningState,
            onRefresh = { learningState = learningRepository.state() },
            onReset = {
                learningState = learningRepository.resetToDefault()
                message = "Learned strategy reset to the stable default. Wake history was preserved."
            },
            modifier = Modifier.padding(top = 32.dp),
        )

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
private fun WakeLearningLab(
    state: WakeLearningState,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Wake Learning",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "Policy v${state.policy.version} · ${state.evidenceSessionCount} evidence sessions",
            style = MaterialTheme.typography.bodySmall,
            color = WmwColors.QuietText,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = state.latestExplanation,
            style = MaterialTheme.typography.bodySmall,
            color = WmwColors.QuietText,
        )
        state.lastAdjustment?.let { adjustment ->
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = adjustment,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(
            modifier = Modifier.padding(top = 10.dp),
            onClick = onRefresh,
        ) {
            Text("Refresh learned strategy")
        }
        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = onReset,
            enabled = state.hasLearnedAdjustment,
        ) {
            Text("Reset learned strategy")
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = "Reset keeps Wake history and calibration. It removes only the learned-policy snapshot.",
            style = MaterialTheme.typography.labelSmall,
            color = WmwColors.QuietText,
        )
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

private fun founderTestSchedule(
    scenarioId: String,
    minutesFromNow: Long,
): WakeSchedule {
    val target = ZonedDateTime.now().plusMinutes(minutesFromNow).withNano(0)
    val scenarioSlug = scenarioId.lowercase().replace('_', '-')
    return WakeSchedule(
        id = WakeScheduleId("lab-$scenarioSlug-${System.currentTimeMillis()}"),
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
