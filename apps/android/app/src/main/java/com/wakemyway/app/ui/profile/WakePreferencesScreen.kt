package com.wakemyway.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.personalization.ConversationAmount
import com.wakemyway.core.personalization.HumorPreference
import com.wakemyway.core.personalization.InterventionStyle
import com.wakemyway.core.personalization.MorningBarrier
import com.wakemyway.core.personalization.MotivationStyle
import com.wakemyway.core.personalization.PerceivedWakeInertia
import com.wakemyway.core.personalization.WakePreferences

@Composable
fun WakePreferencesScreen(
    preferences: WakePreferences,
    onChanged: (WakePreferences) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(WmwSpacing.Lg),
        ) {
            BackHeader("Wake preferences", onBack)
            Text(
                "These choices shape how the voice approaches you. WakeMyWay can learn timing and friction from real mornings, but it will not silently override these preferences.",
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )
            PreferenceGroup(
                title = "What usually gets in the way?",
                detail = "This changes framing, not how WakeMyWay labels you.",
                options = MorningBarrier.entries,
                selected = preferences.morningBarrier,
                label = ::morningBarrierLabel,
                onSelected = { onChanged(preferences.copy(morningBarrier = it)) },
            )
            PreferenceGroup(
                title = "How long until your brain feels properly awake?",
                detail = "A self-reported pacing hint. It never counts as proof of wakefulness.",
                options = PerceivedWakeInertia.entries,
                selected = preferences.perceivedWakeInertia,
                label = ::wakeInertiaLabel,
                onSelected = { onChanged(preferences.copy(perceivedWakeInertia = it)) },
            )
            PreferenceGroup(
                title = "When you really do not want to get up",
                detail = "Firm means more direct wording while the same safety rules still apply.",
                options = InterventionStyle.entries,
                selected = preferences.interventionStyle,
                label = ::interventionStyleLabel,
                onSelected = { onChanged(preferences.copy(interventionStyle = it)) },
            )
            PreferenceGroup(
                title = "What usually gets you moving?",
                detail = "Physical wake steps still come first while you are half asleep.",
                options = MotivationStyle.entries,
                selected = preferences.motivationStyle,
                label = ::motivationStyleLabel,
                onSelected = { onChanged(preferences.copy(motivationStyle = it)) },
            )
            PreferenceGroup(
                title = "How much should I talk?",
                detail = "This bounds optional conversation after the current wake action is clear.",
                options = ConversationAmount.entries,
                selected = preferences.conversationAmount,
                label = ::conversationAmountLabel,
                onSelected = { onChanged(preferences.copy(conversationAmount = it)) },
            )
            PreferenceGroup(
                title = "Humor",
                detail = "Your Character keeps its identity; this controls optional jokes.",
                options = HumorPreference.entries,
                selected = preferences.humorPreference,
                label = ::humorPreferenceLabel,
                onSelected = { onChanged(preferences.copy(humorPreference = it)) },
            )
        }
    }
}

@Composable
private fun <T> PreferenceGroup(
    title: String,
    detail: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    WmwCard(onLightSurface = true) {
        Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = WmwColors.Midnight)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = WmwColors.LightQuietText)
            Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs)) {
                options.forEach { option ->
                    val active = option == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { this.selected = active }
                            .clickable(role = Role.RadioButton) { onSelected(option) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) WmwColors.Sunrise.copy(alpha = 0.14f) else WmwColors.LightSurfaceMuted,
                        border = BorderStroke(
                            1.dp,
                            if (active) WmwColors.Sunrise else WmwColors.DarkHairline,
                        ),
                    ) {
                        Text(
                            text = label(option),
                            modifier = Modifier.padding(horizontal = WmwSpacing.Md, vertical = 13.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.Midnight,
                        )
                    }
                }
            }
        }
    }
}
internal fun morningBarrierLabel(value: MorningBarrier): String = when (value) {
    MorningBarrier.UNSURE -> "Not sure yet"
    MorningBarrier.HALF_ASLEEP -> "I am still half asleep"
    MorningBarrier.SNOOZE_LOOP -> "I keep choosing five more minutes"
    MorningBarrier.AWAKE_BUT_STUCK -> "I am awake but cannot get moving"
    MorningBarrier.MORNING_OVERWHELM -> "The morning feels overwhelming"
    MorningBarrier.LOSE_TRACK_OF_TIME -> "I lose track of time"
    MorningBarrier.USUALLY_GET_UP -> "I usually get up fairly easily"
}

internal fun wakeInertiaLabel(value: PerceivedWakeInertia): String = when (value) {
    PerceivedWakeInertia.UNSURE -> "Not sure"
    PerceivedWakeInertia.FEW_MINUTES -> "A few minutes"
    PerceivedWakeInertia.ABOUT_15_MINUTES -> "Around 15 minutes"
    PerceivedWakeInertia.ABOUT_30_MINUTES -> "Around 30 minutes"
    PerceivedWakeInertia.HOUR_OR_MORE -> "An hour or more"
}

internal fun interventionStyleLabel(value: InterventionStyle): String = when (value) {
    InterventionStyle.GENTLE -> "Gentle"
    InterventionStyle.ENCOURAGING -> "Encouraging"
    InterventionStyle.PERSISTENT -> "Persistent"
    InterventionStyle.FIRM -> "Firm and direct"
}
internal fun motivationStyleLabel(value: MotivationStyle): String = when (value) {
    MotivationStyle.CONCRETE_ACTION -> "Give me one concrete action"
    MotivationStyle.ENCOURAGEMENT -> "Brief encouragement"
    MotivationStyle.ACCOUNTABILITY -> "Remind me of my own plan"
    MotivationStyle.LIGHT_CONVERSATION -> "A little conversation"
    MotivationStyle.HUMOR -> "Humor"
}

internal fun conversationAmountLabel(value: ConversationAmount): String = when (value) {
    ConversationAmount.MINIMAL -> "Minimal"
    ConversationAmount.BALANCED -> "Balanced"
    ConversationAmount.SOCIAL -> "More social"
}

internal fun humorPreferenceLabel(value: HumorPreference): String = when (value) {
    HumorPreference.OFF -> "Off"
    HumorPreference.LIGHT -> "Light"
    HumorPreference.WELCOME -> "Welcome"
}
