package com.wakemyway.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.wakemyway.app.product.account.WakeAccountManager
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.theme.WakeMyWayTheme
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import kotlinx.coroutines.launch

class AuthCallbackActivity : ComponentActivity() {
    private val accountManager by lazy { WakeAccountManager.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WakeMyWayTheme {
                FinishingGoogleSignIn()
            }
        }

        val uri = intent?.data
        val code = uri
            ?.takeIf { it.scheme == CALLBACK_SCHEME && it.host == CALLBACK_HOST }
            ?.getQueryParameter("code")
            ?.takeIf { CODE_PATTERN.matches(it) }
        val hasError = uri?.getQueryParameter("error") != null

        lifecycleScope.launch {
            if (code != null && !hasError) {
                accountManager.completeGoogleSignIn(code)
            } else {
                accountManager.failGoogleSignIn()
            }
            returnToWakeMyWay()
        }
    }

    private fun returnToWakeMyWay() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        private const val CALLBACK_SCHEME = "wakemyway"
        private const val CALLBACK_HOST = "auth"
        private val CODE_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")
    }
}

@Composable
private fun FinishingGoogleSignIn() {
    WmwCircadianSurface(WmwCircadianStage.PLANNING) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(WmwSpacing.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Finishing Google sign-in…",
                    style = MaterialTheme.typography.titleMedium,
                    color = WmwColors.Midnight,
                )
                Text(
                    text = "Your alarms continue to work locally while your account is connected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WmwColors.LightQuietText,
                )
            }
        }
    }
}
