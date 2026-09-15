package com.wakemyway.app.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.components.WmwBrandHero
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwPrimaryAction
import com.wakemyway.app.ui.components.WmwSunriseMark
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val detail: String,
)

private val pages = listOf(
    OnboardingPage(
        eyebrow = "WELCOME",
        title = "Mornings feel better your way.",
        body = "WakeMyWay turns an alarm into a calmer morning you can actually respond to.",
        detail = "Set the time, choose the sound, and let the rest of the wake adapt around you.",
    ),
    OnboardingPage(
        eyebrow = "VOICE CHECK-IN",
        title = "A wake that waits for you.",
        body = "Alfred can speak, listen for a real reply, and keep the wake moving without handing control to the cloud.",
        detail = "Voice permission is requested only when you choose a Voice Check-In alarm. Alarm delivery stays local.",
    ),
    OnboardingPage(
        eyebrow = "TOMORROW CONTRACT",
        title = "Give tomorrow a reason.",
        body = "Leave one private reason for getting up and one simple First Move for the version of you who just woke up.",
        detail = "Tomorrow Contract text stays credential-protected on this device and is never required to fire the alarm.",
    ),
    OnboardingPage(
        eyebrow = "LOCAL FIRST",
        title = "Your alarm does not need the internet.",
        body = "Wake schedules, critical playback, Stop and Snooze are owned locally by the phone.",
        detail = "Android permissions are explained in context when a feature actually needs them. An account is optional, not a Wake Ready requirement.",
    ),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val firstPage = pageIndex == 0
    val lastPage = pageIndex == pages.lastIndex

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Box(Modifier.fillMaxSize()) {
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
                if (firstPage) {
                    WmwBrandHero(
                        tagline = "Brighter mornings. Your way.",
                        modifier = Modifier.padding(top = WmwSpacing.Md),
                    )
                } else {
                    WmwSunriseMark(
                        modifier = Modifier.size(width = 190.dp, height = 108.dp),
                    )
                }

                Spacer(Modifier.height(34.dp))
                Text(
                    text = page.eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = WmwColors.DawnDeep,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = page.title,
                    modifier = Modifier.padding(top = WmwSpacing.Sm),
                    style = MaterialTheme.typography.headlineLarge,
                    color = WmwColors.Midnight,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = page.body,
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WmwColors.LightQuietText,
                    textAlign = TextAlign.Center,
                )

                WmwCard(
                    modifier = Modifier.padding(top = 28.dp),
                    onLightSurface = true,
                ) {
                    Text(
                        text = page.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WmwColors.Midnight,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    pages.indices.forEach { index ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 4.dp).size(if (index == pageIndex) 10.dp else 7.dp),
                            shape = CircleShape,
                            color = if (index == pageIndex) WmwColors.Midnight else WmwColors.Dawn.copy(alpha = 0.45f),
                            border = if (index == pageIndex) null else BorderStroke(1.dp, WmwColors.DarkHairline),
                            content = {},
                        )
                    }
                }

                WmwPrimaryAction(
                    label = if (lastPage) "Get started" else "Continue",
                    onClick = {
                        if (lastPage) onComplete() else pageIndex += 1
                    },
                    modifier = Modifier.padding(top = WmwSpacing.Lg),
                    onLightSurface = true,
                )
            }
        }
    }
}
