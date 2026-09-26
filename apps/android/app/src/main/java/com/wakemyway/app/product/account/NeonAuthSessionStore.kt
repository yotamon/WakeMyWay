package com.wakemyway.app.product.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores account-session material outside Direct Boot and wake authority.
 *
 * Both the opaque Neon session token and the short-lived mobile OAuth PKCE verifier are encrypted
 * with an app-owned Android Keystore key. Android backup rules deny SharedPreferences, so neither
 * value is exported through cloud backup or device transfer.
 */
internal class NeonAuthSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): String? = loadEncrypted(KEY_SESSION_TOKEN)

    fun save(sessionToken: String) {
        require(sessionToken.isNotBlank()) { "Neon Auth session token is empty." }
        saveEncrypted(KEY_SESSION_TOKEN, sessionToken)
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_PENDING_GOOGLE_VERIFIER)
            .apply()
    }

    fun savePendingGoogleVerifier(verifier: String) {
        require(verifier.isNotBlank()) { "Google OAuth verifier is empty." }
        saveEncrypted(KEY_PENDING_GOOGLE_VERIFIER, verifier)
    }

    fun loadPendingGoogleVerifier(): String? = loadEncrypted(KEY_PENDING_GOOGLE_VERIFIER)

    fun clearPendingGoogleVerifier() {
        prefs.edit().remove(KEY_PENDING_GOOGLE_VERIFIER).apply()
    }

    private fun loadEncrypted(name: String): String? {
        val encrypted = prefs.getString(name, null)?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { decrypt(encrypted) }
            .getOrElse {
                prefs.edit().remove(name).apply()
                return null
            }
            .takeIf { it.isNotBlank() }
    }

    private fun saveEncrypted(name: String, value: String) {
        prefs.edit().putString(name, encrypt(value)).apply()
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
        require(envelope.isNotEmpty()) { "Encrypted auth value is empty." }
        val ivSize = envelope[0].toInt() and 0xff
        require(ivSize in 12..16 && envelope.size > 1 + ivSize) {
            "Encrypted auth value is malformed."
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

    companion object {
        private const val PREFS = "wake-account-neon-auth-v1"
        private const val KEY_SESSION_TOKEN = "session-token-aes-gcm"
        private const val KEY_PENDING_GOOGLE_VERIFIER = "pending-google-verifier-aes-gcm"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "wmw-neon-auth-session-v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
