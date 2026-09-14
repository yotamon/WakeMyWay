package com.wakemyway.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
 * The Wake Line is the same visual idea as the logo's ground line: one continuous calm sound wave
 * that can also read as a mountain horizon. It changes amplitude with real wake state and never
 * becomes a decorative equalizer. Live wake states may reveal the brand sunrise behind it.
 */
@Composable
fun WmwWakeLine(
    state: WmwWakeLineState,
    modifier: Modifier = Modifier,
    onLightSurface: Boolean = false,
    height: Dp = WmwSizes.HomeWaveHeight,
    sunrise: Boolean = false,
) {
    val targetAmplitude = when (state) {
        WmwWakeLineState.QUIET -> 0.55f
        WmwWakeLineState.LISTENING -> 0.78f
        WmwWakeLineState.MOVING -> 1f
        WmwWakeLineState.SETTLED -> 0.42f
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
        WmwWakeLineState.QUIET -> if (onLightSurface) WmwColors.Midnight else WmwColors.WarmLight
        WmwWakeLineState.LISTENING -> WmwColors.GoldenLight
        WmwWakeLineState.MOVING -> WmwColors.SunriseSoft
        WmwWakeLineState.SETTLED -> if (onLightSurface) WmwColors.DawnDeep else WmwColors.Dawn
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val centerY = size.height * 0.62f
        val scale = if (height <= WmwSizes.HomeWaveHeight) 0.72f else 1f
        val peak = size.height * 0.26f * amplitude.value * scale
        val valley = size.height * 0.05f * amplitude.value * scale

        if (sunrise) {
            val sunRadius = size.height * 0.30f
            val sunCenter = Offset(size.width * 0.50f, centerY - sunRadius * 0.48f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WmwColors.GoldenLight.copy(alpha = if (onLightSurface) 0.22f else 0.34f),
                        WmwColors.Sunrise.copy(alpha = if (onLightSurface) 0.10f else 0.18f),
                        Color.Transparent,
                    ),
                    center = sunCenter,
                    radius = sunRadius * 1.85f,
                ),
                radius = sunRadius * 1.85f,
                center = sunCenter,
            )
            clipRect(bottom = centerY + 1f) {
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(WmwColors.GoldenLight, WmwColors.Sunrise),
                        startY = sunCenter.y - sunRadius,
                        endY = sunCenter.y + sunRadius,
                    ),
                    radius = sunRadius,
                    center = sunCenter,
                )
            }
        }

        val path = Path().apply {
            moveTo(size.width * 0.02f, centerY)
            cubicTo(
                size.width * 0.08f, centerY,
                size.width * 0.12f, centerY - peak * 0.24f,
                size.width * 0.17f, centerY - peak * 0.42f,
            )
            cubicTo(
                size.width * 0.22f, centerY - peak * 0.48f,
                size.width * 0.26f, centerY + valley * 0.58f,
                size.width * 0.32f, centerY + valley,
            )
            cubicTo(
                size.width * 0.39f, centerY + valley * 0.55f,
                size.width * 0.42f, centerY - peak * 0.82f,
                size.width * 0.50f, centerY - peak,
            )
            cubicTo(
                size.width * 0.58f, centerY - peak * 0.82f,
                size.width * 0.61f, centerY + valley * 0.55f,
                size.width * 0.68f, centerY + valley,
            )
            cubicTo(
                size.width * 0.74f, centerY + valley * 0.60f,
                size.width * 0.78f, centerY - peak * 0.46f,
                size.width * 0.83f, centerY - peak * 0.38f,
            )
            cubicTo(
                size.width * 0.88f, centerY - peak * 0.12f,
                size.width * 0.92f, centerY,
                size.width * 0.98f, centerY,
            )
        }

        drawLine(
            color = if (onLightSurface) WmwColors.Midnight.copy(alpha = 0.08f) else WmwColors.WarmLight.copy(alpha = 0.05f),
            start = Offset(size.width * 0.02f, centerY),
            end = Offset(size.width * 0.98f, centerY),
            strokeWidth = 0.75.dp.toPx(),
        )

        val glowAlpha = when (state) {
            WmwWakeLineState.MOVING -> 0.30f
            WmwWakeLineState.LISTENING -> 0.22f
            WmwWakeLineState.QUIET -> 0.12f
            WmwWakeLineState.SETTLED -> 0.10f
        }
        drawPath(
            path = path,
            color = accent.copy(alpha = if (onLightSurface) 0.10f else glowAlpha),
            style = Stroke(
                width = if (state == WmwWakeLineState.MOVING) 16.dp.toPx() else 10.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
        drawPath(
            path = path,
            color = accent.copy(alpha = if (onLightSurface) 0.86f else 0.98f),
            style = Stroke(
                width = if (state == WmwWakeLineState.MOVING) 2.1.dp.toPx() else 1.55.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
    }
}
