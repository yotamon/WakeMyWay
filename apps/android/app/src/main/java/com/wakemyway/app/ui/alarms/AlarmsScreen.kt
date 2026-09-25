package com.wakemyway.app.ui.alarms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakemyway.app.alarm.AlarmScheduleHealth
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwPageHeader
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwStatusPill
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmSchedulePattern
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AlarmsScreen(
    alarms: List<AlarmDefinition>,
    healthFor: (AlarmDefinition) -> AlarmScheduleHealth?,
    onAddAlarm: () -> Unit,
    onEditAlarm: (AlarmDefinition) -> Unit,
    onSetEnabled: (AlarmDefinition, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]

    WmwCircadianSurface(
        stage = WmwCircadianStage.PLANNING,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
        ) {
            WmwPageHeader(
                title = "Your alarms",
                subtitle = "Each wake keeps its own rhythm, voice and morning plan.",
                trailing = { AddAlarmButton(onClick = onAddAlarm) },
            )

            Spacer(Modifier.height(WmwSpacing.Xl))
            if (alarms.isEmpty()) {
                EmptyAlarmState(onAddAlarm = onAddAlarm)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                    alarms.forEach { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            health = healthFor(alarm),
                            locale = locale,
                            onClick = { onEditAlarm(alarm) },
                            onSetEnabled = { enabled -> onSetEnabled(alarm, enabled) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmDefinition,
    health: AlarmScheduleHealth?,
    locale: Locale,
    onClick: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
) {
    val accessibleLabel = alarm.label.ifBlank { "Wake up" }

    WmwCard(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        onLightSurface = true,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = alarm.schedule.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (alarm.enabled) WmwColors.Midnight else WmwColors.LightFaintText,
                    )
                    Text(
                        text = alarm.label.ifBlank { "Wake up" },
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = WmwColors.Midnight,
                    )
                }
                Switch(
                    checked = alarm.enabled,
                    modifier = Modifier.semantics {
                        contentDescription = "Enable $accessibleLabel alarm"
                    },
                    onCheckedChange = onSetEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WmwColors.Midnight,
                        checkedTrackColor = WmwColors.Sunrise,
                        uncheckedThumbColor = WmwColors.LightQuietText,
                        uncheckedTrackColor = WmwColors.LightSurfaceMuted,
                    ),
                )
            }

            Text(
                text = scheduleSummary(alarm.schedule, locale),
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (alarm.voiceCheckInEnabled) {
                    MetadataChip("Alfred voice")
                } else {
                    Spacer(Modifier.size(1.dp))
                }
                when {
                    !alarm.enabled -> WmwStatusPill("Off", positive = false, onLightSurface = true)
                    health?.ready == true -> WmwStatusPill("Wake Ready", positive = true, onLightSurface = true)
                    else -> WmwStatusPill("Needs attention", positive = false, onLightSurface = true)
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(label: String) {
    Surface(
        shape = CircleShape,
        color = WmwColors.LightSurfaceMuted,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = WmwColors.LightQuietText,
        )
    }
}

@Composable
private fun AddAlarmButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Add alarm"
            },
        shape = CircleShape,
        color = WmwColors.Midnight,
        shadowElevation = 2.dp,
    ) {
        Canvas(Modifier.padding(12.dp)) {
            drawLine(
                color = WmwColors.WarmLight,
                start = Offset(size.width / 2f, 1.dp.toPx()),
                end = Offset(size.width / 2f, size.height - 1.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = WmwColors.WarmLight,
                start = Offset(1.dp.toPx(), size.height / 2f),
                end = Offset(size.width - 1.dp.toPx(), size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun EmptyAlarmState(onAddAlarm: () -> Unit) {
    WmwCard(onLightSurface = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No alarms yet",
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "Create a wake that sounds, speaks and behaves the way you want.",
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )
            Surface(
                modifier = Modifier
                    .padding(top = WmwSpacing.Lg)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onAddAlarm),
                shape = CircleShape,
                color = WmwColors.Sunrise,
            ) {
                Text(
                    text = "Create alarm",
                    modifier = Modifier.padding(horizontal = WmwSpacing.Lg, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = WmwColors.Midnight,
                )
            }
        }
    }
}

private fun scheduleSummary(
    schedule: AlarmSchedulePattern,
    locale: Locale,
): String = when (schedule) {
    is AlarmSchedulePattern.OneShot -> schedule.date.format(
        DateTimeFormatter.ofPattern("EEEE, d MMM", locale),
    )
    is AlarmSchedulePattern.Weekly -> weeklyDaysLabel(schedule.days, locale)
}

private fun weeklyDaysLabel(
    days: Set<DayOfWeek>,
    locale: Locale,
): String {
    val weekdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )
    return when (days) {
        DayOfWeek.entries.toSet() -> "Every day"
        weekdays -> "Weekdays"
        setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
        else -> days
            .sortedBy(DayOfWeek::getValue)
            .joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}
