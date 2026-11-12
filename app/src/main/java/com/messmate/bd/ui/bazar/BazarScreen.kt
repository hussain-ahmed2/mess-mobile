package com.messmate.bd.ui.bazar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messmate.bd.R
import com.messmate.bd.ui.components.EmptyState
import com.messmate.bd.ui.components.LabeledField
import com.messmate.bd.ui.components.StatCard
import com.messmate.bd.ui.util.Formatters

/**
 * The bazar ledger screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BazarScreen(
    modifier: Modifier = Modifier,
    viewModel: BazarViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()
    var dialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.bazar_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogVisible = true }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_bazar))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            StatCard(
                label = stringResource(R.string.bazar_total),
                value = Formatters.money(total),
                icon = Icons.Outlined.ShoppingBasket,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )

            if (entries.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ShoppingBasket,
                    title = stringResource(R.string.label_empty),
                    message = stringResource(R.string.bazar_empty_hint),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries, key = { it.entryId }) { entry ->
                        BazarRow(
                            items = entry.items,
                            shopper = entry.shopperName,
                            date = entry.date,
                            amount = entry.amount,
                        )
                    }
                }
            }
        }
    }

    if (dialogVisible) {
        AddBazarDialog(
            onDismiss = { dialogVisible = false },
            onConfirm = { items, amount, shopper ->
                viewModel.add(items, amount, shopper)
                dialogVisible = false
            },
        )
    }
}

/**
 * One market trip.
 */
@Composable
private fun BazarRow(
    items: String,
    shopper: String,
    date: java.time.LocalDate,
    amount: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = items, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "$shopper · ${Formatters.dateShort(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = Formatters.money(amount), style = MaterialTheme.typography.titleSmall)
    }
}

/**
 * The dialog that adds a market trip.
 */
@Composable
private fun AddBazarDialog(
    onDismiss: () -> Unit,
    onConfirm: (items: String, amount: Double, shopper: String) -> Unit,
) {
    var items by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var shopper by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bazar_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    value = items,
                    onValueChange = { items = it },
                    label = stringResource(R.string.bazar_field_items),
                )
                LabeledField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = stringResource(R.string.bazar_field_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                LabeledField(
                    value = shopper,
                    onValueChange = { shopper = it },
                    label = stringResource(R.string.bazar_field_shopper),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(items, amount.toDoubleOrNull() ?: 0.0, shopper)
                },
                enabled = items.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
