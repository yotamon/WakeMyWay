package com.wakemyway.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.wakemyway.app.alarm.WakeSoundCatalog
import com.wakemyway.app.product.ConsumerPreferences
import com.wakemyway.app.ui.components.WmwBrandHeader
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.alarm.VoiceStyle
import com.wakemyway.core.alarm.WakeSoundId

@Composable
fun ProfileScreen(
    preferences: ConsumerPreferences,
    onPreferencesChanged: (ConsumerPreferences) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val soundOptions = remember(context) {
        WakeSoundCatalog.availableProfiles(context).map { it.id }
    }
    var displayName by remember(preferences.displayName) {
        mutableStateOf(preferences.displayName.orEmpty())
    }
    var firstMove by remember(preferences.defaultFirstMove) {
        mutableStateOf(preferences.defaultFirstMove.orEmpty())
    }

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
                text = preferences.displayName?.takeIf { it.isNotBlank() }?.let { "Good to see you, $it." }
                    ?: "Make mornings yours.",
                modifier = Modifier.padding(top = 28.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "Defaults shape new alarms. Existing alarms stay exactly as you configured them.",
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )

            ProfileSection("You", Modifier.padding(top = 28.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { value ->
                        val next = value.take(ConsumerPreferences.MAX_DISPLAY_NAME_LENGTH)
                        displayName = next
                        onPreferencesChanged(
                            preferences.copy(displayName = next.trim().ifBlank { null }),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    placeholder = { Text("What should WakeMyWay call you?") },
                    singleLine = true,
                )

                WmwCard(onLightSurface = true) {
                    Text(
                        text = "LOCAL PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = WmwColors.DawnDeep,
                    )
                    Text(
                        text = "No account connected",
                        modifier = Modifier.padding(top = WmwSpacing.Xs),
                        style = MaterialTheme.typography.titleMedium,
                        color = WmwColors.Midnight,
                    )
                    Text(
                        text = "Alarms and preferences work fully on this phone. Account sync will remain optional when it is enabled.",
                        modifier = Modifier.padding(top = WmwSpacing.Xs),
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.LightQuietText,
                    )
                }
            }

            ProfileSection("New alarm defaults", Modifier.padding(top = WmwSpacing.Lg)) {
                Text(
                    text = "Wake sound",
                    style = MaterialTheme.typography.titleSmall,
                    color = WmwColors.Midnight,
                )
                ChoiceRow(
                    options = soundOptions.ifEmpty { listOf(WakeSoundCatalog.defaultId) },
                    selected = preferences.defaultSoundId,
                    label = ::soundName,
                    onSelected = { sound ->
                        onPreferencesChanged(preferences.copy(defaultSoundId = sound))
                    },
                )

                ToggleSetting(
                    title = "Voice Check-In",
                    detail = "Use Alfred by default for new alarms.",
                    checked = preferences.defaultVoiceCheckInEnabled,
                    onCheckedChange = { enabled ->
                        onPreferencesChanged(preferences.copy(defaultVoiceCheckInEnabled = enabled))
                    },
                )

                if (preferences.defaultVoiceCheckInEnabled) {
                    Text(
                        text = "Alfred style",
                        style = MaterialTheme.typography.titleSmall,
                        color = WmwColors.Midnight,
                    )
                    ChoiceRow(
                        options = VoiceStyle.entries,
                        selected = preferences.defaultVoiceStyle,
                        label = {
                            when (it) {
                                VoiceStyle.DEFAULT -> "Default"
                                VoiceStyle.MOTIVATIONAL -> "Motivational"
                                VoiceStyle.MINIMAL -> "Minimal"
                            }
                        },
                        onSelected = { style ->
                            onPreferencesChanged(preferences.copy(defaultVoiceStyle = style))
                        },
                    )
                }

                Text(
                    text = "Snooze",
                    style = MaterialTheme.typography.titleSmall,
                    color = WmwColors.Midnight,
                )
                ChoiceRow(
                    options = ConsumerPreferences.ALLOWED_SNOOZE_MINUTES.sorted(),
                    selected = preferences.defaultSnoozeMinutes,
                    label = { "$it min" },
                    onSelected = { minutes ->
                        onPreferencesChanged(preferences.copy(defaultSnoozeMinutes = minutes))
                    },
                )
            }

            ProfileSection("Morning routine", Modifier.padding(top = WmwSpacing.Lg)) {
                Text(
                    text = "Reusable First Move",
                    style = MaterialTheme.typography.titleSmall,
                    color = WmwColors.Midnight,
                )
                Text(
                    text = "This becomes the starting First Move for new alarms and can still be changed per alarm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.LightQuietText,
                )
                OutlinedTextField(
                    value = firstMove,
                    onValueChange = { value ->
                        val next = value.take(ConsumerPreferences.MAX_FIRST_MOVE_LENGTH)
                        firstMove = next
                        onPreferencesChanged(
                            preferences.copy(defaultFirstMove = next.trim().ifBlank { null }),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Open the curtains") },
                    minLines = 2,
                )
            }

            ProfileSection("App", Modifier.padding(top = WmwSpacing.Lg)) {
                ProfileLink(
                    title = "Notifications",
                    detail = "Open WakeMyWay's Android notification controls",
                    onClick = onOpenNotifications,
                )
                ProfileLink(
                    title = "Privacy",
                    detail = "See what stays local and what never enters the wake path",
                    onClick = onOpenPrivacy,
                )
                ProfileLink(
                    title = "Appearance",
                    detail = "Light planning surfaces · branded dark wake surface",
                    onClick = null,
                )
                ProfileLink(
                    title = "About WakeMyWay",
                    detail = "Product principles and version information",
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
        ) {
            BackHeader("Privacy", onBack)
            Text(
                text = "Private by wake design.",
                modifier = Modifier.padding(top = 28.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "Critical waking stays local and does not depend on an account, network, or cloud model.",
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.LightQuietText,
            )

            PrivacyFact(
                title = "Tomorrow Contract",
                body = "Your reason and First Move stay in credential-protected app storage. They are not copied into Direct Boot alarm state.",
            )
            PrivacyFact(
                title = "Voice replies",
                body = "Local Voice Check-In does not persist raw microphone audio or raw transcripts. The alarm remains controllable without voice.",
            )
            PrivacyFact(
                title = "Alarm delivery",
                body = "Exact scheduling, critical playback, Stop and Snooze are local Android responsibilities. Optional cloud enrichment is not Wake Ready authority.",
            )
            PrivacyFact(
                title = "Account",
                body = "No consumer account is connected in this build. Local alarms and preferences do not require one.",
            )
        }
    }
}

@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
        ) {
            BackHeader("About", onBack)
            Spacer(Modifier.weight(1f))
            com.wakemyway.app.ui.components.WmwBrandHero(
                tagline = "Brighter mornings. Your way.",
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "WakeMyWay wakes up with you, not at you.",
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "A local-first, voice-aware wake experience built around reliable alarms, intentional mornings and one achievable first move.",
                modifier = Modifier.fillMaxWidth().padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )
            Text(
                text = "Version $versionName",
                modifier = Modifier.padding(top = WmwSpacing.Lg),
                style = MaterialTheme.typography.labelMedium,
                color = WmwColors.DawnDeep,
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
        Text(
            text = title.uppercase(),
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
private fun ToggleSetting(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = WmwColors.Midnight)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = WmwColors.LightQuietText)
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
                modifier = Modifier.weight(1f).clickable { onSelected(option) },
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

@Composable
private fun ProfileLink(
    title: String,
    detail: String,
    onClick: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = WmwColors.MorningPaper,
        border = BorderStroke(1.dp, WmwColors.DarkHairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WmwSpacing.Md, vertical = WmwSpacing.Md),
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
            if (onClick != null) {
                Text("›", style = MaterialTheme.typography.titleLarge, color = WmwColors.DawnDeep)
            }
        }
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text("‹", style = MaterialTheme.typography.headlineMedium, color = WmwColors.Midnight)
        }
        Text(
            title,
            modifier = Modifier.padding(start = WmwSpacing.Xs),
            style = MaterialTheme.typography.titleLarge,
            color = WmwColors.Midnight,
        )
    }
}

@Composable
private fun PrivacyFact(title: String, body: String) {
    WmwCard(
        modifier = Modifier.padding(top = WmwSpacing.Md),
        onLightSurface = true,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = WmwColors.Midnight)
        Text(
            body,
            modifier = Modifier.padding(top = WmwSpacing.Xs),
            style = MaterialTheme.typography.bodyMedium,
            color = WmwColors.LightQuietText,
        )
    }
}

private fun soundName(id: WakeSoundId): String = when (id) {
    WakeSoundId.MORNING_LIGHT -> "Morning Light"
    WakeSoundId.SOFT_START -> "Soft Start"
    WakeSoundId.MORNING_PULSE -> "Morning Pulse"
    else -> "Wake sound"
}
