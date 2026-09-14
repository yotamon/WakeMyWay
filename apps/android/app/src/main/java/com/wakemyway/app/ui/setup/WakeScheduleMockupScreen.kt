package com.wakemyway.app.ui.setup

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakemyway.app.R
import com.wakemyway.app.ui.components.WmwActionTone
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
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

private enum class MockupScheduleMode { TOMORROW, WEEKLY }

@Composable
fun WakeScheduleMockupScreen(
    existingSchedule: WakeSchedule?,
    onBack: () -> Unit,
    onCommit: (WakeSchedule) -> WakeSetupCommitResult,
    onDisable: () -> WakeSetupCommitResult,
    modifier: Modifier = Modifier,
    onWakeAccessRequired: () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val saveFailedCopy = stringResource(R.string.setup_save_failed)
    val disableFailedCopy = stringResource(R.string.setup_disable_failed)
    val zoneId = ZoneId.systemDefault()
    val tomorrow = ZonedDateTime.now(zoneId).plusDays(1)
    val initialMode = if (existingSchedule?.completionPolicy == WakeCompletionPolicy.RECURRING) {
        MockupScheduleMode.WEEKLY
    } else {
        MockupScheduleMode.TOMORROW
    }
    val initialTomorrowTime = existingSchedule
        ?.takeIf { it.completionPolicy == WakeCompletionPolicy.ONE_SHOT }
        ?.timesByDay?.values?.firstOrNull() ?: LocalTime.of(7, 0)
    val initialWeeklyTimes = existingSchedule
        ?.takeIf { it.completionPolicy == WakeCompletionPolicy.RECURRING }
        ?.timesByDay?.toMap() ?: weekdayDefaults()

    var mode by remember(existingSchedule?.revision) { mutableStateOf(initialMode) }
    var tomorrowTime by remember(existingSchedule?.revision) { mutableStateOf(initialTomorrowTime) }
    var weeklyTimes by remember(existingSchedule?.revision) { mutableStateOf(initialWeeklyTimes) }
    var error by remember(existingSchedule?.revision) { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val dirty = existingSchedule == null || mode != initialMode ||
        tomorrowTime != initialTomorrowTime || weeklyTimes != initialWeeklyTimes

    fun pick(initial: LocalTime, selected: (LocalTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selected(LocalTime.of(hour, minute))
                error = null
            },
            initial.hour,
            initial.minute,
            true,
        ).show()
    }

    fun save() {
        val schedule = WakeSchedule(
            id = existingSchedule?.id ?: WakeScheduleId("primary-wake"),
            zoneId = zoneId,
            timesByDay = if (mode == MockupScheduleMode.TOMORROW) {
                mapOf(tomorrow.dayOfWeek to tomorrowTime)
            } else {
                weeklyTimes
            },
            revision = (existingSchedule?.revision ?: 0L) + 1L,
            completionPolicy = if (mode == MockupScheduleMode.TOMORROW) {
                WakeCompletionPolicy.ONE_SHOT
            } else {
                WakeCompletionPolicy.RECURRING
            },
        )
        val result = runCatching { onCommit(schedule) }
            .getOrElse { WakeSetupCommitResult(false, it.message) }
        if (result.committed) {
            onBack()
            if (!result.wakeReady) onWakeAccessRequired()
        } else {
            error = result.detail ?: saveFailedCopy
        }
    }

    WmwCircadianSurface(WmwCircadianStage.EMERGING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = WmwSpacing.Md, vertical = WmwSpacing.Xs),
        ) {
            Spacer(Modifier.height(60.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("‹", style = MaterialTheme.typography.headlineMedium, color = WmwColors.WarmLight)
                }
                Text(
                    stringResource(R.string.setup_local_badge).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WmwColors.FaintText,
                )
            }

            Text(
                stringResource(R.string.setup_title),
                modifier = Modifier.padding(horizontal = WmwSpacing.Xs),
                style = MaterialTheme.typography.headlineMedium,
                color = WmwColors.WarmLight,
            )
            Spacer(Modifier.height(46.dp))

            ScheduleModeCard(
                time = weeklyTimes.values.firstOrNull() ?: LocalTime.of(7, 30),
                subtitle = weeklySummary(weeklyTimes, locale),
                active = mode == MockupScheduleMode.WEEKLY,
                onActivate = { mode = MockupScheduleMode.WEEKLY },
                onTime = {
                    val current = weeklyTimes.values.firstOrNull() ?: LocalTime.of(7, 30)
                    pick(current) { chosen ->
                        weeklyTimes = if (weeklyTimes.isEmpty()) weekdayDefaults(chosen)
                        else weeklyTimes.keys.associateWith { chosen }
                    }
                },
            )

            Text(
                stringResource(R.string.setup_weekly_label),
                modifier = Modifier.padding(start = WmwSpacing.Xs, top = 27.dp, bottom = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.QuietText,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WmwSpacing.Xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DayOfWeek.values().forEach { day ->
                    val selected = day in weeklyTimes
                    DayCircle(
                        day.getDisplayName(TextStyle.NARROW, locale).uppercase(),
                        selected,
                    ) {
                        mode = MockupScheduleMode.WEEKLY
                        weeklyTimes = if (selected) weeklyTimes - day
                        else weeklyTimes + (day to (weeklyTimes.values.firstOrNull() ?: LocalTime.of(7, 30)))
                    }
                }
            }

            Spacer(Modifier.height(54.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WmwColors.ElevatedNightSurface.copy(alpha = 0.84f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(0.75.dp, WmwColors.Hairline.copy(alpha = 0.42f)),
            ) {
                Column {
                    TomorrowRow(
                        time = tomorrowTime,
                        active = mode == MockupScheduleMode.TOMORROW,
                        onActivate = { mode = MockupScheduleMode.TOMORROW },
                        onTime = { pick(tomorrowTime) { tomorrowTime = it } },
                    )
                    Divider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .padding(horizontal = WmwSpacing.Md),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.setup_pattern_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = WmwColors.QuietText,
                        )
                        Text(
                            if (mode == MockupScheduleMode.TOMORROW) {
                                stringResource(R.string.setup_just_tomorrow)
                            } else {
                                weeklySummary(weeklyTimes, locale)
                            },
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = WmwColors.WarmLight,
                        )
                    }
                    if (existingSchedule != null) {
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clickable { confirmDelete = true }
                                .padding(horizontal = WmwSpacing.Md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⊘", color = WmwColors.Danger)
                            Text(
                                stringResource(R.string.setup_turn_off),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = WmwSpacing.Sm),
                                style = MaterialTheme.typography.bodySmall,
                                color = WmwColors.Danger,
                            )
                            Text("›", color = WmwColors.WarmLight)
                        }
                    }
                }
            }

            error?.let {
                Text(
                    it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WmwSpacing.Md),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.SoftEmber,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))
            if (dirty) {
                WmwPrimaryAction(
                    label = stringResource(
                        if (existingSchedule == null) R.string.setup_save_new else R.string.setup_save_changes,
                    ),
                    onClick = ::save,
                    enabled = mode != MockupScheduleMode.WEEKLY || weeklyTimes.isNotEmpty(),
                    tone = WmwActionTone.WARM,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.setup_disable_title)) },
            text = { Text(stringResource(R.string.setup_disable_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val result = runCatching(onDisable)
                        .getOrElse { WakeSetupCommitResult(false, it.message) }
                    confirmDelete = false
                    if (result.committed) onBack()
                    else error = result.detail ?: disableFailedCopy
                }) {
                    Text(stringResource(R.string.setup_disable_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.setup_disable_cancel))
                }
            },
        )
    }
}

@Composable
private fun ScheduleModeCard(
    time: LocalTime,
    subtitle: String,
    active: Boolean,
    onActivate: () -> Unit,
    onTime: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(137.dp),
        color = WmwColors.ElevatedNightSurface.copy(alpha = if (active) 0.94f else 0.62f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(0.75.dp, WmwColors.Hairline.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.padding(WmwSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = active, onClick = onTime),
            ) {
                Text(
                    time.format(TIME_FORMAT),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (active) WmwColors.WarmLight else WmwColors.QuietText,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WmwColors.QuietText)
            }
            Switch(
                checked = active,
                onCheckedChange = { if (it) onActivate() },
                colors = switchColors(),
            )
        }
    }
}

@Composable
private fun TomorrowRow(
    time: LocalTime,
    active: Boolean,
    onActivate: () -> Unit,
    onTime: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .padding(horizontal = WmwSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(enabled = active, onClick = onTime),
        ) {
            Text(stringResource(R.string.setup_just_tomorrow), style = MaterialTheme.typography.bodyMedium, color = WmwColors.WarmLight)
            Text(
                time.format(TIME_FORMAT),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) WmwColors.WarmLight else WmwColors.QuietText,
            )
        }
        Switch(
            checked = active,
            onCheckedChange = { if (it) onActivate() },
            colors = switchColors(),
        )
    }
}

@Composable
private fun DayCircle(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) WmwColors.SoftEmber else WmwColors.ElevatedNightSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) WmwColors.Ink else WmwColors.QuietText,
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.75.dp)
            .background(WmwColors.Hairline.copy(alpha = 0.58f)),
    )
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = WmwColors.WarmLight,
    checkedTrackColor = WmwColors.Sage.copy(alpha = 0.66f),
    uncheckedThumbColor = WmwColors.QuietText,
    uncheckedTrackColor = WmwColors.DeepDawn,
)

private fun weeklySummary(times: Map<DayOfWeek, LocalTime>, locale: Locale): String {
    val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    return when {
        times.isEmpty() -> "No days selected"
        times.keys == weekdays -> "Weekdays"
        times.keys.size == 7 -> "Every day"
        else -> times.keys.sortedBy { it.value }
            .joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}

private fun weekdayDefaults(time: LocalTime = LocalTime.of(7, 30)): Map<DayOfWeek, LocalTime> =
    listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        .associateWith { time }

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
