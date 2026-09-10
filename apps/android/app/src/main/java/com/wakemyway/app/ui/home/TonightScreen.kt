package com.wakemyway.app.ui.home

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.wakemyway.app.R
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCardEmphasis
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwDetailDivider
import com.wakemyway.app.ui.components.WmwDetailRow
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

data class TonightUiState(
    val wakeTime: String,
    val dateLabel: String,
    val countdownLabel: String,
    val hasOccurrence: Boolean,
    val wakeReady: Boolean,
    val readinessDetail: String,
    val hasTomorrowContract: Boolean,
    val tomorrowContractPrepared: Boolean,
)

@Composable
fun TonightScreen(
    state: TonightUiState,
    onOpenWakeSetup: () -> Unit,
    onOpenTomorrowPlan: () -> Unit,
    onOpenWakeLab: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            TonightHeader(state = state)

            if (state.hasOccurrence) {
                ReadyTonightContent(
                    state = state,
                    onOpenWakeSetup = onOpenWakeSetup,
                    onOpenTomorrowPlan = onOpenTomorrowPlan,
                )
            } else {
                EmptyTonightContent(
                    onOpenWakeSetup = onOpenWakeSetup,
                )
            }

            Spacer(Modifier.height(WmwSpacing.Xxl))

            TextButton(
                onClick = onOpenWakeLab,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.tonight_redesign_developer_tools),
                    style = MaterialTheme.typography.labelMedium,
                    color = WmwColors.QuietText,
                )
            }

            Spacer(Modifier.height(WmwSpacing.Lg))
        }
    }
}

@Composable
private fun TonightHeader(state: TonightUiState) {
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
        if (state.hasOccurrence) {
            WmwStatusPill(
                label = stringResource(
                    if (state.wakeReady) {
                        R.string.tonight_redesign_ready_status
                    } else {
                        R.string.tonight_redesign_attention_status
                    },
                ),
                positive = state.wakeReady,
            )
        }
    }
}

@Composable
private fun ReadyTonightContent(
    state: TonightUiState,
    onOpenWakeSetup: () -> Unit,
    onOpenTomorrowPlan: () -> Unit,
) {
    Spacer(Modifier.height(WmwSpacing.Huge))

    Text(
        text = stringResource(R.string.tonight_redesign_next_wake),
        style = MaterialTheme.typography.labelSmall,
        color = WmwColors.QuietText,
    )
    WmwTimeDisplay(
        time = state.wakeTime,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WmwSpacing.Xxs),
        textAlign = TextAlign.Start,
    )
    Text(
        text = state.dateLabel,
        style = MaterialTheme.typography.titleMedium,
        color = WmwColors.MorningPaper,
    )
    Text(
        text = state.countdownLabel,
        modifier = Modifier.padding(top = WmwSpacing.Xs),
        style = MaterialTheme.typography.bodyMedium,
        color = WmwColors.SoftEmber,
    )

    Spacer(Modifier.height(WmwSpacing.Xxxl))

    AlfredSummary(state = state)

    Spacer(Modifier.height(WmwSpacing.Xxxl))

    Text(
        text = stringResource(R.string.tonight_redesign_morning_title),
        style = MaterialTheme.typography.headlineSmall,
        color = WmwColors.WarmLight,
    )

    WmwCard(
        modifier = Modifier.padding(top = WmwSpacing.Md),
        emphasis = WmwCardEmphasis.RAISED,
    ) {
        Column {
            WmwDetailRow(
                label = stringResource(R.string.tonight_redesign_row_wake),
                value = "${state.dateLabel} · ${state.wakeTime}",
            )
            WmwDetailDivider()
            WmwDetailRow(
                label = stringResource(R.string.tonight_redesign_row_context),
                value = stringResource(
                    when {
                        state.tomorrowContractPrepared -> R.string.tonight_redesign_context_prepared
                        state.hasTomorrowContract -> R.string.tonight_redesign_context_needs_preparation
                        else -> R.string.tonight_redesign_context_optional
                    },
                ),
                valueColor = if (state.hasTomorrowContract && !state.tomorrowContractPrepared) {
                    WmwColors.SoftEmber
                } else {
                    WmwColors.MorningPaper
                },
            )
            WmwDetailDivider()
            WmwDetailRow(
                label = stringResource(R.string.tonight_redesign_row_system),
                value = stringResource(
                    if (state.wakeReady) {
                        R.string.tonight_redesign_system_ready
                    } else {
                        R.string.tonight_redesign_system_attention
                    },
                ),
                valueColor = if (state.wakeReady) WmwColors.Sage else WmwColors.SoftEmber,
            )

            if (!state.wakeReady) {
                Text(
                    text = state.readinessDetail,
                    modifier = Modifier.padding(top = WmwSpacing.Sm),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.QuietText,
                )
            }
        }
    }

    Spacer(Modifier.height(WmwSpacing.Xl))

    WmwPrimaryAction(
        label = stringResource(R.string.tonight_edit_wake),
        onClick = onOpenWakeSetup,
    )
    WmwSecondaryAction(
        label = stringResource(
            if (state.hasTomorrowContract) {
                R.string.tonight_edit_contract
            } else {
                R.string.tonight_add_contract
            },
        ),
        onClick = onOpenTomorrowPlan,
        modifier = Modifier.padding(top = WmwSpacing.Sm),
    )
}

@Composable
private fun AlfredSummary(state: TonightUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WmwPresence(
            state = if (state.wakeReady) WmwPresenceState.LISTENING else WmwPresenceState.QUIET,
            contentDescription = stringResource(
                if (state.wakeReady) {
                    R.string.tonight_redesign_presence_ready_description
                } else {
                    R.string.tonight_redesign_presence_attention_description
                },
            ),
            size = WmwSizes.PresenceCompact,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xxs),
        ) {
            Text(
                text = stringResource(R.string.tonight_redesign_alfred_name),
                style = MaterialTheme.typography.labelLarge,
                color = WmwColors.MorningPaper,
            )
            Text(
                text = stringResource(
                    if (state.wakeReady) {
                        R.string.tonight_redesign_alfred_ready
                    } else {
                        R.string.tonight_redesign_alfred_attention
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.QuietText,
            )
        }
    }
}

@Composable
private fun EmptyTonightContent(onOpenWakeSetup: () -> Unit) {
    Spacer(Modifier.height(WmwSpacing.Hero))

    Text(
        text = stringResource(R.string.tonight_redesign_empty_eyebrow),
        style = MaterialTheme.typography.labelSmall,
        color = WmwColors.QuietText,
    )
    Text(
        text = stringResource(R.string.tonight_redesign_empty_title),
        modifier = Modifier.padding(top = WmwSpacing.Sm),
        style = MaterialTheme.typography.headlineLarge,
        color = WmwColors.WarmLight,
    )
    Text(
        text = stringResource(R.string.tonight_redesign_empty_body),
        modifier = Modifier.padding(top = WmwSpacing.Md),
        style = MaterialTheme.typography.bodyLarge,
        color = WmwColors.QuietText,
    )

    Spacer(Modifier.height(WmwSpacing.Xxxl))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WmwPresence(
            state = WmwPresenceState.QUIET,
            contentDescription = stringResource(R.string.tonight_redesign_presence_empty_description),
            size = WmwSizes.PresenceCompact,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.tonight_redesign_alfred_name),
                style = MaterialTheme.typography.labelLarge,
                color = WmwColors.MorningPaper,
            )
            Text(
                text = stringResource(R.string.tonight_redesign_alfred_empty),
                modifier = Modifier.padding(top = WmwSpacing.Xxs),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.QuietText,
            )
        }
    }

    Spacer(Modifier.height(WmwSpacing.Huge))

    WmwPrimaryAction(
        label = stringResource(R.string.tonight_set_wake),
        onClick = onOpenWakeSetup,
    )
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
                countdownLabel = "in 7h 42m",
                hasOccurrence = true,
                wakeReady = true,
                readinessDetail = "Scheduled locally with critical wake capabilities available.",
                hasTomorrowContract = true,
                tomorrowContractPrepared = true,
            ),
            onOpenWakeSetup = {},
            onOpenTomorrowPlan = {},
            onOpenWakeLab = {},
        )
    }
}

@Preview(
    name = "Tonight attention",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
)
@Composable
private fun TonightAttentionPreview() {
    WakeMyWayTheme {
        TonightScreen(
            state = TonightUiState(
                wakeTime = "07:30",
                dateLabel = "Friday · Sep 11",
                countdownLabel = "in 6h 18m",
                hasOccurrence = true,
                wakeReady = false,
                readinessDetail = "Wake My Way needs Android's exact alarm capability for this wake.",
                hasTomorrowContract = false,
                tomorrowContractPrepared = false,
            ),
            onOpenWakeSetup = {},
            onOpenTomorrowPlan = {},
            onOpenWakeLab = {},
        )
    }
}

@Preview(
    name = "Tonight empty",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
)
@Composable
private fun TonightEmptyPreview() {
    WakeMyWayTheme {
        TonightScreen(
            state = TonightUiState(
                wakeTime = "--:--",
                dateLabel = "Next wake",
                countdownLabel = "",
                hasOccurrence = false,
                wakeReady = false,
                readinessDetail = "Create a wake plan before calling tomorrow ready.",
                hasTomorrowContract = false,
                tomorrowContractPrepared = false,
            ),
            onOpenWakeSetup = {},
            onOpenTomorrowPlan = {},
            onOpenWakeLab = {},
        )
    }
}
