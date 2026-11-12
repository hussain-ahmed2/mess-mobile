package com.messmate.bd.data.remote

/**
 * Supplies the OAuth2 access token that is attached to every Drive / Sheets
 * request.
 *
 * The implementation lives in the auth layer (Credential Manager). The remote
 * layer only knows this interface, which keeps the Google clients testable and
 * free of Android dependencies.
 */
interface GoogleAccessTokenProvider {

    /**
     * Returns a currently valid access token.
     *
     * Called from the Google HTTP client interceptor – therefore **blocking**.
     * Implementations must never throw; return `null` when no account is
     * signed in.
     */
    fun currentToken(): String?

    /**
     * Called when the API rejected the token (HTTP 401). The implementation
     * should drop the cached token so the next call mints a fresh one.
     */
    fun invalidate(token: String)
}
