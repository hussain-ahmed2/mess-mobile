package com.messmate.bd.ui.bazar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.model.BazarEntry
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/**
 * The bazar ledger – the market trips that make up the variable food cost.
 */
@HiltViewModel
class BazarViewModel @Inject constructor(
    private val messRepository: MessRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val entries: StateFlow<List<BazarEntry>> = messRepository.observeBazar().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** The total bazar of the month – the numerator of the meal rate. */
    val total: StateFlow<Double> = entries.map { list -> list.sumOf { it.amount } }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0.0,
    )

    private val _events = MutableStateFlow<String?>(null)
    val events: StateFlow<String?> = _events.asStateFlow()

    /** Adds a market trip. */
    fun add(items: String, amount: Double, shopper: String) {
        viewModelScope.launch {
            val email = accountRepository.observeEmail().first().orEmpty()
            val entry = BazarEntry(
                entryId = UUID.randomUUID().toString(),
                date = LocalDate.now(),
                shopperName = shopper.ifBlank { email.substringBefore('@') },
                items = items.trim(),
                amount = amount,
                loggedBy = email,
                timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )
            messRepository.addBazarEntry(entry).onFailure { error ->
                _events.value = error.message ?: "Could not save the bazar entry."
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
