package com.wakemyway.app.product.account

import android.content.Context
import android.content.Intent
import com.wakemyway.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
 * Supabase Auth owns identity/session acquisition only. WakeMyWay domain state remains local-first,
 * and server-owned authorization is resolved through the Wake API rather than trusted client
 * metadata. A missing or unavailable account backend must never affect alarm scheduling or wake.
 */
class WakeAccountManager private constructor(
    context: Context,
) {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trim()
    private val supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
    private val accountApiBaseUrl = BuildConfig.ACCOUNT_API_BASE_URL.trim().trimEnd('/')

    private val client: SupabaseClient? =
        if (supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()) {
            createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey,
            ) {
                install(Auth) {
                    flowType = FlowType.PKCE
                    scheme = AUTH_SCHEME
                    host = AUTH_HOST
                    defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
                }
            }
        } else {
            null
        }

    private val _state = MutableStateFlow(
        WakeAccountState(configured = client != null),
    )
    val state: StateFlow<WakeAccountState> = _state.asStateFlow()

    init {
        // Supabase Auth uses Android application context internally for browser/deep-link flows.
        context.applicationContext
    }

    suspend fun refresh() {
        val auth = client?.auth ?: run {
            _state.value = WakeAccountState(configured = false)
            return
        }

        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val status = auth.sessionStatus.first { it !is SessionStatus.Initializing }
            when (status) {
                is SessionStatus.Authenticated -> loadAccount(status.session)
                is SessionStatus.NotAuthenticated -> WakeAccountState(configured = true)
                is SessionStatus.RefreshFailure -> WakeAccountState(
                    configured = true,
                    error = "Your session expired. Please sign in again.",
                )
                SessionStatus.Initializing -> error("Auth remained in initialization.")
            }
        }.onSuccess {
            _state.value = it
        }.onFailure { error ->
            _state.value = WakeAccountState(
                configured = true,
                error = friendlyMessage(error),
            )
        }
    }

    suspend fun signIn(email: String, password: String) {
        val auth = requireAuth() ?: return
        if (!validateCredentials(email, password)) return

        runAction {
            auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            loadCurrentAccount(auth)
        }
    }

    suspend fun signUp(email: String, password: String) {
        val auth = requireAuth() ?: return
        if (!validateCredentials(email, password)) return

        runAction {
            auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val session = auth.currentSessionOrNull()
            if (session != null) {
                loadAccount(session)
            } else {
                WakeAccountState(
                    configured = true,
                    notice = "Check ${email.trim()} to confirm your account, then return to WakeMyWay.",
                )
            }
        }
    }

    suspend fun signInWithGoogle() {
        val auth = requireAuth() ?: return
        _state.value = _state.value.copy(loading = true, error = null, notice = null)
        runCatching {
            auth.signInWith(Google)
        }.onSuccess {
            _state.value = WakeAccountState(
                configured = true,
                notice = "Finish signing in with Google to continue.",
            )
        }.onFailure { error ->
            _state.value = WakeAccountState(
                configured = true,
                error = friendlyMessage(error),
            )
        }
    }

    suspend fun signOut() {
        val auth = requireAuth() ?: return
        runAction {
            auth.signOut()
            WakeAccountState(configured = true)
        }
    }

    fun handleAuthCallback(
        intent: Intent,
        onFinished: (Boolean) -> Unit,
    ) {
        val supabase = client ?: run {
            onFinished(false)
            return
        }

        if (intent.data?.getQueryParameter("code").isNullOrBlank()) {
            _state.value = WakeAccountState(
                configured = true,
                error = "The sign-in callback was incomplete. Please try again.",
            )
            onFinished(false)
            return
        }

        supabase.handleDeeplinks(
            intent = intent,
            onSessionSuccess = {
                _state.value = WakeAccountState(
                    configured = true,
                    account = WakeAccount(
                        id = it.user?.id.orEmpty(),
                        email = it.user?.email,
                        role = WakeAccountRole.USER,
                        roleVerified = false,
                    ),
                    notice = "Signed in. Refreshing account permissions…",
                )
                onFinished(true)
            },
            onError = { error ->
                _state.value = WakeAccountState(
                    configured = true,
                    error = friendlyMessage(error),
                )
                onFinished(false)
            },
        )
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

    private suspend fun loadCurrentAccount(auth: Auth): WakeAccountState {
        val session = auth.currentSessionOrNull()
            ?: return WakeAccountState(
                configured = true,
                error = "Sign-in completed without a usable session. Please try again.",
            )
        return loadAccount(session)
    }

    private suspend fun loadAccount(session: UserSession): WakeAccountState {
        val localUser = session.user
        if (accountApiBaseUrl.isBlank()) {
            return WakeAccountState(
                configured = true,
                account = WakeAccount(
                    id = localUser?.id.orEmpty(),
                    email = localUser?.email,
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
                        id = localUser?.id.orEmpty(),
                        email = localUser?.email,
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
                email = account.optString("email").takeIf { it.isNotBlank() },
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

    private fun requireAuth(): Auth? {
        val auth = client?.auth
        if (auth == null) {
            _state.value = WakeAccountState(
                configured = false,
                error = "Account sign-in is not configured in this build yet.",
            )
        }
        return auth
    }

    private fun friendlyMessage(error: Throwable): String {
        val raw = error.message.orEmpty()
        val lower = raw.lowercase()
        return when {
            "invalid login" in lower || "invalid credentials" in lower ->
                "That email and password combination was not accepted."
            "email not confirmed" in lower ->
                "Confirm your email first, then sign in."
            "already registered" in lower ->
                "An account already exists for that email. Try signing in instead."
            "password" in lower && ("weak" in lower || "short" in lower) ->
                "That password does not meet the account security requirements."
            "network" in lower || "timeout" in lower || "unable to resolve" in lower ->
                "WakeMyWay could not reach the account service. Your alarms still work normally."
            raw.isNotBlank() -> raw.take(MAX_ERROR_LENGTH)
            else -> "The account request could not be completed."
        }
    }

    companion object {
        const val AUTH_SCHEME = "wakemyway"
        const val AUTH_HOST = "auth"
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
