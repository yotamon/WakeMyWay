package com.wakemyway.app.voice

import android.content.Context

/**
 * Direct-distribution Realtime readiness refresher.
 *
 * Founder Realtime uses explicit pairing. App start may refresh the local marker, but it never
 * mints a cloud/OpenAI-backed credential without the founder secret.
 */
object DirectRealtimeProvisioner {
    @JvmStatic
    fun provision(context: Context) {
        val appContext = context.applicationContext
        ConversationalAlfredState.setReady(
            appContext,
            FounderRealtimeSettings(appContext).configured(),
        )
    }
}
