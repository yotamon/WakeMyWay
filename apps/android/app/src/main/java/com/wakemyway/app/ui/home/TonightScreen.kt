package com.wakemyway.app.ui.home

import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wakemyway.app.R
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
                .padding(horizontal = WmwSpacing.Xl, vertical = WmwSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = WmwColors.QuietText,
                )
                if (showDeveloperTools) {
                    Text(
                        text = stringResource(R.string.wmw_founder_build).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = WmwColors.QuietText.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(WmwSpacing.Hero))

            WmwTimeDisplay(
                time = state.wakeTime,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.dateLabel,
                modifier = Modifier.padding(top = WmwSpacing.Xs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.MorningPaper,
            )

            WmwWakeLine(
                state = if (state.wakeReady) WmwWakeLineState.QUIET else WmwWakeLineState.SETTLED,
                modifier = Modifier.padding(top = WmwSpacing.Lg),
            )

            WmwStatusPill(
                label = stringResource(
                    if (state.wakeReady) R.string.tonight_wake_ready else R.string.tonight_wake_not_ready,
                ),
                positive = state.wakeReady,
                modifier = Modifier.padding(top = WmwSpacing.Xs),
            )

            Spacer(Modifier.height(WmwSpacing.Xxl))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WmwColors.Hairline),
            )
            Spacer(Modifier.height(WmwSpacing.Xl))

            if (state.hasOccurrence) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm),
                ) {
                    Text(
                        text = stringResource(R.string.tonight_tomorrow_matters).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = WmwColors.QuietText,
                    )
                    Text(
                        text = state.tomorrowContractText?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.tonight_contract_empty),
                        style = MaterialTheme.typography.titleLarge,
                        color = WmwColors.MorningPaper,
                    )
                    state.firstMove?.takeIf { it.isNotBlank() }?.let { firstMove ->
                        Text(
                            text = stringResource(R.string.tonight_first_move, firstMove),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.QuietText,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.tonight_no_wake_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = WmwColors.WarmLight,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = state.readinessDetail,
                        modifier = Modifier.padding(top = WmwSpacing.Sm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WmwColors.QuietText,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (state.hasOccurrence && !state.wakeReady) {
                WakeSystemAttention(
                    state = state,
                    onRepairWakeSystem = onRepairWakeSystem,
                    modifier = Modifier.padding(top = WmwSpacing.Xl),
                )
            }

            Spacer(Modifier.height(WmwSpacing.Xxxl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.wake_character_name).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = WmwColors.MorningPaper,
                )
                Text(
                    text = stringResource(R.string.tonight_alfred_descriptor),
                    modifier = Modifier.padding(top = WmwSpacing.Xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.QuietText,
                )
                Text(
                    text = stringResource(R.string.tonight_alfred_quote),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Md),
                    style = MaterialTheme.typography.titleLarge,
                    color = WmwColors.MorningPaper,
                    textAlign = TextAlign.Center,
                )
            }

            voiceWakeReadiness?.takeIf { it != VoiceWakeReadiness.READY }?.let { readiness ->
                WmwCard(modifier = Modifier.padding(top = WmwSpacing.Xl)) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
                        Text(
                            text = stringResource(R.string.tonight_voice_wake_title),
                            style = MaterialTheme.typography.labelMedium,
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.MorningPaper,
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

            Spacer(Modifier.height(WmwSpacing.Xxl))

            WmwPrimaryAction(
                label = stringResource(
                    if (state.hasOccurrence) R.string.tonight_edit_wake else R.string.tonight_set_wake,
                ),
                onClick = onOpenWakeSetup,
            )
            if (state.hasOccurrence) {
                WmwSecondaryAction(
                    label = stringResource(
                        if (state.hasTomorrowContract) {
                            R.string.tonight_edit_contract
                        } else {
                            R.string.tonight_add_contract
                        },
                    ),
                    onClick = onOpenTomorrowPlan,
                    modifier = Modifier.padding(top = WmwSpacing.Xs),
                )
            }

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

            Spacer(Modifier.height(WmwSpacing.Xxl))
        }
    }
}

@Composable
private fun WakeSystemAttention(
    state: TonightUiState,
    onRepairWakeSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
            Text(
                text = stringResource(R.string.tonight_not_ready_title),
                style = MaterialTheme.typography.titleLarge,
                color = WmwColors.MorningPaper,
            )
            Text(
                text = state.readinessDetail,
                style = MaterialTheme.typography.bodyMedium,
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
                dateLabel = "Tuesday · Jan 14",
                hasOccurrence = true,
                wakeReady = true,
                readinessDetail = "Scheduled locally and ready for tomorrow.",
                hasTomorrowContract = true,
                tomorrowContractPrepared = true,
                tomorrowContractText = "Design review at 10:00. I want to be prepared, showered and have a calm breakfast.",
                firstMove = "Shower",
            ),
            onOpenWakeSetup = {},
            onOpenTomorrowPlan = {},
            onOpenWakeLab = {},
            voiceWakeReadiness = VoiceWakeReadiness.READY,
        )
    }
}
