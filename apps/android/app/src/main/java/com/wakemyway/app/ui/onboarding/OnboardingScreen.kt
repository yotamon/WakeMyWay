package com.wakemyway.app.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.components.WmwBrandHero
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSunriseMark
import com.wakemyway.app.ui.components.WmwWakeHorizon
import com.wakemyway.app.ui.profile.conversationAmountLabel
import com.wakemyway.app.ui.profile.interventionStyleLabel
import com.wakemyway.app.ui.profile.morningBarrierLabel
import com.wakemyway.app.ui.profile.motivationStyleLabel
import com.wakemyway.app.ui.profile.wakeInertiaLabel
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.core.personalization.ConversationAmount
import com.wakemyway.core.personalization.InterventionStyle
import com.wakemyway.core.personalization.MorningBarrier
import com.wakemyway.core.personalization.MotivationStyle
import com.wakemyway.core.personalization.PerceivedWakeInertia
import com.wakemyway.core.personalization.WakePreferences

private data class IntroPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val detail: String,
)

private val introPages = listOf(
    IntroPage(
        eyebrow = "WAKE UP YOUR WAY",
        title = "The alarm that learns how to wake you.",
        body = "WakeMyWay does more than ring. It talks, waits for a real response, asks you to move, and learns which amount of friction actually helps your mornings.",
        detail = "Start with one wake. Every real morning can make the next strategy a little more personal without making the alarm depend on AI or the internet.",
    ),
    IntroPage(
        eyebrow = "LOCAL FIRST",
        title = "Reliable first. Personal second.",
        body = "The alarm, critical sound, Stop and Snooze stay on your phone. Voice Check-In and a private reason for tomorrow are optional parts of the wake you choose.",
        detail = "Permissions are requested only when a feature needs them. You do not need an account to create a wake or use WakeMyWay.",
    ),
)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    initialWakePreferences: WakePreferences = WakePreferences(),
    onWakePreferencesChanged: (WakePreferences) -> Unit = {},
) {
    var step by remember { mutableIntStateOf(0) }
    var wakePreferences by remember(initialWakePreferences) { mutableStateOf(initialWakePreferences) }
    val totalSteps = 8

    fun update(next: WakePreferences) {
        wakePreferences = next
        onWakePreferencesChanged(next)
    }

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = WmwSpacing.Md, end = WmwSpacing.Md),
            ) {
                Text("Skip", color = WmwColors.LightQuietText)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = WmwSpacing.Xl)
                    .padding(top = 72.dp, bottom = WmwSpacing.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (step == 0) {
                    WmwBrandHero(
                        tagline = "Brighter mornings. Your way.",
                        modifier = Modifier.padding(top = WmwSpacing.Md),
                    )
                } else {
                    WmwWakeHorizon(modifier = Modifier.fillMaxWidth().padding(horizontal = WmwSpacing.Xl), progress = step / 7f)
                }

                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (step) {
                        0, 1 -> IntroContent(introPages[step])
                        2 -> PersonalizationEntry(
                            onPersonalize = { step = 3 },
                            onBalancedDefaults = {
                                update(WakePreferences())
                                onComplete()
                            },
                        )
                        3 -> QuestionContent(
                            eyebrow = "YOUR MORNING",
                            title = "What usually happens when the alarm goes off?",
                            options = MorningBarrier.entries,
                            selected = wakePreferences.morningBarrier,
                            label = ::morningBarrierLabel,
                            onSelected = { update(wakePreferences.copy(morningBarrier = it)) },
                        )
                        4 -> QuestionContent(
                            eyebrow = "WAKE PACE",
                            title = "How long until your brain feels properly awake?",
                            options = PerceivedWakeInertia.entries,
                            selected = wakePreferences.perceivedWakeInertia,
                            label = ::wakeInertiaLabel,
                            onSelected = { update(wakePreferences.copy(perceivedWakeInertia = it)) },
                        )
                        5 -> QuestionContent(
                            eyebrow = "APPROACH",
                            title = "When you really do not want to get up, how should I approach you?",
                            options = InterventionStyle.entries,
                            selected = wakePreferences.interventionStyle,
                            label = ::interventionStyleLabel,
                            onSelected = { update(wakePreferences.copy(interventionStyle = it)) },
                        )
                        6 -> QuestionContent(
                            eyebrow = "MOTIVATION",
                            title = "What usually gets you moving?",
                            options = MotivationStyle.entries,
                            selected = wakePreferences.motivationStyle,
                            label = ::motivationStyleLabel,
                            onSelected = { update(wakePreferences.copy(motivationStyle = it)) },
                        )
                        7 -> QuestionContent(
                            eyebrow = "CONVERSATION",
                            title = "How much should I talk in the morning?",
                            options = ConversationAmount.entries,
                            selected = wakePreferences.conversationAmount,
                            label = ::conversationAmountLabel,
                            onSelected = { update(wakePreferences.copy(conversationAmount = it)) },
                        )
                    }
                    Spacer(Modifier.height(WmwSpacing.Md))
                }
                if (step != 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(totalSteps) { index ->
                            Surface(
                                modifier = Modifier.padding(horizontal = 3.dp).size(if (index == step) 9.dp else 6.dp),
                                shape = CircleShape,
                                color = if (index == step) WmwColors.Midnight else WmwColors.Dawn.copy(alpha = 0.45f),
                                border = if (index == step) null else BorderStroke(1.dp, WmwColors.DarkHairline),
                                content = {},
                            )
                        }
                    }
                    WmwPrimaryAction(
                        label = if (step == totalSteps - 1) "Finish" else "Continue",
                        onClick = {
                            when {
                                step == totalSteps - 1 -> onComplete()
                                else -> step += 1
                            }
                        },
                        modifier = Modifier.padding(top = WmwSpacing.Lg),
                        onLightSurface = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroContent(page: IntroPage) {
    Text(page.eyebrow, style = MaterialTheme.typography.labelSmall, color = WmwColors.DawnText)
    Text(
        page.title,
        modifier = Modifier.padding(top = WmwSpacing.Sm),
        style = MaterialTheme.typography.headlineLarge,
        color = WmwColors.Midnight,
        textAlign = TextAlign.Center,
    )
    Text(
        page.body,
        modifier = Modifier.padding(top = WmwSpacing.Md),
        style = MaterialTheme.typography.bodyLarge,
        color = WmwColors.LightQuietText,
        textAlign = TextAlign.Center,
    )
    Text(
        page.detail,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WmwSpacing.Xl, start = WmwSpacing.Sm, end = WmwSpacing.Sm),
        style = MaterialTheme.typography.bodyMedium,
        color = WmwColors.Midnight.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PersonalizationEntry(
    onPersonalize: () -> Unit,
    onBalancedDefaults: () -> Unit,
) {
    Text("PERSONALIZE", style = MaterialTheme.typography.labelSmall, color = WmwColors.DawnText)
    Text(
        "Teach WakeMyWay your mornings.",
        modifier = Modifier.padding(top = WmwSpacing.Sm),
        style = MaterialTheme.typography.headlineLarge,
        color = WmwColors.Midnight,
        textAlign = TextAlign.Center,
    )
    Text(
        "Five quick choices shape how the voice approaches you. You can change every answer later.",
        modifier = Modifier.padding(top = WmwSpacing.Md),
        style = MaterialTheme.typography.bodyLarge,
        color = WmwColors.LightQuietText,
        textAlign = TextAlign.Center,
    )
    WmwPrimaryAction(
        label = "Personalize my wake",
        onClick = onPersonalize,
        modifier = Modifier.padding(top = WmwSpacing.Xl),
        onLightSurface = true,
    )
    TextButton(onClick = onBalancedDefaults, modifier = Modifier.padding(top = WmwSpacing.Sm)) {
        Text("Use balanced defaults", color = WmwColors.LightQuietText)
    }
}
@Composable
private fun <T> QuestionContent(
    eyebrow: String,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = WmwColors.DawnText)
    Text(
        title,
        modifier = Modifier.padding(top = WmwSpacing.Sm),
        style = MaterialTheme.typography.headlineMedium,
        color = WmwColors.Midnight,
        textAlign = TextAlign.Center,
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = WmwSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(WmwSpacing.Xs),
    ) {
        options.forEach { option ->
            val active = option == selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .semantics { this.selected = active }
                    .clickable(role = Role.RadioButton) { onSelected(option) },
                shape = RoundedCornerShape(18.dp),
                color = if (active) WmwColors.Sunrise.copy(alpha = 0.14f) else WmwColors.PaperCard,
                border = BorderStroke(1.dp, if (active) WmwColors.Sunrise else WmwColors.DarkHairline),
            ) {
                Text(
                    label(option),
                    modifier = Modifier.padding(horizontal = WmwSpacing.Md, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.Midnight,
                )
            }
        }
    }
}
