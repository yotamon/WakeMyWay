package com.wakemyway.app.voice

import android.content.Context

/**
 * Small release-safe bridge for optional founder conversation UX.
 *
 * Release builds never contain the Realtime implementation or network permission. This store only
 * lets shared UI know whether an optional debug/founder installation has completed its pairing.
 */
object ConversationalAlfredState {
    private const val PREFS = "conversational-alfred-state-v1"
    private const val KEY_READY = "ready"

    const val DEBUG_SETUP_ACTIVITY = "com.wakemyway.app.voice.VoiceSpikeActivity"

    fun isReady(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_READY, false)

    fun setReady(context: Context, ready: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_READY, ready)
            .apply()
    }
}
