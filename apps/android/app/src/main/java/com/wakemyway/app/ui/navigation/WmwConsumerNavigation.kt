package com.wakemyway.app.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import com.wakemyway.app.ui.theme.WmwSizes
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
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content(Modifier.padding(bottom = WmwSizes.ConsumerNavigationInset))
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
            .padding(horizontal = WmwSpacing.Md)
            .padding(bottom = WmwSpacing.Sm)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = WmwColors.PaperCard.copy(alpha = 0.97f),
        border = BorderStroke(0.75.dp, WmwColors.DarkHairline),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WmwSpacing.Xs, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(WmwSpacing.Xxs),
        ) {
            ConsumerTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab) { onTabSelected(tab) }
                        .semantics { this.selected = selected }
                        .heightIn(min = 58.dp)
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 28.dp)
                            .background(
                                color = if (selected) {
                                    WmwColors.Sunrise.copy(alpha = 0.18f)
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(16.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        ConsumerNavGlyph(tab = tab, selected = selected)
                    }
                    Text(
                        text = tab.label,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) WmwColors.Midnight else WmwColors.LightQuietText,
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
    val color = if (selected) WmwColors.Midnight else WmwColors.LightQuietText
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
