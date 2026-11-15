package com.messmate.bd.data.remote

/**
 * Supplies the OAuth2 access token that is attached to every Drive / Sheets
 * request.
 *
 * The implementation lives in the auth layer (Credential Manager + the GMS
 * `AuthorizationClient`). The remote layer only knows this interface, which
 * keeps the Google clients testable and free of Android dependencies.
 */
interface GoogleAccessTokenProvider {

    /**
     * The **cached** access token – never blocks and never hits the network.
     *
     * Called from the Google HTTP client interceptor, therefore it must be
     * cheap. Use [refreshIfNeeded] before a request if the token might be
     * expired.
     */
    fun currentToken(): String?

    /**
     * Called when the API rejected the token (HTTP 401). The implementation
     * should drop the cached token so the next call mints a fresh one.
     */
    fun invalidate(token: String)

    /**
     * Mints a fresh access token when the cached one is expired.
     *
     * This is a suspending call – it may show the Google consent screen, so it
     * must never be called from inside the HTTP interceptor.
     *
     * @return `true` when a valid token is available afterwards.
     */
    suspend fun refreshIfNeeded(): Boolean
}
