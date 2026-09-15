package com.wakemyway.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wakemyway.app.product.AppAppearance
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing

@Composable
fun AppearanceScreen(
    appearance: AppAppearance,
    onAppearanceChanged: (AppAppearance) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        "‹",
                        style = MaterialTheme.typography.headlineMedium,
                        color = WmwColors.Midnight,
                    )
                }
                Text(
                    "Appearance",
                    modifier = Modifier.padding(start = WmwSpacing.Xs),
                    style = MaterialTheme.typography.titleLarge,
                    color = WmwColors.Midnight,
                )
            }

            Text(
                text = "Choose your planning atmosphere.",
                modifier = Modifier.padding(top = 28.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = "This changes the normal app around your next wake. The active wake keeps its designed night-to-morning progression.",
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.LightQuietText,
            )

            Column(
                modifier = Modifier.padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm),
            ) {
                AppAppearance.entries.forEach { option ->
                    AppearanceChoice(
                        appearance = option,
                        selected = option == appearance,
                        onSelected = { onAppearanceChanged(option) },
                    )
                }
            }

            WmwCard(
                modifier = Modifier.padding(top = WmwSpacing.Lg),
                onLightSurface = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
                    Text(
                        text = "WAKE EXPERIENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = WmwColors.DawnDeep,
                    )
                    Text(
                        text = "Always purpose-designed",
                        style = MaterialTheme.typography.titleMedium,
                        color = WmwColors.Midnight,
                    )
                    Text(
                        text = "Emerging, Listening, Moving and Morning Started never inherit this preference. Their visual progression stays consistent when you are half awake.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.LightQuietText,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceChoice(
    appearance: AppAppearance,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    val preview = when (appearance) {
        AppAppearance.DAYLIGHT -> WmwColors.Cloud
        AppAppearance.WARM_SUNRISE -> Color(0xFFFFEFE4)
        AppAppearance.SOFT_DAWN -> Color(0xFFECEFFA)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected),
        shape = RoundedCornerShape(22.dp),
        color = preview,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) WmwColors.Midnight else WmwColors.DarkHairline,
        ),
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WmwSpacing.Lg, vertical = WmwSpacing.Md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = appearanceName(appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = WmwColors.Midnight,
                )
                Text(
                    text = appearanceDetail(appearance),
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = WmwColors.LightQuietText,
                )
            }
            Text(
                text = if (selected) "●" else "○",
                style = MaterialTheme.typography.titleLarge,
                color = if (selected) WmwColors.Sunrise else WmwColors.LightQuietText,
            )
        }
    }
}

fun appearanceName(appearance: AppAppearance): String = when (appearance) {
    AppAppearance.DAYLIGHT -> "Daylight"
    AppAppearance.WARM_SUNRISE -> "Warm Sunrise"
    AppAppearance.SOFT_DAWN -> "Soft Dawn"
}

private fun appearanceDetail(appearance: AppAppearance): String = when (appearance) {
    AppAppearance.DAYLIGHT -> "Clean paper and cloud"
    AppAppearance.WARM_SUNRISE -> "A little more morning warmth"
    AppAppearance.SOFT_DAWN -> "Cool lavender calm"
}
