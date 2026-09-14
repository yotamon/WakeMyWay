package com.wakemyway.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwMotion
import com.wakemyway.app.ui.theme.WmwSizes

enum class WmwWakeLineState {
    QUIET,
    LISTENING,
    MOVING,
    SETTLED,
}

/**
 * The signature Wake Line from the approved mockup. It is a horizon first and a waveform second:
 * long quiet shoulders, one human-looking central rise, a soft response, then a return to rest.
 */
@Composable
fun WmwWakeLine(
    state: WmwWakeLineState,
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
    height: Dp = WmwSizes.HomeWaveHeight,
) {
    val targetAmplitude = when (state) {
        WmwWakeLineState.QUIET -> 0.42f
        WmwWakeLineState.LISTENING -> 0.64f
        WmwWakeLineState.MOVING -> 1f
        WmwWakeLineState.SETTLED -> 0.30f
    }
    val duration = when (state) {
        WmwWakeLineState.QUIET -> WmwMotion.EmergingAmbientMillis
        WmwWakeLineState.LISTENING -> WmwMotion.EngagedTransitionMillis
        WmwWakeLineState.MOVING -> WmwMotion.ActiveFeedbackMillis
        WmwWakeLineState.SETTLED -> WmwMotion.CompletionSettleMillis
    }
    val amplitude = animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = duration),
        label = "wake-line-amplitude",
    )
    val accent = when (state) {
        WmwWakeLineState.QUIET -> if (onLightSurface) WmwColors.Clay else WmwColors.SoftEmber
        WmwWakeLineState.LISTENING -> WmwColors.SoftEmber
        WmwWakeLineState.MOVING -> Color(0xFFF3C49A)
        WmwWakeLineState.SETTLED -> if (onLightSurface) WmwColors.Clay else WmwColors.Sage
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val centerY = size.height * 0.52f
        val wave = size.height * 0.31f * amplitude.value
        val path = Path().apply {
            moveTo(0f, centerY)
            cubicTo(
                size.width * 0.12f,
                centerY + wave * 0.04f,
                size.width * 0.20f,
                centerY + wave * 0.22f,
                size.width * 0.30f,
                centerY + wave * 0.10f,
            )
            cubicTo(
                size.width * 0.38f,
                centerY,
                size.width * 0.40f,
                centerY - wave * 0.72f,
                size.width * 0.50f,
                centerY - wave,
            )
            cubicTo(
                size.width * 0.59f,
                centerY - wave * 0.80f,
                size.width * 0.61f,
                centerY + wave * 0.46f,
                size.width * 0.72f,
                centerY + wave * 0.36f,
            )
            cubicTo(
                size.width * 0.82f,
                centerY + wave * 0.28f,
                size.width * 0.88f,
                centerY - wave * 0.16f,
                size.width,
                centerY,
            )
        }

        drawLine(
            color = if (onLightSurface) WmwColors.Clay.copy(alpha = 0.13f) else WmwColors.WarmLight.copy(alpha = 0.05f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 0.7.dp.toPx(),
        )

        val glowAlpha = when (state) {
            WmwWakeLineState.MOVING -> 0.24f
            WmwWakeLineState.LISTENING -> 0.17f
            else -> 0.11f
        }
        drawPath(
            path = path,
            color = accent.copy(alpha = if (onLightSurface) 0.10f else glowAlpha),
            style = Stroke(width = if (state == WmwWakeLineState.MOVING) 13.dp.toPx() else 8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = accent.copy(alpha = if (onLightSurface) 0.58f else 0.94f),
            style = Stroke(width = if (state == WmwWakeLineState.MOVING) 1.7.dp.toPx() else 1.15.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
