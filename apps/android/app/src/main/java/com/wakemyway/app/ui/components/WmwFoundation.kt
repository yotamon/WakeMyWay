package com.wakemyway.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.LocalWmwPlanningAtmosphere
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSizes
import com.wakemyway.app.ui.theme.WmwSpacing

enum class WmwCircadianStage {
    /** Calm light surface used while planning the next wake. */
    PLANNING,
    EMERGING,
    ENGAGED,
    ACTIVE,
    ORIENTED,
    COMPLETE,
}

enum class WmwActionTone {
    WARM,
    DARK,
    PAPER,
}

/**
 * WakeMyWay atmosphere. Planning screens live in morning-paper light; the active wake progresses
 * through midnight navy toward daylight. Peach/gold and dawn-lavender light are atmospheric
 * accents only, never generic AI gradients.
 */
/**
 * Branded illustrated background only: stage and critical actions remain controlled elsewhere.
 * Fall back to a solid local color even when decorative painting cannot be displayed.
 */
@Composable
fun WmwCircadianSurface(
    stage: WmwCircadianStage,
    modifier: Modifier = Modifier,
    ambientGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = stage == WmwCircadianStage.EMERGING ||
        stage == WmwCircadianStage.ENGAGED || stage == WmwCircadianStage.ACTIVE
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (dark) WmwColors.Midnight else WmwColors.Paper),
    ) {
        if (ambientGlow) SunroomScene(stage = stage, modifier = Modifier.fillMaxSize())
        content()
    }
}

@Composable
fun WmwTimeDisplay(
    time: String,
    modifier: Modifier = Modifier,
    color: Color = WmwColors.WarmLight,
    compact: Boolean = false,
) {
    Text(
        text = time,
        modifier = modifier,
        style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
fun WmwStatusPill(
    label: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
) {
    val accent = if (positive) WmwColors.Success else WmwColors.Sunrise
    val textColor = if (onLightSurface) WmwColors.Midnight else WmwColors.WarmLight
    val containerColor = if (onLightSurface) {
        accent.copy(alpha = 0.10f)
    } else {
        WmwColors.WarmLight.copy(alpha = 0.08f)
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.75.dp,
            color = accent.copy(alpha = 0.30f),
        ),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WmwSpacing.Sm, vertical = WmwSpacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accent, CircleShape),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

@Composable
fun WmwCard(
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(WmwSpacing.Lg),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.75.dp,
                color = if (onLightSurface) Color(0x33D99176) else WmwColors.Hairline,
                shape = shape,
            ),
        color = if (onLightSurface) {
            Color(0xFFFFFCF7).copy(alpha = 0.96f)
        } else {
            WmwColors.ElevatedNightSurface.copy(alpha = 0.88f)
        },
        contentColor = if (onLightSurface) WmwColors.Midnight else WmwColors.WarmLight,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = if (onLightSurface) 5.dp else 0.dp,
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun WmwPrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLightSurface: Boolean = false,
    tone: WmwActionTone = WmwActionTone.WARM,
) {
    val resolvedTone = tone
    val container = when (resolvedTone) {
        WmwActionTone.WARM -> WmwColors.Sunrise
        WmwActionTone.DARK -> WmwColors.Midnight
        WmwActionTone.PAPER -> WmwColors.PaperCard
    }
    val content = when (resolvedTone) {
        WmwActionTone.WARM -> WmwColors.Midnight
        WmwActionTone.DARK -> WmwColors.WarmLight
        WmwActionTone.PAPER -> WmwColors.Midnight
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.PrimaryActionHeight),
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.10f) else WmwColors.DeepNavy,
            disabledContentColor = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.35f) else WmwColors.FaintText,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp, disabledElevation = 0.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun WmwSecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLightSurface: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.SleepyTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.78f) else WmwColors.WarmLight.copy(alpha = 0.90f),
            disabledContentColor = if (onLightSurface) WmwColors.LightFaintText else WmwColors.FaintText,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
