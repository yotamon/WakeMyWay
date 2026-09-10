package com.wakemyway.app.voice

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Compatibility shape consumed by DebugRealtimeWakeConversation.
 *
 * `operatorToken` is now a scoped founder installation credential, never WMW_INTERNAL_API_KEY.
 * The broker URL is fixed by the app so infrastructure details never appear in user-facing setup.
 */
data class FounderRealtimeConfig(
    val brokerUrl: String,
    val operatorToken: String,
)

/** Debug/founder durable Realtime pairing state backed by Android Keystore. */
class FounderRealtimeSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): FounderRealtimeConfig? {
        val encrypted = prefs.getString(KEY_DEVICE_TOKEN, null).orEmpty()
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (encrypted.isBlank() || expiresAt <= currentEpochSeconds() + EXPIRY_SAFETY_WINDOW_SECONDS) {
            if (encrypted.isNotBlank()) clearCredential()
            return null
        }

        val token = runCatching { decrypt(encrypted) }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: run {
                clearCredential()
                return null
            }

        return FounderRealtimeConfig(
            brokerUrl = BROKER_URL,
            operatorToken = token,
        )
    }

    fun saveInstallationCredential(deviceToken: String, expiresAtEpochSeconds: Long) {
        require(deviceToken.length >= MIN_TOKEN_LENGTH) { "Founder installation credential is invalid" }
        require(expiresAtEpochSeconds > currentEpochSeconds() + EXPIRY_SAFETY_WINDOW_SECONDS) {
            "Founder installation credential expires too soon"
        }
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, encrypt(deviceToken))
            .putLong(KEY_EXPIRES_AT, expiresAtEpochSeconds)
            .apply()
        ConversationalAlfredState.setReady(appContext, true)
    }

    fun installationId(): String {
        prefs.getString(KEY_INSTALLATION_ID, null)?.let { existing ->
            if (runCatching { UUID.fromString(existing) }.isSuccess) return existing
        }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALLATION_ID, created).apply()
        return created
    }

    fun clear() {
        clearCredential()
        // Keep the random installation ID stable across reconnects; it contains no identity data.
    }

    fun configured(): Boolean = load() != null

    private fun clearCredential() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
        ConversationalAlfredState.setReady(appContext, false)
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val envelope = ByteArray(1 + cipher.iv.size + ciphertext.size)
        envelope[0] = cipher.iv.size.toByte()
        System.arraycopy(cipher.iv, 0, envelope, 1, cipher.iv.size)
        System.arraycopy(ciphertext, 0, envelope, 1 + cipher.iv.size, ciphertext.size)
        return Base64.encodeToString(envelope, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val envelope = Base64.decode(encoded, Base64.NO_WRAP)
        require(envelope.isNotEmpty()) { "Encrypted founder credential is empty" }
        val ivSize = envelope[0].toInt() and 0xff
        require(ivSize in 12..16 && envelope.size > 1 + ivSize) {
            "Encrypted founder credential is malformed"
        }
        val iv = envelope.copyOfRange(1, 1 + ivSize)
        val ciphertext = envelope.copyOfRange(1 + ivSize, envelope.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1_000L

    companion object {
        const val API_BASE_URL = "https://wakemyway.vercel.app"
        const val FOUNDER_WAKE_PATH = "/api/internal/voice-spike/founder-wake-token"
        const val PAIR_PATH = "/api/founder/realtime/pair"
        const val STATUS_PATH = "/api/founder/realtime/status"
        const val BROKER_URL = "$API_BASE_URL$FOUNDER_WAKE_PATH"
        const val PAIR_URL = "$API_BASE_URL$PAIR_PATH"
        const val STATUS_URL = "$API_BASE_URL$STATUS_PATH"

        private const val PREFS = "founder-realtime-dogfood-v2"
        private const val KEY_DEVICE_TOKEN = "device-token-aes-gcm"
        private const val KEY_EXPIRES_AT = "device-token-expires-at"
        private const val KEY_INSTALLATION_ID = "installation-id"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "wmw-founder-realtime-token-v2"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MIN_TOKEN_LENGTH = 48
        private const val EXPIRY_SAFETY_WINDOW_SECONDS = 60L
    }
}
