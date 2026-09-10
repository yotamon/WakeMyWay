package com.wakemyway.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

@Composable
fun WmwCircadianSurface(
    stage: WmwCircadianStage,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = when (stage) {
        WmwCircadianStage.EMERGING -> listOf(WmwColors.Ink, WmwColors.DeepDawn)
        WmwCircadianStage.ENGAGED -> listOf(WmwColors.Ink, WmwColors.DeepDawn, WmwColors.Clay.copy(alpha = 0.28f))
        WmwCircadianStage.ACTIVE -> listOf(WmwColors.DeepDawn, WmwColors.Clay.copy(alpha = 0.48f), WmwColors.Ink)
        WmwCircadianStage.ORIENTED -> listOf(WmwColors.DeepDawn, WmwColors.Clay.copy(alpha = 0.58f), WmwColors.Ink)
        WmwCircadianStage.COMPLETE -> listOf(WmwColors.DeepDawn, WmwColors.Sage.copy(alpha = 0.28f), WmwColors.Ink)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val glowColor = when (stage) {
                WmwCircadianStage.EMERGING -> WmwColors.ClayGlow.copy(alpha = 0.16f)
                WmwCircadianStage.ENGAGED -> WmwColors.ClayGlow.copy(alpha = 0.28f)
                WmwCircadianStage.ACTIVE -> WmwColors.EmberGlow.copy(alpha = 0.36f)
                WmwCircadianStage.ORIENTED -> WmwColors.EmberGlow.copy(alpha = 0.44f)
                WmwCircadianStage.COMPLETE -> WmwColors.Sage.copy(alpha = 0.24f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(size.width * 0.72f, size.height * 0.82f),
                    radius = size.maxDimension * 0.72f,
                ),
                radius = size.maxDimension * 0.72f,
                center = Offset(size.width * 0.72f, size.height * 0.82f),
            )
        }
        content()
    }
}

@Composable
fun WmwTimeDisplay(
    time: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = time,
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge,
        color = WmwColors.WarmLight,
        textAlign = TextAlign.Center,
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
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(100.dp),
            )
            .padding(horizontal = WmwSpacing.Md, vertical = WmwSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = WmwColors.MorningPaper,
        )
    }
}

@Composable
fun WmwCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WmwColors.ElevatedNightSurface.copy(alpha = 0.72f),
        contentColor = WmwColors.MorningPaper,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(WmwSpacing.Xl)) {
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
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.SleepyTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
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
