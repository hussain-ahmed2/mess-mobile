package com.messmate.bd.ui.root

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides what the app shows first:
 *
 * * not signed in  → onboarding
 * * signed in but no mess → onboarding (create or join)
 * * signed in with a mess → the dashboard
 *
 * It also reports when Google wants the `drive.file` scope again – the access
 * token of a revoked consent expires and every API call would fail until the
 * user grants it once more.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    messRepository: MessRepository,
) : ViewModel() {

    val state: StateFlow<RootState> = combine(
        combine(
            accountRepository.observeSignedInAccount(),
            messRepository.observeMess(),
        ) { account, mess ->
            account to mess },
        accountRepository.observeNeedsConsent(),
    ) { (account, mess), needsConsent ->
        RootState(
            isSignedIn = account != null,
            accountEmail = account?.email,
            hasMess = mess != null,
            needsConsent = needsConsent,
            // The first value arrived – the splash screen can go.
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RootState(),
    )

    /**
     * Shows the Google consent screen again.
     *
     * @param onError called with a human readable message when it fails.
     */
    fun grantAccess(activity: Activity, onError: (String) -> Unit) {
        viewModelScope.launch {
            accountRepository.requestAccessToken(activity, onError)
        }
    }
}

/** The state of the root screen. */
data class RootState(
    val isSignedIn: Boolean = false,
    val accountEmail: String? = null,
    val hasMess: Boolean = false,
    /** `true` until the first value arrived – the app shows a splash. */
    val isLoading: Boolean = true,
    /** `true` when the Google scope has to be granted again. */
    val needsConsent: Boolean = false,
)
