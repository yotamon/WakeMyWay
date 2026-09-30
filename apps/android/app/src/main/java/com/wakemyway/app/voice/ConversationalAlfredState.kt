package com.wakemyway.app.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Release-safe bridge for optional conversational Alfred setup.
 *
 * The actual realtime implementation remains distribution-scoped. Shared consumer UI can discover
 * whether the current APK exposes a setup activity without linking WebRTC/provider code into Play.
 */
object ConversationalAlfredState {
    private const val PREFS = "conversational-alfred-state-v1"
    private const val KEY_READY = "ready"

    const val DEBUG_SETUP_ACTIVITY = "com.wakemyway.app.voice.VoiceSpikeActivity"

    private val setupActivities = listOf(DEBUG_SETUP_ACTIVITY)

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

    /**
     * Pre-warms an invisible distribution-scoped Realtime credential after account/session startup.
     * Direct uses account authentication; Play has no implementation and silently no-ops.
     */
    fun prewarmIfSupported(context: Context) {
        runCatching {
            val type = Class.forName("com.wakemyway.app.voice.DirectRealtimeProvisioner")
            type.getMethod("provision", Context::class.java)
                .invoke(null, context.applicationContext)
        }
    }

    /** Clears any distribution-scoped Realtime credential when the account signs out. */
    fun clearProvisioningIfSupported(context: Context) {
        runCatching {
            val type = Class.forName("com.wakemyway.app.voice.DirectRealtimeProvisioner")
            type.getMethod("clear", Context::class.java)
                .invoke(null, context.applicationContext)
        }
    }

    fun setupAvailable(context: Context): Boolean = resolveSetupActivity(context) != null

    fun openSetup(context: Context): Boolean {
        val activityName = resolveSetupActivity(context) ?: return false
        return runCatching {
            context.startActivity(
                Intent()
                    .setComponent(ComponentName(context.packageName, activityName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

    private fun resolveSetupActivity(context: Context): String? =
        setupActivities.firstOrNull { activityName ->
            runCatching {
                context.packageManager.getActivityInfo(
                    ComponentName(context.packageName, activityName),
                    0,
                )
                true
            }.getOrDefault(false)
        }
}
