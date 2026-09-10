package com.wakemyway.app.ui.setup

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakemyway.app.R
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.components.WmwStatusPill
import com.wakemyway.app.ui.components.WmwTimeDisplay
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.schedule.WakeCompletionPolicy
import com.wakemyway.core.schedule.WakeSchedule
import com.wakemyway.core.schedule.WakeScheduleId
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class WakeSetupMode {
    TOMORROW_ONLY,
    WEEKLY,
}

data class WakeSetupCommitResult(
    val committed: Boolean,
    val detail: String? = null,
)

@Composable
fun WakeSetupScreen(
    existingSchedule: WakeSchedule?,
    onBack: () -> Unit,
    onCommit: (WakeSchedule) -> WakeSetupCommitResult,
    onDisable: () -> WakeSetupCommitResult,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val zoneId = ZoneId.systemDefault()
    val tomorrow = ZonedDateTime.now(zoneId).plusDays(1)
    val initialMode = if (existingSchedule?.completionPolicy == WakeCompletionPolicy.RECURRING) {
        WakeSetupMode.WEEKLY
    } else {
        WakeSetupMode.TOMORROW_ONLY
    }
    val initialTomorrowTime = existingSchedule
        ?.takeIf { it.completionPolicy == WakeCompletionPolicy.ONE_SHOT }
        ?.timesByDay
        ?.values
        ?.firstOrNull()
        ?: LocalTime.of(8, 0)
    val initialWeeklyTimes = existingSchedule
        ?.takeIf { it.completionPolicy == WakeCompletionPolicy.RECURRING }
        ?.timesByDay
        ?.toMap()
        ?: weekdayDefaults()

    var mode by remember(existingSchedule?.revision) { mutableStateOf(initialMode) }
    var tomorrowTime by remember(existingSchedule?.revision) { mutableStateOf(initialTomorrowTime) }
    var weeklyTimes by remember(existingSchedule?.revision) { mutableStateOf(initialWeeklyTimes) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDisableConfirmation by remember { mutableStateOf(false) }

    fun pickTime(initial: LocalTime, onSelected: (LocalTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onSelected(LocalTime.of(hour, minute)) },
            initial.hour,
            initial.minute,
            true,
        ).show()
    }

    WmwCircadianSurface(
        stage = WmwCircadianStage.ENGAGED,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Xl, vertical = WmwSpacing.Lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.setup_back))
                }
                WmwStatusPill(
                    label = stringResource(R.string.setup_local_badge),
                    positive = true,
                )
            }

            Text(
                text = stringResource(R.string.setup_title),
                modifier = Modifier.padding(top = WmwSpacing.Xl),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.WarmLight,
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.QuietText,
            )

            WmwCard(modifier = Modifier.padding(top = WmwSpacing.Xxl)) {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                    Text(
                        text = stringResource(R.string.setup_pattern_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = WmwColors.QuietText,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Sm),
                    ) {
                        PatternButton(
                            label = stringResource(R.string.setup_just_tomorrow),
                            selected = mode == WakeSetupMode.TOMORROW_ONLY,
                            onClick = { mode = WakeSetupMode.TOMORROW_ONLY },
                            modifier = Modifier.weight(1f),
                        )
                        PatternButton(
                            label = stringResource(R.string.setup_repeat_weekly),
                            selected = mode == WakeSetupMode.WEEKLY,
                            onClick = { mode = WakeSetupMode.WEEKLY },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            when (mode) {
                WakeSetupMode.TOMORROW_ONLY -> {
                    WmwCard(modifier = Modifier.padding(top = WmwSpacing.Sm)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tomorrow.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                                style = MaterialTheme.typography.labelLarge,
                                color = WmwColors.QuietText,
                            )
                            WmwTimeDisplay(
                                time = tomorrowTime.format(TIME_FORMAT),
                                modifier = Modifier.padding(top = WmwSpacing.Xs),
                            )
                            OutlinedButton(
                                onClick = { pickTime(tomorrowTime) { tomorrowTime = it } },
                                modifier = Modifier.padding(top = WmwSpacing.Sm),
                            ) {
                                Text(stringResource(R.string.setup_change_time))
                            }
                            Text(
                                text = stringResource(R.string.setup_tomorrow_hint),
                                modifier = Modifier.padding(top = WmwSpacing.Md),
                                style = MaterialTheme.typography.bodySmall,
                                color = WmwColors.QuietText,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                WakeSetupMode.WEEKLY -> {
                    WmwCard(modifier = Modifier.padding(top = WmwSpacing.Sm)) {
                        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
                            Text(
                                text = stringResource(R.string.setup_weekly_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = WmwColors.QuietText,
                            )
                            DayOfWeek.values().forEach { day ->
                                val selectedTime = weeklyTimes[day]
                                WeeklyDayRow(
                                    day = day,
                                    time = selectedTime,
                                    locale = locale,
                                    onEnabledChange = { enabled ->
                                        weeklyTimes = if (enabled) {
                                            weeklyTimes + (day to fallbackWeeklyTime(weeklyTimes))
                                        } else {
                                            weeklyTimes - day
                                        }
                                    },
                                    onPickTime = {
                                        val initial = selectedTime ?: fallbackWeeklyTime(weeklyTimes)
                                        pickTime(initial) { chosen ->
                                            weeklyTimes = weeklyTimes + (day to chosen)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.SoftEmber,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(WmwSpacing.Xl))

            WmwPrimaryAction(
                label = stringResource(
                    if (existingSchedule == null) R.string.setup_save_new else R.string.setup_save_changes,
                ),
                enabled = mode != WakeSetupMode.WEEKLY || weeklyTimes.isNotEmpty(),
                onClick = {
                    val schedule = WakeSchedule(
                        id = existingSchedule?.id ?: WakeScheduleId(PRIMARY_SCHEDULE_ID),
                        zoneId = zoneId,
                        timesByDay = when (mode) {
                            WakeSetupMode.TOMORROW_ONLY -> mapOf(tomorrow.dayOfWeek to tomorrowTime)
                            WakeSetupMode.WEEKLY -> weeklyTimes.toMap()
                        },
                        revision = (existingSchedule?.revision ?: 0L) + 1L,
                        completionPolicy = when (mode) {
                            WakeSetupMode.TOMORROW_ONLY -> WakeCompletionPolicy.ONE_SHOT
                            WakeSetupMode.WEEKLY -> WakeCompletionPolicy.RECURRING
                        },
                    )
                    val result = runCatching { onCommit(schedule) }
                        .getOrElse { WakeSetupCommitResult(false, it.message) }
                    if (result.committed) {
                        onBack()
                    } else {
                        errorMessage = result.detail ?: context.getString(R.string.setup_save_failed)
                    }
                },
            )

            if (existingSchedule != null) {
                WmwSecondaryAction(
                    label = stringResource(R.string.setup_turn_off),
                    onClick = { showDisableConfirmation = true },
                    modifier = Modifier.padding(top = WmwSpacing.Xs),
                )
            }

            Text(
                text = stringResource(R.string.setup_reliability_note),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Md),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.QuietText,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(WmwSpacing.Xxl))
        }
    }

    if (showDisableConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisableConfirmation = false },
            title = { Text(stringResource(R.string.setup_disable_title)) },
            text = { Text(stringResource(R.string.setup_disable_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val result = runCatching(onDisable)
                            .getOrElse { WakeSetupCommitResult(false, it.message) }
                        if (result.committed) {
                            showDisableConfirmation = false
                            onBack()
                        } else {
                            showDisableConfirmation = false
                            errorMessage = result.detail ?: context.getString(R.string.setup_disable_failed)
                        }
                    },
                ) {
                    Text(stringResource(R.string.setup_disable_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirmation = false }) {
                    Text(stringResource(R.string.setup_disable_cancel))
                }
            },
        )
    }
}

@Composable
private fun PatternButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) WmwColors.SoftEmber else WmwColors.QuietText.copy(alpha = 0.28f),
        ),
    ) {
        Text(
            text = label,
            color = if (selected) WmwColors.WarmLight else WmwColors.MorningPaper,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WeeklyDayRow(
    day: DayOfWeek,
    time: LocalTime?,
    locale: Locale,
    onEnabledChange: (Boolean) -> Unit,
    onPickTime: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Sm),
    ) {
        Switch(
            checked = time != null,
            onCheckedChange = onEnabledChange,
        )
        Text(
            text = day.getDisplayName(TextStyle.FULL, locale),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (time != null) WmwColors.MorningPaper else WmwColors.QuietText,
        )
        TextButton(
            onClick = onPickTime,
            enabled = time != null,
        ) {
            Text(time?.format(TIME_FORMAT) ?: "--:--")
        }
    }
}

private fun weekdayDefaults(): Map<DayOfWeek, LocalTime> =
    listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    ).associateWith { LocalTime.of(8, 0) }

private fun fallbackWeeklyTime(times: Map<DayOfWeek, LocalTime>): LocalTime =
    times.values.firstOrNull() ?: LocalTime.of(8, 0)

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val PRIMARY_SCHEDULE_ID = "primary-wake"

@Preview(
    name = "Wake setup weekly",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
)
@Composable
private fun WakeSetupWeeklyPreview() {
    WakeMyWayTheme {
        WakeSetupScreen(
            existingSchedule = WakeSchedule(
                id = WakeScheduleId(PRIMARY_SCHEDULE_ID),
                zoneId = ZoneId.of("Europe/Berlin"),
                timesByDay = mapOf(
                    DayOfWeek.MONDAY to LocalTime.of(7, 45),
                    DayOfWeek.TUESDAY to LocalTime.of(7, 45),
                    DayOfWeek.WEDNESDAY to LocalTime.of(8, 15),
                    DayOfWeek.THURSDAY to LocalTime.of(7, 45),
                    DayOfWeek.FRIDAY to LocalTime.of(8, 0),
                ),
                revision = 2,
            ),
            onBack = {},
            onCommit = { WakeSetupCommitResult(true) },
            onDisable = { WakeSetupCommitResult(true) },
        )
    }
}
