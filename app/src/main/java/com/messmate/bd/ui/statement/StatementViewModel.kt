package com.messmate.bd.ui.statement

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.R
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.domain.model.Mess
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The monthly statement of every member and the PDF export.
 */
@HiltViewModel
class StatementViewModel @Inject constructor(
    private val messRepository: MessRepository,
    private val pdfExporter: StatementPdfExporter,
) : ViewModel() {

    val statements: StateFlow<List<MemberStatement>> = messRepository.observeStatements().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

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

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    /**
     * Renders the statement as PDF and opens the Android share sheet.
     */
    fun exportPdf(context: Context) {
        viewModelScope.launch {
            val currentMess = mess.value
            val currentConfig = config.value
            val currentStatements = statements.value

            if (currentMess == null || currentConfig == null) {
                _events.value = "The statement is not ready yet."
                return@launch
            }

            runCatching {
                pdfExporter.export(
                    messName = currentMess.name,
                    month = currentConfig.monthYear,
                    config = currentConfig,
                    statements = currentStatements,
                )
            }.onSuccess { uri ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, context.getString(R.string.export_pdf_title)),
                )
            }.onFailure { error ->
                _events.value = error.message ?: "Could not create the PDF."
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
