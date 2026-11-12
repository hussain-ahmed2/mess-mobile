package com.messmate.bd.ui.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.messmate.bd.domain.model.Member
import com.messmate.bd.domain.model.SplitType
import com.messmate.bd.domain.model.UtilityBill
import com.messmate.bd.domain.model.UtilityCategory
import com.messmate.bd.ui.components.EmptyState
import com.messmate.bd.ui.components.LabeledField
import com.messmate.bd.ui.components.StatCard
import com.messmate.bd.ui.components.TopBarActions
import com.messmate.bd.ui.util.Formatters

/**
 * The utilities & rent screen: cook salary, gas, electricity, Wi-Fi, …
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UtilitiesScreen(
    onOpenAudit: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UtilitiesViewModel = hiltViewModel(),
) {
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val equalTotal by viewModel.equalTotal.collectAsStateWithLifecycle()
    val customTotal by viewModel.customTotal.collectAsStateWithLifecycle()
    val isManager by viewModel.isManager.collectAsStateWithLifecycle()

    // Errors of the repository are shown as a snackbar – they are the only
    // feedback the user gets when a write fails (for example offline).
    val event by viewModel.events.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(event) {
        event?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeEvent()
        }
    }
    var dialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.utilities_title)) },
                actions = {
                    TopBarActions(onOpenAudit = onOpenAudit, onOpenSettings = onOpenSettings)
                },
            )
        },
        floatingActionButton = {
            // Only the manager logs the fixed costs.
            if (isManager) {
                FloatingActionButton(onClick = { dialogVisible = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_bill))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = stringResource(R.string.utilities_total_equal),
                    value = Formatters.moneyShort(equalTotal),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.utilities_split_custom),
                    value = Formatters.moneyShort(customTotal),
                    modifier = Modifier.weight(1f),
                )
            }

            if (bills.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Bolt,
                    title = stringResource(R.string.label_empty),
                    message = stringResource(R.string.utilities_title),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bills, key = { it.billId }) { bill ->
                        BillRow(bill = bill, members = members)
                    }
                }
            }
        }
    }

    if (dialogVisible) {
        AddBillDialog(
            members = members,
            onDismiss = { dialogVisible = false },
            onConfirm = { category, amount, splitType, target, notes ->
                viewModel.add(category, amount, splitType, target, notes)
                dialogVisible = false
            },
        )
    }
}

/**
 * One bill with its split type.
 */
@Composable
private fun BillRow(bill: UtilityBill, members: List<Member>) {
    val categoryLabel = when (bill.category) {
        UtilityCategory.COOK -> stringResource(R.string.cat_cook)
        UtilityCategory.ELECTRICITY -> stringResource(R.string.cat_electricity)
        UtilityCategory.GAS -> stringResource(R.string.cat_gas)
        UtilityCategory.WIFI -> stringResource(R.string.cat_wifi)
        UtilityCategory.WATER -> stringResource(R.string.cat_water)
        UtilityCategory.GARBAGE -> stringResource(R.string.cat_garbage)
        UtilityCategory.OTHER -> stringResource(R.string.cat_other)
    }

    val splitLabel = when (bill.splitType) {
        SplitType.EQUAL_ALL -> stringResource(R.string.utilities_split_equal)
        SplitType.CUSTOM_MEMBER -> {
            val name = members.firstOrNull { it.memberId == bill.targetMemberId }
                ?.name
                ?: bill.targetMemberId.orEmpty()
            stringResource(R.string.utilities_split_custom) + " · $name"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = categoryLabel, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = splitLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = Formatters.money(bill.amount), style = MaterialTheme.typography.titleSmall)
    }
}

/**
 * The dialog that adds a bill.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBillDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (UtilityCategory, Double, SplitType, String?, String) -> Unit,
) {
    var category by remember { mutableStateOf(UtilityCategory.COOK) }
    var amount by remember { mutableStateOf("") }
    var splitType by remember { mutableStateOf(SplitType.EQUAL_ALL) }
    var targetMemberId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.utilities_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.utilities_category),
                    style = MaterialTheme.typography.labelMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UtilityCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = {
                                Text(
                                    when (option) {
                                        UtilityCategory.COOK -> stringResource(R.string.cat_cook)
                                        UtilityCategory.ELECTRICITY -> stringResource(R.string.cat_electricity)
                                        UtilityCategory.GAS -> stringResource(R.string.cat_gas)
                                        UtilityCategory.WIFI -> stringResource(R.string.cat_wifi)
                                        UtilityCategory.WATER -> stringResource(R.string.cat_water)
                                        UtilityCategory.GARBAGE -> stringResource(R.string.cat_garbage)
                                        UtilityCategory.OTHER -> stringResource(R.string.cat_other)
                                    },
                                )
                            },
                        )
                    }
                }

                LabeledField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = stringResource(R.string.bazar_field_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                Text(
                    text = stringResource(R.string.utilities_split_type),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = splitType == SplitType.EQUAL_ALL,
                        onClick = { splitType = SplitType.EQUAL_ALL },
                        label = { Text(stringResource(R.string.utilities_split_equal)) },
                    )
                    FilterChip(
                        selected = splitType == SplitType.CUSTOM_MEMBER,
                        onClick = { splitType = SplitType.CUSTOM_MEMBER },
                        label = { Text(stringResource(R.string.utilities_split_custom)) },
                    )
                }

                if (splitType == SplitType.CUSTOM_MEMBER) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.filter { it.isActive }.forEach { member ->
                            FilterChip(
                                selected = targetMemberId == member.memberId,
                                onClick = { targetMemberId = member.memberId },
                                label = { Text(member.name) },
                            )
                        }
                    }
                }

                LabeledField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.utilities_notes),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(category, amount.toDoubleOrNull() ?: 0.0, splitType, targetMemberId, notes)
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    (splitType == SplitType.EQUAL_ALL || targetMemberId != null),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
