package com.wakemyway.app.ui.home

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakemyway.app.R
import com.wakemyway.app.ui.components.WmwActionTone
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.components.WmwStatusPill
import com.wakemyway.app.ui.components.WmwTimeDisplay
import com.wakemyway.app.ui.components.WmwWakeLine
import com.wakemyway.app.ui.components.WmwWakeLineState
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.app.voice.ConversationalAlfredState

data class TonightUiState(
    val wakeTime: String,
    val dateLabel: String,
    val hasOccurrence: Boolean,
    val wakeReady: Boolean,
    val readinessDetail: String,
    val wakeRepairActionLabel: String? = null,
    val hasTomorrowContract: Boolean,
    val tomorrowContractPrepared: Boolean,
    val tomorrowContractText: String? = null,
    val firstMove: String? = null,
)

enum class VoiceWakeReadiness {
    READY,
    SETUP_REQUIRED,
    UNAVAILABLE,
}

@Composable
fun TonightScreen(
    state: TonightUiState,
    onOpenWakeSetup: () -> Unit,
    onOpenTomorrowPlan: () -> Unit,
    onOpenWakeLab: () -> Unit,
    modifier: Modifier = Modifier,
    showDeveloperTools: Boolean = false,
    voiceWakeReadiness: VoiceWakeReadiness? = null,
    onEnableVoiceReplies: () -> Unit = {},
    onRepairWakeSystem: () -> Unit = {},
) {
    val context = LocalContext.current

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
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(50.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WmwColors.QuietText.copy(alpha = 0.82f),
                )
                SettingsGlyph(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                        .clickable(onClick = onOpenWakeSetup)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Edit wake settings"
                        },
                )
            }

            Spacer(Modifier.height(70.dp))

            WmwTimeDisplay(
                time = state.wakeTime,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.dateLabel,
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.WarmLight.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )

            WmwWakeLine(
                state = WmwWakeLineState.QUIET,
                modifier = Modifier.padding(top = 61.dp),
            )

            if (state.hasOccurrence) {
                WmwStatusPill(
                    label = stringResource(
                        if (state.wakeReady) R.string.tonight_wake_ready else R.string.tonight_wake_not_ready,
                    ),
                    positive = state.wakeReady,
                    modifier = Modifier.padding(top = WmwSpacing.Xxs),
                )
            }

            Spacer(Modifier.height(40.dp))
            Hairline()
            Spacer(Modifier.height(42.dp))

            if (state.hasOccurrence) {
                TomorrowContractPreview(
                    state = state,
                    onClick = onOpenTomorrowPlan,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = WmwSpacing.Md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.tonight_no_wake_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = WmwColors.WarmLight,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = state.readinessDetail,
                        modifier = Modifier.padding(top = WmwSpacing.Xs),
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.QuietText,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (state.hasOccurrence && !state.wakeReady) {
                WakeSystemAttention(
                    state = state,
                    onRepairWakeSystem = onRepairWakeSystem,
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                )
            }

            Spacer(Modifier.height(74.dp))
            Hairline(alpha = 0.45f)
            Spacer(Modifier.height(WmwSpacing.Md))

            AlfredSignature(
                quote = if (state.hasOccurrence) {
                    stringResource(R.string.tonight_alfred_quote, state.wakeTime)
                } else {
                    stringResource(R.string.tonight_alfred_empty_quote)
                },
            )

            voiceWakeReadiness?.takeIf { it != VoiceWakeReadiness.READY }?.let { readiness ->
                WmwCard(modifier = Modifier.padding(top = WmwSpacing.Md)) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
                        Text(
                            text = stringResource(R.string.tonight_voice_wake_title).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = WmwColors.QuietText,
                        )
                        Text(
                            text = stringResource(
                                when (readiness) {
                                    VoiceWakeReadiness.READY -> R.string.tonight_voice_wake_ready_detail
                                    VoiceWakeReadiness.SETUP_REQUIRED -> R.string.tonight_voice_wake_setup_detail
                                    VoiceWakeReadiness.UNAVAILABLE -> R.string.tonight_voice_wake_unavailable_detail
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = WmwColors.WarmLight,
                        )
                        if (readiness == VoiceWakeReadiness.SETUP_REQUIRED) {
                            WmwSecondaryAction(
                                label = stringResource(R.string.tonight_voice_wake_enable),
                                onClick = onEnableVoiceReplies,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f, fill = true))
            Spacer(Modifier.height(WmwSpacing.Md))

            WmwPrimaryAction(
                label = stringResource(
                    if (state.hasOccurrence) R.string.tonight_edit_wake else R.string.tonight_set_wake,
                ),
                onClick = onOpenWakeSetup,
                tone = if (state.hasOccurrence) WmwActionTone.DARK else WmwActionTone.WARM,
            )

            if (showDeveloperTools) {
                val conversationReady = ConversationalAlfredState.isReady(context)
                WmwSecondaryAction(
                    label = if (conversationReady) "Review Alfred connection" else "Connect Alfred",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent().setClassName(
                                    context.packageName,
                                    ConversationalAlfredState.DEBUG_SETUP_ACTIVITY,
                                ),
                            )
                        }
                    },
                )
                WmwSecondaryAction(
                    label = stringResource(R.string.tonight_open_lab),
                    onClick = onOpenWakeLab,
                )
            }
        }
    }
}

@Composable
private fun TomorrowContractPreview(
    state: TonightUiState,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Edit Tomorrow Contract"
            },
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = stringResource(R.string.tonight_tomorrow_matters),
            style = MaterialTheme.typography.bodyLarge,
            color = WmwColors.WarmLight.copy(alpha = 0.82f),
        )
        Text(
            text = state.tomorrowContractText?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.tonight_contract_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = WmwColors.WarmLight,
        )
        state.firstMove?.takeIf { it.isNotBlank() }?.let { firstMove ->
            Text(
                text = stringResource(R.string.tonight_first_move, firstMove),
                modifier = Modifier.padding(top = WmwSpacing.Xxs),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.QuietText,
            )
        }
    }
}

@Composable
private fun AlfredSignature(quote: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Md),
    ) {
        AlfredMark(Modifier.size(34.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.wake_character_name).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = WmwColors.WarmLight,
            )
            Text(
                text = quote,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.QuietText,
            )
        }
    }
}

@Composable
private fun AlfredMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension * 0.30f
        drawCircle(
            color = WmwColors.SoftEmber.copy(alpha = 0.9f),
            radius = radius,
            style = Stroke(width = 1.1.dp.toPx()),
        )
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val start = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius * 1.24f,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius * 1.24f,
            )
            val end = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius * 1.48f,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius * 1.48f,
            )
            drawLine(
                color = WmwColors.SoftEmber.copy(alpha = 0.82f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SettingsGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.19f
        drawCircle(
            color = WmwColors.WarmLight.copy(alpha = 0.76f),
            radius = r,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = WmwColors.WarmLight.copy(alpha = 0.76f),
            radius = r * 0.32f,
            style = Stroke(width = 1.dp.toPx()),
        )
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val start = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * r * 1.22f,
                center.y + kotlin.math.sin(angle).toFloat() * r * 1.22f,
            )
            val end = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * r * 1.55f,
                center.y + kotlin.math.sin(angle).toFloat() * r * 1.55f,
            )
            drawLine(
                color = WmwColors.WarmLight.copy(alpha = 0.68f),
                start = start,
                end = end,
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun Hairline(alpha: Float = 1f) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.75.dp)
            .background(WmwColors.Hairline.copy(alpha = WmwColors.Hairline.alpha * alpha)),
    )
}

@Composable
private fun WakeSystemAttention(
    state: TonightUiState,
    onRepairWakeSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
            Text(
                text = stringResource(R.string.tonight_not_ready_title),
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.WarmLight,
            )
            Text(
                text = state.readinessDetail,
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.QuietText,
            )
            state.wakeRepairActionLabel?.let { repairLabel ->
                WmwPrimaryAction(
                    label = repairLabel,
                    onClick = onRepairWakeSystem,
                )
            }
        }
    }
}

@Preview(
    name = "Tonight ready",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
)
@Composable
private fun TonightReadyPreview() {
    WakeMyWayTheme {
        TonightScreen(
            state = TonightUiState(
                wakeTime = "07:30",
                dateLabel = "Tuesday, 14 Jan",
                hasOccurrence = true,
                wakeReady = true,
                readinessDetail = "Scheduled locally and ready for tomorrow.",
                hasTomorrowContract = true,
                tomorrowContractPrepared = true,
                tomorrowContractText = "Design review at 10:00. You wanted time to shower and eat.",
                firstMove = "Shower",
            ),
            onOpenWakeSetup = {},
            onOpenTomorrowPlan = {},
            onOpenWakeLab = {},
            voiceWakeReadiness = VoiceWakeReadiness.READY,
        )
    }
}
