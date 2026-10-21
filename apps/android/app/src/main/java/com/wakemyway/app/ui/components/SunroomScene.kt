package com.wakemyway.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.LocalWmwPlanningAtmosphere
import com.wakemyway.app.ui.theme.WmwColors

/**
 * Sunroom Club's offline, entirely decorative illustrated world.
 *
 * Shapes are proportional to the available drawing area: no baked text, fixed-resolution
 * screenshots, runtime downloads or new wake/session states. The wake stage is an input owned by
 * existing Wake Runtime. Rendering failure cannot prevent Stop/Snooze from being laid out.
 */
@Composable
fun SunroomScene(
    stage: WmwCircadianStage,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    editorHeader: Boolean = false,
) {
    val planningAtmosphere = LocalWmwPlanningAtmosphere.current
    Canvas(modifier.fillMaxSize().clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val dark = stage == WmwCircadianStage.EMERGING ||
            stage == WmwCircadianStage.ENGAGED ||
            stage == WmwCircadianStage.ACTIVE
        val readyMorning = stage == WmwCircadianStage.ORIENTED ||
            stage == WmwCircadianStage.COMPLETE

        val sky = when (stage) {
            WmwCircadianStage.PLANNING -> listOf(
                planningAtmosphere.top, planningAtmosphere.middle,
                planningAtmosphere.bottom, WmwColors.SunroomPaper,
            )
            WmwCircadianStage.EMERGING -> listOf(
                Color(0xFF090F2B), Color(0xFF192049), Color(0xFF3D3159), Color(0xFF161D3E)
            )
            WmwCircadianStage.ENGAGED -> listOf(
                Color(0xFF111633), Color(0xFF32305A), Color(0xFF84546E), Color(0xFF202A4E)
            )
            WmwCircadianStage.ACTIVE -> listOf(
                Color(0xFF131832), Color(0xFF4E3C69), Color(0xFFC87D79), Color(0xFF293455)
            )
            WmwCircadianStage.ORIENTED -> listOf(
                Color(0xFFF4F4F9), Color(0xFFFFEFD9), Color(0xFFFFCA9F), Color(0xFFFFF7EB)
            )
            WmwCircadianStage.COMPLETE -> listOf(
                Color(0xFFFFF7E7), Color(0xFFFFE9C9), Color(0xFFFFC995), Color(0xFFFFFAF1)
            )
        }
        drawRect(Brush.verticalGradient(sky))

        // Tiny stars only while the user is waking in the dark.
        if (dark) {
            val stars = listOf(
                .10f to .11f, .21f to .22f, .39f to .14f, .53f to .29f,
                .72f to .12f, .89f to .23f, .81f to .38f, .17f to .37f,
            )
            stars.forEachIndexed { index, point ->
                drawCircle(
                    color = Color(0xFFFFF0DA).copy(alpha = if (index % 3 == 0) .65f else .35f),
                    radius = if (index % 3 == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                    center = Offset(w * point.first, h * point.second),
                )
            }
        }

        val sunX = when (stage) {
            WmwCircadianStage.PLANNING -> if (editorHeader) w * .95f else w * .81f
            WmwCircadianStage.ORIENTED -> w * .68f
            else -> w * .54f
        }
        val sunY = when (stage) {
            WmwCircadianStage.PLANNING -> if (editorHeader) h * .10f else h * .205f
            WmwCircadianStage.EMERGING -> h * .75f
            WmwCircadianStage.ENGAGED -> h * .67f
            WmwCircadianStage.ACTIVE -> h * .60f
            WmwCircadianStage.ORIENTED -> h * .54f
            WmwCircadianStage.COMPLETE -> h * .32f
        }
        val radius = (w * (if (editorHeader) .075f else if (stage == WmwCircadianStage.PLANNING) .115f else .18f))
            .coerceAtMost(h * .105f)
        if (stage != WmwCircadianStage.PLANNING || hero) {
            drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    WmwColors.GoldenLight.copy(alpha = if (dark) .27f else .37f),
                    WmwColors.Sunrise.copy(alpha = .10f),
                    Color.Transparent,
                ),
                center = Offset(sunX, sunY), radius = radius * 2.9f,
            ),
            radius = radius * 2.9f, center = Offset(sunX, sunY),
        )
        drawCircle(
            brush = Brush.verticalGradient(
                colors = if (dark) listOf(Color(0xFFFFE5A3), Color(0xFFFFB77F))
                else listOf(Color(0xFFFFE6A0), Color(0xFFFFAC76)),
                startY = sunY - radius,
                endY = sunY + radius,
            ),
            center = Offset(sunX, sunY),
            radius = radius,
        )

        // A friendly *graphic* sun, not an assistant/character; only on noncritical surfaces.
        if (!dark) {
            val face = Color(0xFF8E493D)
            val eye = radius * .055f
            drawCircle(face, eye, Offset(sunX - radius * .27f, sunY - radius * .04f))
            drawCircle(face, eye, Offset(sunX + radius * .27f, sunY - radius * .04f))
            drawCircle(
                Color(0xFFFF8B7F).copy(alpha = .42f),
                radius * .14f,
                Offset(sunX - radius * .47f, sunY + radius * .19f),
            )
            drawCircle(
                Color(0xFFFF8B7F).copy(alpha = .42f),
                radius * .14f,
                Offset(sunX + radius * .47f, sunY + radius * .19f),
            )
            val smile = Path().apply {
                moveTo(sunX - radius * .24f, sunY + radius * .10f)
                quadraticBezierTo(sunX, sunY + radius * .43f, sunX + radius * .24f, sunY + radius * .10f)
            }
            drawPath(smile, color = face, style = Stroke(width = (radius * .046f).coerceAtLeast(1.5f), cap = StrokeCap.Round))
        }

        }

        // Airy clouds, almost transparent in planning, small and low contrast in active wake.
        val cloudAlpha = if (dark) .13f else .38f
        val cloudColor = if (dark) Color(0xFFF7C8BE) else Color.White
        fun cloud(cx: Float, cy: Float, scale: Float) {
            drawOval(cloudColor.copy(alpha = cloudAlpha),
                topLeft = Offset(cx - 46f * scale, cy - 12f * scale),
                size = Size(105f * scale, 24f * scale))
            drawCircle(cloudColor.copy(alpha = cloudAlpha),
                radius = 21f * scale, center = Offset(cx - 8f * scale, cy - 5f * scale))
            drawCircle(cloudColor.copy(alpha = cloudAlpha),
                radius = 15f * scale, center = Offset(cx + 19f * scale, cy - 7f * scale))
        }
        val scale = w / 393.dp.toPx().coerceAtLeast(1f)
        cloud(w * .24f, h * .19f, scale)
        cloud(w * .87f, h * (if (dark) .42f else .38f), scale * .68f)

        // Landscape layers, drawn only at the lower edge in planning.
        val startY = when (stage) {
            WmwCircadianStage.PLANNING -> h * .83f
            WmwCircadianStage.EMERGING -> h * .72f
            WmwCircadianStage.ENGAGED -> h * .66f
            WmwCircadianStage.ACTIVE -> h * .65f
            else -> h * .68f
        }
        fun hill(y: Float, amp: Float, color: Color, phase: Float) {
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, y + amp * .45f)
                cubicTo(w * .17f, y - amp * .64f + phase, w * .31f, y + amp, w * .48f, y + amp * .17f)
                cubicTo(w * .65f, y - amp * .9f, w * .81f, y + amp * .8f - phase, w, y - amp * .15f)
                lineTo(w, h)
                close()
            }
            drawPath(path, color)
        }
        if (dark) {
            hill(startY + h * .03f, h * .09f, Color(0xFF685278).copy(alpha = .80f), 0f)
            hill(startY + h * .115f, h * .085f, Color(0xFF343758), 12f)
            hill(startY + h * .22f, h * .06f, Color(0xFF171F3D), -8f)
        } else {
            hill(startY, h * .035f, Color(0xFFF2BFAC).copy(alpha = if (readyMorning) .67f else .44f), 0f)
            hill(startY + h * .065f, h * .045f, Color(0xFFE9B2A4).copy(alpha = .48f), 8f)
            hill(startY + h * .14f, h * .025f, Color(0xFFF0D2BC).copy(alpha = .54f), 3f)
        }

        drawSunroomDetails(stage = stage, hero = hero, editorHeader = editorHeader)

    }
}

/** Decorative accent only. The authoritative WakeMyWay logo and Alfred persona remain separate. */
@Composable
fun SunroomSunStamp(modifier: Modifier = Modifier) {
    Canvas(modifier.size(46.dp).clearAndSetSemantics {}) {
        val r = size.minDimension * .41f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xFFFFC98E), r, center)
        val face = Color(0xFF914D46)
        drawCircle(face, r * .075f, Offset(center.x - r * .28f, center.y - r * .08f))
        drawCircle(face, r * .075f, Offset(center.x + r * .28f, center.y - r * .08f))
        val smile = Path().apply {
            moveTo(center.x - r * .24f, center.y + r * .1f)
            quadraticBezierTo(center.x, center.y + r * .44f, center.x + r * .24f, center.y + r * .1f)
        }
        drawPath(smile, color = face, style = Stroke(width = (r * .07f).coerceAtLeast(1.5f), cap = StrokeCap.Round))
    }
}
