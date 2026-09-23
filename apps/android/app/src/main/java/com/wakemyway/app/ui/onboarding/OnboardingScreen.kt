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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        eyebrow = "WAKE UP YOUR WAY",
        title = "The alarm that learns how to wake you.",
        body = "WakeMyWay does more than ring. It talks, waits for a real response, asks you to move, and learns which amount of friction actually helps your mornings.",
        detail = "Start with one wake. Every real morning can make the next strategy a little more personal without making the alarm depend on AI or the internet.",
    ),
    OnboardingPage(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
                    Spacer(Modifier.height(WmwSpacing.Md))
                }

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
