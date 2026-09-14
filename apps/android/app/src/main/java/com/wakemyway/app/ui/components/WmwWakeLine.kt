package com.wakemyway.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwMotion

enum class WmwWakeLineState {
    QUIET,
    LISTENING,
    MOVING,
    SETTLED,
}

@Composable
fun WmwWakeLine(
    state: WmwWakeLineState,
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
) {
    val targetAmplitude = when (state) {
        WmwWakeLineState.QUIET -> 0.22f
        WmwWakeLineState.LISTENING -> 0.5f
        WmwWakeLineState.MOVING -> 0.9f
        WmwWakeLineState.SETTLED -> 0.14f
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
        WmwWakeLineState.QUIET -> WmwColors.Clay
        WmwWakeLineState.LISTENING -> WmwColors.SoftEmber
        WmwWakeLineState.MOVING -> WmwColors.SoftEmber
        WmwWakeLineState.SETTLED -> if (onLightSurface) WmwColors.Clay else WmwColors.Sage
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        val centerY = size.height * 0.52f
        val wave = size.height * 0.22f * amplitude.value
        val path = Path().apply {
            moveTo(0f, centerY)
            cubicTo(
                size.width * 0.13f,
                centerY,
                size.width * 0.18f,
                centerY - wave * 0.22f,
                size.width * 0.27f,
                centerY - wave * 0.12f,
            )
            cubicTo(
                size.width * 0.36f,
                centerY,
                size.width * 0.37f,
                centerY + wave,
                size.width * 0.48f,
                centerY + wave * 0.08f,
            )
            cubicTo(
                size.width * 0.57f,
                centerY - wave * 0.92f,
                size.width * 0.63f,
                centerY - wave * 0.92f,
                size.width * 0.72f,
                centerY + wave * 0.1f,
            )
            cubicTo(
                size.width * 0.81f,
                centerY + wave * 0.62f,
                size.width * 0.87f,
                centerY - wave * 0.32f,
                size.width,
                centerY,
            )
        }

        drawLine(
            color = accent.copy(alpha = if (onLightSurface) 0.18f else 0.12f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
        )
        drawPath(
            path = path,
            color = accent.copy(alpha = 0.16f),
            style = Stroke(
                width = 7.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
        drawPath(
            path = path,
            color = accent.copy(alpha = 0.9f),
            style = Stroke(
                width = 1.35.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
    }
}
