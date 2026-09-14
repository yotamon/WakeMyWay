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
        WmwCircadianStage.EMERGING -> listOf(WmwColors.Ink, Color(0xFF15161A))
        WmwCircadianStage.ENGAGED -> listOf(WmwColors.Ink, WmwColors.DeepDawn.copy(alpha = 0.96f), WmwColors.Ink)
        WmwCircadianStage.ACTIVE -> listOf(WmwColors.DeepDawn, Color(0xFF35262A), WmwColors.Ink)
        WmwCircadianStage.ORIENTED -> listOf(WmwColors.WarmLight, WmwColors.MorningPaper)
        WmwCircadianStage.COMPLETE -> listOf(WmwColors.WarmLight, Color(0xFFF0E6DA))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette)),
    ) {
        if (stage != WmwCircadianStage.ORIENTED && stage != WmwCircadianStage.COMPLETE) {
            Canvas(Modifier.fillMaxSize()) {
                val glowColor = when (stage) {
                    WmwCircadianStage.EMERGING -> WmwColors.ClayGlow.copy(alpha = 0.09f)
                    WmwCircadianStage.ENGAGED -> WmwColors.ClayGlow.copy(alpha = 0.18f)
                    WmwCircadianStage.ACTIVE -> WmwColors.EmberGlow.copy(alpha = 0.3f)
                    WmwCircadianStage.ORIENTED,
                    WmwCircadianStage.COMPLETE,
                    -> Color.Transparent
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(size.width * 0.55f, size.height * 0.78f),
                        radius = size.maxDimension * 0.65f,
                    ),
                    radius = size.maxDimension * 0.65f,
                    center = Offset(size.width * 0.55f, size.height * 0.78f),
                )
            }
        }
        content()
    }
}

@Composable
fun WmwTimeDisplay(
    time: String,
    modifier: Modifier = Modifier,
    color: Color = WmwColors.WarmLight,
) {
    Text(
        text = time,
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge,
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
    val accent = if (positive) WmwColors.Sage else WmwColors.SoftEmber
    val textColor = if (onLightSurface) WmwColors.Ink else WmwColors.MorningPaper
    Row(
        modifier = modifier
            .background(
                color = accent.copy(alpha = if (onLightSurface) 0.11f else 0.12f),
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
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
fun WmwCard(
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (onLightSurface) {
            Color.White.copy(alpha = 0.42f)
        } else {
            WmwColors.ElevatedNightSurface.copy(alpha = 0.56f)
        },
        contentColor = if (onLightSurface) WmwColors.Ink else WmwColors.MorningPaper,
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
    onLightSurface: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.PrimaryActionHeight),
        enabled = enabled,
        shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (onLightSurface) WmwColors.Ink.copy(alpha = 0.74f) else WmwColors.SoftEmber,
            contentColor = if (onLightSurface) WmwColors.WarmLight else WmwColors.Ink,
            disabledContainerColor = if (onLightSurface) WmwColors.Ink.copy(alpha = 0.14f) else WmwColors.DeepDawn,
            disabledContentColor = if (onLightSurface) WmwColors.Ink.copy(alpha = 0.42f) else WmwColors.QuietText,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
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
            contentColor = if (onLightSurface) WmwColors.Ink else WmwColors.MorningPaper,
            disabledContentColor = WmwColors.QuietText,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
