package com.messmate.bd.ui.members

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messmate.bd.R
import com.messmate.bd.domain.model.Deposit
import com.messmate.bd.domain.model.DepositMethod
import com.messmate.bd.domain.model.DepositStatus
import com.messmate.bd.domain.model.Member
import com.messmate.bd.ui.components.LabeledField
import com.messmate.bd.ui.components.SectionCard
import com.messmate.bd.ui.components.TopBarActions
import com.messmate.bd.ui.util.Formatters
import com.messmate.bd.ui.util.QrCodeGenerator

/**
 * The members screen: the roster, the seat rents, the deposits and the invite
 * QR code that new members scan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    onOpenAudit: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = hiltViewModel(),
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val statements by viewModel.statements.collectAsStateWithLifecycle()
    val deposits by viewModel.deposits.collectAsStateWithLifecycle()
    val mess by viewModel.mess.collectAsStateWithLifecycle()
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

    var addDialogVisible by remember { mutableStateOf(false) }
    var depositDialogVisible by remember { mutableStateOf(false) }
    var qrDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.members_title)) },
                actions = {
                    IconButton(onClick = { qrDialogVisible = true }) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = stringResource(R.string.members_invite_qr))
                    }
                    TopBarActions(onOpenAudit = onOpenAudit, onOpenSettings = onOpenSettings)
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addDialogVisible = true }) {
                Icon(Icons.Outlined.GroupAdd, contentDescription = stringResource(R.string.add_member))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(
                    title = stringResource(R.string.members_title),
                    subtitle = stringResource(R.string.members_add_hint),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        members.forEach { member ->
                            MemberRow(
                                member = member,
                                balance = statements.firstOrNull { it.member.memberId == member.memberId }
                                    ?.netBalance
                                    ?: 0.0,
                                isManager = isManager,
                                onSeatRentChange = { viewModel.updateSeatRent(member.memberId, it) },
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = stringResource(R.string.deposits_title),
                    trailing = {
                        OutlinedButton(onClick = { depositDialogVisible = true }) {
                            Text(stringResource(R.string.action_add))
                        }
                    },
                ) {
                    if (deposits.isEmpty()) {
                        Text(
                            text = stringResource(R.string.label_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            deposits.forEach { deposit ->
                                DepositRow(
                                    deposit = deposit,
                                    isManager = isManager,
                                    onVerify = { viewModel.resolveDeposit(deposit.depositId, true) },
                                    onReject = { viewModel.resolveDeposit(deposit.depositId, false) },
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (addDialogVisible) {
        AddMemberDialog(
            onDismiss = { addDialogVisible = false },
            onConfirm = { name, gmail, rent ->
                viewModel.addMember(name, gmail, rent)
                addDialogVisible = false
            },
        )
    }

    if (depositDialogVisible) {
        AddDepositDialog(
            members = members,
            onDismiss = { depositDialogVisible = false },
            onConfirm = { memberId, amount, method, trxId ->
                viewModel.addDeposit(memberId, amount, method, trxId)
                depositDialogVisible = false
            },
        )
    }

    if (qrDialogVisible) {
        val spreadsheetId = mess?.spreadsheetId.orEmpty()
        InviteQrDialog(
            link = QrCodeGenerator.joinLink(spreadsheetId),
            onDismiss = { qrDialogVisible = false },
        )
    }
}

/**
 * One member with the seat rent and the balance.
 *
 * Tapping the rent opens the edit dialog – the rent is written to the sheet
 * with a single cell update.
 */
@Composable
private fun MemberRow(
    member: Member,
    balance: Double,
    isManager: Boolean,
    onSeatRentChange: (Double) -> Unit,
) {
    var editDialogVisible by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${member.name} (${member.memberId})",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${if (member.isManager) stringResource(R.string.role_manager_short) else stringResource(R.string.role_member_short)}" +
                    " · ${Formatters.money(balance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isManager) {
            OutlinedButton(onClick = { editDialogVisible = true }) {
                Text(text = Formatters.money(member.seatRent))
            }
        } else {
            Text(text = Formatters.money(member.seatRent))
        }
    }

    if (editDialogVisible) {
        SeatRentDialog(
            member = member,
            onDismiss = { editDialogVisible = false },
            onConfirm = { rent ->
                onSeatRentChange(rent)
                editDialogVisible = false
            },
        )
    }
}

/**
 * The dialog that changes the seat rent of a member.
 */
@Composable
private fun SeatRentDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var seatRent by remember(member.memberId) {
        mutableStateOf(member.seatRent.takeIf { it > 0 }?.toInt()?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${member.name} · ${stringResource(R.string.utilities_seat_rent)}") },
        text = {
            LabeledField(
                value = seatRent,
                onValueChange = { seatRent = it.filter { char -> char.isDigit() || char == '.' } },
                label = stringResource(R.string.utilities_seat_rent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(seatRent.toDoubleOrNull() ?: 0.0) },
                enabled = (seatRent.toDoubleOrNull() ?: 0.0) >= 0.0,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * One deposit with the verify/reject buttons.
 */
@Composable
private fun DepositRow(
    deposit: Deposit,
    isManager: Boolean,
    onVerify: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${deposit.memberId} · ${Formatters.money(deposit.amount)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${deposit.method.sheetValue}" +
                    (if (deposit.trxId.isNotEmpty()) " · ${deposit.trxId}" else "") +
                    " · ${deposit.status.sheetValue}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (deposit.status == DepositStatus.PENDING && isManager) {
            IconButton(onClick = onVerify) {
                Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.deposits_verify))
            }
            IconButton(onClick = onReject) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.deposits_rejected))
            }
        }
    }
}

/**
 * The dialog that adds a member.
 */
@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, gmail: String, seatRent: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var gmail by remember { mutableStateOf("") }
    var seatRent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.members_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.members_field_name),
                )
                LabeledField(
                    value = gmail,
                    onValueChange = { gmail = it },
                    label = stringResource(R.string.members_field_gmail),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                LabeledField(
                    value = seatRent,
                    onValueChange = { seatRent = it.filter { char -> char.isDigit() || char == '.' } },
                    label = stringResource(R.string.utilities_seat_rent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name, gmail, seatRent.toDoubleOrNull() ?: 0.0)
                },
                enabled = gmail.contains("@"),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * The dialog that logs a deposit.
 */
@Composable
private fun AddDepositDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (memberId: String, amount: Double, method: DepositMethod, trxId: String) -> Unit,
) {
    var memberId by remember { mutableStateOf(members.firstOrNull()?.memberId.orEmpty()) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(DepositMethod.CASH) }
    var trxId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.deposits_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    members.take(4).forEach { member ->
                        FilterChip(
                            selected = memberId == member.memberId,
                            onClick = { memberId = member.memberId },
                            label = { Text(member.memberId) },
                        )
                    }
                }

                LabeledField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = stringResource(R.string.deposits_field_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DepositMethod.entries.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { method = option },
                            label = { Text(option.sheetValue) },
                        )
                    }
                }

                LabeledField(
                    value = trxId,
                    onValueChange = { trxId = it },
                    label = stringResource(R.string.deposits_field_trx),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(memberId, amount.toDoubleOrNull() ?: 0.0, method, trxId)
                },
                enabled = memberId.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * The invite QR code – new members scan it with the app.
 */
@Composable
private fun InviteQrDialog(link: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(link) { QrCodeGenerator.generate(link, 640) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.members_invite_qr)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.members_invite_qr),
                        modifier = Modifier.size(220.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.qr_invite_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
            ) { Text(stringResource(R.string.action_share)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
