package com.wakemyway.app

import android.Manifest
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.wakemyway.app.alarm.AlarmHealth
import com.wakemyway.app.alarm.AlarmKernel
import com.wakemyway.app.alarm.WakeTimingTrace
import com.wakemyway.app.ui.home.VoiceWakeReadiness
import com.wakemyway.app.ui.navigation.WakeMyWayApp
import com.wakemyway.app.ui.theme.WakeMyWayTheme

class MainActivity : ComponentActivity() {
    private var showVoicePermissionPrimer by mutableStateOf(false)
    private var voiceWakeReadiness by mutableStateOf(VoiceWakeReadiness.UNAVAILABLE)

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        showVoicePermissionPrimer = false
        refreshVoiceWakeReadiness()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val kernel = AlarmKernel(this)
        val health = kernel.reconcile()
        recordCapabilities(WakeTimingTrace(this), health)
        refreshVoiceWakeReadiness()

        setContent {
            WakeMyWayTheme {
                WakeMyWayApp(
                    voiceWakeReadiness = voiceWakeReadiness,
                    onEnableVoiceReplies = ::beginVoicePermissionSetup,
                )

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
        refreshVoiceWakeReadiness()
    }

    private fun beginVoicePermissionSetup() {
        if (voiceWakeReadiness != VoiceWakeReadiness.SETUP_REQUIRED) return

        val requestedBefore = getSharedPreferences(VOICE_PERMISSION_PREFS, MODE_PRIVATE)
            .getBoolean(VOICE_PERMISSION_REQUESTED, false)
        val shouldExplainAgain = shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)

        if (requestedBefore && !shouldExplainAgain) {
            openAppPermissionSettings()
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

    private fun openAppPermissionSettings() {
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
