package com.wakemyway.app.preparation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.schedule.WakeOccurrence

@Composable
fun TomorrowContractLab(
    wakeOccurrence: WakeOccurrence?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember { WakePreparationManager(context) }
    val occurrenceKey = wakeOccurrence?.id?.value
    val initial = remember(occurrenceKey) {
        wakeOccurrence?.let { manager.snapshotFor(it.id) }
    }
    var snapshot by remember(occurrenceKey) { mutableStateOf(initial) }
    var rawText by remember(occurrenceKey) { mutableStateOf(initial?.contract?.rawText.orEmpty()) }
    var firstMove by remember(occurrenceKey) { mutableStateOf(initial?.contract?.firstMove.orEmpty()) }
    var message by remember(occurrenceKey) { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Text(
            modifier = Modifier.padding(top = 28.dp),
            text = "Tomorrow Contract",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "Private, local night-before context. It never enters the Direct-Boot alarm snapshot or reliability logs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        if (wakeOccurrence == null) {
            Text(
                modifier = Modifier.padding(top = 14.dp),
                text = "Schedule a wake first. A Tomorrow Contract is attached to one concrete wake occurrence.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = "For ${wakeOccurrence.scheduledAt}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            value = rawText,
            onValueChange = { rawText = it.take(TomorrowContract.MAX_RAW_TEXT_CHARACTERS) },
            label = { Text("Why tomorrow matters / what to remember") },
            supportingText = {
                Text("${rawText.length}/${TomorrowContract.MAX_RAW_TEXT_CHARACTERS}")
            },
            minLines = 3,
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            value = firstMove,
            onValueChange = { firstMove = it.take(TomorrowContract.MAX_FIRST_MOVE_CHARACTERS) },
            label = { Text("First move (optional)") },
            supportingText = {
                Text("${firstMove.length}/${TomorrowContract.MAX_FIRST_MOVE_CHARACTERS}")
            },
            singleLine = true,
        )

        Button(
            modifier = Modifier.padding(top = 12.dp),
            enabled = rawText.isNotBlank(),
            onClick = {
                runCatching {
                    manager.saveAndPrepare(
                        wakeOccurrenceId = wakeOccurrence.id,
                        rawText = rawText,
                        firstMove = firstMove,
                    )
                }.onSuccess {
                    snapshot = it
                    message = "Saved and prepared locally. Wake-time loading does not need the network."
                }.onFailure {
                    snapshot = manager.snapshotFor(wakeOccurrence.id)
                    message = "Could not save private preparation state. Generic wake content remains available."
                }
            },
        ) {
            Text("Save & prepare locally")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                manager.clear()
                rawText = ""
                firstMove = ""
                snapshot = manager.snapshotFor(wakeOccurrence.id)
                message = "Private Tomorrow Contract and prepared plan cleared."
            },
        ) {
            Text("Clear private preparation")
        }

        message?.let {
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        val current = snapshot ?: manager.snapshotFor(wakeOccurrence.id)
        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = "Prepared state · ${current.status.name}",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = current.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        current.plan?.let { plan ->
            Text(
                modifier = Modifier.padding(top = 14.dp),
                text = "Morning preview",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = plan.orientationLeadIn,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = plan.reminderLine,
                style = MaterialTheme.typography.bodyMedium,
            )
            plan.firstMoveLine?.let { line ->
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "format v${plan.formatVersion} · source revision ${plan.sourceContractRevision} · checksum ${plan.checksum.take(10)}…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        val wakeTimeContent = manager.loadForWake(wakeOccurrence.id)
        Text(
            modifier = Modifier.padding(top = 14.dp),
            text = when (wakeTimeContent) {
                is WakeTimePreparedContent.Prepared -> "Offline wake-time read: prepared private plan ready"
                is WakeTimePreparedContent.GenericFallback -> "Offline wake-time read: generic local fallback (${wakeTimeContent.reason.name})"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
