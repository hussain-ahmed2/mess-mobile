package com.messmate.bd.auth

/**
 * The signed-in Google session.
 *
 * @param email        the Google account – it is the identity of the member.
 * @param idToken      the OIDC id token (identity proof, not used for the APIs).
 * @param accessToken  OAuth2 access token for the Drive / Sheets REST APIs.
 * @param grantedScopes the API scopes the user actually approved.
 * @param issuedAtMillis when the access token was minted.
 */
data class GoogleSession(
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val idToken: String,
    val accessToken: String,
    val grantedScopes: List<String>,
    val issuedAtMillis: Long,
) {
    /** `true` when the access token is (almost) expired and should be renewed. */
    fun isExpired(nowMillis: Long, lifetimeMillis: Long = DEFAULT_LIFETIME_MILLIS): Boolean =
        nowMillis - issuedAtMillis >= lifetimeMillis

    /** `true` when the app may talk to the Drive / Sheets APIs. */
    val hasDriveFileScope: Boolean
        get() = grantedScopes.any { it.equals(DRIVE_FILE_SCOPE, ignoreCase = true) }

    companion object {
        /** Google access tokens are valid for one hour – renew a bit earlier. */
        const val DEFAULT_LIFETIME_MILLIS: Long = 55 * 60 * 1000L

        /**
         * The only cloud scope the app asks for.
         *
         * `drive.file` grants read/write access to the files the user picks or
         * that the app creates – never to the whole Drive. The restricted
         * `drive` and `spreadsheets` scopes are never requested.
         */
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
