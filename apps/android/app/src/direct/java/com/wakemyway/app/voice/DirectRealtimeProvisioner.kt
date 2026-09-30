package com.wakemyway.app.voice

import android.content.Context
import com.wakemyway.app.product.account.WakeAccountManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking

/** Invisible account-backed Realtime provisioning for Direct builds. */
object DirectRealtimeProvisioner {
    private val provisioning = AtomicBoolean(false)

    @JvmStatic
    fun provision(context: Context) {
        val appContext = context.applicationContext
        val store = RealtimeAccountCredentialStore(appContext)
        if (store.load() != null) {
            ConversationalAlfredState.setReady(appContext, true)
            return
        }
        if (!provisioning.compareAndSet(false, true)) return

        Thread({
            try {
                val accessToken = runBlocking {
                    WakeAccountManager.get(appContext).currentAccessTokenForRealtime()
                } ?: error("WakeMyWay account session is unavailable")
                val credential = AccountRealtimeProvisioningClient().provision(
                    accessToken = accessToken,
                    installationId = store.installationId(),
                )
                store.save(credential)
                ConversationalAlfredState.setReady(appContext, true)
            } catch (_: Throwable) {
                ConversationalAlfredState.setReady(appContext, false)
            } finally {
                provisioning.set(false)
            }
        }, "wmw-account-realtime-provision").apply {
            isDaemon = true
            start()
        }
    }

    @JvmStatic
    fun clear(context: Context) {
        RealtimeAccountCredentialStore(context.applicationContext).clear()
        ConversationalAlfredState.setReady(context.applicationContext, false)
    }
}
