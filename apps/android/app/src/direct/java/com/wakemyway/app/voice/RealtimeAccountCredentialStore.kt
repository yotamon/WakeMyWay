package com.wakemyway.app.voice

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import com.wakemyway.app.product.account.AccountInstallationStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class RealtimeDeviceCredential(
    val deviceToken: String,
    val expiresAtEpochSeconds: Long,
)

/**
 * Stores the account-authorized Realtime device credential outside Direct Boot/wake authority.
 * The token is scoped to Realtime wake enrichment and encrypted with Android Keystore.
 */
class RealtimeAccountCredentialStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val installationStore = AccountInstallationStore(appContext)

    fun load(): RealtimeDeviceCredential? {
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
        return RealtimeDeviceCredential(token, expiresAt)
    }

    fun save(credential: RealtimeDeviceCredential) {
        require(credential.deviceToken.length >= MIN_TOKEN_LENGTH) { "Realtime credential is invalid" }
        require(credential.expiresAtEpochSeconds > currentEpochSeconds() + EXPIRY_SAFETY_WINDOW_SECONDS) {
            "Realtime credential expires too soon"
        }
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, encrypt(credential.deviceToken))
            .putLong(KEY_EXPIRES_AT, credential.expiresAtEpochSeconds)
            .apply()
    }

    fun installationId(): String = installationStore.id()

    fun clear() = clearCredential()

    private fun clearCredential() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
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
        require(envelope.isNotEmpty()) { "Encrypted Realtime credential is empty" }
        val ivSize = envelope[0].toInt() and 0xff
        require(ivSize in 12..16 && envelope.size > 1 + ivSize) {
            "Encrypted Realtime credential is malformed"
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

    private companion object {
        const val PREFS = "account-realtime-device-v1"
        const val KEY_DEVICE_TOKEN = "device-token-aes-gcm"
        const val KEY_EXPIRES_AT = "device-token-expires-at"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "wmw-account-realtime-device-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val MIN_TOKEN_LENGTH = 48
        const val EXPIRY_SAFETY_WINDOW_SECONDS = 5 * 60L
    }
}
