package com.wakemyway.app.ui.preparation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wakemyway.app.R
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakePreparationSnapshot
import com.wakemyway.app.preparation.WakePreparationStatus
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.components.WmwStatusPill
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.schedule.WakeOccurrence
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TomorrowPlanScreen(
    wakeOccurrence: WakeOccurrence?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember { WakePreparationManager(context) }
    val occurrenceId = wakeOccurrence?.id
    val initialSnapshot = remember(occurrenceId) {
        occurrenceId?.let(manager::snapshotFor)
    }
    var snapshot by remember(occurrenceId) { mutableStateOf(initialSnapshot) }
    var rawText by remember(occurrenceId) {
        mutableStateOf(initialSnapshot?.contract?.rawText.orEmpty())
    }
    var firstMove by remember(occurrenceId) {
        mutableStateOf(initialSnapshot?.contract?.firstMove.orEmpty())
    }
    var message by remember(occurrenceId) { mutableStateOf<String?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    ProtectPrivateScreenFromCapture()

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
                    Text(stringResource(R.string.tomorrow_plan_back))
                }
                WmwStatusPill(
                    label = stringResource(R.string.tomorrow_plan_private_badge),
                    positive = true,
                )
            }

            Text(
                text = stringResource(R.string.tomorrow_plan_title),
                modifier = Modifier.padding(top = WmwSpacing.Xl),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.WarmLight,
            )
            Text(
                text = stringResource(R.string.tomorrow_plan_subtitle),
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.QuietText,
            )

            if (wakeOccurrence == null) {
                WmwCard(modifier = Modifier.padding(top = WmwSpacing.Xxl)) {
                    Text(
                        text = stringResource(R.string.tomorrow_plan_no_wake),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WmwColors.MorningPaper,
                    )
                }
                WmwPrimaryAction(
                    label = stringResource(R.string.tomorrow_plan_done),
                    onClick = onBack,
                    modifier = Modifier.padding(top = WmwSpacing.Xl),
                )
                return@Column
            }

            val wakeLabel = wakeOccurrence.scheduledAt.format(
                DateTimeFormatter.ofPattern("EEEE · MMM d · HH:mm", Locale.getDefault()),
            )
            Text(
                text = wakeLabel,
                modifier = Modifier.padding(top = WmwSpacing.Lg),
                style = MaterialTheme.typography.labelLarge,
                color = WmwColors.MorningPaper,
            )

            WmwCard(modifier = Modifier.padding(top = WmwSpacing.Lg)) {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = {
                            rawText = it.take(TomorrowContract.MAX_RAW_TEXT_CHARACTERS)
                            message = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.tomorrow_plan_reason_label)) },
                        supportingText = {
                            Text("${rawText.length}/${TomorrowContract.MAX_RAW_TEXT_CHARACTERS}")
                        },
                        minLines = 4,
                    )
                    OutlinedTextField(
                        value = firstMove,
                        onValueChange = {
                            firstMove = it.take(TomorrowContract.MAX_FIRST_MOVE_CHARACTERS)
                            message = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.tomorrow_plan_first_move_label)) },
                        supportingText = {
                            Text("${firstMove.length}/${TomorrowContract.MAX_FIRST_MOVE_CHARACTERS}")
                        },
                        singleLine = true,
                    )
                }
            }

            snapshot?.takeIf { it.status == WakePreparationStatus.READY }?.let {
                WmwCard(modifier = Modifier.padding(top = WmwSpacing.Sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.tomorrow_plan_ready_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = WmwColors.MorningPaper,
                            )
                            Text(
                                text = stringResource(R.string.tomorrow_plan_ready_body),
                                modifier = Modifier.padding(top = WmwSpacing.Xs),
                                style = MaterialTheme.typography.bodySmall,
                                color = WmwColors.QuietText,
                            )
                        }
                        WmwStatusPill(
                            label = stringResource(R.string.tomorrow_plan_ready_badge),
                            positive = true,
                        )
                    }
                }
            }

            message?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.QuietText,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(WmwSpacing.Xl))

            WmwPrimaryAction(
                label = stringResource(R.string.tomorrow_plan_save),
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
                        message = context.getString(R.string.tomorrow_plan_saved)
                    }.onFailure {
                        snapshot = manager.snapshotFor(wakeOccurrence.id)
                        message = context.getString(R.string.tomorrow_plan_save_failed)
                    }
                },
            )
            WmwSecondaryAction(
                label = stringResource(R.string.tomorrow_plan_done),
                onClick = onBack,
                modifier = Modifier.padding(top = WmwSpacing.Xs),
            )
            if (snapshot?.contract != null) {
                WmwSecondaryAction(
                    label = stringResource(R.string.tomorrow_plan_clear),
                    onClick = { showClearConfirmation = true },
                )
            }

            Text(
                text = stringResource(R.string.tomorrow_plan_privacy_note),
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

    if (showClearConfirmation && wakeOccurrence != null) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.tomorrow_plan_clear_title)) },
            text = { Text(stringResource(R.string.tomorrow_plan_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        manager.clear()
                        rawText = ""
                        firstMove = ""
                        snapshot = manager.snapshotFor(wakeOccurrence.id)
                        message = context.getString(R.string.tomorrow_plan_cleared)
                        showClearConfirmation = false
                    },
                ) {
                    Text(stringResource(R.string.tomorrow_plan_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.tomorrow_plan_clear_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProtectPrivateScreenFromCapture() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        val window = activity?.window
        val secureWasAlreadySet = window != null &&
            (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        if (window != null && !secureWasAlreadySet) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (window != null && !secureWasAlreadySet) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
