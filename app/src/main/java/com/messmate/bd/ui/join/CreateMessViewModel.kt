package com.messmate.bd.ui.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.model.BlueprintMember
import com.messmate.bd.domain.model.MessBlueprint
import com.messmate.bd.domain.model.Role
import com.messmate.bd.domain.repository.AccountRepository
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

/**
 * Collects the data of the "create a mess" form and provisions the sheet.
 */
@HiltViewModel
class CreateMessViewModel @Inject constructor(
    private val messRepository: MessRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateMessState())
    val state: StateFlow<CreateMessState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Pre-fill the manager with the signed-in Google account.
            val email = accountRepository.observeEmail().first()
            _state.value = _state.value.copy(managerEmail = email.orEmpty())
        }
    }

    fun onMessNameChange(value: String) = update { copy(messName = value) }
    fun onMonthChange(value: YearMonth) = update { copy(month = value) }
    fun onManagerNameChange(value: String) = update { copy(managerName = value) }
    fun onBreakfastWeightChange(value: Double) = update { copy(breakfastWeight = value) }
    fun onBreakfastCutoffChange(value: LocalTime) = update { copy(breakfastCutoff = value) }
    fun onLunchCutoffChange(value: LocalTime) = update { copy(lunchCutoff = value) }
    fun onDinnerCutoffChange(value: LocalTime) = update { copy(dinnerCutoff = value) }

    fun addMember() = update {
        copy(members = members + MemberDraft())
    }

    fun removeMember(index: Int) = update {
        copy(members = members.filterIndexed { i, _ -> i != index })
    }

    fun onMemberNameChange(index: Int, value: String) = update {
        copy(members = members.mapIndexed { i, m -> if (i == index) m.copy(name = value) else m })
    }

    fun onMemberGmailChange(index: Int, value: String) = update {
        copy(members = members.mapIndexed { i, m -> if (i == index) m.copy(gmail = value) else m })
    }

    fun onMemberSeatRentChange(index: Int, value: String) = update {
        copy(members = members.mapIndexed { i, m -> if (i == index) m.copy(seatRent = value) else m })
    }

    /**
     * Creates the spreadsheet, writes all eight tabs and shares the file with
     * the members.
     */
    fun create() {
        val current = _state.value
        if (current.isBusy) return

        val name = current.messName.trim()
        if (name.isEmpty()) {
            update { copy(error = "Please enter a name for the mess.") }
            return
        }
        if (current.managerEmail.isBlank()) {
            update { copy(error = "Please sign in with Google first.") }
            return
        }

        viewModelScope.launch {
            update { copy(isBusy = true, error = null) }

            // The manager is always the first member (M001) – the meal grid and
            // the summary rows depend on that order.
            val blueprintMembers = mutableListOf(
                BlueprintMember(
                    memberId = "M001",
                    name = current.managerName.trim().ifEmpty { current.managerEmail.substringBefore('@') },
                    gmail = current.managerEmail.trim(),
                    role = Role.MANAGER,
                    seatRent = 0.0,
                ),
            )

            current.members
                .filter { it.gmail.isNotBlank() }
                .forEachIndexed { index, draft ->
                    blueprintMembers += BlueprintMember(
                        memberId = "M" + (index + 2).toString().padStart(3, '0'),
                        name = draft.name.trim().ifEmpty { draft.gmail.substringBefore('@') },
                        gmail = draft.gmail.trim(),
                        role = Role.MEMBER,
                        seatRent = draft.seatRent.toDoubleOrNull() ?: 0.0,
                    )
                }

            val blueprint = MessBlueprint(
                messName = name,
                monthYear = current.month,
                managerName = current.managerName.trim(),
                managerEmail = current.managerEmail.trim(),
                members = blueprintMembers,
                breakfastWeight = current.breakfastWeight,
                breakfastCutoff = current.breakfastCutoff,
                lunchCutoff = current.lunchCutoff,
                dinnerCutoff = current.dinnerCutoff,
            )

            messRepository.createMess(blueprint).fold(
                onSuccess = { update { copy(isBusy = false, created = true) } },
                onFailure = { error ->
                    update { copy(isBusy = false, error = error.message ?: "Could not create the mess.") }
                },
            )
        }
    }

    fun dismissError() = update { copy(error = null) }

    private inline fun update(transform: CreateMessState.() -> CreateMessState) {
        _state.value = _state.value.transform()
    }
}

/** The state of the "create a mess" form. */
data class CreateMessState(
    val messName: String = "",
    val month: YearMonth = YearMonth.now(),
    val managerName: String = "",
    val managerEmail: String = "",
    val breakfastWeight: Double = 1.0,
    val breakfastCutoff: LocalTime = LocalTime.of(7, 0),
    val lunchCutoff: LocalTime = LocalTime.of(10, 0),
    val dinnerCutoff: LocalTime = LocalTime.of(17, 0),
    val members: List<MemberDraft> = emptyList(),
    val isBusy: Boolean = false,
    val error: String? = null,
    val created: Boolean = false,
)

/** A member that is added while creating the mess. */
data class MemberDraft(
    val name: String = "",
    val gmail: String = "",
    val seatRent: String = "",
)
