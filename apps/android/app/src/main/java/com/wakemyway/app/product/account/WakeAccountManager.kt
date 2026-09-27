package com.wakemyway.app.product.account

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.wakemyway.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class WakeAccountRole {
    USER,
    ADMIN,
}

data class WakeAccount(
    val id: String,
    val email: String?,
    val role: WakeAccountRole,
    val roleVerified: Boolean,
)

data class WakeAccountState(
    val configured: Boolean,
    val loading: Boolean = false,
    val account: WakeAccount? = null,
    val notice: String? = null,
    val error: String? = null,
)

/**
 * Optional account boundary.
 *
 * Neon Managed Better Auth owns identity/session acquisition only. WakeMyWay domain state remains
 * local-first, and server-owned authorization is resolved through the Wake API rather than trusted
 * provider metadata. A missing or unavailable account backend must never affect alarm scheduling,
 * active wake execution, Stop or Snooze.
 */
class WakeAccountManager private constructor(
    context: Context,
) {
    private val neonAuthUrl = BuildConfig.NEON_AUTH_URL.trim().trimEnd('/')
    private val accountApiBaseUrl = BuildConfig.ACCOUNT_API_BASE_URL.trim().trimEnd('/')
    private val accountRequestOrigin = accountApiBaseUrl.toHttpOriginOrNull()
    private val authClient = neonAuthUrl.takeIf { it.isNotBlank() }?.let {
        NeonAuthClient(
            context = context.applicationContext,
            baseUrl = it,
            requestOrigin = accountRequestOrigin,
        )
    }

    val googleSignInConfigured: Boolean
        get() = authClient != null &&
            accountApiBaseUrl.isNotBlank() &&
            accountRequestOrigin != null &&
            BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    val emailPasswordSignInConfigured: Boolean
        get() = authClient != null && BuildConfig.EMAIL_PASSWORD_AUTH_ENABLED

    private val _state = MutableStateFlow(
        WakeAccountState(configured = authClient != null),
    )
    val state: StateFlow<WakeAccountState> = _state.asStateFlow()

    suspend fun refresh() {
        val auth = requireAuth() ?: return
        _state.value = _state.value.copy(loading = true, error = null)

        runCatching { auth.currentSession() }
            .onSuccess { session ->
                _state.value = if (session == null) {
                    WakeAccountState(configured = true)
                } else {
                    loadAccount(session)
                }
            }
            .onFailure { error ->
                _state.value = WakeAccountState(
                    configured = true,
                    error = friendlyMessage(error),
                )
            }
    }

    suspend fun signIn(email: String, password: String) {
        val auth = requireAuth() ?: return
        if (!emailPasswordSignInConfigured) {
            _state.value = _state.value.copy(
                error = "Email and password sign-in is not enabled in this build.",
            )
            return
        }
        if (!validateCredentials(email, password)) return

        runAction {
            auth.signIn(email.trim(), password)
            loadCurrentAccount(auth)
        }
    }

    suspend fun signUp(email: String, password: String) {
        val auth = requireAuth() ?: return
        if (!emailPasswordSignInConfigured) {
            _state.value = _state.value.copy(
                error = "Email account creation is not enabled in this build.",
            )
            return
        }
        if (!validateCredentials(email, password)) return

        runAction {
            auth.signUp(
                email = email.trim(),
                password = password,
                name = email.substringBefore('@').ifBlank { "WakeMyWay user" },
            )
            auth.currentSession()?.let { loadAccount(it) }
                ?: WakeAccountState(
                    configured = true,
                    notice = "Account created. Verify your email if requested, then sign in.",
                )
        }
    }

    suspend fun signInWithGoogle(activityContext: Context) {
        val auth = requireAuth() ?: return
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (!googleSignInConfigured || webClientId.isBlank()) {
            _state.value = _state.value.copy(
                error = "Google sign-in is not configured in this build yet.",
            )
            return
        }

        runAction {
            val nonce = secureGoogleNonce()
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .setNonce(nonce)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val result = CredentialManager.create(activityContext).getCredential(
                context = activityContext,
                request = request,
            )
            val credential = result.credential
            if (
                credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                error("Google sign-in returned an unsupported credential.")
            }

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            auth.signInWithGoogleIdToken(
                idToken = googleCredential.idToken,
                nonce = nonce,
            )
            loadCurrentAccount(auth)
        }
    }

    suspend fun completeGoogleSignIn(handoff: String?) {
        _state.value = _state.value.copy(
            loading = false,
            error = if (handoff.isNullOrBlank()) {
                "Google sign-in was not completed. Please try again from WakeMyWay."
            } else {
                "This WakeMyWay build now uses native Google sign-in. Please start sign-in again."
            },
        )
    }

    suspend fun signOut() {
        val auth = requireAuth() ?: return
        runAction {
            auth.signOut()
            WakeAccountState(configured = true)
        }
    }

    private suspend fun runAction(action: suspend () -> WakeAccountState) {
        _state.value = _state.value.copy(loading = true, error = null, notice = null)
        runCatching { action() }
            .onSuccess { _state.value = it }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = friendlyMessage(error),
                )
            }
    }

    private suspend fun loadCurrentAccount(auth: NeonAuthClient): WakeAccountState {
        val session = auth.currentSession()
            ?: return WakeAccountState(
                configured = true,
                error = "Sign-in completed without a usable session. Please try again.",
            )
        return loadAccount(session)
    }

    private suspend fun loadAccount(session: NeonAuthSession): WakeAccountState {
        if (accountApiBaseUrl.isBlank()) {
            return WakeAccountState(
                configured = true,
                account = WakeAccount(
                    id = session.userId,
                    email = session.email,
                    role = WakeAccountRole.USER,
                    roleVerified = false,
                ),
            )
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                fetchAccountProfile(session.accessToken)
            }
        }.fold(
            onSuccess = { profile ->
                WakeAccountState(configured = true, account = profile)
            },
            onFailure = { error ->
                WakeAccountState(
                    configured = true,
                    account = WakeAccount(
                        id = session.userId,
                        email = session.email,
                        role = WakeAccountRole.USER,
                        roleVerified = false,
                    ),
                    error = "Signed in, but account permissions could not be refreshed: ${friendlyMessage(error)}",
                )
            },
        )
    }

    private fun fetchAccountProfile(accessToken: String): WakeAccount {
        val connection = (URL("$accountApiBaseUrl/api/v1/account/me").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) error("Wake API returned HTTP $status")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (body.length > MAX_RESPONSE_CHARS) error("Wake API response was too large")
            val account = JSONObject(body).getJSONObject("account")
            return WakeAccount(
                id = account.getString("id"),
                email = account.optString("email").takeIf { it.isNotBlank() && it != "null" },
                role = when (account.getString("role")) {
                    "admin" -> WakeAccountRole.ADMIN
                    else -> WakeAccountRole.USER
                },
                roleVerified = true,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun secureGoogleNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        val normalizedEmail = email.trim()
        if (!normalizedEmail.contains('@')) {
            _state.value = _state.value.copy(error = "Enter a valid email address.")
            return false
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            _state.value = _state.value.copy(
                error = "Use at least $MIN_PASSWORD_LENGTH characters for your password.",
            )
            return false
        }
        return true
    }

    private fun requireAuth(): NeonAuthClient? {
        val auth = authClient
        if (auth == null) {
            _state.value = WakeAccountState(
                configured = false,
                error = "Account sign-in is not configured in this build yet.",
            )
        }
        return auth
    }

    private fun String.toHttpOriginOrNull(): String? {
        if (isBlank()) return null
        return runCatching {
            val url = URL(this)
            if (url.protocol != "https") return@runCatching null
            val port = if (url.port == -1 || url.port == url.defaultPort) "" else ":${url.port}"
            "${url.protocol}://${url.host}$port"
        }.getOrNull()
    }

    private fun friendlyMessage(error: Throwable): String {
        val raw = error.message.orEmpty()
        val lower = raw.lowercase()
        return when {
            "invalid email or password" in lower ||
                "invalid credentials" in lower ||
                "invalid password" in lower ->
                "That email and password combination was not accepted."
            "email not verified" in lower || "email not confirmed" in lower ->
                "Verify your email first, then sign in."
            "already exists" in lower || "already registered" in lower ->
                "An account already exists for that email. Try signing in instead."
            "password" in lower && ("weak" in lower || "short" in lower) ->
                "That password does not meet the account security requirements."
            "cancel" in lower ->
                "Google sign-in was canceled."
            "no credential" in lower ->
                "No Google account was available for sign-in on this device."
            "network" in lower ||
                "timeout" in lower ||
                "unable to resolve" in lower ||
                "failed to connect" in lower ->
                "WakeMyWay could not reach the account service. Your alarms still work normally."
            raw.isNotBlank() -> raw.take(MAX_ERROR_LENGTH)
            else -> "The account request could not be completed."
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val NETWORK_TIMEOUT_MS = 10_000
        private const val MAX_RESPONSE_CHARS = 32_000
        private const val MAX_ERROR_LENGTH = 180

        @Volatile
        private var instance: WakeAccountManager? = null

        fun get(context: Context): WakeAccountManager =
            instance ?: synchronized(this) {
                instance ?: WakeAccountManager(context.applicationContext).also { instance = it }
            }
    }
}
