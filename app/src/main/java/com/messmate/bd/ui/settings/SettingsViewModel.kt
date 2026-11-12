package com.messmate.bd.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.core.locale.LocaleController
import com.messmate.bd.domain.model.Mess
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * The settings: language, cut-off times, breakfast weight and the month end.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val messRepository: MessRepository,
    private val accountRepository: AccountRepository,
    private val localeController: LocaleController,
) : ViewModel() {

    val config: StateFlow<MessConfig?> = messRepository.observeConfig().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val mess: StateFlow<Mess?> = messRepository.observeMess().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    /** The selected language tag (`en`, `bn`). */
    val languageTag: String get() = localeController.currentTag()

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    /**
     * Switches the language and recreates the activity so that every resource
     * is resolved in the new language.
     */
    fun setLanguage(tag: String, activity: Activity?) {
        localeController.setLanguage(tag)
        activity?.recreate()
    }

    /** Updates the three cut-off times. */
    fun updateCutoffs(breakfast: LocalTime, lunch: LocalTime, dinner: LocalTime) {
        viewModelScope.launch {
            messRepository.updateCutoffs(breakfast, lunch, dinner).onFailure { error ->
                _events.value = error.message ?: "Could not update the cut-off times."
            }
        }
    }

    /** Updates the breakfast weight (0.5 or 1.0). */
    fun updateBreakfastWeight(weight: Double) {
        viewModelScope.launch {
            messRepository.updateBreakfastWeight(weight).onFailure { error ->
                _events.value = error.message ?: "Could not update the breakfast weight."
            }
        }
    }

    /** Closes the month and opens the next one. */
    fun closeMonth() {
        viewModelScope.launch {
            messRepository.closeMonthAndRollover().onFailure { error ->
                _events.value = error.message ?: "Could not close the month."
            }
        }
    }

    /** Signs the user out – the sheet stays untouched. */
    fun signOut() {
        viewModelScope.launch {
            accountRepository.signOut()
        }
    }

    /** Removes the mess from this device. */
    fun leaveMess() {
        viewModelScope.launch {
            messRepository.leaveMess().onFailure { error ->
                _events.value = error.message ?: "Could not leave the mess."
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
