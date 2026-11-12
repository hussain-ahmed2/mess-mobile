package com.messmate.bd.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted storage for the Google session.
 *
 * The access token is a bearer token – anybody who steals it can read and
 * write the user's mess sheet. Therefore the whole session blob is encrypted
 * with an **AES-256-GCM** key that lives inside the Android Keystore and never
 * leaves the device.
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Stores (or overwrites) the session. */
    fun save(session: GoogleSession) {
        val payload = json.encodeToString(
            StoredSession.serializer(),
            StoredSession(
                email = session.email,
                displayName = session.displayName,
                photoUrl = session.photoUrl,
                idToken = session.idToken,
                accessToken = session.accessToken,
                grantedScopes = session.grantedScopes,
                issuedAtMillis = session.issuedAtMillis,
            ),
        )

        prefs.edit()
            .putString(KEY_SESSION, encrypt(payload))
            .apply()
    }

    /** Reads the session – `null` when nothing is stored or it is corrupted. */
    fun load(): GoogleSession? {
        val encrypted = prefs.getString(KEY_SESSION, null) ?: return null
        val payload = decrypt(encrypted) ?: return null

        return runCatching {
            val stored = json.decodeFromString(StoredSession.serializer(), payload)
            GoogleSession(
                email = stored.email,
                displayName = stored.displayName,
                photoUrl = stored.photoUrl,
                idToken = stored.idToken,
                accessToken = stored.accessToken,
                grantedScopes = stored.grantedScopes,
                issuedAtMillis = stored.issuedAtMillis,
            )
        }.getOrNull()
    }

    /** Removes the session (sign out). */
    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    // -----------------------------------------------------------------------
    // Encryption
    // -----------------------------------------------------------------------

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? = runCatching {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }.getOrNull()

    /** The AES key – generated once and kept inside the Android Keystore. */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        existing?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /** The serializable form of a session. */
    @Serializable
    private data class StoredSession(
        val email: String,
        val displayName: String,
        val photoUrl: String? = null,
        val idToken: String,
        val accessToken: String,
        val grantedScopes: List<String> = emptyList(),
        val issuedAtMillis: Long,
    )

    private companion object {
        const val PREFS_NAME = "messmate_session"
        const val KEY_SESSION = "session"
        const val KEY_ALIAS = "messmate.session.key.v1"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
