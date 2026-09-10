package com.wakemyway.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.AlarmPlaybackService
import com.wakemyway.app.alarm.AlarmPresentationAccess
import com.wakemyway.app.alarm.AlarmRepairTarget
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.alarm.repairTarget
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import com.wakemyway.app.ui.navigation.WakeMyWayApp
import com.wakemyway.app.ui.theme.WakeMyWayTheme

class MainActivity : ComponentActivity() {
    private val alarmKernel by lazy { AlarmKernel(this) }

    private var showVoicePermissionPrimer by mutableStateOf(false)
    private var showNotificationPermissionPrimer by mutableStateOf(false)
    private var voiceWakeReadiness by mutableStateOf(VoiceWakeReadiness.UNAVAILABLE)
    private var wakeSystemRevision by mutableIntStateOf(0)

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        showVoicePermissionPrimer = false
        refreshProductReadiness()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        showNotificationPermissionPrimer = false
        refreshProductReadiness()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AlarmPresentationAccess.ensureChannel(this)
        val health = alarmKernel.reconcile()
        recordCapabilities(WakeTimingTrace(this), health)
        refreshProductReadiness()

        setContent {
            WakeMyWayTheme {
                WakeMyWayApp(
                    voiceWakeReadiness = voiceWakeReadiness,
                    onEnableVoiceReplies = ::beginVoicePermissionSetup,
                    wakeSystemRevision = wakeSystemRevision,
                    onRepairWakeSystem = ::repairWakeSystem,
                )

                if (showNotificationPermissionPrimer) {
                    AlertDialog(
                        onDismissRequest = { showNotificationPermissionPrimer = false },
                        title = { Text(stringResource(R.string.notification_permission_title)) },
                        text = { Text(stringResource(R.string.notification_permission_body)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    markNotificationPermissionRequested()
                                    showNotificationPermissionPrimer = false
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                            ) {
                                Text(stringResource(R.string.notification_permission_enable))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationPermissionPrimer = false }) {
                                Text(stringResource(R.string.voice_permission_not_now))
                            }
                        },
                    )
                }

                if (showVoicePermissionPrimer) {
                    AlertDialog(
                        onDismissRequest = { showVoicePermissionPrimer = false },
                        title = { Text(stringResource(R.string.voice_permission_title)) },
                        text = { Text(stringResource(R.string.voice_permission_body)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    markVoicePermissionRequested()
                                    showVoicePermissionPrimer = false
                                    voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                            ) {
                                Text(stringResource(R.string.voice_permission_enable))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showVoicePermissionPrimer = false }) {
                                Text(stringResource(R.string.voice_permission_not_now))
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProductReadiness()
        resumeActiveWakeIfNeeded()
    }

    private fun refreshProductReadiness() {
        refreshVoiceWakeReadiness()
        alarmKernel.reconcile()
        wakeSystemRevision++
    }

    /**
     * Last-resort user recovery path.
     *
     * Full-screen intents are the correct background alarm mechanism. If Android/OEM presentation
     * access is nevertheless missing, opening Wake My Way manually while an occurrence is active
     * must never strand the user on Tonight with an unstoppable alarm. Because MainActivity is now
     * foreground, it can safely hand the active occurrence to the real WakeActivity.
     */
    private fun resumeActiveWakeIfNeeded() {
        val active = alarmKernel.activeOccurrence() ?: return
        startActivity(
            Intent(this, WakeActivity::class.java)
                .setData(
                    Uri.Builder()
                        .scheme("wakemyway")
                        .authority("active-wake-rescue")
                        .appendPath(active.id.value)
                        .build(),
                )
                .putExtra(AlarmPlaybackService.EXTRA_OCCURRENCE_ID, active.id.value)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    private fun repairWakeSystem() {
        when (alarmKernel.health().repairTarget()) {
            AlarmRepairTarget.EXACT_ALARM -> openAppDetailsSettings()
            AlarmRepairTarget.NOTIFICATIONS -> beginNotificationPermissionSetup()
            AlarmRepairTarget.ACTIVE_WAKE_CHANNEL -> openActiveWakeChannelSettings()
            AlarmRepairTarget.FULL_SCREEN_INTENT -> openFullScreenAlarmSettings()
            AlarmRepairTarget.NONE -> Unit
        }
    }

    private fun beginNotificationPermissionSetup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            openAppNotificationSettings()
            return
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            openAppNotificationSettings()
            return
        }

        val requestedBefore = getSharedPreferences(NOTIFICATION_PERMISSION_PREFS, MODE_PRIVATE)
            .getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)
        val shouldExplainAgain = shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

        if (requestedBefore && !shouldExplainAgain) {
            openAppNotificationSettings()
        } else {
            showNotificationPermissionPrimer = true
        }
    }

    private fun openActiveWakeChannelSettings() {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, AlarmPresentationAccess.CHANNEL_ID)
        }
        runCatching { startActivity(intent) }
            .onFailure { openAppNotificationSettings() }
    }

    private fun openFullScreenAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.fromParts("package", packageName, null),
        )
        runCatching { startActivity(intent) }
            .onFailure { openAppDetailsSettings() }
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        runCatching { startActivity(intent) }
            .onFailure { openAppDetailsSettings() }
    }

    private fun beginVoicePermissionSetup() {
        if (voiceWakeReadiness != VoiceWakeReadiness.SETUP_REQUIRED) return

        val requestedBefore = getSharedPreferences(VOICE_PERMISSION_PREFS, MODE_PRIVATE)
            .getBoolean(VOICE_PERMISSION_REQUESTED, false)
        val shouldExplainAgain = shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

        if (requestedBefore && !shouldExplainAgain) {
            openAppDetailsSettings()
        } else {
            showVoicePermissionPrimer = true
        }
    }

    private fun refreshVoiceWakeReadiness() {
        voiceWakeReadiness = when {
            !supportsOnDeviceVoiceReplies() -> VoiceWakeReadiness.UNAVAILABLE
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> VoiceWakeReadiness.READY
            else -> VoiceWakeReadiness.SETUP_REQUIRED
        }
    }

    private fun supportsOnDeviceVoiceReplies(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(this) }
            .getOrDefault(false)
    }

    private fun markVoicePermissionRequested() {
        getSharedPreferences(VOICE_PERMISSION_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(VOICE_PERMISSION_REQUESTED, true)
            .apply()
    }

    private fun markNotificationPermissionRequested() {
        getSharedPreferences(NOTIFICATION_PERMISSION_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true)
            .apply()
    }

    private fun openAppDetailsSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    private companion object {
        const val VOICE_PERMISSION_PREFS = "voice-permission"
        const val VOICE_PERMISSION_REQUESTED = "record-audio-requested-v1"
        const val NOTIFICATION_PERMISSION_PREFS = "notification-permission"
        const val NOTIFICATION_PERMISSION_REQUESTED = "post-notifications-requested-v1"
    }
}

private fun recordCapabilities(
    timingTrace: WakeTimingTrace,
    health: AlarmHealth,
) {
    val occurrence = health.nextOccurrence ?: health.activeOccurrence ?: return
    timingTrace.capabilities(
        occurrenceId = occurrence.id,
        exactAlarmAllowed = health.exactAlarmAllowed,
        notificationsAllowed = health.notificationsAllowed,
        fullScreenIntentAllowed = health.fullScreenIntentAllowed,
    )
}
