package com.wakemyway.app.ui.home

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Surface
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
import com.wakemyway.app.ui.components.WmwPageHeader
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.components.WmwStatusPill
import com.wakemyway.app.ui.components.WmwSunriseMark
import com.wakemyway.app.ui.components.WmwTimeDisplay
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.app.voice.ConversationalAlfredState
import java.time.LocalTime

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
    val voiceCheckInEnabled: Boolean = true,
    val tomorrowContractAvailable: Boolean = true,
    val tomorrowContractPromptRequired: Boolean = false,
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
    showWakeLab: Boolean = showDeveloperTools,
    voiceWakeReadiness: VoiceWakeReadiness? = null,
    onEnableVoiceReplies: () -> Unit = {},
    onRepairWakeSystem: () -> Unit = {},
    hasMorningCheckIn: Boolean = false,
    onOpenMorningCheckIn: () -> Unit = {},
    greetingOverride: String? = null,
) {
    val context = LocalContext.current
    val greeting = greetingOverride ?: when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
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
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
        ) {
            WmwPageHeader(
                title = stringResource(R.string.tonight_brand_home_title),
                subtitle = stringResource(R.string.tonight_brand_home_subtitle),
                intro = greeting,
            )

            Spacer(Modifier.height(WmwSpacing.Xl))
            NextWakeCard(
                state = state,
                onClick = onOpenWakeSetup,
            )

            if (hasMorningCheckIn) {
                WmwCard(
                    modifier = Modifier
                        .padding(top = WmwSpacing.Md)
                        .clickable(role = Role.Button, onClick = onOpenMorningCheckIn),
                    onLightSurface = true,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
                        Text(
                            text = "MORNING CHECK-IN",
                            style = MaterialTheme.typography.labelSmall,
                            color = WmwColors.DawnText,
                        )
                        Text(
                            text = "Did the last wake actually stick?",
                            style = MaterialTheme.typography.titleMedium,
                            color = WmwColors.Midnight,
                        )
                        Text(
                            text = "One tap helps WakeMyWay learn the difference between phone-observed activation and a morning that really worked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WmwColors.LightQuietText,
                        )
                    }
                }
            }

            if (state.hasOccurrence && !state.wakeReady) {
                WakeSystemAttention(
                    state = state,
                    onRepairWakeSystem = onRepairWakeSystem,
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                )
            }

            if (state.tomorrowContractAvailable) {
                Spacer(Modifier.height(WmwSpacing.Md))
                TomorrowContractPreview(
                    state = state,
                    onClick = onOpenTomorrowPlan,
                )
            }

            voiceWakeReadiness
                ?.takeIf { state.voiceCheckInEnabled && it != VoiceWakeReadiness.READY }
                ?.let { readiness ->
                    WmwCard(
                        modifier = Modifier.padding(top = WmwSpacing.Md),
                        onLightSurface = true,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
                            Text(
                                text = stringResource(R.string.tonight_voice_wake_title).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = WmwColors.LightQuietText,
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
                                color = WmwColors.Midnight,
                            )
                            if (readiness == VoiceWakeReadiness.SETUP_REQUIRED) {
                                WmwSecondaryAction(
                                    label = stringResource(R.string.tonight_voice_wake_enable),
                                    onClick = onEnableVoiceReplies,
                                    onLightSurface = true,
                                )
                            }
                        }
                    }
                }

            if (state.voiceCheckInEnabled) {
                AlfredSignature(
                    quote = if (state.hasOccurrence) {
                        stringResource(R.string.tonight_alfred_quote, state.wakeTime)
                    } else {
                        stringResource(R.string.tonight_alfred_empty_quote)
                    },
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                )
            }

            Spacer(Modifier.height(WmwSpacing.Xl))
            WmwPrimaryAction(
                label = stringResource(
                    if (state.hasOccurrence) R.string.tonight_edit_wake else R.string.tonight_set_wake,
                ),
                onClick = onOpenWakeSetup,
                onLightSurface = true,
                tone = WmwActionTone.WARM,
            )

            if (showDeveloperTools) {
                val conversationReady = ConversationalAlfredState.isReady(context)
                WmwSecondaryAction(
                    label = if (conversationReady) {
                        stringResource(R.string.tonight_review_alfred_connection)
                    } else {
                        stringResource(R.string.tonight_connect_alfred)
                    },
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
                    onLightSurface = true,
                )
            }

            if (showWakeLab) {
                WmwSecondaryAction(
                    label = stringResource(R.string.tonight_open_lab),
                    onClick = onOpenWakeLab,
                    onLightSurface = true,
                )
            }
        }
    }
}

@Composable
private fun NextWakeCard(
    state: TonightUiState,
    onClick: () -> Unit,
) {
    WmwCard(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WmwSpacing.Lg, vertical = WmwSpacing.Lg),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.tonight_next_wake_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = WmwColors.QuietText,
                    )
                    if (state.hasOccurrence) {
                        WmwStatusPill(
                            label = stringResource(
                                if (state.wakeReady) R.string.tonight_wake_ready else R.string.tonight_wake_not_ready,
                            ),
                            positive = state.wakeReady,
                        )
                    }
                }

                WmwTimeDisplay(
                    time = state.wakeTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Xs),
                    compact = true,
                )
                Text(
                    text = state.dateLabel,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.QuietText,
                    textAlign = TextAlign.Center,
                )

                WmwSunriseMark(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .padding(horizontal = WmwSpacing.Xl),
                    onDark = true,
                )

                if (!state.hasOccurrence) {
                    Text(
                        text = state.readinessDetail,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.QuietText,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun TomorrowContractPreview(
    state: TonightUiState,
    onClick: () -> Unit,
) {
    val editDescription = stringResource(R.string.tonight_edit_contract)
    WmwCard(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = editDescription
            },
        onLightSurface = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tonight_section_contract).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WmwColors.LightQuietText,
                )
                Text(
                    text = when {
                        state.hasTomorrowContract -> stringResource(R.string.tonight_contract_ready)
                        state.tomorrowContractPromptRequired -> stringResource(R.string.tonight_contract_prompt)
                        else -> stringResource(R.string.tonight_contract_optional)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.DawnText,
                )
            }
            Text(
                text = stringResource(R.string.tonight_tomorrow_matters),
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = state.tomorrowContractText?.takeIf { it.isNotBlank() }
                    ?: stringResource(
                        if (state.tomorrowContractPromptRequired) {
                            R.string.tonight_contract_prompt_empty
                        } else {
                            R.string.tonight_contract_empty
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )
            state.firstMove?.takeIf { it.isNotBlank() }?.let { firstMove ->
                Text(
                    text = stringResource(R.string.tonight_first_move, firstMove),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.Midnight,
                )
            }
        }
    }
}

@Composable
private fun AlfredSignature(
    quote: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WmwSpacing.Sm, vertical = WmwSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Md),
    ) {
        AlfredMark(Modifier.size(38.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.wake_character_name),
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.Midnight,
            )
            Text(
                text = quote,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightQuietText,
            )
        }
    }
}

@Composable
private fun AlfredMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(
            color = WmwColors.Sunrise.copy(alpha = 0.14f),
            radius = size.minDimension * 0.48f,
        )
        val radius = size.minDimension * 0.24f
        drawCircle(
            color = WmwColors.Sunrise,
            radius = radius,
            style = Stroke(width = 1.3.dp.toPx()),
        )
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val start = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius * 1.28f,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius * 1.28f,
            )
            val end = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * radius * 1.50f,
                y = center.y + kotlin.math.sin(angle).toFloat() * radius * 1.50f,
            )
            drawLine(
                color = WmwColors.Sunrise,
                start = start,
                end = end,
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SettingsGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.23f
        drawCircle(
            color = WmwColors.Midnight.copy(alpha = 0.82f),
            radius = r,
            style = Stroke(width = 1.2.dp.toPx()),
        )
        drawCircle(
            color = WmwColors.Midnight.copy(alpha = 0.82f),
            radius = r * 0.31f,
            style = Stroke(width = 1.2.dp.toPx()),
        )
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val start = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * r * 1.22f,
                center.y + kotlin.math.sin(angle).toFloat() * r * 1.22f,
            )
            val end = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * r * 1.53f,
                center.y + kotlin.math.sin(angle).toFloat() * r * 1.53f,
            )
            drawLine(
                color = WmwColors.Midnight.copy(alpha = 0.72f),
                start = start,
                end = end,
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun WakeSystemAttention(
    state: TonightUiState,
    onRepairWakeSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCard(
        modifier = modifier,
        onLightSurface = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
            Text(
                text = stringResource(R.string.tonight_not_ready_title),
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = state.readinessDetail,
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightQuietText,
            )
            state.wakeRepairActionLabel?.let { repairLabel ->
                WmwPrimaryAction(
                    label = repairLabel,
                    onClick = onRepairWakeSystem,
                    onLightSurface = true,
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