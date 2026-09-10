package com.wakemyway.app.voice

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

/** Small no-secret client used only by the founder setup surface. */
class FounderRealtimePairingClient {
    data class ServerStatus(
        val available: Boolean,
        val missing: List<String>,
    )

    data class PairingResult(
        val deviceToken: String,
        val expiresAtEpochSeconds: Long,
    )

    class PairingException(
        val kind: Kind,
        message: String,
    ) : Exception(message) {
        enum class Kind {
            ACCESS_CODE_REJECTED,
            SERVER_NOT_READY,
            NETWORK,
            MALFORMED_RESPONSE,
        }
    }

    fun status(): ServerStatus {
        val connection = open(FounderRealtimeSettings.STATUS_URL, "GET")
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw PairingException(PairingException.Kind.NETWORK, "WakeMyWay cloud returned HTTP $status")
            }
            val json = JSONObject(readBounded(connection.inputStream))
            val missingJson = json.optJSONArray("missing") ?: JSONArray()
            val missing = buildList {
                repeat(missingJson.length()) { index ->
                    missingJson.optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
                }
            }
            return ServerStatus(
                available = json.optBoolean("available", false),
                missing = missing,
            )
        } catch (error: PairingException) {
            throw error
        } catch (_: Throwable) {
            throw PairingException(PairingException.Kind.MALFORMED_RESPONSE, "Could not read WakeMyWay cloud status")
        } finally {
            connection.disconnect()
        }
    }

    fun pair(accessCode: String, installationId: String): PairingResult {
        val normalizedCode = accessCode.trim()
        if (normalizedCode.length < MIN_ACCESS_CODE_LENGTH) {
            throw PairingException(PairingException.Kind.ACCESS_CODE_REJECTED, "Founder access code is too short")
        }

        val connection = open(FounderRealtimeSettings.PAIR_URL, "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        val body = JSONObject()
            .put("code", normalizedCode)
            .put("installationId", installationId)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        try {
            connection.outputStream.use { it.write(body) }
            return when (val status = connection.responseCode) {
                in 200..299 -> {
                    val json = JSONObject(readBounded(connection.inputStream))
                    val token = json.optString("deviceToken").trim()
                    val expiresAt = json.optLong("expiresAt", 0L)
                    if (token.length < 48 || expiresAt <= System.currentTimeMillis() / 1_000L) {
                        throw PairingException(
                            PairingException.Kind.MALFORMED_RESPONSE,
                            "WakeMyWay cloud returned an invalid installation credential",
                        )
                    }
                    PairingResult(token, expiresAt)
                }
                401, 403 -> throw PairingException(
                    PairingException.Kind.ACCESS_CODE_REJECTED,
                    "That founder access code was not accepted",
                )
                503 -> throw PairingException(
                    PairingException.Kind.SERVER_NOT_READY,
                    "Conversational Alfred is not configured on the WakeMyWay server yet",
                )
                else -> throw PairingException(
                    PairingException.Kind.NETWORK,
                    "WakeMyWay cloud returned HTTP $status",
                )
            }
        } catch (error: PairingException) {
            throw error
        } catch (_: Throwable) {
            throw PairingException(PairingException.Kind.NETWORK, "Could not reach WakeMyWay cloud")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Verifies the full server → OpenAI client-secret path before the UI claims Ready.
     * The short-lived Realtime secret is read into memory only long enough to validate its shape,
     * then discarded without opening a model session.
     */
    fun probe(deviceToken: String) {
        val connection = open(FounderRealtimeSettings.BROKER_URL, "POST").apply {
            setRequestProperty("Authorization", "Bearer $deviceToken")
            setRequestProperty("Content-Length", "0")
        }
        try {
            when (val status = connection.responseCode) {
                in 200..299 -> {
                    val json = JSONObject(readBounded(connection.inputStream))
                    val token = json.optString("token").trim()
                    val callsUrl = json.optString("realtimeCallsUrl").trim()
                    val mode = json.optString("connectionMode").trim()
                    if (
                        token.isBlank() ||
                        callsUrl != "https://api.openai.com/v1/realtime/calls" ||
                        mode != "webrtc-ephemeral"
                    ) {
                        throw PairingException(
                            PairingException.Kind.MALFORMED_RESPONSE,
                            "WakeMyWay cloud could not verify the Realtime credential",
                        )
                    }
                }
                401, 403 -> throw PairingException(
                    PairingException.Kind.ACCESS_CODE_REJECTED,
                    "The installation credential was not accepted",
                )
                503 -> throw PairingException(
                    PairingException.Kind.SERVER_NOT_READY,
                    "OpenAI Realtime is not configured on the WakeMyWay server yet",
                )
                else -> throw PairingException(
                    PairingException.Kind.NETWORK,
                    "Realtime readiness check returned HTTP $status",
                )
            }
        } catch (error: PairingException) {
            throw error
        } catch (_: Throwable) {
            throw PairingException(PairingException.Kind.NETWORK, "Could not verify OpenAI Realtime")
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
        }

    private fun readBounded(stream: InputStream): String = stream.use { input ->
        val output = ByteArrayOutputStream(8 * 1024)
        val buffer = ByteArray(4 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            total += count
            if (total > MAX_RESPONSE_BYTES) {
                throw PairingException(PairingException.Kind.MALFORMED_RESPONSE, "WakeMyWay cloud response was too large")
            }
            output.write(buffer, 0, count)
        }
        String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    private companion object {
        const val NETWORK_TIMEOUT_MS = 12_000
        const val MAX_RESPONSE_BYTES = 24 * 1024
        const val MIN_ACCESS_CODE_LENGTH = 12
    }
}
