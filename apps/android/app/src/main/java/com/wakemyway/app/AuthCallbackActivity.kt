package com.wakemyway.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.wakemyway.app.product.account.WakeAccountManager
import kotlinx.coroutines.launch

/**
 * Receives only the short-lived, PKCE-bound mobile OAuth handoff.
 *
 * No Google token or Neon session token is placed in the deep link. The handoff can only be
 * exchanged by this app while it still holds the encrypted PKCE verifier generated before opening
 * the browser.
 */
class AuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val handoff = intent?.data?.let(::handoffFromUri)
        lifecycleScope.launch {
            WakeAccountManager.get(applicationContext).completeGoogleSignIn(handoff)
            startActivity(
                Intent(this@AuthCallbackActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
            )
            finish()
        }
    }

    private fun handoffFromUri(uri: android.net.Uri): String? {
        uri.getQueryParameter("handoff")?.takeIf { it.isNotBlank() }?.let { return it }
        val fragment = uri.fragment?.takeIf { it.isNotBlank() } ?: return null
        return android.net.Uri.parse("https://localhost/?$fragment")
            .getQueryParameter("handoff")
            ?.takeIf { it.isNotBlank() }
    }
}
