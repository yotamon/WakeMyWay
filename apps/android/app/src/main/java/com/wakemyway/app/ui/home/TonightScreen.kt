package com.wakemyway.app.ui.home

import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.wakemyway.app.R
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPresence
import com.wakemyway.app.ui.components.WmwPresenceState
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSecondaryAction
import com.wakemyway.app.ui.components.WmwStatusPill
import com.wakemyway.app.ui.components.WmwTimeDisplay
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSizes
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
                    text = stringResource(R.string.wmw_brand_short),
                    style = MaterialTheme.typography.titleLarge,
                    color = WmwColors.WarmLight,
                    fontWeight = FontWeight.SemiBold,
                )
                if (showDeveloperTools) {
                    Text(
                        text = stringResource(R.string.wmw_founder_build),
                        style = MaterialTheme.typography.labelMedium,
                        color = WmwColors.QuietText,
                    )
                }
            }

            Spacer(Modifier.height(WmwSpacing.Hero))

            Text(
                text = stringResource(R.string.tonight_greeting),
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.QuietText,
            )
            WmwTimeDisplay(
                time = state.wakeTime,
                modifier = Modifier.padding(top = WmwSpacing.Xs),
            )
            Text(
                text = state.dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.MorningPaper,
            )

            Spacer(Modifier.height(WmwSpacing.Xxl))

            WmwPresence(
                state = if (state.wakeReady) WmwPresenceState.LISTENING else WmwPresenceState.QUIET,
                contentDescription = stringResource(
                    if (state.wakeReady) {
                        R.string.tonight_presence_description
                    } else {
                        R.string.tonight_presence_waiting_description
                    },
                ),
                size = WmwSizes.PresenceSmall,
            )
            Text(
                text = stringResource(
                    if (state.wakeReady) {
                        R.string.tonight_character_ready
                    } else {
                        R.string.tonight_character_waiting
                    },
                ),
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.labelLarge,
                color = WmwColors.MorningPaper,
            )

            Spacer(Modifier.height(WmwSpacing.Xxxl))

            Text(
                text = stringResource(
                    when {
                        !state.hasOccurrence -> R.string.tonight_no_wake_title
                        state.wakeReady -> R.string.tonight_ready_title
                        else -> R.string.tonight_not_ready_title
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = WmwColors.WarmLight,
            )

            val showCriticalRepairFirst = state.hasOccurrence && !state.wakeReady
            if (showCriticalRepairFirst) {
                WakeSystemCard(
                    state = state,
                    onRepairWakeSystem = onRepairWakeSystem,
                    modifier = Modifier.padding(top = WmwSpacing.Lg),
                )
            }

            WmwCard(
                modifier = Modifier.padding(
                    top = if (showCriticalRepairFirst) WmwSpacing.Sm else WmwSpacing.Lg,
                ),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
                    Text(
                        text = stringResource(R.string.tonight_section_tomorrow),
                        style = MaterialTheme.typography.labelMedium,
                        color = WmwColors.QuietText,
                    )
                    Text(
                        text = if (state.hasOccurrence) {
                            "${state.dateLabel} · ${state.wakeTime}"
                        } else {
                            stringResource(R.string.tonight_no_occurrence)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = WmwColors.MorningPaper,
                    )
                }
            }

            if (state.hasOccurrence) {
                WmwCard(modifier = Modifier.padding(top = WmwSpacing.Sm)) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.tonight_section_contract),
                                style = MaterialTheme.typography.titleMedium,
                                color = WmwColors.MorningPaper,
                            )
                            WmwStatusPill(
                                label = stringResource(
                                    if (state.tomorrowContractPrepared) {
                                        R.string.tonight_contract_ready
                                    } else {
                                        R.string.tonight_contract_optional
                                    },
                                ),
                                positive = state.tomorrowContractPrepared,
                            )
                        }
                        Text(
                            text = stringResource(
                                if (state.tomorrowContractPrepared) {
                                    R.string.tonight_contract_prepared
                                } else {
                                    R.string.tonight_contract_empty
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.QuietText,
                        )
                    }
                }
            }

            if (!showCriticalRepairFirst) {
                WakeSystemCard(
                    state = state,
                    onRepairWakeSystem = onRepairWakeSystem,
                    modifier = Modifier.padding(top = WmwSpacing.Sm),
                )
            }

            voiceWakeReadiness?.let { readiness ->
                WmwCard(modifier = Modifier.padding(top = WmwSpacing.Sm)) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.tonight_voice_wake_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = WmwColors.MorningPaper,
                            )
                            WmwStatusPill(
                                label = stringResource(
                                    when (readiness) {
                                        VoiceWakeReadiness.READY -> R.string.tonight_voice_wake_ready
                                        VoiceWakeReadiness.SETUP_REQUIRED -> R.string.tonight_voice_wake_setup
                                        VoiceWakeReadiness.UNAVAILABLE -> R.string.tonight_voice_wake_unavailable
                                    },
                                ),
                                positive = readiness == VoiceWakeReadiness.READY,
                            )
                        }
                        Text(
                            text = stringResource(
                                when (readiness) {
                                    VoiceWakeReadiness.READY -> R.string.tonight_voice_wake_ready_detail
                                    VoiceWakeReadiness.SETUP_REQUIRED -> R.string.tonight_voice_wake_setup_detail
                                    VoiceWakeReadiness.UNAVAILABLE -> R.string.tonight_voice_wake_unavailable_detail
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.QuietText,
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

            if (showDeveloperTools) {
                val conversationReady = ConversationalAlfredState.isReady(context)
                WmwCard(modifier = Modifier.padding(top = WmwSpacing.Sm)) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Conversational Alfred",
                                style = MaterialTheme.typography.titleMedium,
                                color = WmwColors.MorningPaper,
                            )
                            WmwStatusPill(
                                label = if (conversationReady) "Ready" else "Optional",
                                positive = conversationReady,
                            )
                        }
                        Text(
                            text = if (conversationReady) {
                                "Natural Realtime conversation is connected for this phone. Local Alfred remains the automatic fallback."
                            } else {
                                "Connect once for natural back-and-forth conversation. Alarm safety and Wake Ready remain completely local."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.QuietText,
                        )
                        WmwSecondaryAction(
                            label = if (conversationReady) "Review connection" else "Connect Alfred",
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
                    }
                }
            }

            Spacer(Modifier.height(WmwSpacing.Xl))

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
                WmwSecondaryAction(
                    label = stringResource(R.string.tonight_open_lab),
                    onClick = onOpenWakeLab,
                )
                Text(
                    text = stringResource(R.string.tonight_lab_note),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.QuietText,
                )
            }

            Spacer(Modifier.height(WmwSpacing.Xxl))
        }
    }
}

@Composable
private fun WakeSystemCard(
    state: TonightUiState,
    onRepairWakeSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tonight_section_wake_system),
                    style = MaterialTheme.typography.titleMedium,
                    color = WmwColors.MorningPaper,
                )
                WmwStatusPill(
                    label = stringResource(
                        if (state.wakeReady) {
                            R.string.tonight_wake_ready
                        } else {
                            R.string.tonight_wake_not_ready
                        },
                    ),
                    positive = state.wakeReady,
                )
            }
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
                wakeTime = "08:00",
                dateLabel = "Thursday · Sep 10",
                hasOccurrence = true,
                wakeReady = true,
                readinessDetail = "Scheduled locally and ready for tomorrow.",
                hasTomorrowContract = true,
                tomorrowContractPrepared = true,
            ),
            onOpenWakeSetup = {},
            onOpenTomorrowPlan = {},
            onOpenWakeLab = {},
            voiceWakeReadiness = VoiceWakeReadiness.READY,
        )
    }
}

@Preview(
    name = "Tonight needs wake access",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
)
@Composable
private fun TonightNeedsWakeAccessPreview() {
    WakeMyWayTheme {
        TonightScreen(
            state = TonightUiState(
                wakeTime = "08:00",
                dateLabel = "Thursday · Sep 10",
                hasOccurrence = true,
                wakeReady = false,
                readinessDetail = "Allow full-screen alarms so Wake My Way can open the wake screen when your phone is locked.",
                wakeRepairActionLabel = "Allow full-screen alarms",
                hasTomorrowContract = false,
                tomorrowContractPrepared = false,
            ),
            onOpenWakeSetup = {},
            onOpenTomorrowPlan = {},
            onOpenWakeLab = {},
            voiceWakeReadiness = VoiceWakeReadiness.SETUP_REQUIRED,
        )
    }
}
