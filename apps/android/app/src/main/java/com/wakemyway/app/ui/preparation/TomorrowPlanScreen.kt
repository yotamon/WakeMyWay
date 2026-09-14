package com.wakemyway.app.ui.preparation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakemyway.app.R
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.ui.components.WmwActionTone
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.theme.WmwColors
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
    val dictationUnavailableCopy = stringResource(R.string.tomorrow_plan_dictation_unavailable)
    val dictationNoSpeechCopy = stringResource(R.string.tomorrow_plan_dictation_no_speech)
    val dictationActionCopy = stringResource(R.string.tomorrow_plan_dictation_action)
    val manager = remember { WakePreparationManager(context) }
    val dictation = remember(context) { TomorrowContractDictation(context) }
    val occurrenceId = wakeOccurrence?.id
    val initialSnapshot = remember(occurrenceId) { occurrenceId?.let(manager::snapshotFor) }
    var snapshot by remember(occurrenceId) { mutableStateOf(initialSnapshot) }
    var rawText by remember(occurrenceId) { mutableStateOf(initialSnapshot?.contract?.rawText.orEmpty()) }
    var firstMove by remember(occurrenceId) { mutableStateOf(initialSnapshot?.contract?.firstMove.orEmpty()) }
    var message by remember(occurrenceId) { mutableStateOf<String?>(null) }
    var isListening by remember(occurrenceId) { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    ProtectPrivateScreenFromCapture()
    DisposableEffect(dictation) {
        onDispose { dictation.close() }
    }

    fun toggleDictation() {
        if (isListening) {
            dictation.cancel()
            isListening = false
            return
        }
        message = null
        isListening = dictation.listen { result ->
            isListening = false
            when (result) {
                is TomorrowContractDictation.Result.Transcript -> {
                    rawText = listOf(rawText.trim(), result.text)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .take(TomorrowContract.MAX_RAW_TEXT_CHARACTERS)
                    message = null
                }
                TomorrowContractDictation.Result.NoSpeech -> message = dictationNoSpeechCopy
                TomorrowContractDictation.Result.Unavailable -> message = dictationUnavailableCopy
                TomorrowContractDictation.Result.Cancelled -> Unit
            }
        }
    }

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
            Spacer(Modifier.height(20.dp))
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

            Spacer(Modifier.height(36.dp))
            Text(
                text = stringResource(R.string.tomorrow_plan_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = WmwColors.WarmLight,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(68.dp))
            ContractOrb(
                listening = isListening,
                enabled = wakeOccurrence != null,
                contentDescription = dictationActionCopy,
                onClick = ::toggleDictation,
            )
            Text(
                text = stringResource(
                    if (isListening) R.string.tomorrow_plan_listening else R.string.tomorrow_plan_tap_to_speak,
                ),
                modifier = Modifier.padding(top = 38.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isListening) WmwColors.SoftEmber else WmwColors.QuietText,
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
            )

            TextField(
                value = rawText,
                onValueChange = {
                    rawText = it.take(TomorrowContract.MAX_RAW_TEXT_CHARACTERS)
                    message = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 35.dp),
                placeholder = { Text(stringResource(R.string.tomorrow_plan_reason_placeholder)) },
                minLines = 5,
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
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors,
            )

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
                        dictation.cancel(deliverCancellation = false)
                        isListening = false
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
private fun ContractOrb(
    listening: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val accent = if (listening) Color(0xFFF3C49A) else WmwColors.SoftEmber
    Box(
        modifier = Modifier
            .size(156.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WmwColors.EmberGlow.copy(alpha = if (listening) 0.56f else 0.42f),
                        WmwColors.ClayGlow.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.50f,
                ),
                radius = size.minDimension * 0.50f,
            )
            drawCircle(
                color = accent.copy(alpha = 0.76f),
                radius = size.minDimension * 0.29f,
                style = Stroke(width = 1.2.dp.toPx()),
            )
            drawCircle(
                color = accent.copy(alpha = 0.16f),
                radius = size.minDimension * 0.23f,
            )

            val micWidth = 15.dp.toPx()
            val micHeight = 25.dp.toPx()
            val micTop = center.y - micHeight * 0.58f
            drawRoundRect(
                color = WmwColors.WarmLight,
                topLeft = Offset(center.x - micWidth / 2f, micTop),
                size = Size(micWidth, micHeight),
                cornerRadius = CornerRadius(micWidth / 2f, micWidth / 2f),
                style = Stroke(width = 1.6.dp.toPx()),
            )
            val cradle = Path().apply {
                moveTo(center.x - 12.dp.toPx(), center.y + 1.dp.toPx())
                cubicTo(
                    center.x - 12.dp.toPx(), center.y + 14.dp.toPx(),
                    center.x + 12.dp.toPx(), center.y + 14.dp.toPx(),
                    center.x + 12.dp.toPx(), center.y + 1.dp.toPx(),
                )
            }
            drawPath(
                path = cradle,
                color = WmwColors.WarmLight,
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
            )
            drawLine(
                color = WmwColors.WarmLight,
                start = Offset(center.x, center.y + 14.dp.toPx()),
                end = Offset(center.x, center.y + 22.dp.toPx()),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = WmwColors.WarmLight,
                start = Offset(center.x - 7.dp.toPx(), center.y + 22.dp.toPx()),
                end = Offset(center.x + 7.dp.toPx(), center.y + 22.dp.toPx()),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
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
