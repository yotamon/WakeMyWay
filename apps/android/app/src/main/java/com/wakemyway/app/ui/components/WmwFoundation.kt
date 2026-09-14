package com.wakemyway.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.graphicsLayer
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

enum class WmwActionTone {
    WARM,
    DARK,
    PAPER,
}

/**
 * Full-screen atmosphere from the approved concept board. Dark states stay genuinely black and
 * warmth is concentrated around the Wake Line rather than washing the whole screen brown.
 */
@Composable
fun WmwCircadianSurface(
    stage: WmwCircadianStage,
    modifier: Modifier = Modifier,
    ambientGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = when (stage) {
        WmwCircadianStage.EMERGING -> Brush.verticalGradient(
            listOf(Color(0xFF040607), Color(0xFF07090A), Color(0xFF090909)),
        )
        WmwCircadianStage.ENGAGED -> Brush.verticalGradient(
            listOf(Color(0xFF050708), Color(0xFF0A0B0C), Color(0xFF0B0A0A)),
        )
        WmwCircadianStage.ACTIVE -> Brush.verticalGradient(
            listOf(
                Color(0xFF07090A),
                Color(0xFF10100F),
                Color(0xFF241C18),
                Color(0xFF5D402F),
                Color(0xFF33241C),
                Color(0xFF11100F),
                Color(0xFF07090A),
            ),
        )
        WmwCircadianStage.ORIENTED -> Brush.verticalGradient(
            listOf(Color(0xFFF4E8DD), WmwColors.MorningPaper, Color(0xFFEFE2D5)),
        )
        WmwCircadianStage.COMPLETE -> Brush.verticalGradient(
            listOf(Color(0xFFF6EBDD), Color(0xFFF0E1D1), Color(0xFFEBDCCB)),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        if (
            ambientGlow &&
            stage != WmwCircadianStage.ORIENTED &&
            stage != WmwCircadianStage.COMPLETE
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = when (stage) {
                    WmwCircadianStage.EMERGING -> Offset(size.width * 0.50f, size.height * 0.58f)
                    WmwCircadianStage.ENGAGED -> Offset(size.width * 0.50f, size.height * 0.62f)
                    WmwCircadianStage.ACTIVE -> Offset(size.width * 0.50f, size.height * 0.60f)
                    WmwCircadianStage.ORIENTED,
                    WmwCircadianStage.COMPLETE,
                    -> Offset.Zero
                }
                val glow = when (stage) {
                    WmwCircadianStage.EMERGING -> WmwColors.ClayGlow.copy(alpha = 0.055f)
                    WmwCircadianStage.ENGAGED -> WmwColors.ClayGlow.copy(alpha = 0.12f)
                    WmwCircadianStage.ACTIVE -> WmwColors.EmberGlow.copy(alpha = 0.48f)
                    WmwCircadianStage.ORIENTED,
                    WmwCircadianStage.COMPLETE,
                    -> Color.Transparent
                }
                val radiusFraction = when (stage) {
                    WmwCircadianStage.EMERGING -> 0.30f
                    WmwCircadianStage.ENGAGED -> 0.36f
                    WmwCircadianStage.ACTIVE -> 0.25f
                    WmwCircadianStage.ORIENTED,
                    WmwCircadianStage.COMPLETE,
                    -> 0f
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glow,
                            glow.copy(alpha = glow.alpha * 0.48f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.maxDimension * radiusFraction,
                    ),
                    radius = size.maxDimension * radiusFraction,
                    center = center,
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
    compact: Boolean = false,
) {
    Text(
        text = time,
        modifier = if (compact) {
            modifier.graphicsLayer {
                scaleX = 1.36f
                scaleY = 1.36f
            }
        } else {
            modifier
        },
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
    val accent = if (positive) WmwColors.Success else WmwColors.SoftEmber
    val textColor = if (onLightSurface) WmwColors.Ink else WmwColors.WarmLight
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accent, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
        )
    }
}

@Composable
fun WmwCard(
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(WmwSpacing.Lg),
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.75.dp,
                color = if (onLightSurface) WmwColors.DarkHairline else WmwColors.Hairline.copy(alpha = 0.52f),
                shape = shape,
            ),
        color = if (onLightSurface) {
            WmwColors.PaperCard.copy(alpha = 0.72f)
        } else {
            WmwColors.ElevatedNightSurface.copy(alpha = 0.78f)
        },
        contentColor = if (onLightSurface) WmwColors.Ink else WmwColors.WarmLight,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
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
    val resolvedTone = if (onLightSurface && tone == WmwActionTone.WARM) WmwActionTone.DARK else tone
    val container = when (resolvedTone) {
        WmwActionTone.WARM -> WmwColors.SoftEmber
        WmwActionTone.DARK -> WmwColors.ElevatedNightSurface
        WmwActionTone.PAPER -> WmwColors.MorningPaper
    }
    val content = when (resolvedTone) {
        WmwActionTone.WARM -> WmwColors.Ink
        WmwActionTone.DARK -> WmwColors.WarmLight
        WmwActionTone.PAPER -> WmwColors.Ink
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WmwSizes.PrimaryActionHeight),
        enabled = enabled,
        shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (onLightSurface) WmwColors.Ink.copy(alpha = 0.12f) else WmwColors.DeepDawn,
            disabledContentColor = if (onLightSurface) WmwColors.Ink.copy(alpha = 0.36f) else WmwColors.FaintText,
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
            contentColor = if (onLightSurface) WmwColors.Ink.copy(alpha = 0.82f) else WmwColors.WarmLight.copy(alpha = 0.9f),
            disabledContentColor = WmwColors.FaintText,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
