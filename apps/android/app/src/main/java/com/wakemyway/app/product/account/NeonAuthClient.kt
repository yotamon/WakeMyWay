package com.wakemyway.app.product.account

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class NeonAuthSession(
    val userId: String,
    val email: String?,
    val accessToken: String,
)

internal class NeonAuthClient(
    context: Context,
    private val baseUrl: String,
) {
    private val sessionStore = NeonAuthSessionStore(context.applicationContext)

    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        authenticate(
            path = "/sign-in/email",
            payload = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("rememberMe", true),
        )
    }

    suspend fun signUp(email: String, password: String, name: String) = withContext(Dispatchers.IO) {
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

    suspend fun signInWithGoogle(idToken: String, nonce: String) = withContext(Dispatchers.IO) {
        authenticate(
            path = "/sign-in/social",
            payload = JSONObject()
                .put("provider", "google")
                .put("disableRedirect", true)
                .put(
                    "idToken",
                    JSONObject()
                        .put("token", idToken)
                        .put("nonce", nonce),
                ),
        )
    }

    suspend fun currentSession(): NeonAuthSession? = withContext(Dispatchers.IO) {
        val sessionToken = sessionStore.load() ?: return@withContext null
        val sessionResponse = request(
            path = "/get-session",
            method = "GET",
            bearer = sessionToken,
            allowUnauthorized = true,
        )
        if (sessionResponse.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            sessionStore.clear()
            return@withContext null
        }

        val body = sessionResponse.body.trim()
        if (body.isBlank() || body == "null") {
            sessionStore.clear()
            return@withContext null
        }

        val user = JSONObject(body).getJSONObject("user")
        val jwtResponse = request(
            path = "/token",
            method = "GET",
            bearer = sessionToken,
            allowUnauthorized = true,
        )
        if (jwtResponse.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            sessionStore.clear()
            return@withContext null
        }

        val accessToken = JSONObject(jwtResponse.body).getString("token")
        require(accessToken.count { it == '.' } == 2) {
            "Neon Auth returned an invalid access token."
        }

        NeonAuthSession(
            userId = user.getString("id"),
            email = user.optString("email").takeIf { it.isNotBlank() && it != "null" },
            accessToken = accessToken,
        )
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val sessionToken = sessionStore.load()
        try {
            if (sessionToken != null) {
                request(
                    path = "/sign-out",
                    method = "POST",
                    bearer = sessionToken,
                    payload = JSONObject(),
                    allowUnauthorized = true,
                )
            }
        } finally {
            sessionStore.clear()
        }
    }

    private fun authenticate(
        path: String,
        payload: JSONObject,
        sessionMayBeAbsent: Boolean = false,
    ) {
        val response = request(path = path, method = "POST", payload = payload)
        val body = JSONObject(response.body)
        val sessionToken = body.optString("token").takeIf { it.isNotBlank() && it != "null" }

        if (sessionToken == null) {
            if (sessionMayBeAbsent) {
                sessionStore.clear()
                return
            }
            error("Neon Auth did not return a usable session.")
        }
        sessionStore.save(sessionToken)
    }

    private fun request(
        path: String,
        method: String,
        payload: JSONObject? = null,
        bearer: String? = null,
        allowUnauthorized: Boolean = false,
    ): HttpResponse {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
            if (bearer != null) {
                setRequestProperty("Authorization", "Bearer $bearer")
            }
            if (payload != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (payload != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (body.length > MAX_RESPONSE_CHARS) throw IOException("Account service response was too large.")

            if (
                status !in 200..299 &&
                !(allowUnauthorized && status == HttpURLConnection.HTTP_UNAUTHORIZED)
            ) {
                val message = runCatching {
                    JSONObject(body).optString("message").ifBlank {
                        JSONObject(body).optString("error")
                    }
                }.getOrNull().orEmpty()
                throw IOException(
                    message.ifBlank { "Account service returned HTTP $status." },
                )
            }
            return HttpResponse(status = status, body = body)
        } finally {
            connection.disconnect()
        }
    }

    private data class HttpResponse(
        val status: Int,
        val body: String,
    )

    companion object {
        private const val NETWORK_TIMEOUT_MS = 10_000
        private const val MAX_RESPONSE_CHARS = 64_000
    }
}
