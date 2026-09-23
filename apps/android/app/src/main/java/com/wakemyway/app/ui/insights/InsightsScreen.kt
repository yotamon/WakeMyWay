package com.wakemyway.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wakemyway.app.product.history.WakeHistoryTerminalReason
import com.wakemyway.app.product.learning.WakeLearningState
import com.wakemyway.core.learning.WakeCalibrationOutcome
import com.wakemyway.core.schedule.WakeOccurrenceId
import com.wakemyway.app.product.insights.WakeInsightsPeriod
import com.wakemyway.app.product.insights.WakeInsightsSummary
import com.wakemyway.app.product.insights.WakeMorningInsight
import com.wakemyway.app.ui.components.WmwBrandHeader
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(
    summary: WakeInsightsSummary,
    learningState: WakeLearningState,
    onPeriodSelected: (WakeInsightsPeriod) -> Unit,
    onCalibrateMorning: (WakeOccurrenceId, WakeCalibrationOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Lg, bottom = WmwSpacing.Xl),
        ) {
            WmwBrandHeader()
            Text(
                text = "WakeMyWay is learning your mornings.",
                modifier = Modifier.padding(top = 28.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "See what actually happened, confirm whether the wake stuck, and understand what changes for next time.",
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )

            PeriodSelector(
                selected = summary.period,
                onSelected = onPeriodSelected,
                modifier = Modifier.padding(top = WmwSpacing.Xl),
            )

            if (summary.totalMorningCount == 0) {
                EmptyInsights(modifier = Modifier.padding(top = WmwSpacing.Lg))
                return@Column
            }

            summary.pendingCalibration?.let { morning ->
                MorningCheckInCard(
                    morning = morning,
                    onCalibrate = { outcome ->
                        onCalibrateMorning(morning.finalOccurrenceId, outcome)
                    },
                    modifier = Modifier.padding(top = WmwSpacing.Lg),
                )
            }

            LearningCard(
                state = learningState,
                modifier = Modifier.padding(top = WmwSpacing.Md),
            )

            WmwCard(
                modifier = Modifier.padding(top = WmwSpacing.Lg),
                onLightSurface = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Lg)) {
                    Text(
                        text = "MORNING RHYTHM",
                        style = MaterialTheme.typography.labelSmall,
                        color = WmwColors.DawnDeep,
                    )
                    Row(Modifier.fillMaxWidth()) {
                        SummaryMetric(
                            value = summary.totalMorningCount.toString(),
                            label = "Mornings",
                            modifier = Modifier.weight(1f),
                        )
                        SummaryMetric(
                            value = "${summary.completedMorningCount}/${summary.totalMorningCount}",
                            label = "Activation completed",
                            modifier = Modifier.weight(1f),
                        )
                        SummaryMetric(
                            value = "${summary.snoozedMorningCount}/${summary.totalMorningCount}",
                            label = "Used Snooze",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    MorningChainChart(
                        mornings = summary.mornings.take(7).reversed(),
                        locale = locale,
                    )
                    Text(
                        text = "Each bar is one morning. Taller bars mean more Snoozes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.LightQuietText,
                    )
                }
            }

            WmwCard(
                modifier = Modifier.padding(top = WmwSpacing.Md),
                onLightSurface = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                    Text(
                        text = "INTERACTIVE PACE",
                        style = MaterialTheme.typography.labelSmall,
                        color = WmwColors.DawnDeep,
                    )
                    TimingMetric(
                        title = "First engagement",
                        duration = summary.averageFirstEngagement,
                        samples = summary.firstEngagementSampleCount,
                    )
                    TimingMetric(
                        title = "Activation Completion",
                        duration = summary.averageActivationCompletion,
                        samples = summary.activationCompletionSampleCount,
                    )
                    val escalationValue = if (summary.comparableBehaviorSessionCount > 0) {
                        "${summary.escalatedBehaviorSessionCount}/${summary.comparableBehaviorSessionCount}"
                    } else {
                        "—"
                    }
                    InsightValueRow(
                        title = "Needed escalation",
                        value = escalationValue,
                        detail = if (summary.comparableBehaviorSessionCount > 0) {
                            "interactive wake sessions"
                        } else {
                            "No comparable sessions yet"
                        },
                    )
                    Text(
                        text = "Timing starts when an interactive Wake session begins. Each Snooze starts a new session, so timing stays session-based.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.LightQuietText,
                    )
                }
            }

            Text(
                text = "RECENT MORNINGS",
                modifier = Modifier.padding(top = WmwSpacing.Xl, bottom = WmwSpacing.Sm),
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.LightQuietText,
            )
            WmwCard(onLightSurface = true) {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                    summary.mornings.take(7).forEachIndexed { index, morning ->
                        RecentMorningRow(
                            morning = morning,
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter,
                        )
                        if (index != summary.mornings.take(7).lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(WmwColors.DarkHairline),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: WakeInsightsPeriod,
    onSelected: (WakeInsightsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
    ) {
        WakeInsightsPeriod.entries.forEach { period ->
            val active = period == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = active,
                        onClick = { onSelected(period) },
                        role = Role.RadioButton,
                    ),
                shape = RoundedCornerShape(100.dp),
                color = if (active) WmwColors.Midnight else WmwColors.PaperCard.copy(alpha = 0.82f),
                border = if (active) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    WmwColors.DarkHairline,
                ),
            ) {
                Text(
                    text = period.label,
                    modifier = Modifier.padding(vertical = 11.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) WmwColors.WarmLight else WmwColors.LightQuietText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MorningCheckInCard(
    morning: WakeMorningInsight,
    onCalibrate: (WakeCalibrationOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCard(modifier = modifier, onLightSurface = true) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
            Text(
                text = "MORNING CHECK-IN",
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.DawnDeep,
            )
            Text(
                text = if (morning.finalReason == WakeHistoryTerminalReason.STOPPED) {
                    "You stopped the wake early. Did you stay up?"
                } else {
                    "Did this wake actually stick?"
                },
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "This one-tap check helps WakeMyWay distinguish phone-observed activation from a morning that really worked.",
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightQuietText,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
            ) {
                CalibrationChoice(
                    label = "I'm up",
                    modifier = Modifier.weight(1f),
                    onClick = { onCalibrate(WakeCalibrationOutcome.GOT_UP) },
                )
                CalibrationChoice(
                    label = "Back to bed",
                    modifier = Modifier.weight(1f),
                    onClick = { onCalibrate(WakeCalibrationOutcome.RETURNED_TO_BED) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
            ) {
                CalibrationChoice(
                    label = "Up later",
                    modifier = Modifier.weight(1f),
                    onClick = { onCalibrate(WakeCalibrationOutcome.GOT_UP_LATER) },
                )
                CalibrationChoice(
                    label = "Skip",
                    modifier = Modifier.weight(1f),
                    quiet = true,
                    onClick = { onCalibrate(WakeCalibrationOutcome.SKIPPED) },
                )
            }
        }
    }
}

@Composable
private fun CalibrationChoice(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quiet: Boolean = false,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(100.dp),
        color = if (quiet) WmwColors.LightSurfaceMuted else WmwColors.Midnight,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = WmwSpacing.Sm, vertical = 11.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (quiet) WmwColors.LightQuietText else WmwColors.WarmLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun LearningCard(
    state: WakeLearningState,
    modifier: Modifier = Modifier,
) {
    WmwCard(modifier = modifier, onLightSurface = true) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
            Text(
                text = "WHAT WAKEMYWAY LEARNED",
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.DawnDeep,
            )
            Text(
                text = when {
                    state.changedOnLatestRefresh -> "Your next wake has a small adjustment."
                    state.hasLearnedAdjustment -> "Your wake strategy is holding steady."
                    else -> "Learning how your mornings work."
                },
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = state.lastAdjustment ?: when {
                    state.evidenceSessionCount < 4 ->
                        "${state.evidenceSessionCount}/4 comparable wake sessions collected before the first bounded adjustment can be considered."
                    else ->
                        "Recent evidence supports the current strategy, so WakeMyWay is not adding friction just to make a metric move."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )
            Text(
                text = "Current strategy · v${state.policy.version}",
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.LightQuietText,
            )
        }
    }
}

@Composable
private fun EmptyInsights(modifier: Modifier = Modifier) {
    WmwCard(modifier = modifier, onLightSurface = true) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
            Text(
                text = "YOUR HISTORY STARTS HERE",
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.DawnDeep,
            )
            Text(
                text = "After your first tracked wake, this page will show the morning exactly as WakeMyWay observed it.",
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "Missing evidence stays unknown. WakeMyWay will not turn absent engagement, a missing sleep signal, or Health Connect permission into a zero or a score.",
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = WmwColors.Midnight,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WmwColors.LightQuietText,
        )
    }
}

@Composable
private fun MorningChainChart(
    mornings: List<WakeMorningInsight>,
    locale: Locale,
) {
    if (mornings.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        mornings.forEach { morning ->
            val barHeight = (20 + morning.snoozeCount.coerceAtMost(4) * 11).dp
            val barColor = when (morning.finalReason) {
                WakeHistoryTerminalReason.COMPLETED -> WmwColors.Sunrise
                WakeHistoryTerminalReason.STOPPED -> WmwColors.DawnDeep
                WakeHistoryTerminalReason.SNOOZED -> WmwColors.GoldenLight
                WakeHistoryTerminalReason.UNRECOVERABLE -> WmwColors.Danger
            }
            val accessibleOutcome = when (morning.finalReason) {
                WakeHistoryTerminalReason.COMPLETED -> "completed"
                WakeHistoryTerminalReason.STOPPED -> "stopped"
                WakeHistoryTerminalReason.SNOOZED -> "snoozed"
                WakeHistoryTerminalReason.UNRECOVERABLE -> "unrecoverable"
            }
            val accessibleDay = morning.scheduledLocalDateTime
                ?.dayOfWeek
                ?.getDisplayName(TextStyle.FULL, locale)
                ?: "Unknown day"
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription =
                            "$accessibleDay, ${morning.snoozeCount} snoozes, $accessibleOutcome"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.height(68.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(100.dp))
                            .background(barColor),
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = morning.scheduledLocalDateTime
                        ?.dayOfWeek
                        ?.getDisplayName(TextStyle.NARROW, locale)
                        ?: "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.LightQuietText,
                )
            }
        }
    }
}

@Composable
private fun TimingMetric(
    title: String,
    duration: Duration?,
    samples: Int,
) {
    InsightValueRow(
        title = title,
        value = duration?.let(::formatDuration) ?: "—",
        detail = if (samples > 0) {
            "$samples comparable session${if (samples == 1) "" else "s"}"
        } else {
            "No comparable sessions yet"
        },
    )
}

@Composable
private fun InsightValueRow(
    title: String,
    value: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.Midnight,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightQuietText,
            )
        }
        Text(
            text = value,
            modifier = Modifier.padding(start = WmwSpacing.Md),
            style = MaterialTheme.typography.titleLarge,
            color = WmwColors.Midnight,
        )
    }
}

@Composable
private fun RecentMorningRow(
    morning: WakeMorningInsight,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
) {
    val scheduleLabel = morning.scheduledLocalDateTime?.let { local ->
        "${dateFormatter.format(local)} · ${timeFormatter.format(local)}"
    } ?: "Earlier wake record"
    val outcome = when (morning.finalReason) {
        WakeHistoryTerminalReason.COMPLETED -> "Activation completed"
        WakeHistoryTerminalReason.STOPPED -> "Stopped"
        WakeHistoryTerminalReason.SNOOZED -> "Snoozed"
        WakeHistoryTerminalReason.UNRECOVERABLE -> "Unavailable"
    }
    val snoozeDetail = when (morning.snoozeCount) {
        0 -> "No Snooze"
        1 -> "1 Snooze"
        else -> "${morning.snoozeCount} Snoozes"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = scheduleLabel,
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.Midnight,
            )
            Text(
                text = snoozeDetail,
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightQuietText,
            )
        }
        Surface(
            modifier = Modifier.padding(start = WmwSpacing.Md),
            shape = RoundedCornerShape(100.dp),
            color = when (morning.finalReason) {
                WakeHistoryTerminalReason.COMPLETED -> WmwColors.SunriseSoft.copy(alpha = 0.35f)
                WakeHistoryTerminalReason.STOPPED -> WmwColors.Dawn.copy(alpha = 0.28f)
                WakeHistoryTerminalReason.SNOOZED -> WmwColors.GoldenLight.copy(alpha = 0.36f)
                WakeHistoryTerminalReason.UNRECOVERABLE -> WmwColors.Danger.copy(alpha = 0.12f)
            },
        ) {
            Text(
                text = outcome,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.Midnight,
            )
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes <= 0 -> "${seconds}s"
        seconds == 0L -> "${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}
