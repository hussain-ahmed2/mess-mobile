package com.messmate.bd.data.repository

import android.app.Activity
import com.messmate.bd.auth.GoogleAuthManager
import com.messmate.bd.data.local.dao.MessDao
import com.messmate.bd.domain.model.Mess
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.GoogleAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the [GoogleAuthManager] and the domain layer.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val messDao: MessDao,
) : AccountRepository {

    override fun observeSignedInAccount(): Flow<GoogleAccount?> =
        googleAuthManager.session.map { session ->
            session?.let {
                GoogleAccount(email = it.email, displayName = it.displayName, photoUrl = it.photoUrl)
            }
        }

    override fun observeEmail(): Flow<String?> =
        googleAuthManager.session.map { it?.email }

    override fun observeMess(): Flow<Mess?> =
        messDao.observeActiveMess().map { entity ->
            entity?.let {
                Mess(
                    spreadsheetId = it.spreadsheetId,
                    spreadsheetUrl = it.spreadsheetUrl,
                    name = it.name,
                    role = it.role,
                    managerEmail = it.managerEmail,
                    monthYear = it.monthYear,
                    myMemberId = it.myMemberId,
                )
            }
        }

    override suspend fun signIn(onError: (String) -> Unit): Boolean =
        googleAuthManager.signIn().fold(
            onSuccess = { true },
            onFailure = { error ->
                onError(error.message ?: "Google sign-in failed.")
                false
            },
        )

    override suspend fun requestAccessToken(activity: Activity, onError: (String) -> Unit): Boolean =
        googleAuthManager.requestAccessToken(activity).fold(
            onSuccess = { true },
            onFailure = { error ->
                onError(error.message ?: "Could not get access to Google Drive.")
                false
            },
        )

    override suspend fun signOut() {
        googleAuthManager.signOut()
    }

    override suspend fun refreshAccessToken(): Boolean =
        googleAuthManager.refreshAccessToken() != null
}
