package com.wakemyway.app.ui.alarms

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.components.WmwActionTone
import com.wakemyway.app.ui.components.WmwBrandLockup
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.alarm.AlarmDefinition
import com.wakemyway.core.alarm.AlarmDefinitionId
import com.wakemyway.core.alarm.AlarmSchedulePattern
import com.wakemyway.core.alarm.CharacterId
import com.wakemyway.core.alarm.SnoozePolicy
import com.wakemyway.core.alarm.TomorrowContractMode
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

private enum class EditorScheduleMode { WEEKLY, ONE_SHOT }

data class AlarmEditorResult(
    val saved: Boolean,
    val detail: String? = null,
)

@Composable
fun AlarmEditorScreen(
    existing: AlarmDefinition?,
    onBack: () -> Unit,
    onSave: (AlarmDefinition) -> AlarmEditorResult,
    onDelete: ((AlarmDefinition) -> AlarmEditorResult)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val nowDate = LocalDate.now()
    val initialMode = when (existing?.schedule) {
        is AlarmSchedulePattern.OneShot -> EditorScheduleMode.ONE_SHOT
        else -> EditorScheduleMode.WEEKLY
    }
    val initialTime = existing?.schedule?.time ?: LocalTime.of(7, 0)
    val initialDays = (existing?.schedule as? AlarmSchedulePattern.Weekly)?.days
        ?: setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
    val initialDate = (existing?.schedule as? AlarmSchedulePattern.OneShot)?.date
        ?: nowDate.plusDays(1)

    var label by remember(existing?.revision) { mutableStateOf(existing?.label.orEmpty()) }
    var mode by remember(existing?.revision) { mutableStateOf(initialMode) }
    var time by remember(existing?.revision) { mutableStateOf(initialTime) }
    var days by remember(existing?.revision) { mutableStateOf(initialDays) }
    var date by remember(existing?.revision) { mutableStateOf(initialDate) }
    var voiceCheckIn by remember(existing?.revision) {
        mutableStateOf(existing?.voiceCheckInEnabled ?: true)
    }
    var voiceStyle by remember(existing?.revision) {
        mutableStateOf(existing?.voiceStyle ?: VoiceStyle.DEFAULT)
    }
    var snoozeEnabled by remember(existing?.revision) {
        mutableStateOf(existing?.snoozePolicy?.enabled ?: true)
    }
    var snoozeMinutes by remember(existing?.revision) {
        mutableStateOf(existing?.snoozePolicy?.duration?.toMinutes()?.toInt() ?: 5)
    }
    var contractMode by remember(existing?.revision) {
        mutableStateOf(existing?.tomorrowContractMode ?: TomorrowContractMode.OPTIONAL)
    }
    var firstMove by remember(existing?.revision) {
        mutableStateOf(existing?.firstMoveDefault.orEmpty())
    }
    var error by remember(existing?.revision) { mutableStateOf<String?>(null) }

    fun chooseTime() {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                time = LocalTime.of(hour, minute)
                error = null
            },
            time.hour,
            time.minute,
            true,
        ).show()
    }

    fun chooseDate() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                date = LocalDate.of(year, month + 1, day)
                error = null
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1_000L
        }.show()
    }

    fun save() {
        if (mode == EditorScheduleMode.WEEKLY && days.isEmpty()) {
            error = "Choose at least one day."
            return
        }
        if (mode == EditorScheduleMode.ONE_SHOT && date.isBefore(LocalDate.now())) {
            error = "Choose a future date."
            return
        }
        val instant = Instant.now()
        val definition = AlarmDefinition(
            id = existing?.id ?: AlarmDefinitionId("alarm-${UUID.randomUUID()}"),
            label = label.trim(),
            enabled = existing?.enabled ?: true,
            zoneId = existing?.zoneId ?: ZoneId.systemDefault(),
            schedule = when (mode) {
                EditorScheduleMode.WEEKLY -> AlarmSchedulePattern.Weekly(days = days, time = time)
                EditorScheduleMode.ONE_SHOT -> AlarmSchedulePattern.OneShot(date = date, time = time)
            },
            // The branded sound selector is intentionally not exposed until the three WAV assets
            // are bundled and AlarmPlaybackService resolves this id at runtime.
            soundId = existing?.soundId ?: WakeSoundId.MORNING_LIGHT,
            voiceCheckInEnabled = voiceCheckIn,
            characterId = existing?.characterId ?: CharacterId.ALFRED,
            voiceStyle = voiceStyle,
            snoozePolicy = SnoozePolicy(
                enabled = snoozeEnabled,
                duration = Duration.ofMinutes(snoozeMinutes.toLong()),
                maxCount = existing?.snoozePolicy?.maxCount,
            ),
            tomorrowContractMode = contractMode,
            firstMoveDefault = firstMove.trim().ifBlank { null },
            revision = (existing?.revision ?: 0L) + 1L,
            createdAt = existing?.createdAt ?: instant,
            updatedAt = instant,
        )
        val result = runCatching { onSave(definition) }
            .getOrElse { AlarmEditorResult(saved = false, detail = it.message) }
        if (result.saved) onBack() else error = result.detail ?: "Could not save this alarm."
    }

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
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
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = if (existing == null) "Create alarm" else "Edit alarm",
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "Shape the whole wake, not just the time.",
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )

            EditorSection(title = "Basic", modifier = Modifier.padding(top = 28.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(AlarmDefinition.MAX_LABEL_CHARACTERS) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Label") },
                    placeholder = { Text("Morning wake") },
                    singleLine = true,
                )
                TimeRow(time = time, onClick = ::chooseTime)
                ChoiceRow(
                    options = EditorScheduleMode.entries,
                    selected = mode,
                    label = { selectedMode ->
                        when (selectedMode) {
                            EditorScheduleMode.WEEKLY -> "Weekly"
                            EditorScheduleMode.ONE_SHOT -> "One time"
                        }
                    },
                    onSelected = { mode = it },
                )
                if (mode == EditorScheduleMode.WEEKLY) {
                    DayPicker(days = days, locale = locale) { day ->
                        days = if (day in days) days - day else days + day
                    }
                } else {
                    SettingRow(
                        title = "Date",
                        detail = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                        onClick = ::chooseDate,
                    )
                }
            }

            EditorSection(title = "Voice", modifier = Modifier.padding(top = WmwSpacing.Md)) {
                ToggleSetting(
                    title = "Voice Check-In",
                    detail = "Alfred speaks with you and waits for a real response.",
                    checked = voiceCheckIn,
                    onCheckedChange = { voiceCheckIn = it },
                )
                if (voiceCheckIn) {
                    SettingRow(title = "Character", detail = "Alfred")
                    ChoiceRow(
                        options = VoiceStyle.entries,
                        selected = voiceStyle,
                        label = { style ->
                            when (style) {
                                VoiceStyle.DEFAULT -> "Default"
                                VoiceStyle.MOTIVATIONAL -> "Motivational"
                                VoiceStyle.MINIMAL -> "Minimal"
                            }
                        },
                        onSelected = { voiceStyle = it },
                    )
                }
            }

            EditorSection(title = "More", modifier = Modifier.padding(top = WmwSpacing.Md)) {
                ToggleSetting(
                    title = "Snooze",
                    detail = if (snoozeEnabled) "$snoozeMinutes minutes" else "Disabled",
                    checked = snoozeEnabled,
                    onCheckedChange = { snoozeEnabled = it },
                )
                if (snoozeEnabled) {
                    ChoiceRow(
                        options = listOf(5, 10, 15),
                        selected = snoozeMinutes,
                        label = { "$it min" },
                        onSelected = { snoozeMinutes = it },
                    )
                }

                Text(
                    text = "Tomorrow Contract",
                    style = MaterialTheme.typography.titleSmall,
                    color = WmwColors.Midnight,
                )
                ChoiceRow(
                    options = TomorrowContractMode.entries,
                    selected = contractMode,
                    label = { value ->
                        when (value) {
                            TomorrowContractMode.OPTIONAL -> "Optional"
                            TomorrowContractMode.ALWAYS_PROMPT -> "Prompt"
                            TomorrowContractMode.DISABLED -> "Off"
                        }
                    },
                    onSelected = { contractMode = it },
                )
                if (contractMode != TomorrowContractMode.DISABLED) {
                    OutlinedTextField(
                        value = firstMove,
                        onValueChange = { firstMove = it.take(AlarmDefinition.MAX_FIRST_MOVE_CHARACTERS) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default First Move") },
                        placeholder = { Text("Open the curtains") },
                        minLines = 2,
                    )
                }
            }

            error?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.Danger,
                )
            }

            WmwPrimaryAction(
                label = if (existing == null) "Create alarm" else "Save changes",
                onClick = ::save,
                modifier = Modifier.padding(top = WmwSpacing.Xl),
                onLightSurface = true,
                tone = WmwActionTone.WARM,
            )

            if (existing != null && onDelete != null) {
                TextButton(
                    onClick = {
                        val result = runCatching { onDelete(existing) }
                            .getOrElse { AlarmEditorResult(false, it.message) }
                        if (result.saved) onBack() else error = result.detail ?: "Could not delete this alarm."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Sm),
                ) {
                    Text("Delete alarm", color = WmwColors.Danger)
                }
            }
        }
    }
}

@Composable
private fun EditorSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(start = 2.dp, bottom = WmwSpacing.Sm),
            style = MaterialTheme.typography.labelSmall,
            color = WmwColors.LightQuietText,
        )
        WmwCard(onLightSurface = true) {
            Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                content()
            }
        }
    }
}

@Composable
private fun TimeRow(time: LocalTime, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = WmwColors.MorningPaper,
        border = BorderStroke(1.dp, WmwColors.DarkHairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WmwSpacing.Lg, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Wake time", style = MaterialTheme.typography.titleSmall, color = WmwColors.Midnight)
            Text(
                time.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                color = WmwColors.Midnight,
            )
        }
    }
}

@Composable
private fun DayPicker(
    days: Set<DayOfWeek>,
    locale: Locale,
    onToggle: (DayOfWeek) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DayOfWeek.entries.forEach { day ->
            val selected = day in days
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onToggle(day) },
                shape = CircleShape,
                color = if (selected) WmwColors.Midnight else WmwColors.LightSurfaceMuted,
                border = if (selected) null else BorderStroke(1.dp, WmwColors.DarkHairline),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        day.getDisplayName(TextStyle.NARROW, locale).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) WmwColors.WarmLight else WmwColors.LightQuietText,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = WmwColors.Midnight)
            Text(
                detail,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightQuietText,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WmwColors.Midnight,
                checkedTrackColor = WmwColors.Sunrise,
                uncheckedThumbColor = WmwColors.LightQuietText,
                uncheckedTrackColor = WmwColors.LightSurfaceMuted,
            ),
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    detail: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = WmwColors.Midnight)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = WmwColors.LightQuietText)
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
    ) {
        options.forEach { option ->
            val active = option == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelected(option) },
                shape = CircleShape,
                color = if (active) WmwColors.Midnight else WmwColors.LightSurfaceMuted,
                border = if (active) null else BorderStroke(1.dp, WmwColors.DarkHairline),
            ) {
                Text(
                    text = label(option),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) WmwColors.WarmLight else WmwColors.LightQuietText,
                )
            }
        }
    }
}