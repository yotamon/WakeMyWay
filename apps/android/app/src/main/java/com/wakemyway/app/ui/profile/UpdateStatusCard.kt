package com.wakemyway.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import com.wakemyway.app.update.UpdateDeferralReason
import com.wakemyway.app.update.UpdateSource
import com.wakemyway.app.update.UpdateState
import com.wakemyway.app.update.UpdateUrgency

@Composable
internal fun UpdateStatusCard(
    state: UpdateState,
    onCheckForUpdates: () -> Unit,
    onBeginUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenInstallPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WmwCard(
        modifier = modifier,
        onLightSurface = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm),
        ) {
            Text(
                text = state.title(),
                style = MaterialTheme.typography.titleMedium,
                color = WmwColors.Midnight,
            )
            Text(
                text = state.detail(),
                style = MaterialTheme.typography.bodyMedium,
                color = WmwColors.LightQuietText,
            )

            val release = state.release()
            if (release != null && release.urgency != UpdateUrgency.NORMAL) {
                Text(
                    text = when (release.urgency) {
                        UpdateUrgency.IMPORTANT -> "Important update"
                        UpdateUrgency.CRITICAL -> "Critical update"
                        UpdateUrgency.NORMAL -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = WmwColors.DawnDeep,
                )
            }

            if (state is UpdateState.Downloading) {
                if (state.progress != null) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = WmwColors.Sunrise,
                        trackColor = WmwColors.LightSurfaceMuted,
                    )
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = WmwColors.LightQuietText,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = WmwColors.Sunrise,
                        trackColor = WmwColors.LightSurfaceMuted,
                    )
                }
            }

            release
                ?.releaseNotes
                ?.take(4)
                ?.forEach { note ->
                    Text(
                        text = "• $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = WmwColors.Midnight,
                    )
                }

            when (state) {
                UpdateState.Idle,
                UpdateState.UpToDate,
                is UpdateState.Error,
                -> UpdateTextAction(
                    label = if (state is UpdateState.Error) "Try again" else "Check for updates",
                    onClick = onCheckForUpdates,
                )

                is UpdateState.Available -> UpdatePrimaryAction(
                    label = "Update WakeMyWay",
                    onClick = onBeginUpdate,
                )

                is UpdateState.ReadyToInstall -> UpdatePrimaryAction(
                    label = if (state.release.source == UpdateSource.PLAY) {
                        "Restart & update"
                    } else {
                        "Install update"
                    },
                    onClick = onInstallUpdate,
                )

                is UpdateState.InstallPermissionRequired -> UpdatePrimaryAction(
                    label = "Allow installation",
                    onClick = onOpenInstallPermission,
                )

                is UpdateState.Deferred -> UpdateTextAction(
                    label = "Check safety again",
                    onClick = onInstallUpdate,
                )

                UpdateState.Checking,
                is UpdateState.Downloading,
                is UpdateState.Installing,
                -> Unit
            }
        }
    }
}

@Composable
private fun UpdatePrimaryAction(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WmwColors.Midnight,
            contentColor = WmwColors.WarmLight,
        ),
    ) {
        Text(label)
    }
}

@Composable
private fun UpdateTextAction(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(label, color = WmwColors.DawnDeep)
    }
}

private fun UpdateState.title(): String = when (this) {
    UpdateState.Idle -> "App updates"
    UpdateState.Checking -> "Checking for updates"
    UpdateState.UpToDate -> "You're up to date"
    is UpdateState.Available -> release.versionName?.let { "WakeMyWay $it is ready" } ?: "Update available"
    is UpdateState.Downloading -> "Downloading update"
    is UpdateState.ReadyToInstall -> "Update ready"
    is UpdateState.InstallPermissionRequired -> "One Android permission needed"
    is UpdateState.Deferred -> "Update safely paused"
    is UpdateState.Installing -> "Finishing update"
    is UpdateState.Error -> "Couldn't check for updates"
}

private fun UpdateState.detail(): String = when (this) {
    UpdateState.Idle -> "WakeMyWay can check for a newer version without affecting your alarms."
    UpdateState.Checking -> "This happens outside the critical wake path."
    UpdateState.UpToDate -> "This device already has the newest WakeMyWay version."
    is UpdateState.Available ->
        "Download the update now. WakeMyWay will protect nearby and active alarms before installation."
    is UpdateState.Downloading ->
        "You can keep using WakeMyWay while the update downloads."
    is UpdateState.ReadyToInstall ->
        "The update is downloaded and verified. Installation will only start when it is safe for your next wake."
    is UpdateState.InstallPermissionRequired ->
        "Android needs your permission to let this direct WakeMyWay build install its verified update."
    is UpdateState.Deferred -> when (reason) {
        UpdateDeferralReason.ACTIVE_WAKE ->
            "A wake session is active. The update will stay ready until the wake has finished."
        UpdateDeferralReason.UPCOMING_WAKE ->
            "Your next alarm is less than 90 minutes away. WakeMyWay will not restart before it."
    }
    is UpdateState.Installing ->
        "Android is applying the update. Your alarm configuration remains stored locally."
    is UpdateState.Error ->
        message
}

private fun UpdateState.release() = when (this) {
    is UpdateState.Available -> release
    is UpdateState.Downloading -> release
    is UpdateState.ReadyToInstall -> release
    is UpdateState.InstallPermissionRequired -> release
    is UpdateState.Deferred -> release
    is UpdateState.Installing -> release
    is UpdateState.Error -> release
    UpdateState.Idle,
    UpdateState.Checking,
    UpdateState.UpToDate,
    -> null
}
