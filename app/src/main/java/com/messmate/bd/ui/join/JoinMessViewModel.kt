package com.messmate.bd.ui.join

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
 * Joins an existing mess sheet.
 */
@HiltViewModel
class JoinMessViewModel @Inject constructor(
    private val messRepository: MessRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(JoinMessState())
    val state: StateFlow<JoinMessState> = _state.asStateFlow()

    fun onSheetIdChange(value: String) = update { copy(sheetId = value, error = null) }

    /** Fills the field with an id that was picked or scanned. */
    fun onSpreadsheetIdDetected(raw: String) {
        val id = SpreadsheetIdExtractor.extract(raw)
        if (id == null) {
            update { copy(error = "Could not read the sheet id from that code.") }
        } else {
            update { copy(sheetId = id, error = null) }
        }
    }

    /**
     * Joins the mess.
     *
     * The sheet has to be accessible with the `drive.file` scope – the manager
     * shares it with the member's Gmail address, the member picks it in Drive
     * (or the app is allowed to read it because the manager shared it).
     */
    fun join() {
        val current = _state.value
        if (current.isBusy) return

        val id = SpreadsheetIdExtractor.extract(current.sheetId)
        if (id == null) {
            update { copy(error = "Please enter a valid sheet id.") }
            return
        }

        viewModelScope.launch {
            update { copy(isBusy = true, error = null) }

            messRepository.joinMess(id).fold(
                onSuccess = { update { copy(isBusy = false, joined = true) } },
                onFailure = { error ->
                    update {
                        copy(
                            isBusy = false,
                            error = error.message ?: "Could not join this mess.",
                        )
                    }
                },
            )
        }
    }

    fun dismissError() = update { copy(error = null) }

    private inline fun update(transform: JoinMessState.() -> JoinMessState) {
        _state.value = _state.value.transform()
    }
}

/** The state of the "join a mess" screen. */
data class JoinMessState(
    val sheetId: String = "",
    val isBusy: Boolean = false,
    val error: String? = null,
    val joined: Boolean = false,
)
