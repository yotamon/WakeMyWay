package com.wakemyway.app.voice

import com.wakemyway.app.network.WakeHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AccountRealtimeProvisioningClient {
    suspend fun provision(accessToken: String, installationId: String): RealtimeDeviceCredential {
        val body = JSONObject()
            .put("installationId", installationId)
            .toString()
            .toRequestBody(WakeHttpClient.jsonMediaType)
        val response = WakeHttpClient.execute(
            Request.Builder()
                .url(PROVISION_URL)
                .post(body)
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .build(),
            MAX_RESPONSE_BYTES,
        )
        if (response.status !in 200..299) error("Realtime provisioning failed")

        val json = JSONObject(response.body)
        return RealtimeDeviceCredential(
            deviceToken = json.getString("deviceToken").trim(),
            expiresAtEpochSeconds = json.getLong("expiresAt"),
        )
    }

    companion object {
        const val API_BASE_URL = "https://wakemyway.vercel.app"
        const val PROVISION_PATH = "/api/v1/account/realtime-provision"
        const val TOKEN_PATH = "/api/v1/account/realtime-token"
        const val TOKEN_URL = "$API_BASE_URL$TOKEN_PATH"
        private const val PROVISION_URL = "$API_BASE_URL$PROVISION_PATH"
        private const val MAX_RESPONSE_BYTES = 32L * 1024
    }
}
