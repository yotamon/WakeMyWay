package com.wakemyway.app.voice

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class FounderRealtimeConfig(
    val brokerUrl: String,
    val operatorToken: String,
)

/**
 * Debug-only durable configuration for founder dogfood.
 *
 * The operator token is encrypted with a non-exportable Android Keystore key. This remains an
 * internal dogfood mechanism and must never be promoted as product authentication.
 */
class FounderRealtimeSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): FounderRealtimeConfig? {
        val brokerUrl = prefs.getString(KEY_BROKER_URL, null)?.trim().orEmpty()
        val encrypted = prefs.getString(KEY_OPERATOR_TOKEN, null).orEmpty()
        if (brokerUrl.isBlank() || encrypted.isBlank()) return null
        return runCatching {
            FounderRealtimeConfig(
                brokerUrl = validateBrokerUrl(brokerUrl),
                operatorToken = decrypt(encrypted),
            )
        }.getOrNull()?.takeIf { it.operatorToken.isNotBlank() }
    }

    fun save(brokerUrl: String, operatorToken: String) {
        val validUrl = validateBrokerUrl(brokerUrl)
        require(operatorToken.length >= MIN_TOKEN_LENGTH) { "Operator token is too short" }
        prefs.edit()
            .putString(KEY_BROKER_URL, validUrl)
            .putString(KEY_OPERATOR_TOKEN, encrypt(operatorToken))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun configured(): Boolean = load() != null

    private fun validateBrokerUrl(raw: String): String {
        val value = raw.trim().trimEnd('/')
        val uri = Uri.parse(value)
        val valid =
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.path.orEmpty().endsWith(FOUNDER_WAKE_PATH)
        require(valid) {
            "Wake conversation broker must be HTTPS and end with $FOUNDER_WAKE_PATH"
        }
        return value
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
        require(envelope.isNotEmpty()) { "Encrypted founder token is empty" }
        val ivSize = envelope[0].toInt() and 0xff
        require(ivSize in 12..16 && envelope.size > 1 + ivSize) { "Encrypted founder token is malformed" }
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

    companion object {
        const val FOUNDER_WAKE_PATH = "/api/internal/voice-spike/founder-wake-token"

        private const val PREFS = "founder-realtime-dogfood-v1"
        private const val KEY_BROKER_URL = "broker-url"
        private const val KEY_OPERATOR_TOKEN = "operator-token-aes-gcm"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "wmw-founder-realtime-token-v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MIN_TOKEN_LENGTH = 24
    }
}
