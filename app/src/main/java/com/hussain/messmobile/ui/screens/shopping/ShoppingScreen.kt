package com.hussain.messmobile.ui.screens.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.messmobile.R
import com.hussain.messmobile.ui.components.EmptyState
import com.hussain.messmobile.ui.components.IconAction
import com.hussain.messmobile.ui.components.SectionCard
import com.hussain.messmobile.ui.components.ShoppingItemDialog
import com.hussain.messmobile.ui.components.StatTile
import com.hussain.messmobile.ui.components.ThemedText
import com.hussain.messmobile.ui.mess.ExpensesViewModel
import com.hussain.messmobile.ui.mess.ViewModelFactory
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** The shopping list of the mess. */
@Composable
fun ShoppingScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = viewModel(factory = ViewModelFactory.Factory),
) {
    val items by viewModel.shoppingItems.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val pending = items.filter { !it.purchased }
    val estimatedTotal = pending.sumOf { it.estimatedCost }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.four),
        verticalArrangement = Arrangement.spacedBy(Spacing.two),
    ) {
        item {
            SectionCard(title = "Shopping list") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.two)) {
                    StatTile(
                        label = "To buy",
                        value = pending.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Estimated",
                        value = DateUtils.formatMoney(estimatedTotal),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (items.any { it.purchased }) {
                    TextButton(onClick = { viewModel.clearPurchased() }) {
                        ThemedText(text = "Clear the bought items", style = AppTextStyle.LinkPrimary)
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing on the list",
                    message = "Add everything the mess has to buy.",
                )
            }
        }

        items(items, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.two),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.two),
            ) {
                Checkbox(
                    checked = item.purchased,
                    onCheckedChange = { checked ->
                        viewModel.setPurchased(item, checked)
                    },
                )
                Column(modifier = Modifier.weight(1f)) {
                    ThemedText(text = item.name, style = AppTextStyle.Default)
                    val details = buildList {
                        if (item.quantity.isNotBlank()) add(item.quantity)
                        if (item.estimatedCost > 0.0) add(DateUtils.formatMoney(item.estimatedCost))
                    }
                    if (details.isNotEmpty()) {
                        ThemedText(
                            text = details.joinToString(" · "),
                            style = AppTextStyle.Small,
                            color = appColors.textSecondary,
                        )
                    }
                }
                IconAction(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete item",
                    onClick = { viewModel.deleteShoppingItem(item) },
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
                    contentDescription = "Add item",
                )
            }
        }
    }

    if (showDialog) {
        ShoppingItemDialog(
            onDismiss = { showDialog = false },
            onSave = { name, quantity, cost ->
                viewModel.addShoppingItem(name, quantity, cost)
                showDialog = false
            },
        )
    }
}
