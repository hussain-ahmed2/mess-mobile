package com.messmate.bd.domain.repository

import android.app.Activity
import com.messmate.bd.domain.model.Mess
import kotlinx.coroutines.flow.Flow

/**
 * Authentication of the Google account that owns (or joins) a mess sheet.
 *
 * The app uses two Google APIs:
 * * **Credential Manager** for the identity (e-mail address of the member).
 * * **GMS `AuthorizationClient`** for the `drive.file` access token that the
 *   Drive and Sheets REST calls need.
 */
interface AccountRepository {

    /** The signed-in Google account, or `null` when nobody is signed in. */
    fun observeSignedInAccount(): Flow<GoogleAccount?>

    /** The e-mail of the signed-in account, or `null`. */
    fun observeEmail(): Flow<String?>

    /** The mess of the signed-in account, or `null`. */
    fun observeMess(): Flow<Mess?>

    /**
     * Starts the Google sign-in flow.
     *
     * @param onError called with a human readable message when the flow fails.
     * @return `true` when the user signed in successfully.
     */
    suspend fun signIn(onError: (String) -> Unit): Boolean

    /**
     * Requests the `drive.file` scope and returns an access token.
     *
     * The first call shows the Google consent screen.
     */
    suspend fun requestAccessToken(activity: Activity, onError: (String) -> Unit): Boolean

    /** Signs the user out and clears the cached session. */
    suspend fun signOut()

    /**
     * Re-mints a fresh access token (the old ones expire after ~1 hour).
     *
     * @return `true` when a new token is available.
     */
    suspend fun refreshAccessToken(): Boolean
}

/** A signed-in Google account. */
data class GoogleAccount(
    val email: String,
    val displayName: String,
    val photoUrl: String?,
)
