package com.wakemyway.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.wakemyway.app.ui.navigation.WakeMyWayApp
import com.wakemyway.app.ui.theme.WakeMyWayTheme

class MainActivity : ComponentActivity() {
    private var showVoicePermissionPrimer by mutableStateOf(false)

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        showVoicePermissionPrimer = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val kernel = AlarmKernel(this)
        val health = kernel.reconcile()
        recordCapabilities(WakeTimingTrace(this), health)
        showVoicePermissionPrimer = shouldOfferVoicePermissionPrimer()

        setContent {
            WakeMyWayTheme {
                WakeMyWayApp()

                if (showVoicePermissionPrimer) {
                    AlertDialog(
                        onDismissRequest = { dismissVoicePermissionPrimer() },
                        title = { Text(stringResource(R.string.voice_permission_title)) },
                        text = { Text(stringResource(R.string.voice_permission_body)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    rememberVoicePermissionDecision()
                                    showVoicePermissionPrimer = false
                                    voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                            ) {
                                Text(stringResource(R.string.voice_permission_enable))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { dismissVoicePermissionPrimer() }) {
                                Text(stringResource(R.string.voice_permission_not_now))
                            }
                        },
                    )
                }
            }
        }
    }

    private fun shouldOfferVoicePermissionPrimer(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (
            getSharedPreferences(VOICE_PERMISSION_PREFS, MODE_PRIVATE)
                .getBoolean(VOICE_PERMISSION_DECIDED, false)
        ) {
            return false
        }
        return runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(this) }
            .getOrDefault(false)
    }

    private fun dismissVoicePermissionPrimer() {
        rememberVoicePermissionDecision()
        showVoicePermissionPrimer = false
    }

    private fun rememberVoicePermissionDecision() {
        getSharedPreferences(VOICE_PERMISSION_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(VOICE_PERMISSION_DECIDED, true)
            .apply()
    }

    private companion object {
        const val VOICE_PERMISSION_PREFS = "voice-permission"
        const val VOICE_PERMISSION_DECIDED = "primer-decided-v1"
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
