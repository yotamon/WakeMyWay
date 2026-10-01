package com.wakemyway.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Signature WakeMyWay visual language.
 *
 * The horizon is deliberately not an AI orb or decorative illustration. It acts as a quiet,
 * reusable brand object across planning and wake surfaces while remaining presentation-only.
 */
@Composable
fun WmwWakeHorizon(
    modifier: Modifier = Modifier,
    progress: Float = 0.46f,
    onDark: Boolean = false,
    active: Boolean = false,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val ink = if (onDark) WmwColors.WarmLight else WmwColors.Midnight
    val horizon = if (onDark) WmwColors.QuietText else WmwColors.LightQuietText
    val sun = if (active) WmwColors.GoldenLight else WmwColors.Sunrise

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp),
    ) {
        val baselineY = size.height * 0.72f
        val margin = size.width * 0.08f
        val stroke = 1.4.dp.toPx()
        val center = Offset(size.width / 2f, baselineY)
        val radius = size.minDimension * 0.25f

        drawLine(
            color = horizon.copy(alpha = if (onDark) 0.38f else 0.26f),
            start = Offset(margin, baselineY),
            end = Offset(size.width - margin, baselineY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        drawArc(
            color = ink.copy(alpha = if (onDark) 0.34f else 0.22f),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - radius, baselineY - radius * 0.82f),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        val angle = PI * (1.0 + clamped)
        val orbitX = center.x + cos(angle).toFloat() * radius * 0.92f
        val orbitY = baselineY + sin(angle).toFloat() * radius * 0.72f
        drawCircle(
            color = sun,
            radius = if (active) 8.dp.toPx() else 7.dp.toPx(),
            center = Offset(orbitX, orbitY),
        )
        drawCircle(
            color = sun.copy(alpha = if (active) 0.16f else 0.09f),
            radius = if (active) 23.dp.toPx() else 18.dp.toPx(),
            center = Offset(orbitX, orbitY),
        )
    }
}

@Composable
fun WmwInlineStatus(
    label: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val accent = if (positive) WmwColors.Success else WmwColors.Sunrise
    val textColor = if (onDark) WmwColors.WarmLight else WmwColors.Midnight
    Row(
        modifier = modifier.heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(7.dp),
            shape = CircleShape,
            color = accent,
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

@Composable
fun WmwOpenSection(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xxs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WmwColors.Midnight,
                )
                detail?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.LightQuietText,
                    )
                }
            }
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = WmwSpacing.Sm, vertical = WmwSpacing.Xs),
                    style = MaterialTheme.typography.labelLarge,
                    color = WmwColors.DawnText,
                )
            }
        }
        content()
    }
}
