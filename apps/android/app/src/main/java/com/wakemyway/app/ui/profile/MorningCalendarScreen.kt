package com.wakemyway.app.ui.profile

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wakemyway.app.calendar.MorningCalendarRepository
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optional source consent is independent from Neon account sign-in and base Wake Ready.
 * Reads ONLY Google calendars already synced on this phone (no new OAuth/token backend).
 */
@Composable
fun MorningCalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember(context) { MorningCalendarRepository(context) }
    var permitted by remember { mutableStateOf(repo.hasPermission()) }
    var settings by remember { mutableStateOf(repo.settings()) }
    var choices by remember { mutableStateOf<List<MorningCalendarRepository.CalendarChoice>>(emptyList()) }
    var loading by remember { mutableStateOf(permitted) }
    var refreshRevision by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<String?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permitted = granted
        if (!granted) error = "Calendar permission wasn't granted. Your alarms still work normally."
    }

    LaunchedEffect(permitted, refreshRevision) {
        if (!permitted) {
            choices = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val result = withContext(Dispatchers.IO) { runCatching { repo.availableGoogleCalendars() } }
        choices = result.getOrElse { emptyList() }
        error = result.exceptionOrNull()?.let { "Calendar data is unavailable on this phone." }
        loading = false
    }

    LaunchedEffect(permitted, settings.consentRevision, refreshRevision) {
        if (!permitted || !settings.enabled) {
            preview = null
            previewLoading = false
            return@LaunchedEffect
        }
        previewLoading = true
        val result = withContext(Dispatchers.IO) { runCatching { repo.readNextFact() } }
        preview = result.getOrNull()?.description
        previewLoading = false
    }

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Morning context", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Let your wake voice mention the first relevant event, once you're ready for your day. " +
                    "This is optional and never changes your alarm.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Google calendars already synced to Android are used directly. " +
                    "Your WakeMyWay account sign-in is separate and doesn't grant calendar access.",
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider()
            if (!permitted) {
                Text("Allow read-only calendar access", style = MaterialTheme.typography.titleMedium)
                Text(
                    "WakeMyWay reads only a selected calendar, and never modifies events. " +
                        "Without this permission, morning context stays off.",
                )
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                    Text("Allow calendar access")
                }
            } else {
                Text("Choose one Google calendar", style = MaterialTheme.typography.titleMedium)
                if (loading) CircularProgressIndicator()
                if (!loading && choices.isEmpty()) {
                    Text(
                        "No synced Google calendars were found. Enable calendar sync for a Google " +
                            "account on your phone, then return here.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { refreshRevision += 1 }) {
                        Text("Check again")
                    }
                }
                choices.forEach { choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            repo.selectCalendar(choice.id)
                            settings = repo.settings()
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = settings.selectedCalendarId == choice.id,
                            onClick = {
                                repo.selectCalendar(choice.id)
                                settings = repo.settings()
                            },
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(choice.name, style = MaterialTheme.typography.titleSmall)
                            Text(choice.accountLabel, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (settings.enabled) {
                    HorizontalDivider()
                    Text("What can the voice say?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "By default, OpenAI receives only the time of the next eligible event. " +
                            "Private, declined and all-day events are excluded.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Allow selected event titles to be sent to OpenAI and spoken aloud",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = settings.includeEventTitlesInVoice,
                            onCheckedChange = { enabled ->
                                repo.setShareTitles(enabled)
                                settings = repo.settings()
                            },
                        )
                    }
                    Text(
                        "Event details are read directly on your phone when voice starts. " +
                            "Titles aren't stored, logged or used for learning. " +
                            "Your phone's calendar sync may not be up to date.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    HorizontalDivider()
                    Text("Preview · next 12 hours", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            previewLoading -> "Checking locally synced events…"
                            preview != null -> preview.orEmpty()
                            else -> "No eligible event found in the next 12 hours, or calendar sync is unavailable."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "The morning voice reads again after the alarm fires. This isn't a live Google server status.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = {
                        repo.disconnect()
                        settings = repo.settings()
                    }) { Text("Disconnect calendar") }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
