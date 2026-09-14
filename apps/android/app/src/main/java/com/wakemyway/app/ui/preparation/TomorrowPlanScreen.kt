package com.wakemyway.app.ui.preparation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakemyway.app.R
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.preparation.WakePreparationStatus
import com.wakemyway.app.ui.components.WmwActionTone
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwStatusPill
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSizes
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.preparation.TomorrowContract
import com.wakemyway.core.schedule.WakeOccurrence
import java.time.format.DateTimeFormatter

@Composable
fun TomorrowPlanScreen(
    wakeOccurrence: WakeOccurrence?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val savedCopy = stringResource(R.string.tomorrow_plan_saved)
    val saveFailedCopy = stringResource(R.string.tomorrow_plan_save_failed)
    val clearedCopy = stringResource(R.string.tomorrow_plan_cleared)
    val manager = remember { WakePreparationManager(context) }
    val occurrenceId = wakeOccurrence?.id
    val initialSnapshot = remember(occurrenceId) { occurrenceId?.let(manager::snapshotFor) }
    var snapshot by remember(occurrenceId) { mutableStateOf(initialSnapshot) }
    var rawText by remember(occurrenceId) { mutableStateOf(initialSnapshot?.contract?.rawText.orEmpty()) }
    var firstMove by remember(occurrenceId) { mutableStateOf(initialSnapshot?.contract?.firstMove.orEmpty()) }
    var message by remember(occurrenceId) { mutableStateOf<String?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    ProtectPrivateScreenFromCapture()

    WmwCircadianSurface(
        stage = WmwCircadianStage.EMERGING,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Xs, bottom = WmwSpacing.Md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
                if (snapshot?.contract != null) {
                    TextButton(onClick = { showClearConfirmation = true }) {
                        Text(
                            text = stringResource(R.string.tomorrow_plan_clear),
                            style = MaterialTheme.typography.bodySmall,
                            color = WmwColors.QuietText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(WmwSpacing.Md))
            Text(
                text = stringResource(R.string.tomorrow_plan_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = WmwColors.WarmLight,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(WmwSpacing.Xl))
            ContractOrb()
            Text(
                text = stringResource(R.string.tomorrow_plan_tap_to_write),
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.QuietText,
            )

            if (wakeOccurrence == null) {
                Text(
                    text = stringResource(R.string.tomorrow_plan_no_wake),
                    modifier = Modifier.padding(top = WmwSpacing.Xxl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.QuietText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f, fill = true))
                WmwPrimaryAction(
                    label = stringResource(R.string.tomorrow_plan_done),
                    onClick = onBack,
                    tone = WmwActionTone.DARK,
                )
                return@Column
            }

            Text(
                text = wakeOccurrence.scheduledAt.format(DateTimeFormatter.ofPattern("EEEE · HH:mm")),
                modifier = Modifier.padding(top = WmwSpacing.Md),
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.FaintText,
            )

            val fieldColors = TextFieldDefaults.colors(
                focusedTextColor = WmwColors.WarmLight,
                unfocusedTextColor = WmwColors.WarmLight,
                cursorColor = WmwColors.SoftEmber,
                focusedContainerColor = WmwColors.ElevatedNightSurface.copy(alpha = 0.82f),
                unfocusedContainerColor = WmwColors.ElevatedNightSurface.copy(alpha = 0.76f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedPlaceholderColor = WmwColors.FaintText,
                unfocusedPlaceholderColor = WmwColors.FaintText,
                focusedLabelColor = WmwColors.QuietText,
                unfocusedLabelColor = WmwColors.QuietText,
                focusedSupportingTextColor = WmwColors.FaintText,
                unfocusedSupportingTextColor = WmwColors.FaintText,
            )

            TextField(
                value = rawText,
                onValueChange = {
                    rawText = it.take(TomorrowContract.MAX_RAW_TEXT_CHARACTERS)
                    message = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Lg),
                placeholder = { Text(stringResource(R.string.tomorrow_plan_reason_placeholder)) },
                supportingText = {
                    Text("${rawText.length}/${TomorrowContract.MAX_RAW_TEXT_CHARACTERS}")
                },
                minLines = 4,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors,
            )

            TextField(
                value = firstMove,
                onValueChange = {
                    firstMove = it.take(TomorrowContract.MAX_FIRST_MOVE_CHARACTERS)
                    message = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Xs),
                placeholder = { Text(stringResource(R.string.tomorrow_plan_first_move_label)) },
                supportingText = {
                    Text("${firstMove.length}/${TomorrowContract.MAX_FIRST_MOVE_CHARACTERS}")
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors,
            )

            snapshot?.takeIf { it.status == WakePreparationStatus.READY }?.let {
                WmwStatusPill(
                    label = stringResource(R.string.tomorrow_plan_ready_badge),
                    positive = true,
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                )
            }

            message?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = WmwSpacing.Sm),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.QuietText,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f, fill = true))
            Spacer(Modifier.height(WmwSpacing.Lg))
            WmwPrimaryAction(
                label = stringResource(R.string.tomorrow_plan_save),
                enabled = rawText.isNotBlank(),
                tone = WmwActionTone.WARM,
                onClick = {
                    runCatching {
                        manager.saveAndPrepare(
                            wakeOccurrenceId = wakeOccurrence.id,
                            rawText = rawText,
                            firstMove = firstMove,
                        )
                    }.onSuccess {
                        snapshot = it
                        message = savedCopy
                    }.onFailure {
                        snapshot = manager.snapshotFor(wakeOccurrence.id)
                        message = saveFailedCopy
                    }
                },
            )
            Text(
                text = stringResource(R.string.tomorrow_plan_privacy_note),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WmwSpacing.Md, vertical = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.FaintText,
                textAlign = TextAlign.Center,
            )
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
                        message = clearedCopy
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
private fun ContractOrb() {
    Box(
        modifier = Modifier.size(WmwSizes.MicOrb),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WmwColors.EmberGlow.copy(alpha = 0.34f),
                        WmwColors.ClayGlow.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.50f,
                ),
                radius = size.minDimension * 0.50f,
            )
            drawCircle(
                color = WmwColors.SoftEmber.copy(alpha = 0.74f),
                radius = size.minDimension * 0.34f,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = WmwColors.SoftEmber.copy(alpha = 0.20f),
                radius = size.minDimension * 0.22f,
            )
        }
        Text(
            text = "“ ”",
            style = MaterialTheme.typography.headlineMedium,
            color = WmwColors.WarmLight,
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
