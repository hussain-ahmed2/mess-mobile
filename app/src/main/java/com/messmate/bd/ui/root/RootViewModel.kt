package com.messmate.bd.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Decides what the app shows first:
 *
 * * not signed in  → onboarding
 * * signed in but no mess → onboarding (create or join)
 * * signed in with a mess → the dashboard
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    accountRepository: AccountRepository,
    messRepository: MessRepository,
) : ViewModel() {

    val state: StateFlow<RootState> = combine(
        accountRepository.observeSignedInAccount(),
        messRepository.observeMess(),
    ) { account, mess ->
        RootState(
            isSignedIn = account != null,
            accountEmail = account?.email,
            hasMess = mess != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RootState(),
    )
}

/** The state of the root screen. */
data class RootState(
    val isSignedIn: Boolean = false,
    val accountEmail: String? = null,
    val hasMess: Boolean = false,
    /** `true` until the first value arrived – the app shows a splash. */
    val isLoading: Boolean = true,
)
