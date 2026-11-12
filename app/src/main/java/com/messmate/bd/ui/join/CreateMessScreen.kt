package com.messmate.bd.ui.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messmate.bd.R
import com.messmate.bd.ui.components.ErrorBanner
import com.messmate.bd.ui.components.LabeledField
import com.messmate.bd.ui.components.SectionCard
import com.messmate.bd.ui.components.SettingRow
import com.messmate.bd.ui.components.TimeField
import com.messmate.bd.ui.util.Formatters
import java.time.YearMonth

/**
 * The "create a new mess" form.
 *
 * Everything that is entered here is written into the generated Google Sheet:
 * the mess name, the month, the cut-off times and the first members.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMessScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateMessViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The mess is ready – the root switches to the dashboard automatically.
    LaunchedEffect(state.created) {
        if (state.created) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_create_mess)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.error != null) {
                ErrorBanner(message = state.error.orEmpty())
            }

            SectionCard(title = stringResource(R.string.create_mess_details)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(
                        value = state.messName,
                        onValueChange = viewModel::onMessNameChange,
                        label = stringResource(R.string.mess_name),
                        placeholder = stringResource(R.string.mess_name_hint),
                    )
                    LabeledField(
                        value = state.managerName,
                        onValueChange = viewModel::onManagerNameChange,
                        label = stringResource(R.string.manager_name),
                    )
                    LabeledField(
                        value = state.managerEmail,
                        onValueChange = {},
                        label = stringResource(R.string.manager_email),
                        enabled = false,
                    )
                }
            }

            SectionCard(title = stringResource(R.string.month)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        YearMonth.now().minusMonths(1),
                        YearMonth.now(),
                        YearMonth.now().plusMonths(1),
                    ).forEach { month ->
                        FilterChip(
                            selected = state.month == month,
                            onClick = { viewModel.onMonthChange(month) },
                            label = { Text(Formatters.month(month)) },
                        )
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_cutoffs)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField(
                        label = stringResource(R.string.meal_breakfast),
                        time = state.breakfastCutoff,
                        onTimeChange = viewModel::onBreakfastCutoffChange,
                    )
                    TimeField(
                        label = stringResource(R.string.meal_lunch),
                        time = state.lunchCutoff,
                        onTimeChange = viewModel::onLunchCutoffChange,
                    )
                    TimeField(
                        label = stringResource(R.string.meal_dinner),
                        time = state.dinnerCutoff,
                        onTimeChange = viewModel::onDinnerCutoffChange,
                    )

                    SettingRow(
                        label = stringResource(R.string.settings_breakfast_weight),
                        supportingText = stringResource(
                            R.string.meal_weight_hint,
                            if (state.breakfastWeight == 0.5) "0.5" else "1",
                        ),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.5, 1.0).forEach { weight ->
                                FilterChip(
                                    selected = state.breakfastWeight == weight,
                                    onClick = { viewModel.onBreakfastWeightChange(weight) },
                                    label = { Text(if (weight == 0.5) "0.5" else "1") },
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(
                title = stringResource(R.string.members_title),
                subtitle = stringResource(R.string.members_add_hint),
                trailing = {
                    OutlinedButton(onClick = viewModel::addMember) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add))
                    }
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.members.isEmpty()) {
                        Text(
                            text = stringResource(R.string.members_add_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.members.forEachIndexed { index, member ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LabeledField(
                                    value = member.name,
                                    onValueChange = { viewModel.onMemberNameChange(index, it) },
                                    label = stringResource(R.string.members_field_name),
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.removeMember(index) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                }
                            }
                            LabeledField(
                                value = member.gmail,
                                onValueChange = { viewModel.onMemberGmailChange(index, it) },
                                label = stringResource(R.string.members_field_gmail),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            )
                            LabeledField(
                                value = member.seatRent,
                                onValueChange = { viewModel.onMemberSeatRentChange(index, it) },
                                label = stringResource(R.string.utilities_seat_rent),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::create,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.action_create_mess))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
