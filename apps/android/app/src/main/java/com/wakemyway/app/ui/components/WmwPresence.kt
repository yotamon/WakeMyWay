package com.wakemyway.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwMotion
import com.wakemyway.app.ui.theme.WmwSizes

enum class WmwPresenceState {
    QUIET,
    LISTENING,
    MOVING,
    COMPLETE,
}

/**
 * WMW's signature presence is intentionally geometric and non-anthropomorphic. It is not an
 * AI orb, mascot or behavioral authority. The internal horizon stroke gives the shape a stable
 * identity while the outer contour can still resolve with presentation state.
 */
@Composable
fun WmwPresence(
    state: WmwPresenceState,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = WmwSizes.PresenceLarge,
) {
    val targetProgress = when (state) {
        WmwPresenceState.QUIET -> 0.08f
        WmwPresenceState.LISTENING -> 0.34f
        WmwPresenceState.MOVING -> 0.72f
        WmwPresenceState.COMPLETE -> 1f
    }
    val duration = when (state) {
        WmwPresenceState.QUIET -> WmwMotion.EmergingAmbientMillis
        WmwPresenceState.LISTENING -> WmwMotion.EngagedTransitionMillis
        WmwPresenceState.MOVING -> WmwMotion.ActiveFeedbackMillis
        WmwPresenceState.COMPLETE -> WmwMotion.CompletionSettleMillis
    }
    val progress = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = duration),
        label = "wmw-presence-morph",
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .drawWithCache {
                val radius = this.size.minDimension * 0.46f
                val rounding = CornerRounding(
                    radius = radius * 0.24f,
                    smoothing = 0.78f,
                )
                val start = RoundedPolygon(
                    numVertices = 6,
                    radius = radius,
                    centerX = this.size.width / 2f,
                    centerY = this.size.height / 2f,
                    rounding = rounding,
                )
                val end = RoundedPolygon(
                    numVertices = 4,
                    radius = radius,
                    centerX = this.size.width / 2f,
                    centerY = this.size.height / 2f,
                    rounding = rounding,
                )
                val morph = Morph(start = start, end = end)
                val path = morph.toPath(progress = progress.value).asComposePath()
                val accent = when (state) {
                    WmwPresenceState.QUIET -> WmwColors.Clay
                    WmwPresenceState.LISTENING -> WmwColors.SoftEmber
                    WmwPresenceState.MOVING -> WmwColors.SoftEmber
                    WmwPresenceState.COMPLETE -> WmwColors.Sage
                }
                val horizonY = this.size.height * (0.56f - (progress.value * 0.05f))
                val horizonStart = Offset(this.size.width * 0.27f, horizonY)
                val horizonEnd = Offset(this.size.width * 0.73f, horizonY)
                val resolvedEnd = Offset(
                    x = this.size.width * (0.42f + (0.24f * progress.value)),
                    y = horizonY,
                )

                onDrawBehind {
                    drawPath(
                        path = path,
                        color = WmwColors.DeepDawn.copy(alpha = 0.78f),
                    )
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.62f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = this.size.minDimension * 0.017f,
                        ),
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.28f),
                        start = horizonStart,
                        end = horizonEnd,
                        strokeWidth = this.size.minDimension * 0.014f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.86f),
                        start = horizonStart,
                        end = resolvedEnd,
                        strokeWidth = this.size.minDimension * 0.018f,
                        cap = StrokeCap.Round,
                    )
                }
            },
    )
}
