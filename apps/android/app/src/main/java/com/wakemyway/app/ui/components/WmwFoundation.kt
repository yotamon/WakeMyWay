package com.wakemyway.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSizes
import com.wakemyway.app.ui.theme.WmwSpacing

enum class WmwCircadianStage {
    EMERGING,
    ENGAGED,
    ACTIVE,
    ORIENTED,
    COMPLETE,
}

/**
 * Circadian atmosphere is deliberately bounded. The screen stays neutral/ink and gains a
 * low, horizon-like source of warmth as cognition rises instead of becoming a brown gradient.
 * This is presentation enrichment only and can disappear without changing wake behavior.
 */
@Composable
fun WmwCircadianSurface(
    stage: WmwCircadianStage,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val glowColor = when (stage) {
        WmwCircadianStage.EMERGING -> WmwColors.Clay.copy(alpha = 0.07f)
        WmwCircadianStage.ENGAGED -> WmwColors.Clay.copy(alpha = 0.11f)
        WmwCircadianStage.ACTIVE -> WmwColors.SoftEmber.copy(alpha = 0.16f)
        WmwCircadianStage.ORIENTED -> WmwColors.SoftEmber.copy(alpha = 0.20f)
        WmwCircadianStage.COMPLETE -> WmwColors.Sage.copy(alpha = 0.14f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WmwColors.Ink),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val horizonCenter = Offset(size.width * 0.50f, size.height * 1.06f)
            val horizonRadius = size.maxDimension * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = horizonCenter,
                    radius = horizonRadius,
                ),
                radius = horizonRadius,
                center = horizonCenter,
            )

            // A cool-neutral depth field keeps the top half dimensional without introducing
            // another brand color or an AI-style multicolor gradient.
            val depthCenter = Offset(size.width * 0.18f, size.height * 0.18f)
            val depthRadius = size.maxDimension * 0.54f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WmwColors.DeepDawn.copy(alpha = 0.22f), Color.Transparent),
                    center = depthCenter,
                    radius = depthRadius,
                ),
                radius = depthRadius,
                center = depthCenter,
            )
        }
        content()
    }
}

@Composable
fun WmwTimeDisplay(
    time: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = time,
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge,
        color = WmwColors.WarmLight,
        textAlign = textAlign,
        maxLines = 1,
    )
}

@Composable
fun WmwStatusPill(
    label: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (positive) WmwColors.Sage else WmwColors.SoftEmber
    Row(
        modifier = modifier
            .background(
                color = accent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(100.dp),
            )
            .padding(horizontal = WmwSpacing.Sm, vertical = WmwSpacing.Xs),
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
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.MorningPaper,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

enum class WmwCardEmphasis {
    QUIET,
    STANDARD,
    RAISED,
}

@Composable
fun WmwCard(
    modifier: Modifier = Modifier,
    emphasis: WmwCardEmphasis = WmwCardEmphasis.STANDARD,
    content: @Composable () -> Unit,
) {
    val surfaceColor = when (emphasis) {
        WmwCardEmphasis.QUIET -> WmwColors.QuietSurface.copy(alpha = 0.76f)
        WmwCardEmphasis.STANDARD -> WmwColors.ElevatedNightSurface.copy(alpha = 0.88f)
        WmwCardEmphasis.RAISED -> WmwColors.RaisedSurface.copy(alpha = 0.94f)
    }
    val borderColor = when (emphasis) {
        WmwCardEmphasis.QUIET -> WmwColors.Hairline.copy(alpha = 0.55f)
        WmwCardEmphasis.STANDARD -> WmwColors.Hairline
        WmwCardEmphasis.RAISED -> WmwColors.StrongHairline
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = surfaceColor,
        contentColor = WmwColors.MorningPaper,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(modifier = Modifier.padding(WmwSpacing.Xl)) {
            content()
        }
    }
}

@Composable
fun WmwDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = WmwColors.MorningPaper,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.MinimumTouchTarget),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = WmwColors.QuietText,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.35f),
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun WmwDetailDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = WmwColors.Hairline,
    )
}

@Composable
fun WmwPrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.PrimaryActionHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = WmwColors.SoftEmber,
            contentColor = WmwColors.Ink,
            disabledContainerColor = WmwColors.DeepDawn,
            disabledContentColor = WmwColors.QuietText,
        ),
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
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.SleepyTouchTarget),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, WmwColors.Hairline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = WmwColors.MorningPaper,
            disabledContentColor = WmwColors.QuietText,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
