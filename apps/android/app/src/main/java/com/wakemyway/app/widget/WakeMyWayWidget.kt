package com.wakemyway.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wakemyway.app.R
import com.wakemyway.app.product.AppAppearance
import com.wakemyway.core.learning.WakeCalibrationOutcome

class WakeMyWayWidget : GlanceAppWidget() {
    /**
     * Exact gives the composition the launcher-provided bounds, while the UI itself deliberately
     * snaps to three product breakpoints. This keeps the hierarchy stable across OEM grid geometry.
     */
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 250.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WakeWidgetSnapshotProjector(context).project()
        provideContent {
            WakeWidgetContent(snapshot)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            WakeWidgetContent(WakeWidgetSnapshot.preview())
        }
    }
}

class WakeMyWayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WakeMyWayWidget()
}

internal enum class WakeWidgetLayout {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

internal fun selectWakeWidgetLayout(
    widthDp: Float,
    heightDp: Float,
): WakeWidgetLayout = when {
    widthDp < 190f -> WakeWidgetLayout.COMPACT
    heightDp >= 240f -> WakeWidgetLayout.EXPANDED
    else -> WakeWidgetLayout.MEDIUM
}

internal data class WakeWidgetBodyTarget(
    val destination: WakeWidgetDestination,
    val alarmId: String? = null,
)

internal fun selectWakeWidgetBodyTarget(
    activeWake: Boolean,
    nextAlarmId: String?,
    readiness: WakeWidgetReadiness,
): WakeWidgetBodyTarget = when {
    activeWake -> WakeWidgetBodyTarget(WakeWidgetDestination.HOME)
    nextAlarmId != null -> WakeWidgetBodyTarget(
        destination = WakeWidgetDestination.ALARM_EDITOR,
        alarmId = nextAlarmId,
    )
    readiness == WakeWidgetReadiness.NONE ->
        WakeWidgetBodyTarget(WakeWidgetDestination.ALARM_EDITOR)
    else -> WakeWidgetBodyTarget(WakeWidgetDestination.HOME)
}

private data class WidgetPalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val quietText: Color,
    val accent: Color,
    val accentSoft: Color,
    val positive: Color,
    val attention: Color,
)

@Composable
private fun WakeWidgetContent(snapshot: WakeWidgetSnapshot) {
    val context = LocalContext.current
    val size = LocalSize.current
    val layout = selectWakeWidgetLayout(
        widthDp = size.width.value,
        heightDp = size.height.value,
    )
    val palette = snapshot.appearance.palette()
    val bodyTarget = selectWakeWidgetBodyTarget(
        activeWake = snapshot.activeWake,
        nextAlarmId = snapshot.nextAlarmId,
        readiness = snapshot.readiness,
    )
    val bodyIntent = WakeWidgetLaunch.intent(
        context = context,
        destination = bodyTarget.destination,
        alarmId = bodyTarget.alarmId,
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .cornerRadius(28.dp)
            .clickable(actionStartActivity(bodyIntent)),
    ) {
        when (layout) {
            WakeWidgetLayout.COMPACT -> CompactWidget(snapshot, palette)
            WakeWidgetLayout.MEDIUM -> MediumWidget(snapshot, palette)
            WakeWidgetLayout.EXPANDED -> ExpandedWidget(snapshot, palette)
        }
    }
}

@Composable
private fun CompactWidget(
    snapshot: WakeWidgetSnapshot,
    palette: WidgetPalette,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(11.dp),
    ) {
        BrandRow(palette = palette, compact = true)
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = snapshot.time,
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = if (snapshot.time.length > 5) 23.sp else 32.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Text(
            text = snapshot.dateLabel,
            style = TextStyle(
                color = ColorProvider(palette.quietText),
                fontSize = 9.sp,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        StatusText(snapshot, palette, compact = true)
        Spacer(GlanceModifier.defaultWeight())
        WakeLine()
    }
}

@Composable
private fun MediumWidget(
    snapshot: WakeWidgetSnapshot,
    palette: WidgetPalette,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            BrandRow(palette = palette, compact = true)
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = snapshot.bodyActionLabel().uppercase(),
                style = TextStyle(
                    color = ColorProvider(palette.accent),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = snapshot.time,
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = if (snapshot.time.length > 5) 30.sp else 40.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = snapshot.dateLabel,
                style = TextStyle(
                    color = ColorProvider(palette.quietText),
                    fontSize = 9.sp,
                ),
                maxLines = 1,
            )
            snapshot.nextAlarmSchedule?.let { schedule ->
                Text(
                    text = "  ·  $schedule",
                    style = TextStyle(
                        color = ColorProvider(palette.quietText),
                        fontSize = 9.sp,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            StatusText(snapshot, palette, compact = true)
        }
        Spacer(GlanceModifier.defaultWeight())
        WakeLine()
    }
}

@Composable
private fun ExpandedWidget(
    snapshot: WakeWidgetSnapshot,
    palette: WidgetPalette,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(15.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            BrandRow(palette = palette, compact = false)
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = snapshot.bodyActionLabel().uppercase(),
                style = TextStyle(
                    color = ColorProvider(palette.accent),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = if (snapshot.activeWake) "WAKE IN PROGRESS" else "NEXT WAKE",
            style = TextStyle(
                color = ColorProvider(palette.quietText),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(
            text = snapshot.time,
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = if (snapshot.time.length > 5) 32.sp else 42.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = snapshot.dateLabel,
                style = TextStyle(
                    color = ColorProvider(palette.quietText),
                    fontSize = 10.sp,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            StatusText(snapshot, palette, compact = false)
        }

        Spacer(GlanceModifier.height(10.dp))
        if (
            snapshot.primaryAction == WakeWidgetPrimaryAction.MORNING_CHECK_IN &&
            snapshot.pendingMorningCheckInOccurrenceId != null
        ) {
            MorningCheckInCard(
                occurrenceId = snapshot.pendingMorningCheckInOccurrenceId,
                palette = palette,
            )
        } else {
            ExpandedDetails(snapshot, palette)
        }

        Spacer(GlanceModifier.defaultWeight())
        WakeLine()
    }
}

@Composable
private fun BrandRow(
    palette: WidgetPalette,
    compact: Boolean,
) {
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.wmw_widget_mark),
            contentDescription = null,
            modifier = GlanceModifier.size(if (compact) 20.dp else 24.dp),
        )
        Spacer(GlanceModifier.size(5.dp))
        Text(
            text = "WakeMyWay",
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusText(
    snapshot: WakeWidgetSnapshot,
    palette: WidgetPalette,
    compact: Boolean,
) {
    val (text, color) = snapshot.statusLabel(palette)
    Text(
        text = text,
        style = TextStyle(
            color = ColorProvider(color),
            fontSize = if (compact) 9.sp else 10.sp,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
    )
}

@Composable
private fun ExpandedDetails(
    snapshot: WakeWidgetSnapshot,
    palette: WidgetPalette,
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        DetailRow(
            label = "Tomorrow plan",
            value = when (snapshot.planState) {
                WakeWidgetPlanState.READY -> "Ready ✓"
                WakeWidgetPlanState.AVAILABLE -> "Not prepared"
                WakeWidgetPlanState.NONE -> "Optional"
            },
            palette = palette,
        )
        SoftDivider(palette)
        DetailRow(
            label = "Alarm",
            value = snapshot.nextAlarmSchedule ?: when (snapshot.readiness) {
                WakeWidgetReadiness.NONE -> "No wake set"
                WakeWidgetReadiness.UNAVAILABLE -> "Open app to refresh"
                else -> "Next wake"
            },
            palette = palette,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    palette: WidgetPalette,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(palette.quietText),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun SoftDivider(palette: WidgetPalette) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.accentSoft),
    ) {}
}

@Composable
private fun MorningCheckInCard(
    occurrenceId: String,
    palette: WidgetPalette,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(palette.card)
            .cornerRadius(18.dp)
            .padding(12.dp),
    ) {
        Text(
            text = "MORNING CHECK-IN",
            style = TextStyle(
                color = ColorProvider(palette.quietText),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Did that wake actually stick?",
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            CheckInChoice(
                label = "Yes",
                palette = palette,
                modifier = GlanceModifier.defaultWeight(),
                action = morningCheckInAction(
                    occurrenceId = occurrenceId,
                    outcome = WakeCalibrationOutcome.GOT_UP,
                ),
            )
            Spacer(GlanceModifier.size(8.dp))
            CheckInChoice(
                label = "Not really",
                palette = palette,
                modifier = GlanceModifier.defaultWeight(),
                action = morningCheckInAction(
                    occurrenceId = occurrenceId,
                    outcome = WakeCalibrationOutcome.RETURNED_TO_BED,
                ),
            )
        }
    }
}

@Composable
private fun CheckInChoice(
    label: String,
    palette: WidgetPalette,
    modifier: GlanceModifier,
    action: androidx.glance.action.Action,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(palette.accentSoft)
            .cornerRadius(14.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun WakeLine() {
    Spacer(GlanceModifier.height(3.dp))
    Image(
        provider = ImageProvider(R.drawable.wmw_widget_wake_line),
        contentDescription = null,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(8.dp),
        contentScale = ContentScale.FillBounds,
    )
}

private fun WakeWidgetSnapshot.bodyActionLabel(): String = when {
    activeWake -> "Open wake  ›"
    nextAlarmId != null -> "Edit alarm  ›"
    readiness == WakeWidgetReadiness.NONE -> "Add alarm  ›"
    else -> "Open app  ›"
}

private fun WakeWidgetSnapshot.statusLabel(palette: WidgetPalette): Pair<String, Color> = when {
    activeWake -> "● Wake in progress" to palette.accent
    readiness == WakeWidgetReadiness.READY -> "✓ Wake Ready" to palette.positive
    readiness == WakeWidgetReadiness.NEEDS_ATTENTION -> "! Needs attention" to palette.attention
    readiness == WakeWidgetReadiness.NONE -> "No wake set" to palette.quietText
    else -> "Open to refresh" to palette.quietText
}

private fun AppAppearance.palette(): WidgetPalette = when (this) {
    AppAppearance.DAYLIGHT -> WidgetPalette(
        background = Color(0xFFFFFCF8),
        card = Color(0xFFF3F1F2),
        text = Color(0xFF08142F),
        quietText = Color(0xFF66728B),
        accent = Color(0xFFFF9F6D),
        accentSoft = Color(0xFFFFE6D8),
        positive = Color(0xFF2F7758),
        attention = Color(0xFFC43131),
    )
    AppAppearance.WARM_SUNRISE -> WidgetPalette(
        background = Color(0xFFFFF6EE),
        card = Color(0xFFFFEDE2),
        text = Color(0xFF08142F),
        quietText = Color(0xFF66728B),
        accent = Color(0xFFFF9F6D),
        accentSoft = Color(0xFFFFD7C2),
        positive = Color(0xFF2F7758),
        attention = Color(0xFFC43131),
    )
    AppAppearance.SOFT_DAWN -> WidgetPalette(
        background = Color(0xFFF3F4FA),
        card = Color(0xFFE8EBF8),
        text = Color(0xFF08142F),
        quietText = Color(0xFF66728B),
        accent = Color(0xFF7188E8),
        accentSoft = Color(0xFFE3E7FF),
        positive = Color(0xFF2F7758),
        attention = Color(0xFFC43131),
    )
}
