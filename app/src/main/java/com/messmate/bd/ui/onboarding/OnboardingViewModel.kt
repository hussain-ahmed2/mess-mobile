package com.messmate.bd.ui.onboarding

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the sign-in flow of the onboarding screen.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    /**
     * Signs in with Google and asks for the `drive.file` scope.
     *
     * Two steps, because they come from two different APIs:
     * 1. Credential Manager → the e-mail address of the member.
     * 2. GMS `AuthorizationClient` → the access token for Drive and Sheets.
     */
    fun signIn(activity: Activity) {
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, error = null)

            val signedIn = accountRepository.signIn { error ->
                _state.value = _state.value.copy(error = error)
            }
            if (!signedIn) {
                _state.value = _state.value.copy(isBusy = false)
                return@launch
            }

            val tokenGranted = accountRepository.requestAccessToken(activity) { error ->
                _state.value = _state.value.copy(error = error)
            }

            _state.value = _state.value.copy(
                isBusy = false,
                signedInEmail = accountRepository.observeEmail().first(),
                error = if (tokenGranted) null else _state.value.error,
            )
        }
    }

    /** Clears an error message. */
    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}

/** The state of the onboarding screen. */
data class OnboardingState(
    val isBusy: Boolean = false,
    val error: String? = null,
    val signedInEmail: String? = null,
)
