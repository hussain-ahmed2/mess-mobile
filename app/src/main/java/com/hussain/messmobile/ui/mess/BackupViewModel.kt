package com.hussain.messmobile.ui.mess

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.messmobile.data.backup.BackupFile
import com.hussain.messmobile.data.backup.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the backup screen shows. */
data class BackupUiState(
    val busy: Boolean = false,
    /** The backup that was picked, waiting for the confirmation of the user. */
    val pendingRestore: BackupFile? = null,
    /** The file the restore came from, only used for the message. */
    val pendingSource: String? = null,
    val message: String? = null,
    val error: String? = null,
    val lastExportAt: Long? = null,
)

/**
 * Exports and restores the data of the mess.
 *
 * A backup is a plain file, the user decides where it is stored. It survives the uninstall of
 * the app, restoring it brings the mess back exactly where it was left.
 */
class BackupViewModel(private val backupManager: BackupManager) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state

    /** Writes the current data of the app to the given file. */
    fun export(uri: Uri, appVersion: String, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, message = null)
            runCatching { backupManager.write(uri, appVersion) }
                .onSuccess { backup ->
                    _state.value = _state.value.copy(
                        busy = false,
                        message = "Saved ${backup.recordCount} records to $fileName.",
                        lastExportAt = backup.exportedAt,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        busy = false,
                        error = error.message ?: "The backup could not be written.",
                    )
                }
        }
    }

    /** Reads a backup file and asks the user to confirm the restore. */
    fun inspect(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, message = null)
            runCatching { backupManager.read(uri) }
                .onSuccess { backup ->
                    _state.value = _state.value.copy(
                        busy = false,
                        pendingRestore = backup,
                        pendingSource = fileName,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        busy = false,
                        error = error.message ?: "The backup file could not be read.",
                    )
                }
        }
    }

    /** Replaces everything that is stored with the content of the picked backup. */
    fun restore() {
        val backup = _state.value.pendingRestore ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, message = null)
            runCatching { backupManager.restore(backup) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        busy = false,
                        pendingRestore = null,
                        pendingSource = null,
                        message = "Restored ${backup.recordCount} records.",
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        busy = false,
                        error = error.message ?: "The backup could not be restored.",
                    )
                }
        }
    }

    /** Throws the picked backup away. */
    fun cancelRestore() {
        _state.value = _state.value.copy(pendingRestore = null, pendingSource = null)
    }

    /** Removes the message or the error that is shown. */
    fun clearFeedback() {
        _state.value = _state.value.copy(message = null, error = null)
    }
}
