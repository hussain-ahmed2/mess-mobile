package com.hussain.messmobile.ui.screens.meals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.R
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.ui.components.ConfirmDialog
import com.hussain.messmobile.ui.components.DatePickerField
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.IconAction
import com.hussain.messmobile.ui.components.MealDialog
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.MealsViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** The meals of a day. */
@Composable
fun MealsScreen(
    modifier: Modifier = Modifier,
    viewModel: MealsViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val meals by viewModel.meals.collectAsState()
    val members by viewModel.members.collectAsState()
    val selectedDate by viewModel.selectedDateMillis.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MealEntity?>(null) }
    var deleting by remember { mutableStateOf<MealEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.two),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.two),
            ) {
                DatePickerField(
                    label = DateUtils.formatLongDate(selectedDate),
                    millis = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                )
                IconAction(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "Previous day",
                    onClick = { viewModel.shiftDate(-1) },
                )
                IconAction(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "Next day",
                    onClick = { viewModel.shiftDate(1) },
                )
            }
        }

        if (meals.isEmpty()) {
            item {
                EmptyState(
                    title = "No meals yet",
                    message = "Add the breakfast, the lunch or the dinner of this day.",
                )
            }
        }

        items(meals, key = { it.meal.id }) { mealWithEntries ->
            val meal = mealWithEntries.meal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        editing = meal
                        showDialog = true
                    }
                    .padding(vertical = Spacing.two),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.three),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ThemedText(text = meal.type.label, style = AppTextStyle.Default)
                    if (meal.description.isNotBlank()) {
                        ThemedText(
                            text = meal.description,
                            style = AppTextStyle.Small,
                            color = appColors.textSecondary,
                        )
                    }
                    ThemedText(
                        text = "${mealWithEntries.optedIn} joined · ${mealWithEntries.eating} ate" +
                            if (meal.totalCost > 0.0) " · ${DateUtils.formatMoney(meal.totalCost)}" else "",
                        style = AppTextStyle.Small,
                        color = appColors.textSecondary,
                    )
                }

                Column(horizontalArrangement = Arrangement.spacedBy(Spacing.one)) {
                    if (meal.totalCost > 0.0 && mealWithEntries.optedIn > 0) {
                        TextButton(onClick = { viewModel.splitCost(meal.id) }) {
                            ThemedText(text = "Share the cost", style = AppTextStyle.LinkPrimary)
                        }
                    }
                    IconAction(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Delete meal",
                        onClick = { deleting = meal },
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(Spacing.two))
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add meal",
                )
            }
        }
    }

    if (showDialog) {
        val meal = editing
        MealDialog(
            members = members,
            meal = meal,
            onDismiss = { showDialog = false },
            onSave = { type, description, cookId, cost ->
                if (meal == null) {
                    viewModel.addMeal(type, description, cookId, cost)
                } else {
                    viewModel.updateMeal(meal, description, cookId, cost)
                }
                showDialog = false
            },
        )
    }

    deleting?.let { meal ->
        ConfirmDialog(
            title = "Delete the ${meal.type.label.lowercase()}?",
            text = "Everybody who signed up for this meal is removed from it.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteMeal(meal)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
