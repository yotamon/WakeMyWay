package com.wakemyway.app.commerce

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONObject

internal class PlayPurchaseVerifier private constructor(
    private val endpoint: String,
    override val available: Boolean,
) : PurchaseVerifier {
    override fun verify(
        request: PurchaseVerificationRequest,
        onResult: (PurchaseVerificationResult) -> Unit,
    ) {
        if (!available) {
            onResult(PurchaseVerificationResult.RetryLater)
            return
        }

        Thread(
            {
                onResult(runCatching { verifyBlocking(request) }.getOrElse {
                    PurchaseVerificationResult.RetryLater
                })
            },
            "wmw-play-verifier",
        ).start()
    }

    private fun verifyBlocking(request: PurchaseVerificationRequest): PurchaseVerificationResult {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            useCaches = false
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject()
            .put("productId", request.productId)
            .put("purchaseToken", request.purchaseToken)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)

        return try {
            connection.outputStream.use { it.write(body) }

            when (connection.responseCode) {
                in 200..299 -> parseVerified(readBounded(connection.inputStream))
                400, 401, 403, 404 -> PurchaseVerificationResult.Rejected
                408, 425, 429 -> PurchaseVerificationResult.RetryLater
                in 500..599 -> PurchaseVerificationResult.RetryLater
                else -> PurchaseVerificationResult.RetryLater
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseVerified(payload: String): PurchaseVerificationResult {
        val json = JSONObject(payload)
        val entitlement = when (json.optString("entitlement")) {
            "ACTIVE" -> EntitlementState.ACTIVE
            "CANCELLED_ENTITLED" -> EntitlementState.CANCELLED_ENTITLED
            "GRACE_PERIOD" -> EntitlementState.GRACE_PERIOD
            "ON_HOLD" -> EntitlementState.ON_HOLD
            "PAUSED" -> EntitlementState.PAUSED
            "EXPIRED" -> EntitlementState.EXPIRED
            "UNKNOWN" -> EntitlementState.UNKNOWN
            else -> return PurchaseVerificationResult.RetryLater
        }

        return PurchaseVerificationResult.Verified(entitlement)
    }

    private fun readBounded(stream: InputStream): String = stream.use { input ->
        val output = ByteArrayOutputStream(2 * 1024)
        val buffer = ByteArray(2 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            total += count
            if (total > MAX_RESPONSE_BYTES) {
                throw IllegalStateException("Play verification response exceeded the safe bound")
            }
            output.write(buffer, 0, count)
        }
        String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    companion object {
        fun create(
            enabled: Boolean,
            apiBaseUrl: String,
        ): PurchaseVerifier {
            val normalizedBase = apiBaseUrl.trim().trimEnd('/')
            val isHttps = normalizedBase.startsWith("https://")
            return if (enabled && normalizedBase.isNotBlank() && isHttps) {
                PlayPurchaseVerifier(
                    endpoint = "$normalizedBase/api/v1/commerce/play-verify",
                    available = true,
                )
            } else {
                PurchaseVerifier.UNAVAILABLE
            }
        }

        private const val NETWORK_TIMEOUT_MS = 12_000
        private const val MAX_RESPONSE_BYTES = 16 * 1024
    }
}
