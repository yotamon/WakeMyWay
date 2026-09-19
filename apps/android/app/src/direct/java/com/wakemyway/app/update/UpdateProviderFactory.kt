package com.wakemyway.app.update

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

object UpdateProviderFactory {
    fun create(
        activity: ComponentActivity,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
        onEvent: (UpdateProviderEvent) -> Unit,
    ): UpdateProvider {
        @Suppress("UNUSED_VARIABLE")
        val ignoredLauncher = updateLauncher
        return DirectUpdateProvider(activity, onEvent)
    }
}
