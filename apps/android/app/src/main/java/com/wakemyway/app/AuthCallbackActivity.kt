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

        val handoff = intent?.data?.getQueryParameter("handoff")
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
}
