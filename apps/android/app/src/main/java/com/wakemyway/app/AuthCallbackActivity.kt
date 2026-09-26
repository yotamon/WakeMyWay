package com.wakemyway.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.wakemyway.app.product.account.WakeAccountManager
import kotlinx.coroutines.launch

/**
 * Isolated OAuth/email-confirmation callback surface.
 *
 * This activity has no wake responsibility. It only lets Supabase exchange the PKCE callback,
 * refreshes the optional account boundary, and returns the user to the normal app.
 */
class AuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WakeAccountManager.get(this).handleAuthCallback(intent) { success ->
            lifecycleScope.launch {
                if (success) {
                    WakeAccountManager.get(this@AuthCallbackActivity).refresh()
                }
                startActivity(
                    Intent(this@AuthCallbackActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
                finish()
            }
        }
    }
}
