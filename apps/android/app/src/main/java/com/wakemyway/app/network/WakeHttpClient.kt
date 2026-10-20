package com.wakemyway.app.network

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync

data class WakeHttpResponse(
    val status: Int,
    val body: String,
    val headers: Headers,
)

object WakeHttpClient {
    val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaType()
    val sdpMediaType: MediaType = "application/sdp".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun execute(
        request: Request,
        maxResponseBytes: Long,
        callTimeoutMillis: Long = 15_000L,
    ): WakeHttpResponse {
        require(maxResponseBytes > 0 && maxResponseBytes < Long.MAX_VALUE) {
            "HTTP response limit must be positive and finite."
        }
        require(callTimeoutMillis in 1L..60_000L) {
            "HTTP call timeout must be between 1 and 60000 milliseconds."
        }

        // Override the deadline only for this call. Other account and wake
        // requests retain the existing 15-second default; Live's authorized
        // SDP broker can take longer than its upstream provider deadline.
        val call = client.newCall(request)
        call.timeout().timeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
        return call.executeAsync().use { response ->
            val source = response.body?.source()
            val body = if (source == null) {
                ""
            } else {
                source.request(maxResponseBytes + 1)
                if (source.buffer.size > maxResponseBytes) {
                    throw IOException("HTTP response exceeded the allowed size.")
                }
                // Read only the bytes already buffered by the bounded request.
                // Reading from the source itself would continue until EOF and
                // could silently exceed maxResponseBytes for a streamed body.
                String(source.buffer.readByteArray(), StandardCharsets.UTF_8)
            }
            WakeHttpResponse(
                status = response.code,
                body = body,
                headers = response.headers,
            )
        }
    }
}
