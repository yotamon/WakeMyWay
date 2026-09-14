package com.wakemyway.app.ui.setup

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.wakemyway.app.ui.components.WmwBrandLockup
import com.wakemyway.app.ui.components.WmwCard
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

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg, vertical = WmwSpacing.Md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("‹", style = MaterialTheme.typography.headlineMedium, color = WmwColors.Midnight)
                }
                WmwBrandLockup(modifier = Modifier.padding(start = 2.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.setup_local_badge).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WmwColors.LightQuietText,
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                stringResource(R.string.setup_subtitle),
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )

            Spacer(Modifier.height(28.dp))
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
                stringResource(R.string.setup_weekly_label).uppercase(),
                modifier = Modifier.padding(top = WmwSpacing.Lg, bottom = WmwSpacing.Sm),
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.LightQuietText,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(WmwSpacing.Xl))
            WmwCard(onLightSurface = true) {
                Column {
                    TomorrowRow(
                        time = tomorrowTime,
                        active = mode == MockupScheduleMode.TOMORROW,
                        onActivate = { mode = MockupScheduleMode.TOMORROW },
                        onTime = { pick(tomorrowTime) { tomorrowTime = it } },
                    )
                    BrandedDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = WmwSpacing.Md),
                    ) {
                        Text(
                            stringResource(R.string.setup_pattern_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = WmwColors.LightQuietText,
                        )
                        Text(
                            if (mode == MockupScheduleMode.TOMORROW) {
                                stringResource(R.string.setup_just_tomorrow)
                            } else {
                                weeklySummary(weeklyTimes, locale)
                            },
                            modifier = Modifier.padding(top = 5.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.Midnight,
                        )
                    }
                    if (existingSchedule != null) {
                        BrandedDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { confirmDelete = true },
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
                            Text("›", color = WmwColors.LightQuietText)
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
                    color = WmwColors.Danger,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = stringResource(R.string.setup_reliability_note),
                modifier = Modifier.padding(horizontal = WmwSpacing.Md, vertical = WmwSpacing.Lg),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightFaintText,
                textAlign = TextAlign.Center,
            )

            if (dirty) {
                WmwPrimaryAction(
                    label = stringResource(
                        if (existingSchedule == null) R.string.setup_save_new else R.string.setup_save_changes,
                    ),
                    onClick = ::save,
                    enabled = mode != MockupScheduleMode.WEEKLY || weeklyTimes.isNotEmpty(),
                    onLightSurface = true,
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
                    Text(stringResource(R.string.setup_disable_confirm), color = WmwColors.Danger)
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
            .height(142.dp),
        color = if (active) WmwColors.PaperCard else WmwColors.LightSurfaceMuted,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            if (active) 1.25.dp else 0.75.dp,
            if (active) WmwColors.Sunrise.copy(alpha = 0.65f) else WmwColors.DarkHairline,
        ),
        shadowElevation = if (active) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(WmwSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = active, onClick = onTime),
            ) {
                Text(
                    stringResource(R.string.setup_repeat_weekly).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WmwColors.LightQuietText,
                )
                Text(
                    time.format(TIME_FORMAT),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (active) WmwColors.Midnight else WmwColors.LightQuietText,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WmwColors.LightQuietText)
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
            .height(82.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(enabled = active, onClick = onTime),
        ) {
            Text(
                stringResource(R.string.setup_just_tomorrow),
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.Midnight,
            )
            Text(
                time.format(TIME_FORMAT),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) WmwColors.Midnight else WmwColors.LightQuietText,
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
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) WmwColors.Sunrise else WmwColors.PaperCard,
        border = BorderStroke(0.75.dp, if (selected) WmwColors.Sunrise else WmwColors.DarkHairline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) WmwColors.Midnight else WmwColors.LightQuietText,
            )
        }
    }
}

@Composable
private fun BrandedDivider() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.75.dp),
        color = WmwColors.DarkHairline,
    ) {}
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = WmwColors.PaperCard,
    checkedTrackColor = WmwColors.Sunrise,
    uncheckedThumbColor = WmwColors.LightQuietText,
    uncheckedTrackColor = WmwColors.LightSurfaceMuted,
    uncheckedBorderColor = WmwColors.DarkHairline,
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
