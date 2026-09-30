package com.wakemyway.app.product.account

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.wakemyway.app.network.WakeHttpClient
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal data class NeonAuthSession(
    val userId: String,
    val email: String?,
    val accessToken: String,
)

internal class NeonAuthClient(
    context: Context,
    private val baseUrl: String,
    private val requestOrigin: String?,
) {
    private val sessionStore = NeonAuthSessionStore(context.applicationContext)

    suspend fun signIn(email: String, password: String) {
        authenticate(
            path = "/sign-in/email",
            payload = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("rememberMe", true),
        )
    }

    suspend fun signUp(email: String, password: String, name: String) {
        authenticate(
            path = "/sign-up/email",
            payload = JSONObject()
                .put("name", name)
                .put("email", email)
                .put("password", password)
                .put("rememberMe", true),
            sessionMayBeAbsent = true,
        )
    }

    fun prepareGoogleBrowserSignIn(accountApiBaseUrl: String): Uri {
        require(accountApiBaseUrl.startsWith("https://")) {
            "WakeMyWay account API must use HTTPS for Google sign-in."
        }
        val verifier = secureVerifier()
        val challenge = pkceChallenge(verifier)
        sessionStore.savePendingGoogleVerifier(verifier)
        return Uri.parse(accountApiBaseUrl)
            .buildUpon()
            .appendEncodedPath("api/v1/account/mobile-google-start")
            .appendQueryParameter("challenge", challenge)
            .build()
    }

    suspend fun completeGoogleBrowserSignIn(
        accountApiBaseUrl: String,
        handoff: String,
    ) {
        val verifier = sessionStore.loadPendingGoogleVerifier()
            ?: error("Google sign-in expired. Please try again.")

        val response = requestAbsolute(
            url = "$accountApiBaseUrl/api/v1/account/mobile-google-exchange",
            method = "POST",
            payload = JSONObject()
                .put("handoff", handoff)
                .put("verifier", verifier),
        )
        val body = JSONObject(response.body)
        val sessionCookie = body.optString("sessionCookie")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: error("WakeMyWay did not receive a Neon session.")

        sessionStore.save(sessionCookie)
        sessionStore.clearPendingGoogleVerifier()
    }

    suspend fun currentSession(): NeonAuthSession? {
        val sessionCookie = sessionStore.load() ?: return null
        val sessionResponse = request(
            path = "/get-session",
            method = "GET",
            sessionCookie = sessionCookie,
            allowUnauthorized = true,
        )
        if (sessionResponse.status == HTTP_UNAUTHORIZED) {
            sessionStore.clear()
            return null
        }

        val body = sessionResponse.body.trim()
        if (body.isBlank() || body == "null") {
            sessionStore.clear()
            return null
        }

        val user = JSONObject(body).getJSONObject("user")
        val jwtResponse = request(
            path = "/token",
            method = "GET",
            sessionCookie = sessionCookie,
            allowUnauthorized = true,
        )
        if (jwtResponse.status == HTTP_UNAUTHORIZED) {
            sessionStore.clear()
            return null
        }

        val accessToken = JSONObject(jwtResponse.body).getString("token")
        require(accessToken.count { it == '.' } == 2) {
            "Neon Auth returned an invalid access token."
        }

        return NeonAuthSession(
            userId = user.getString("id"),
            email = user.optString("email").takeIf { it.isNotBlank() && it != "null" },
            accessToken = accessToken,
        )
    }

    suspend fun signOut() {
        val sessionCookie = sessionStore.load()
        try {
            if (sessionCookie != null) {
                request(
                    path = "/sign-out",
                    method = "POST",
                    sessionCookie = sessionCookie,
                    payload = JSONObject(),
                    allowUnauthorized = true,
                )
            }
        } finally {
            sessionStore.clear()
        }
    }

    private suspend fun authenticate(
        path: String,
        payload: JSONObject,
        sessionMayBeAbsent: Boolean = false,
    ) {
        val response = request(path = path, method = "POST", payload = payload)
        val sessionCookie = extractSessionCookie(response.setCookieHeaders)

        if (sessionCookie == null) {
            if (sessionMayBeAbsent) {
                sessionStore.clear()
                return
            }
            error("Neon Auth completed sign-in but did not return a session cookie.")
        }
        sessionStore.save(sessionCookie)
    }

    private suspend fun request(
        path: String,
        method: String,
        payload: JSONObject? = null,
        sessionCookie: String? = null,
        allowUnauthorized: Boolean = false,
    ): HttpResponse = requestAbsolute(
        url = "$baseUrl$path",
        method = method,
        payload = payload,
        sessionCookie = sessionCookie,
        allowUnauthorized = allowUnauthorized,
    )

    private suspend fun requestAbsolute(
        url: String,
        method: String,
        payload: JSONObject? = null,
        sessionCookie: String? = null,
        allowUnauthorized: Boolean = false,
    ): HttpResponse {
        val body = payload?.toString()?.toRequestBody(WakeHttpClient.jsonMediaType)
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
        requestOrigin?.let { builder.header("Origin", it) }
        sessionCookie?.let { builder.header("Cookie", it) }

        val request = when (method) {
            "GET" -> builder.get().build()
            "POST" -> builder.post(body ?: EMPTY_JSON_BODY).build()
            else -> builder.method(method, body).build()
        }
        val response = WakeHttpClient.execute(request, MAX_RESPONSE_BYTES)

        if (
            response.status !in 200..299 &&
            !(allowUnauthorized && response.status == HTTP_UNAUTHORIZED)
        ) {
            val message = runCatching {
                JSONObject(response.body).optString("message").ifBlank {
                    JSONObject(response.body).optString("error")
                }
            }.getOrNull().orEmpty()
            throw IOException(
                message.ifBlank { "Account service returned HTTP ${response.status}." },
            )
        }
        return HttpResponse(
            status = response.status,
            body = response.body,
            setCookieHeaders = response.headers.values("Set-Cookie"),
        )
    }

    private data class HttpResponse(
        val status: Int,
        val body: String,
        val setCookieHeaders: List<String>,
    )

    private fun secureVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    private fun pkceChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val MAX_RESPONSE_BYTES = 64L * 1024
        private val EMPTY_JSON_BODY = "{}".toRequestBody(WakeHttpClient.jsonMediaType)
        private val SESSION_COOKIE_PATTERN = Regex(
            pattern = """(?:^|[\s,])([\w.-]*session_token)=([^;,\s]+)""",
            option = RegexOption.IGNORE_CASE,
        )

        internal fun extractSessionCookie(setCookieHeaders: List<String>): String? =
            setCookieHeaders
                .asSequence()
                .flatMap { SESSION_COOKIE_PATTERN.findAll(it).asSequence() }
                .mapNotNull { match ->
                    val name = match.groupValues.getOrNull(1).orEmpty()
                    val value = match.groupValues.getOrNull(2).orEmpty()
                    if (name.isBlank() || value.isBlank()) null else "$name=$value"
                }
                .firstOrNull()
    }
}
