package com.hussain.messmobile.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.R
import com.hussain.messmobile.ui.components.ConfirmDialog
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.ExpenseDialog
import com.hussain.messmobile.ui.components.IconAction
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.StatTile
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.ExpensesViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** The shared expenses of the mess. */
@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val expenses by viewModel.expenses.collectAsState()
    val totalThisMonth by viewModel.totalThisMonth.collectAsState()
    val members by viewModel.members.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.two),
    ) {
        item {
            SectionCard(title = "This month") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.two)) {
                    StatTile(
                        label = "Spent",
                        value = DateUtils.formatMoney(totalThisMonth),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Entries",
                        value = expenses.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                EmptyState(
                    title = "No expenses yet",
                    message = "Add what the mess spends on groceries, power or repairs.",
                )
            }
        }

        items(expenses, key = { it.id }) { expense ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.two),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.three),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ThemedText(text = expense.description, style = AppTextStyle.Default)
                    ThemedText(
                        text = "${expense.category.label} · ${DateUtils.formatShortDate(expense.date)}",
                        style = AppTextStyle.Small,
                        color = appColors.textSecondary,
                    )
                    if (expense.paidByMemberId != null) {
                        val payer = members.firstOrNull { it.id == expense.paidByMemberId }
                        if (payer != null) {
                            ThemedText(
                                text = "Paid by ${payer.name}",
                                style = AppTextStyle.Small,
                                color = appColors.textSecondary,
                            )
                        }
                    }
                }
                ThemedText(text = DateUtils.formatMoney(expense.amount), style = AppTextStyle.Default)

                IconAction(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete expense",
                    onClick = { deleting = expense.id },
                )
            }
        }

        item {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.padding(Spacing.three),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add expense",
                )
            }
        }
    }

    if (showDialog) {
        ExpenseDialog(
            members = members,
            onDismiss = { showDialog = false },
            onSave = { description, amount, category, date, paidByMemberId, shared ->
                viewModel.addExpense(description, amount, category, date, paidByMemberId, shared)
                showDialog = false
            },
        )
    }

    deleting?.let { id ->
        val expense = expenses.firstOrNull { it.id == id }
        ConfirmDialog(
            title = "Delete the expense?",
            text = expense?.description ?: "This entry is removed from the list.",
            confirmLabel = "Delete",
            onConfirm = {
                expense?.let { viewModel.deleteExpense(it) }
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
