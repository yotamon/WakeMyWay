package com.wakemyway.app.voice

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class AccountRealtimeProvisioningClient {
    fun provision(accessToken: String, installationId: String): RealtimeDeviceCredential {
        val connection = (URL(PROVISION_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            val body = JSONObject().put("installationId", installationId).toString()
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) error("Realtime provisioning failed")
            val json = JSONObject(readBounded(connection.inputStream, MAX_RESPONSE_BYTES))
            return RealtimeDeviceCredential(
                deviceToken = json.getString("deviceToken").trim(),
                expiresAtEpochSeconds = json.getLong("expiresAt"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readBounded(stream: InputStream, maxBytes: Int): String = stream.use { input ->
        val output = ByteArrayOutputStream(minOf(maxBytes, 8 * 1024))
        val buffer = ByteArray(4 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            total += count
            check(total <= maxBytes)
            output.write(buffer, 0, count)
        }
        String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    companion object {
        const val API_BASE_URL = "https://wakemyway.vercel.app"
        const val PROVISION_PATH = "/api/v1/account/realtime-provision"
        const val TOKEN_PATH = "/api/v1/account/realtime-token"
        const val TOKEN_URL = "$API_BASE_URL$TOKEN_PATH"
        private const val PROVISION_URL = "$API_BASE_URL$PROVISION_PATH"
        private const val NETWORK_TIMEOUT_MS = 12_000
        private const val MAX_RESPONSE_BYTES = 32 * 1024
    }
}
