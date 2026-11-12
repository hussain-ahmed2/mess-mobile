package com.messmate.bd.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.messmate.bd.BuildConfig
import com.messmate.bd.data.remote.GoogleAccessTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google sign-in for MessMate.
 *
 * The flow is split in two steps because the two APIs are provided by different
 * libraries:
 *
 * 1. **Identity – Credential Manager** (`GetSignInWithGoogleOption`)
 *    The user picks a Google account, the app receives a
 *    [GoogleIdTokenCredential] with the e-mail address, the display name and
 *    an id token. This is the "who are you" step and it needs no scope.
 *
 * 2. **API access – GMS `AuthorizationClient`**
 *    The app asks for exactly one scope,
 *    `https://www.googleapis.com/auth/drive.file`, and receives an OAuth2
 *    access token for the Drive and Sheets REST APIs. `drive.file` is a
 *    non-sensitive scope: no app verification, no restricted-scope audit.
 *
 * Both steps use the *installed* Google Play services – the user never types a
 * password inside the app.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
) : GoogleAccessTokenProvider {

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _session = MutableStateFlow(sessionStore.load())

    /** The current session – `null` when nobody is signed in. */
    val session: StateFlow<GoogleSession?> = _session.asStateFlow()

    /** Guards against two concurrent refresh attempts. */
    private val refreshing = AtomicBoolean(false)

    /** Completes when the scope consent screen returns with a result. */
    private var pendingAuthorization: CompletableDeferred<String?>? = null

    /** `true` when a server client id is configured (see `BuildConfig`). */
    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_SERVER_CLIENT_ID.isNotBlank()

    // =======================================================================
    // Step 1 – identity (Credential Manager)
    // =======================================================================

    /**
     * Runs the interactive sign-in flow and stores the identity in the
     * encrypted session.
     */
    suspend fun signIn(): Result<GoogleSession> = runCatching {
        check(isConfigured) {
            "No Google client id configured – add MESSMATE_SERVER_CLIENT_ID to local.properties."
        }

        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
            .setNonce(createNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .setCredentialOptions(listOf(option))
            .build()

        val response = try {
            credentialManager.getCredential(context = context, request = request)
        } catch (noCredential: NoCredentialException) {
            throw IllegalStateException(
                "No Google account is available on this device.",
                noCredential,
            )
        }
        val credential = response.credential
        val googleCredential = when (credential) {
            is GoogleIdTokenCredential -> credential
            else -> runCatching { GoogleIdTokenCredential.createFrom(credential.data) }.getOrNull()
        } ?: throw IllegalStateException("Google returned an unknown credential type.")

        val email = googleCredential.email
            ?: throw IllegalStateException("The Google account did not share its e-mail address.")

        // Keep the existing access token (if any) – the user may already have
        // granted the drive.file scope in a previous session.
        val previous = _session.value
        val session = GoogleSession(
            email = email,
            displayName = googleCredential.displayName.orEmpty(),
            photoUrl = googleCredential.profilePictureUri?.toString(),
            idToken = googleCredential.idToken,
            accessToken = previous?.accessToken.orEmpty(),
            grantedScopes = previous?.grantedScopes.orEmpty(),
            issuedAtMillis = previous?.issuedAtMillis ?: 0L,
        )

        sessionStore.save(session)
        _session.value = session
        session
    }

    /** Signs the user out and removes the encrypted session. */
    fun signOut() {
        sessionStore.clear()
        _session.value = null
    }

    // =======================================================================
    // Step 2 – API access (GMS AuthorizationClient)
    // =======================================================================

    /**
     * Requests the `drive.file` scope and returns an access token.
     *
     * The first call shows the Google consent screen. When the user approves,
     * the token is cached by the system and later calls return immediately.
     *
     * @param activity the activity used to show the consent screen.
     */
    suspend fun requestAccessToken(activity: Activity): Result<String> = runCatching {
        val client = Identity.getAuthorizationClient(activity)

        val request = AuthorizationRequest.Builder()
            .setRequestedScopes(listOf(Scope(GoogleSession.DRIVE_FILE_SCOPE)))
            .build()

        val result = client.authorize(request).await()

        if (result.hasResolution()) {
            // The user has to approve the scope in a consent screen.
            val deferred = CompletableDeferred<String?>()
            pendingAuthorization = deferred
            val intentSender = result.pendingIntent?.intentSender
                ?: throw IllegalStateException("Google did not provide the consent screen.")
            activity.startIntentSenderForResult(
                intentSender,
                REQUEST_CODE_AUTHORIZATION,
                null,
                0,
                0,
                0,
            )
            val token = deferred.await()
                ?: throw IllegalStateException("The drive.file scope was not granted.")
            storeAccessToken(token, result.grantedScopes)
            return@runCatching token
        }

        val token = result.accessToken
            ?: throw IllegalStateException("Google did not return an access token.")
        storeAccessToken(token, result.grantedScopes)
        token
    }

    /**
     * Forwarded from `Activity.onActivityResult` – it completes the pending
     * scope consent.
     */
    fun onActivityResult(requestCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE_AUTHORIZATION) return
        val deferred = pendingAuthorization
        pendingAuthorization = null
        if (deferred == null) return

        val token = runCatching {
            val result: AuthorizationResult =
                Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            result.accessToken
        }.getOrNull()

        deferred.complete(token)
    }

    /** Stores a freshly minted access token in the session. */
    private fun storeAccessToken(token: String, grantedScopes: List<String>) {
        val current = _session.value
        val session = (current ?: return).copy(
            accessToken = token,
            grantedScopes = grantedScopes.ifEmpty { current.grantedScopes },
            issuedAtMillis = System.currentTimeMillis(),
        )
        sessionStore.save(session)
        _session.value = session
    }

    // =======================================================================
    // GoogleAccessTokenProvider
    // =======================================================================

    /**
     * Returns a token that can be used right now.
     *
     * Called from the HTTP interceptor → must be blocking.
     */
    override fun currentToken(): String? {
        val session = _session.value ?: return null
        if (session.accessToken.isEmpty()) return null
        if (!session.isExpired(System.currentTimeMillis())) return session.accessToken

        // The token expired – ask GMS for a new one. The scope is already
        // granted, so this does not show any UI.
        val refreshed = runBlocking { refreshAccessToken() }
        return refreshed ?: session.accessToken
    }

    /** Drops a token the API rejected (HTTP 401). */
    override fun invalidate(token: String) {
        val current = _session.value ?: return
        if (current.accessToken == token) {
            _session.value = current.copy(issuedAtMillis = 0L)
        }
    }

    /**
     * Mints a fresh access token without any UI.
     *
     * @return the new token or `null` when it could not be refreshed.
     */
    suspend fun refreshAccessToken(): String? {
        if (!refreshing.compareAndSet(false, true)) return _session.value?.accessToken
        return try {
            val client = Identity.getAuthorizationClient(context)
            val request = AuthorizationRequest.Builder()
                .setRequestedScopes(listOf(Scope(GoogleSession.DRIVE_FILE_SCOPE)))
                .build()
            val result = client.authorize(request).await()
            if (result.hasResolution()) {
                // The user has to approve again – the UI layer handles that.
                null
            } else {
                result.accessToken?.also { token ->
                    storeAccessToken(token, result.grantedScopes)
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            refreshing.set(false)
        }
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    /** A random nonce for the id token (replay protection). */
    private fun createNonce(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private companion object {
        const val REQUEST_CODE_AUTHORIZATION = 0x4D45
    }
}
