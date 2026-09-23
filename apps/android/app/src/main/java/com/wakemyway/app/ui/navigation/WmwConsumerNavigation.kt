package com.wakemyway.app.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing

enum class ConsumerTab(val label: String) {
    HOME("Home"),
    ALARMS("Alarms"),
    INSIGHTS("Insights"),
    PROFILE("Profile"),
}

/** Consumer shell navigation exposes only destinations backed by real product behavior. */
@Composable
fun WmwConsumerScaffold(
    selectedTab: ConsumerTab,
    onTabSelected: (ConsumerTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Modifier) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        content(Modifier.padding(bottom = 88.dp))
        WmwBottomBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun WmwBottomBar(
    selectedTab: ConsumerTab,
    onTabSelected: (ConsumerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = WmwSpacing.Lg, vertical = WmwSpacing.Sm)
            .fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = WmwColors.Midnight.copy(alpha = 0.97f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ConsumerTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selected) WmwColors.Sunrise else Color.Transparent,
                            shape = RoundedCornerShape(24.dp),
                        )
                        .clickable(role = Role.Tab) { onTabSelected(tab) }
                        .semantics { this.selected = selected }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 2.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ConsumerNavGlyph(tab = tab, selected = selected)
                    Text(
                        text = tab.label,
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) WmwColors.Midnight else WmwColors.QuietText,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsumerNavGlyph(
    tab: ConsumerTab,
    selected: Boolean,
) {
    val color = if (selected) WmwColors.Midnight else WmwColors.QuietText
    Canvas(Modifier.size(18.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (tab) {
            ConsumerTab.HOME -> {
                val centerX = size.width / 2f
                drawLine(color, Offset(2.dp.toPx(), 8.dp.toPx()), Offset(centerX, 3.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(centerX, 3.dp.toPx()), Offset(size.width - 2.dp.toPx(), 8.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(4.dp.toPx(), 7.dp.toPx()), Offset(4.dp.toPx(), 15.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width - 4.dp.toPx(), 7.dp.toPx()), Offset(size.width - 4.dp.toPx(), 15.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(4.dp.toPx(), 15.dp.toPx()), Offset(size.width - 4.dp.toPx(), 15.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            ConsumerTab.ALARMS -> {
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    center = center,
                    style = stroke,
                )
                drawLine(color, center, Offset(center.x, center.y - 3.2.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, center, Offset(center.x + 2.6.dp.toPx(), center.y + 1.8.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(4.dp.toPx(), 2.5.dp.toPx()), Offset(2.dp.toPx(), 4.5.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width - 4.dp.toPx(), 2.5.dp.toPx()), Offset(size.width - 2.dp.toPx(), 4.5.dp.toPx()), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            ConsumerTab.INSIGHTS -> {
                drawLine(color, Offset(4.dp.toPx(), 14.dp.toPx()), Offset(4.dp.toPx(), 10.dp.toPx()), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 14.dp.toPx()), Offset(9.dp.toPx(), 6.dp.toPx()), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(14.dp.toPx(), 14.dp.toPx()), Offset(14.dp.toPx(), 3.dp.toPx()), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }

            ConsumerTab.PROFILE -> {
                drawCircle(
                    color = color,
                    radius = 3.2.dp.toPx(),
                    center = Offset(center.x, 5.3.dp.toPx()),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(3.dp.toPx(), 9.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 7.dp.toPx()),
                    style = stroke,
                )
            }
        }
    }
}
