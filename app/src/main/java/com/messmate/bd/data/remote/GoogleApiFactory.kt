package com.messmate.bd.data.remote

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpExecuteInterceptor
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the Google API clients.
 *
 * Both clients share one [HttpRequestInitializer] that injects the OAuth2
 * bearer token on **every** request. That way a token that expires in the
 * middle of a long session is replaced automatically on the next call.
 *
 * ### Scopes
 *
 * The app only ever requests:
 * * `https://www.googleapis.com/auth/drive.file` – read/write access to the
 *   files the user picks or the app creates (never the whole Drive).
 * * `openid email profile` – identity of the signed-in account.
 *
 * The restricted `drive` and `spreadsheets` scopes are never requested, which
 * is exactly why the sheet has to be created programmatically: a file created
 * by the app is fully accessible through `drive.file`.
 */
@Singleton
class GoogleApiFactory @Inject constructor(
    private val tokenProvider: GoogleAccessTokenProvider,
) {

    private val transport by lazy {
        try {
            GoogleNetHttpTransport.newTrustedTransport()
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException("Secure transport for Google APIs unavailable", e)
        } catch (e: IOException) {
            throw IllegalStateException("Unable to create Google API transport", e)
        }
    }

    private val jsonFactory by lazy { GsonFactory.getDefaultInstance() }

    /** Google Sheets v4 client. */
    fun createSheetsService(): Sheets = Sheets.Builder(transport, jsonFactory, null)
        .setApplicationName(APPLICATION_NAME)
        .setHttpRequestInitializer(tokenInitializer())
        .build()

    /** Google Drive v3 client. */
    fun createDriveService(): Drive = Drive.Builder(transport, jsonFactory, null)
        .setApplicationName(APPLICATION_NAME)
        .setHttpRequestInitializer(tokenInitializer())
        .build()

    /**
     * Attaches the bearer token to every request.
     *
     * When no account is signed in the request fails fast with a clear message
     * instead of an opaque HTTP error.
     */
    private fun tokenInitializer(): HttpRequestInitializer = HttpRequestInitializer { request ->
        request.interceptor = HttpExecuteInterceptor { httpRequest ->
            val token = tokenProvider.currentToken()
                ?: throw IOException("No Google account signed in – cannot talk to Drive/Sheets")
            httpRequest.headers.setAuthorization("Bearer $token")
        }
    }

    private companion object {
        const val APPLICATION_NAME = "MessMate BD"
    }
}
