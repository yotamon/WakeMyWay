package com.wakemyway.app.learning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakemyway.core.learning.WakeCalibration
import com.wakemyway.core.learning.WakeFeedbackRating
import com.wakemyway.core.learning.WakeLearningDecision
import com.wakemyway.core.learning.WakeOutcomeDeriver
import com.wakemyway.core.learning.WakeTimeline
import com.wakemyway.core.learning.WakeTimelineFact
import com.wakemyway.core.runtime.MotionEvidenceKind
import com.wakemyway.core.runtime.WakeOutcome
import com.wakemyway.core.runtime.WakeSessionId
import java.time.Duration

@Composable
fun WakeLearningLab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember { WakeLearningManager(context) }
    var snapshot by remember { mutableStateOf(manager.snapshot()) }
    var lastDecision by remember { mutableStateOf<String?>(null) }

    fun recordFixture(
        prefix: String,
        engagementSeconds: Long,
        movementSeconds: Long,
        calibration: WakeCalibration,
        annoyance: Int,
        agency: Int,
    ) {
        val policy = snapshot.activePolicy
        val sessionId = WakeSessionId("$prefix-${System.nanoTime()}")
        val timeline = WakeTimeline(
            sessionId = sessionId,
            policyVersion = policy.version,
            startedAtEpochMillis = System.currentTimeMillis().coerceAtLeast(1),
            terminalOutcome = WakeOutcome.COMPLETED,
            facts = listOf(
                WakeTimelineFact.EngagementObserved(Duration.ofSeconds(engagementSeconds)),
                WakeTimelineFact.InterventionDepth(Duration.ofSeconds(engagementSeconds + 2), 1),
                WakeTimelineFact.MotionObserved(
                    Duration.ofSeconds(movementSeconds),
                    MotionEvidenceKind.SUSTAINED_MOVEMENT,
                ),
                WakeTimelineFact.ActivationCompleted(Duration.ofSeconds(movementSeconds + 20)),
            ),
        )
        val outcome = WakeOutcomeDeriver.derive(
            timeline = timeline,
            calibration = calibration,
            annoyance = WakeFeedbackRating(annoyance),
            agency = WakeFeedbackRating(agency),
        )
        val result = manager.recordOutcome(outcome)
        snapshot = result.snapshot
        lastDecision = when (val decision = result.decision) {
            is WakeLearningDecision.PolicyUpdated -> "Policy ${decision.previousPolicy.version} → ${decision.nextPolicy.version}: ${decision.explanation}"
            is WakeLearningDecision.NoChange -> "No change (${decision.reason.name}): ${decision.explanation}"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Text(
            modifier = Modifier.padding(top = 28.dp),
            text = "Wake Learning v0",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = "Local deterministic learning from privacy-minimized Wake Outcomes. No transcript, Tomorrow Contract, cloud, AI, or ML input.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        Text(
            modifier = Modifier.padding(top = 14.dp),
            text = "Active future policy · v${snapshot.activePolicy.version}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = "Movement prompt delay ${snapshot.activePolicy.movementPromptDelay.seconds}s · activation threshold ${snapshot.activePolicy.activationThreshold} (not learnable in v0)",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = "${snapshot.status.name} · ${snapshot.detail}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )

        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
                recordFixture(
                    prefix = "slow-movement",
                    engagementSeconds = 10,
                    movementSeconds = 90,
                    calibration = WakeCalibration.GOT_UP,
                    annoyance = 2,
                    agency = 4,
                )
            },
        ) {
            Text("Add slow-movement morning")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                recordFixture(
                    prefix = "smooth-friction",
                    engagementSeconds = 8,
                    movementSeconds = 30,
                    calibration = WakeCalibration.GOT_UP,
                    annoyance = 4,
                    agency = 4,
                )
            },
        ) {
            Text("Add smooth but annoying morning")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                recordFixture(
                    prefix = "returned-to-bed",
                    engagementSeconds = 10,
                    movementSeconds = 35,
                    calibration = WakeCalibration.RETURNED_TO_BED,
                    annoyance = 3,
                    agency = 3,
                )
            },
        ) {
            Text("Add Activation Complete → returned to bed")
        }

        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {
                manager.reset()
                snapshot = manager.snapshot()
                lastDecision = "Learning history/profile reset. Stable default policy restored."
            },
        ) {
            Text("Reset Wake Learning")
        }

        lastDecision?.let { message ->
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        snapshot.profile?.let { profile ->
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = "Latest learned change",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = profile.explanation,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            modifier = Modifier.padding(top = 18.dp),
            text = "Outcome history · ${snapshot.outcomes.size}/${PrivateWakeLearningStore.MAX_OUTCOMES}",
            style = MaterialTheme.typography.labelLarge,
        )
        snapshot.outcomes.takeLast(6).reversed().forEach { outcome ->
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "policy v${outcome.policyVersion} · engage ${outcome.firstEngagementAfter?.seconds ?: "—"}s · move ${outcome.meaningfulMovementAfter?.seconds ?: "—"}s · activation ${if (outcome.activationCompleted) "yes" else "no"} · confirmed ${outcome.confirmedWakeSuccess.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
