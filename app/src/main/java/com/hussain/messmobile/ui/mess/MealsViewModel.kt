package com.hussain.messmobile.ui.mess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.data.local.entity.MealEntryEntity
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.model.MealType
import com.hussain.messmobile.data.repository.MessRepository
import com.hussain.messmobile.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A meal of the selected day together with the people who signed up for it. */
data class MealWithEntries(
    val meal: MealEntity,
    val entries: List<MealEntryEntity>,
) {
    val eating: Int get() = entries.count { it.eaten }
    val optedIn: Int get() = entries.count { it.optedIn }
}

/** The meals of a day and the attendance on them. */
@OptIn(ExperimentalCoroutinesApi::class)
class MealsViewModel(private val repository: MessRepository) : ViewModel() {

    private val selectedDate = MutableStateFlow(DateUtils.startOfToday())
    private val selectedMealId = MutableStateFlow<Long?>(null)

    val selectedDateMillis: StateFlow<Long> = selectedDate

    val selectedMealId: StateFlow<Long?> = selectedMealId

    val members: StateFlow<List<MemberEntity>> = repository.activeMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** The meals of the selected day. */
    val meals: StateFlow<List<MealWithEntries>> = combine(
        selectedDate.flatMapLatest { date -> repository.mealsForDate(date) },
        selectedDate.flatMapLatest { date -> repository.mealEntriesForDate(date) },
    ) { meals, entries ->
        val byMeal = entries.groupBy { it.mealId }
        meals.map { meal -> MealWithEntries(meal, byMeal[meal.id].orEmpty()) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** The entries of the meal that is currently open. */
    val selectedMealEntries: StateFlow<List<MealEntryEntity>> = selectedMealId
        .flatMapLatest { mealId ->
            if (mealId == null) flowOf(emptyList()) else repository.entriesForMeal(mealId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun selectDate(dateMillis: Long) {
        selectedDate.value = dateMillis
    }

    fun shiftDate(days: Long) {
        selectedDate.value = DateUtils.startOfDay(
            DateUtils.toLocalDate(selectedDate.value).plusDays(days),
        )
    }

    fun selectMeal(mealId: Long?) {
        selectedMealId.value = mealId
    }

    fun addMeal(
        type: MealType,
        description: String,
        cookId: Long?,
        totalCost: Double,
        onDone: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            val id = repository.addMeal(selectedDate.value, type, description, cookId, totalCost)
            selectedMealId.value = id
            onDone(id)
        }
    }

    fun updateMeal(meal: MealEntity, description: String, cookId: Long?, totalCost: Double) {
        viewModelScope.launch {
            repository.updateMeal(
                meal.copy(
                    description = description.trim(),
                    cookId = cookId,
                    totalCost = totalCost,
                ),
            )
        }
    }

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
            if (selectedMealId.value == meal.id) selectedMealId.value = null
        }
    }

    fun setOptIn(mealId: Long, memberId: Long, optedIn: Boolean) {
        viewModelScope.launch { repository.setOptIn(mealId, memberId, optedIn) }
    }

    fun setEaten(mealId: Long, memberId: Long, eaten: Boolean) {
        viewModelScope.launch { repository.setEaten(mealId, memberId, eaten) }
    }

    fun splitCost(mealId: Long) {
        viewModelScope.launch { repository.splitMealCost(mealId) }
    }
}
