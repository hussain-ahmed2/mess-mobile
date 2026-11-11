package com.hussain.messmobile.ui.screens.attendance

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
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.ui.components.DatePickerField
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.IconAction
import com.hussain.messmobile.ui.components.MemberAvatar
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.MealsViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** Marks who ate on the meals of a day. */
@Composable
fun AttendanceScreen(
    modifier: Modifier = Modifier,
    viewModel: MealsViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val meals by viewModel.meals.collectAsState()
    val members by viewModel.members.collectAsState()
    val selectedDate by viewModel.selectedDateMillis.collectAsState()
    val entries by viewModel.selectedMealEntries.collectAsState()

    val selectedMealId by viewModel.selectedMealId.collectAsState()
    val selectedMeal = meals.firstOrNull { it.meal.id == selectedMealId }

    LaunchedEffect(meals.size, selectedMealId) {
        if (selectedMealId == null && meals.isNotEmpty()) {
            viewModel.selectMeal(meals.first().meal.id)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.three),
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
                    title = "Nothing to mark",
                    message = "There is no meal on this day, add one first.",
                )
            }
        }

        items(meals, key = { it.meal.id }) { mealWithEntries ->
            val meal = mealWithEntries.meal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.selectMeal(meal.id)
                    },
            ) {
                SectionCard(title = meal.type.label) {
                    if (meal.description.isNotBlank()) {
                        ThemedText(
                            text = meal.description,
                            style = AppTextStyle.Default,
                            color = appColors.textSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.two))
                    ThemedText(
                        text = "${mealWithEntries.eating} of ${members.size} ate",
                        style = AppTextStyle.Small,
                        color = appColors.textSecondary,
                    )
                }
            }
        }

        if (selectedMeal != null) {
            item {
                SectionCard(title = "Who ate the ${selectedMeal.meal.type.label.lowercase()}?") {
                    if (members.isEmpty()) {
                        EmptyState(
                            title = "No members",
                            message = "Add the members of the mess first.",
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.two)) {
                            members.forEach { member ->
                                val entry = entries.firstOrNull { it.memberId == member.id }
                                val eaten = entry?.eaten == true
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.three),
                                ) {
                                    MemberAvatar(name = member.name, size = 32)
                                    Column(modifier = Modifier.weight(1f)) {
                                        ThemedText(text = member.name, style = AppTextStyle.Default)
                                        TextButton(
                                            onClick = {
                                                viewModel.setOptIn(
                                                    selectedMeal.meal.id,
                                                    member.id,
                                                    !(entry?.optedIn == true),
                                                )
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                        ) {
                                            ThemedText(
                                                text = if (entry?.optedIn == true) {
                                                    "Signed up"
                                                } else {
                                                    "Not signed up"
                                                },
                                                style = AppTextStyle.Small,
                                                color = appColors.textSecondary,
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = eaten,
                                        onCheckedChange = { checked ->
                                            viewModel.setEaten(selectedMeal.meal.id, member.id, checked)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
