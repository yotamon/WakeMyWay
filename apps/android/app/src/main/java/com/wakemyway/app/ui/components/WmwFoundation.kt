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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
@Composable
fun WmwCircadianSurface(
    stage: WmwCircadianStage,
    modifier: Modifier = Modifier,
    ambientGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = when (stage) {
        WmwCircadianStage.PLANNING -> Brush.verticalGradient(
            listOf(WmwColors.Paper, WmwColors.Cloud, Color(0xFFF3F1F7)),
        )
        WmwCircadianStage.EMERGING -> Brush.verticalGradient(
            listOf(Color(0xFF050B19), WmwColors.Midnight, Color(0xFF0C1D3D)),
        )
        WmwCircadianStage.ENGAGED -> Brush.verticalGradient(
            listOf(Color(0xFF07122B), Color(0xFF0D2145), Color(0xFF18274D)),
        )
        WmwCircadianStage.ACTIVE -> Brush.verticalGradient(
            listOf(WmwColors.Midnight, Color(0xFF11264C), Color(0xFF24345D), Color(0xFF142445)),
        )
        WmwCircadianStage.ORIENTED -> Brush.verticalGradient(
            listOf(Color(0xFFFFF9F3), WmwColors.MorningPaper, Color(0xFFF3F2FA)),
        )
        WmwCircadianStage.COMPLETE -> Brush.verticalGradient(
            listOf(WmwColors.Paper, Color(0xFFFFF4EA), Color(0xFFF3F2FA)),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        if (ambientGlow) {
            Canvas(Modifier.fillMaxSize()) {
                when (stage) {
                    WmwCircadianStage.PLANNING -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    WmwColors.Sunrise.copy(alpha = 0.12f),
                                    WmwColors.GoldenLight.copy(alpha = 0.06f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.08f),
                                radius = size.maxDimension * 0.34f,
                            ),
                            radius = size.maxDimension * 0.34f,
                            center = Offset(size.width * 0.85f, size.height * 0.08f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    WmwColors.Dawn.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.10f, size.height * 0.92f),
                                radius = size.maxDimension * 0.30f,
                            ),
                            radius = size.maxDimension * 0.30f,
                            center = Offset(size.width * 0.10f, size.height * 0.92f),
                        )
                    }
                    WmwCircadianStage.EMERGING,
                    WmwCircadianStage.ENGAGED,
                    WmwCircadianStage.ACTIVE,
                    -> {
                        val center = when (stage) {
                            WmwCircadianStage.EMERGING -> Offset(size.width * 0.50f, size.height * 0.68f)
                            WmwCircadianStage.ENGAGED -> Offset(size.width * 0.50f, size.height * 0.62f)
                            WmwCircadianStage.ACTIVE -> Offset(size.width * 0.50f, size.height * 0.58f)
                            else -> Offset.Zero
                        }
                        val alpha = when (stage) {
                            WmwCircadianStage.EMERGING -> 0.10f
                            WmwCircadianStage.ENGAGED -> 0.18f
                            WmwCircadianStage.ACTIVE -> 0.30f
                            else -> 0f
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    WmwColors.Sunrise.copy(alpha = alpha),
                                    WmwColors.GoldenLight.copy(alpha = alpha * 0.52f),
                                    WmwColors.Dawn.copy(alpha = alpha * 0.16f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.maxDimension * 0.36f,
                            ),
                            radius = size.maxDimension * 0.36f,
                            center = center,
                        )
                    }
                    WmwCircadianStage.ORIENTED,
                    WmwCircadianStage.COMPLETE,
                    -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    WmwColors.GoldenLight.copy(alpha = 0.14f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.72f, size.height * 0.14f),
                                radius = size.maxDimension * 0.30f,
                            ),
                            radius = size.maxDimension * 0.30f,
                            center = Offset(size.width * 0.72f, size.height * 0.14f),
                        )
                    }
                }
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
                color = if (onLightSurface) WmwColors.DarkHairline else WmwColors.Hairline,
                shape = shape,
            ),
        color = if (onLightSurface) {
            WmwColors.PaperCard.copy(alpha = 0.90f)
        } else {
            WmwColors.ElevatedNightSurface.copy(alpha = 0.88f)
        },
        contentColor = if (onLightSurface) WmwColors.Midnight else WmwColors.WarmLight,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = if (onLightSurface) 1.dp else 0.dp,
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
        shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.10f) else WmwColors.DeepNavy,
            disabledContentColor = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.35f) else WmwColors.FaintText,
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
