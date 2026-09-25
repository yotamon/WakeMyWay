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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wakemyway.app.R
import com.wakemyway.app.preparation.WakePreparationManager
import com.wakemyway.app.ui.components.WmwActionTone
import com.wakemyway.app.ui.components.WmwBrandLockup
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSunriseMark
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
    defaultFirstMove: String? = null,
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
    var firstMove by remember(occurrenceId, defaultFirstMove) {
        mutableStateOf(
            initialSnapshot?.contract?.firstMove
                ?: defaultFirstMove.orEmpty(),
        )
    }
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
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Lg),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        if (LocalLayoutDirection.current == LayoutDirection.Rtl) "›" else "‹",
                        style = MaterialTheme.typography.headlineMedium,
                        color = WmwColors.Midnight,
                    )
                }
                WmwBrandLockup(modifier = Modifier.padding(start = 2.dp))
                Spacer(Modifier.weight(1f))
                if (snapshot?.contract != null) {
                    TextButton(onClick = { showClearConfirmation = true }) {
                        Text(
                            text = stringResource(R.string.tomorrow_plan_clear),
                            style = MaterialTheme.typography.bodySmall,
                            color = WmwColors.LightQuietText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(WmwSpacing.Xl))
            Text(
                text = stringResource(R.string.tomorrow_plan_private_badge).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.DawnText,
            )
            Text(
                text = stringResource(R.string.tomorrow_plan_title),
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = stringResource(R.string.tomorrow_plan_subtitle),
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )

            Spacer(Modifier.height(26.dp))
            WmwCard(onLightSurface = true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WmwSunriseMark(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                            .padding(horizontal = WmwSpacing.Xxl),
                    )
                    ContractVoiceButton(
                        listening = isListening,
                        enabled = wakeOccurrence != null,
                        contentDescription = dictationActionCopy,
                        onClick = ::toggleDictation,
                        modifier = Modifier.padding(top = WmwSpacing.Sm),
                    )
                    Text(
                        text = stringResource(
                            if (isListening) R.string.tomorrow_plan_listening else R.string.tomorrow_plan_tap_to_speak,
                        ),
                        modifier = Modifier.padding(top = WmwSpacing.Sm),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isListening) WmwColors.Sunrise else WmwColors.LightQuietText,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (wakeOccurrence == null) {
                Text(
                    text = stringResource(R.string.tomorrow_plan_no_wake),
                    modifier = Modifier.padding(top = WmwSpacing.Xl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.LightQuietText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(WmwSpacing.Xl))
                WmwPrimaryAction(
                    label = stringResource(R.string.tomorrow_plan_done),
                    onClick = onBack,
                    onLightSurface = true,
                    tone = WmwActionTone.WARM,
                )
                return@Column
            }

            Text(
                text = wakeOccurrence.scheduledAt.format(DateTimeFormatter.ofPattern("EEEE · HH:mm")),
                modifier = Modifier.padding(top = WmwSpacing.Lg),
                style = MaterialTheme.typography.labelSmall,
                color = WmwColors.LightFaintText,
            )

            val fieldColors = TextFieldDefaults.colors(
                focusedTextColor = WmwColors.Midnight,
                unfocusedTextColor = WmwColors.Midnight,
                cursorColor = WmwColors.Sunrise,
                focusedContainerColor = WmwColors.PaperCard,
                unfocusedContainerColor = WmwColors.PaperCard.copy(alpha = 0.90f),
                focusedIndicatorColor = WmwColors.Sunrise.copy(alpha = 0.52f),
                unfocusedIndicatorColor = WmwColors.DarkHairline,
                disabledIndicatorColor = Color.Transparent,
                focusedPlaceholderColor = WmwColors.LightFaintText,
                unfocusedPlaceholderColor = WmwColors.LightFaintText,
                focusedLabelColor = WmwColors.LightQuietText,
                unfocusedLabelColor = WmwColors.LightQuietText,
            )

            TextField(
                value = rawText,
                onValueChange = {
                    rawText = it.take(TomorrowContract.MAX_RAW_TEXT_CHARACTERS)
                    message = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WmwSpacing.Md),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Sm),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.LightQuietText,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(WmwSpacing.Xl))
            WmwPrimaryAction(
                label = stringResource(R.string.tomorrow_plan_save),
                enabled = rawText.isNotBlank(),
                onLightSurface = true,
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
                color = WmwColors.LightFaintText,
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
                        firstMove = defaultFirstMove.orEmpty()
                        snapshot = manager.snapshotFor(wakeOccurrence.id)
                        message = clearedCopy
                        showClearConfirmation = false
                    },
                ) {
                    Text(stringResource(R.string.tomorrow_plan_clear_confirm), color = WmwColors.Danger)
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
private fun ContractVoiceButton(
    listening: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (listening) WmwColors.Sunrise else WmwColors.DarkHairline
    Surface(
        modifier = modifier
            .size(84.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = if (listening) WmwColors.Sunrise.copy(alpha = 0.14f) else WmwColors.MorningPaper,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(34.dp)) {
                val strokeWidth = 2.dp.toPx()
                val bodyWidth = size.width * 0.34f
                val bodyHeight = size.height * 0.50f
                drawRoundRect(
                    color = if (listening) WmwColors.Sunrise else WmwColors.Midnight,
                    topLeft = Offset((size.width - bodyWidth) / 2f, size.height * 0.12f),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(bodyWidth / 2f),
                    style = Stroke(width = strokeWidth),
                )
                val path = Path().apply {
                    moveTo(size.width * 0.27f, size.height * 0.48f)
                    cubicTo(
                        size.width * 0.27f,
                        size.height * 0.72f,
                        size.width * 0.73f,
                        size.height * 0.72f,
                        size.width * 0.73f,
                        size.height * 0.48f,
                    )
                }
                drawPath(
                    path,
                    color = if (listening) WmwColors.Sunrise else WmwColors.Midnight,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawLine(
                    color = if (listening) WmwColors.Sunrise else WmwColors.Midnight,
                    start = Offset(size.width / 2f, size.height * 0.72f),
                    end = Offset(size.width / 2f, size.height * 0.88f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
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
