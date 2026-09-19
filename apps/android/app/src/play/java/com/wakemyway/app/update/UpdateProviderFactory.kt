package com.wakemyway.app.update

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

object UpdateProviderFactory {
    fun create(
        activity: ComponentActivity,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onEvent: (UpdateProviderEvent) -> Unit,
    ): UpdateProvider = PlayUpdateProvider(
        activity = activity,
        updateLauncher = updateLauncher,
        onEvent = onEvent,
    )
}
