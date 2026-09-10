package com.wakemyway.app.character

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wakemyway.core.character.AlfredCharacter
import com.wakemyway.core.character.RenderedWakeLine
import com.wakemyway.core.character.WakeLineKey
import com.wakemyway.core.runtime.SpeechIntent

@Composable
fun AlfredCharacterLab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var speechState by remember { mutableStateOf<LocalSpeechState>(LocalSpeechState.Initializing) }
    var previewIndex by remember { mutableIntStateOf(0) }
    var renderSequence by remember { mutableIntStateOf(0) }
    var lastLine by remember { mutableStateOf<RenderedWakeLine?>(null) }
    var lastSpeechResult by remember { mutableStateOf("No preview spoken yet") }

    val speaker = remember(context) {
        LocalCharacterSpeaker(
            context = context,
            character = AlfredCharacter.spec,
            onStateChanged = { speechState = it },
        )
    }
    DisposableEffect(speaker) {
        onDispose { speaker.close() }
    }

    val intent = PREVIEW_INTENTS[previewIndex]

    Column(modifier = modifier) {
        Text(
            text = "Alfred · local character lab",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            modifier = Modifier.padding(top = 5.dp),
            text = speechState.label(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "Intent: ${intent.label()}",
            style = MaterialTheme.typography.labelLarge,
        )

        lastLine?.let { line ->
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "“${line.text}”",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "Alfred v${line.characterVersion} · variant ${line.variantIndex}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Button(
            modifier = Modifier.padding(top = 12.dp),
            onClick = {
                val line = AlfredCharacter.render(
                    intent = intent,
                    key = WakeLineKey("lab-$renderSequence-${intent.label()}"),
                )
                renderSequence += 1
                lastLine = line
                val started = speaker.speak(
                    line = line,
                    utteranceId = "alfred-lab-$renderSequence",
                ) { result ->
                    lastSpeechResult = result.label()
                }
                if (!started) {
                    lastSpeechResult = "Silent fallback · local speech unavailable"
                } else {
                    lastSpeechResult = "Speaking locally"
                }
            },
        ) {
            Text("Render + speak locally")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                previewIndex = (previewIndex + 1) % PREVIEW_INTENTS.size
                lastLine = null
                lastSpeechResult = "Intent changed; no speech queued"
            },
        ) {
            Text("Next speech intent")
        }

        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = lastSpeechResult,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "Production Wake now uses the same offline speech adapter; this screen remains diagnostics only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

private fun LocalSpeechState.label(): String = when (this) {
    LocalSpeechState.Initializing -> "Voice: checking installed offline voices…"
    is LocalSpeechState.Ready -> "Voice: offline ready · $languageTag · $voiceId"
    is LocalSpeechState.Unavailable -> "Voice: silent fallback · ${reason.name.lowercase()}"
}

private fun LocalSpeechResult.label(): String = when (this) {
    LocalSpeechResult.Completed -> "Local speech completed"
    is LocalSpeechResult.Failed -> "Silent fallback · ${reason.name.lowercase()}"
}

private fun SpeechIntent.label(): String = when (this) {
    SpeechIntent.InitialWake -> "INITIAL_WAKE"
    SpeechIntent.AskToSitUp -> "ASK_TO_SIT_UP"
    SpeechIntent.AskToMove -> "ASK_TO_MOVE"
    is SpeechIntent.ReEngage -> "RE_ENGAGE_$escalationLevel"
    SpeechIntent.SnoozeConfirmation -> "SNOOZE_CONFIRMATION"
    SpeechIntent.SnoozeFailed -> "SNOOZE_FAILED"
    SpeechIntent.Orientation -> "ORIENTATION"
}

private val PREVIEW_INTENTS = listOf(
    SpeechIntent.InitialWake,
    SpeechIntent.AskToSitUp,
    SpeechIntent.AskToMove,
    SpeechIntent.ReEngage(1),
    SpeechIntent.ReEngage(3),
    SpeechIntent.SnoozeConfirmation,
    SpeechIntent.SnoozeFailed,
    SpeechIntent.Orientation,
)
