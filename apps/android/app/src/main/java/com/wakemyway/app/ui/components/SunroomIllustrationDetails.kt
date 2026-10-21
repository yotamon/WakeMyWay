package com.wakemyway.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hand-drawn details for the existing offline Sunroom scenes.
 *
 * The decorative drawing always has empty semantics. Every part uses relative coordinates,
 * deterministic geometry and local paint; no network, decoded bitmaps, frame loops or sensors.
 */
internal fun DrawScope.drawSunroomDetails(
    stage: WmwCircadianStage,
    hero: Boolean,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val dark = stage == WmwCircadianStage.EMERGING ||
        stage == WmwCircadianStage.ENGAGED || stage == WmwCircadianStage.ACTIVE
    val morning = stage == WmwCircadianStage.ORIENTED ||
        stage == WmwCircadianStage.COMPLETE
    val sunVisible = stage != WmwCircadianStage.PLANNING || hero
    val sunX = when (stage) {
        WmwCircadianStage.PLANNING -> w * .81f
        WmwCircadianStage.ORIENTED -> w * .68f
        else -> w * .54f
    }
    val sunY = when (stage) {
        WmwCircadianStage.PLANNING -> h * .205f
        WmwCircadianStage.EMERGING -> h * .75f
        WmwCircadianStage.ENGAGED -> h * .67f
        WmwCircadianStage.ACTIVE -> h * .60f
        WmwCircadianStage.ORIENTED -> h * .54f
        WmwCircadianStage.COMPLETE -> h * .32f
    }
    val r = (w * (if (stage == WmwCircadianStage.PLANNING) .115f else .18f))
        .coerceAtMost(h * .105f)

    // Sunrise rays are small editorial details, never a second button or the application icon.
    if (sunVisible && !dark) {
        val rayColor = Color(0xFFEF8E64).copy(alpha = if (morning) .58f else .70f)
        repeat(9) { index ->
            val angle = (index * 40.0 - 90.0) * PI / 180.0
            val vx = cos(angle).toFloat()
            val vy = sin(angle).toFloat()
            drawLine(
                rayColor,
                Offset(sunX + vx * r * 1.28f, sunY + vy * r * 1.28f),
                Offset(sunX + vx * r * 1.51f, sunY + vy * r * 1.51f),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }

    if (dark) {
        // A faint meteor gives the night scene depth, but leaves the listening copy untouched.
        val shooting = Path().apply {
            moveTo(w * .16f, h * .15f)
            quadraticBezierTo(w * .205f, h * .104f, w * .28f, h * .085f)
        }
        drawPath(
            shooting, Color(0xFFFFD9AB).copy(alpha = .55f),
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round),
        )
        // A warm reflection in the lake makes the distant sun feel embedded in the landscape.
        val reflection = Color(0xFFFFB68E)
        repeat(11) { i ->
            val y = h * (.79f + .015f * i)
            val spread = (.025f + .004f * i)
            val drift = if (i % 2 == 0) -.012f else .017f
            drawLine(
                reflection.copy(alpha = .29f - i * .012f),
                Offset(w * (.54f - spread + drift), y),
                Offset(w * (.54f + spread + drift), y),
                strokeWidth = if (i % 3 == 0) 1.4.dp.toPx() else .75.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    } else if (morning || hero) {
        // Shore highlights & birds: a tranquil lake scene without fictitious information.
        val water = Color(0xFFFFF8E7).copy(alpha = if (morning) .68f else .48f)
        repeat(8) { i ->
            val y = h * (if (morning) .69f + .024f * i else .89f + .009f * i)
            val left = w * (.28f + .045f * (i % 3))
            drawLine(
                water, Offset(left, y), Offset(left + w * (.12f + .027f * (i % 4)), y),
                strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round,
            )
        }
        val birdY = if (morning) h * .42f else h * .43f
        repeat(if (morning) 3 else 2) { i ->
            val x = w * (.66f + i * .067f)
            val y = birdY + ((i % 2) * .022f * h)
            val bird = Path().apply {
                moveTo(x - w * .015f, y)
                quadraticBezierTo(x - w * .006f, y - h * .012f, x, y)
                quadraticBezierTo(x + w * .007f, y - h * .012f, x + w * .015f, y)
            }
            drawPath(
                bird, Color(0xFF9D6A72).copy(alpha = .62f),
                style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }

    // Asymmetric branches on both edges make the landscape recognizable even at thumbnail size.
    // On secondary planning pages leave the foreground clean for long forms and error states.
    if (stage == WmwCircadianStage.PLANNING && !hero) return
    val stemColor = when {
        dark -> Color(0xFF1B2440)
        morning -> Color(0xFF65536A).copy(alpha = .80f)
        else -> Color(0xFF907480).copy(alpha = .49f)
    }
    val leafColor = when {
        dark -> Color(0xFF303A59)
        morning -> Color(0xFF726079).copy(alpha = .90f)
        else -> Color(0xFFB68C85).copy(alpha = .72f)
    }
    for (side in 0..1) {
        val sign = if (side == 0) 1f else -1f
        val baseX = if (side == 0) w * -.025f else w * 1.025f
        val baseY = h * 1.07f
        val topY = if (dark) h * .635f else if (morning) h * .55f else h * .78f
        val tipX = baseX + sign * w * (if (morning) .19f else .13f)
        val stem = Path().apply {
            moveTo(baseX, baseY)
            cubicTo(
                baseX + sign * w * .06f, h * .87f,
                tipX - sign * w * .055f, topY + h * .12f,
                tipX, topY,
            )
        }
        drawPath(stem, color = stemColor, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
        repeat(7) { i ->
            val t = (i + 1f) / 8f
            val x = baseX + (tipX - baseX) * t
            val y = baseY + (topY - baseY) * t
            val outward = if (i % 2 == 0) sign else -sign
            val reach = w * (.034f + .015f * ((i + side) % 3))
            val rise = h * (.021f + .01f * ((i + 1) % 3))
            val tip = Offset(x + outward * reach, y - rise)
            drawLine(
                stemColor, Offset(x, y), tip,
                strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round,
            )
            drawSunroomLeaf(
                Offset(x + outward * reach * .37f, y - rise * .35f),
                Offset(tip.x + outward * reach * .22f, tip.y - rise * .24f),
                w * .012f,
                leafColor.copy(alpha = if (i % 3 == 0) .70f else 1f),
            )
            if (i % 2 == 0) {
                drawSunroomLeaf(
                    Offset(x, y),
                    Offset(x - outward * reach * .46f, y - rise * 1.24f),
                    w * .010f,
                    leafColor.copy(alpha = .82f),
                )
            }
        }
    }

    // Tiny seed-heads and flowers ground the scene without drawing over the controls.
    if (!dark) {
        val pink = Color(0xFFE98B89)
        repeat(6) { i ->
            val x = w * (if (i < 3) (.06f + i * .050f) else (.78f + (i - 3) * .050f))
            val y = h * (.957f + (i % 3) * .009f)
            drawLine(stemColor.copy(alpha = .48f), Offset(x, h), Offset(x, y),
                strokeWidth = 1.dp.toPx())
            drawCircle(pink.copy(alpha = .62f), 2.2.dp.toPx(), Offset(x, y))
            drawCircle(Color(0xFFFFE0AE), 1.1.dp.toPx(), Offset(x + 1.dp.toPx(), y))
        }
    }
}

private fun DrawScope.drawSunroomLeaf(
    base: Offset,
    tip: Offset,
    width: Float,
    color: Color,
) {
    val dx = tip.x - base.x
    val dy = tip.y - base.y
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(.001f)
    val nx = -dy / length
    val ny = dx / length
    val midX = (base.x + tip.x) / 2f
    val midY = (base.y + tip.y) / 2f
    val leaf = Path().apply {
        moveTo(base.x, base.y)
        quadraticBezierTo(midX + nx * width, midY + ny * width, tip.x, tip.y)
        quadraticBezierTo(midX - nx * width, midY - ny * width, base.x, base.y)
        close()
    }
    drawPath(leaf, color)
}
