package com.messmate.bd.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messmate.bd.domain.calculation.CutoffPolicy
import com.messmate.bd.domain.model.MealType
import com.messmate.bd.domain.model.MemberStatement
import com.messmate.bd.domain.model.Mess
import com.messmate.bd.domain.model.MessConfig
import com.messmate.bd.domain.model.MessTotals
import com.messmate.bd.domain.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * The data of the dashboard.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val messRepository: MessRepository,
    private val cutoffPolicy: CutoffPolicy,
) : ViewModel() {

    /** Emits every 30 seconds so the cut-off countdown stays alive. */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    private val _events = MutableStateFlow<HomeEvent?>(null)
    val events: StateFlow<HomeEvent?> = _events.asStateFlow()

    val state: StateFlow<HomeUiState> = combine(
        combine(
            messRepository.observeMess(),
            messRepository.observeConfig(),
            messRepository.observeTotals(),
            messRepository.observeStatements(),
        ) { mess, config, totals, statements ->
            DashboardData(mess, config, totals, statements)
        },
        combine(
            messRepository.observeMeals(),
            messRepository.observePendingSyncCount(),
        ) { meals, pending -> meals to pending },
        ticker,
    ) { data, (meals, pending), _ ->
        data.toState(meals, pending, cutoffPolicy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /**
     * Turns a meal on or off.
     *
     * The repository enforces the cut-off rules – the UI only mirrors them so
     * that the button can be disabled in time.
     */
    fun toggleMeal(date: LocalDate, type: MealType, enabled: Boolean) {
        val memberId = state.value.myMemberId ?: return
        viewModelScope.launch {
            messRepository.setMeal(memberId, date, type, enabled).onFailure { error ->
                _events.value = HomeEvent.Message(error.message ?: "Could not change the meal.")
            }
        }
    }

    /** Sets the number of guest meals of a day. */
    fun setGuestMeals(date: LocalDate, count: Int) {
        val memberId = state.value.myMemberId ?: return
        viewModelScope.launch {
            messRepository.setGuestMeals(memberId, date, count).onFailure { error ->
                _events.value = HomeEvent.Message(error.message ?: "Could not update the guest meals.")
            }
        }
    }

    /** Pulls the latest data from the sheet. */
    fun refresh() {
        viewModelScope.launch {
            messRepository.refresh().onFailure { error ->
                _events.value = HomeEvent.Message(error.message ?: "Could not sync.")
            }
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}

/** One-shot events of the dashboard. */
sealed interface HomeEvent {
    data class Message(val text: String) : HomeEvent
}

/** The state of the dashboard. */
data class HomeUiState(
    val messName: String = "",
    val month: YearMonth = YearMonth.now(),
    val isManager: Boolean = false,
    val myMemberId: String? = null,
    val totals: MessTotals = MessTotals(0.0, 0.0, 0.0, 0.0),
    val myStatement: MemberStatement? = null,
    val today: List<MealToggleUi> = emptyList(),
    val tomorrow: List<MealToggleUi> = emptyList(),
    val pendingSync: Int = 0,
    val isLocked: Boolean = false,
    val isLoading: Boolean = true,
)

/** A meal switch on the dashboard. */
data class MealToggleUi(
    val type: MealType,
    val enabled: Boolean,
    /** `true` when the cut-off has passed – only the manager can still change it. */
    val locked: Boolean,
    /** Time left until the cut-off – `null` when it already passed. */
    val remaining: Duration?,
    val guestMeals: Int = 0,
)

/** The raw data the dashboard is built from. */
private data class DashboardData(
    val mess: Mess?,
    val config: MessConfig?,
    val totals: MessTotals,
    val statements: List<MemberStatement>,
)

/**
 * Builds the dashboard state out of the cached sheet data.
 *
 * All numbers come from the local cache (Room), which mirrors the `Summary`
 * tab – the app therefore shows the same numbers as the spreadsheet.
 */
private fun DashboardData.toState(
    meals: List<com.messmate.bd.domain.model.MealEntry>,
    pendingSync: Int,
    cutoffPolicy: CutoffPolicy,
): HomeUiState {
    val config = config
    val myMemberId = mess?.myMemberId
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    fun togglesFor(date: LocalDate): List<MealToggleUi> {
        if (config == null || myMemberId == null) return emptyList()
        val entry = meals.firstOrNull { it.memberId == myMemberId && it.date == date }

        return MealType.entries.map { type ->
            val remaining = cutoffPolicy.remainingUntilCutoff(config, type, date)
            MealToggleUi(
                type = type,
                enabled = when (type) {
                    MealType.BREAKFAST -> entry?.breakfast == 1
                    MealType.LUNCH -> entry?.lunch == 1
                    MealType.DINNER -> entry?.dinner == 1
                },
                locked = !cutoffPolicy.canMemberEdit(config, type, date),
                remaining = remaining,
                guestMeals = entry?.guestMeals ?: 0,
            )
        }
    }

    return HomeUiState(
        messName = mess?.name.orEmpty(),
        month = config?.monthYear ?: mess?.monthYear ?: YearMonth.now(),
        isManager = mess?.isManager == true,
        myMemberId = myMemberId,
        totals = totals,
        myStatement = statements.firstOrNull { it.member.memberId == myMemberId },
        today = togglesFor(today),
        tomorrow = togglesFor(tomorrow),
        pendingSync = pendingSync,
        isLocked = config?.isLocked == true,
        isLoading = config == null,
    )
}
