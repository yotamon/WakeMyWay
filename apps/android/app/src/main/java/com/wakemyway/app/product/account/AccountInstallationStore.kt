package com.wakemyway.app.product.account

import android.content.Context
import java.util.UUID

/**
 * Stable random installation identifier for optional account-scoped capabilities.
 *
 * This is not an authentication credential and never becomes alarm authority. The legacy Direct
 * Realtime preference is read once so existing installations preserve their server identity during
 * the migration to shared account infrastructure.
 */
internal class AccountInstallationStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun id(): String {
        prefs.getString(KEY_INSTALLATION_ID, null)
            ?.takeIf(::isUuid)
            ?.let { return it }

        val legacy = appContext
            .getSharedPreferences(LEGACY_REALTIME_PREFS, Context.MODE_PRIVATE)
            .getString(LEGACY_INSTALLATION_ID, null)
            ?.takeIf(::isUuid)

        val resolved = legacy ?: UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALLATION_ID, resolved).apply()
        return resolved
    }

    private fun isUuid(value: String): Boolean =
        runCatching { UUID.fromString(value) }.isSuccess

    private companion object {
        const val PREFS = "wake-account-installation-v1"
        const val KEY_INSTALLATION_ID = "installation-id"
        const val LEGACY_REALTIME_PREFS = "account-realtime-device-v1"
        const val LEGACY_INSTALLATION_ID = "installation-id"
    }
}
