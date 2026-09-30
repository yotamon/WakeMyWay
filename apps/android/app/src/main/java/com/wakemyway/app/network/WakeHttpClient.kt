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
    ): WakeHttpResponse {
        require(maxResponseBytes > 0) { "HTTP response limit must be positive." }

        return client.newCall(request).executeAsync().use { response ->
            val source = response.body?.source()
            val body = if (source == null) {
                ""
            } else {
                source.request(maxResponseBytes + 1)
                if (source.buffer.size > maxResponseBytes) {
                    throw IOException("HTTP response exceeded the allowed size.")
                }
                String(source.readByteArray(), StandardCharsets.UTF_8)
            }
            WakeHttpResponse(
                status = response.code,
                body = body,
                headers = response.headers,
            )
        }
    }
}
