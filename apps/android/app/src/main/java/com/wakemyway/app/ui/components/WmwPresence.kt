package com.wakemyway.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
                    radius = radius * 0.22f,
                    smoothing = 0.72f,
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

                onDrawBehind {
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.16f),
                    )
                    drawPath(
                        path = path,
                        color = accent.copy(alpha = 0.58f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = this.size.minDimension * 0.018f,
                        ),
                    )
                }
            },
    )
}
