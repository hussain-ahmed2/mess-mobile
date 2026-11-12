package com.messmate.bd.ui.join

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Joins a mess from a scanned QR code (the deep link `messmate://join/<id>`).
 */
@HiltViewModel
class JoinMessActivityViewModel @Inject constructor(
    private val messRepository: MessRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(JoinActivityState())
    val state: StateFlow<JoinActivityState> = _state.asStateFlow()

    /** Joins the mess of the scanned code. */
    fun join(sheetId: String?) {
        if (_state.value.isBusy) return

        val id = SpreadsheetIdExtractor.extract(sheetId)
        if (id == null) {
            _state.value = JoinActivityState(error = "This QR code does not contain a sheet id.")
            return
        }

        viewModelScope.launch {
            _state.value = JoinActivityState(isBusy = true)

            messRepository.joinMess(id).fold(
                onSuccess = { _state.value = JoinActivityState(joined = true) },
                onFailure = { error ->
                    _state.value = JoinActivityState(
                        error = error.message ?: "Could not join this mess.",
                    )
                },
            )
        }
    }
}

/** The state of the deep link activity. */
data class JoinActivityState(
    val isBusy: Boolean = false,
    val joined: Boolean = false,
    val error: String? = null,
)
