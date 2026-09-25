package com.wakemyway.app.voice

import android.content.Context

/**
 * Direct-distribution pre-warmer for zero-setup Realtime access.
 *
 * Runs off the app-start critical path, provisions only the scoped installation credential, and
 * never opens a model session. Wake delivery remains fully local if provisioning fails.
 */
object DirectRealtimeProvisioner {
    @JvmStatic
    fun provision(context: Context) {
        val appContext = context.applicationContext
        val settings = FounderRealtimeSettings(appContext)
        if (settings.load() != null) return

        Thread(
            {
                runCatching {
                    val paired = FounderRealtimePairingClient().bootstrap(settings.installationId())
                    settings.saveInstallationCredential(
                        paired.deviceToken,
                        paired.expiresAtEpochSeconds,
                    )
                }
            },
            "wmw-realtime-prewarm",
        ).apply {
            isDaemon = true
            start()
        }
    }
}
