package com.wakemyway.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakemyway.app.R
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSizes
import com.wakemyway.app.ui.theme.WmwSpacing

/**
 * Canonical WakeMyWay symbol: a sunrise above one continuous horizon that reads simultaneously as
 * landscape and a calm sound wave. Keep this geometry consistent across the app icon, wordmark,
 * wake runtime and marketing surfaces.
 */
@Composable
fun WmwSunriseMark(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    glow: Boolean = true,
) {
    Canvas(modifier = modifier) {
        val horizonY = size.height * 0.69f
        val sunRadius = size.height * 0.285f
        val sunCenter = Offset(size.width * 0.50f, horizonY - sunRadius * 0.48f)

        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WmwColors.GoldenLight.copy(alpha = if (onDark) 0.30f else 0.20f),
                        WmwColors.Sunrise.copy(alpha = if (onDark) 0.16f else 0.10f),
                        Color.Transparent,
                    ),
                    center = sunCenter,
                    radius = sunRadius * 1.85f,
                ),
                radius = sunRadius * 1.85f,
                center = sunCenter,
            )
        }

        clipRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = horizonY + 1f,
        ) {
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

        val wave = brandWavePath(size.width, size.height, horizonY)
        val lineColor = if (onDark) WmwColors.WarmLight else WmwColors.Midnight

        drawPath(
            path = wave,
            color = if (onDark) WmwColors.Sunrise.copy(alpha = 0.22f) else WmwColors.Dawn.copy(alpha = 0.20f),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = wave,
            color = lineColor,
            style = Stroke(width = 2.15.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
fun WmwBrandLockup(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    compact: Boolean = true,
) {
    val foreground = if (onDark) WmwColors.WarmLight else WmwColors.Midnight
    val markWidth = if (compact) WmwSizes.BrandMarkSmallWidth else 68.dp
    val markHeight = if (compact) WmwSizes.BrandMarkSmallHeight else 38.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        WmwSunriseMark(
            modifier = Modifier.size(width = markWidth, height = markHeight),
            onDark = onDark,
            glow = !compact,
        )
        Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
        Text(
            text = stringResource(R.string.wmw_brand_name),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (compact) 16.sp else 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.35).sp,
            ),
            color = foreground,
            maxLines = 1,
        )
    }
}

@Composable
fun WmwBrandHeader(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WmwBrandLockup(onDark = onDark)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun WmwBrandHero(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    tagline: String? = null,
) {
    val foreground = if (onDark) WmwColors.WarmLight else WmwColors.Midnight
    val secondary = if (onDark) WmwColors.QuietText else WmwColors.LightQuietText

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WmwSunriseMark(
            modifier = Modifier.size(
                width = WmwSizes.BrandMarkHeroWidth,
                height = WmwSizes.BrandMarkHeroHeight,
            ),
            onDark = onDark,
        )
        Text(
            text = stringResource(R.string.wmw_brand_name),
            style = MaterialTheme.typography.headlineMedium,
            color = foreground,
            textAlign = TextAlign.Center,
        )
        tagline?.let {
            Spacer(Modifier.height(WmwSpacing.Xs))
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun brandWavePath(width: Float, height: Float, horizonY: Float): Path {
    val smallLift = height * 0.075f
    val centerLift = height * 0.185f
    val valley = height * 0.035f

    return Path().apply {
        moveTo(width * 0.02f, horizonY)
        cubicTo(
            width * 0.08f, horizonY,
            width * 0.12f, horizonY - smallLift * 0.15f,
            width * 0.17f, horizonY - smallLift,
        )
        cubicTo(
            width * 0.22f, horizonY - smallLift * 1.18f,
            width * 0.26f, horizonY + valley * 0.55f,
            width * 0.32f, horizonY + valley,
        )
        cubicTo(
            width * 0.39f, horizonY + valley * 0.55f,
            width * 0.42f, horizonY - centerLift * 0.85f,
            width * 0.50f, horizonY - centerLift,
        )
        cubicTo(
            width * 0.58f, horizonY - centerLift * 0.85f,
            width * 0.61f, horizonY + valley * 0.55f,
            width * 0.68f, horizonY + valley,
        )
        cubicTo(
            width * 0.74f, horizonY + valley * 0.60f,
            width * 0.78f, horizonY - smallLift * 1.05f,
            width * 0.83f, horizonY - smallLift * 0.82f,
        )
        cubicTo(
            width * 0.88f, horizonY - smallLift * 0.25f,
            width * 0.92f, horizonY,
            width * 0.98f, horizonY,
        )
    }
}
