package com.hussain.messmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.model.ExpenseCategory
import com.hussain.messmobile.data.model.MealType
import com.hussain.messmobile.data.model.MemberRole
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** A list of options the user picks one of. */
@Composable
fun <T> OptionPicker(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ThemedText(
            text = label,
            style = AppTextStyle.Small,
            color = appColors.textSecondary,
        )
        Spacer(modifier = Modifier.size(Spacing.one))
        if (options.isEmpty()) {
            ThemedText(text = "Nothing to choose from yet.", style = AppTextStyle.Small)
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = Spacing.one),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OptionDot(selected = isSelected)
                        Spacer(modifier = Modifier.width(Spacing.two))
                        ThemedText(text = optionLabel(option), style = AppTextStyle.Default)
                    }
                }
            }
        }
    }
}

/** The little dot that shows whether an option is selected. */
@Composable
private fun OptionDot(selected: Boolean) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(18.dp)
            .background(
                color = if (selected) appColors.text else appColors.backgroundSelected,
                shape = CircleShape,
            ),
    )
}

/** Adds or edits a member of the mess. */
@Composable
fun MemberDialog(
    member: MemberEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String?, role: MemberRole) -> Unit,
) {
    var name by remember { mutableStateOf(member?.name.orEmpty()) }
    var phone by remember { mutableStateOf(member?.phone.orEmpty()) }
    var role by remember { mutableStateOf(member?.role ?: MemberRole.Member) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ThemedText(
                text = if (member == null) "Add member" else "Edit member",
                style = AppTextStyle.Subtitle,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.three)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { ThemedText(text = "Name", style = AppTextStyle.Default) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { ThemedText(text = "Phone (optional)", style = AppTextStyle.Default) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionPicker(
                    label = "Role",
                    options = MemberRole.entries.toList(),
                    selected = role,
                    optionLabel = { it.label },
                    onSelect = { role = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, phone, role) },
                enabled = name.isNotBlank(),
            ) {
                ThemedText(text = "Save", style = AppTextStyle.LinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}

/** Adds or edits a meal. */
@Composable
fun MealDialog(
    members: List<MemberEntity>,
    meal: MealEntity?,
    onDismiss: () -> Unit,
    onSave: (type: MealType, description: String, cookId: Long?, totalCost: Double) -> Unit,
) {
    var type by remember { mutableStateOf(meal?.type ?: MealType.Lunch) }
    var description by remember { mutableStateOf(meal?.description.orEmpty()) }
    var cookId by remember { mutableStateOf(meal?.cookId) }
    var cost by remember { mutableStateOf(meal?.totalCost) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ThemedText(
                text = if (meal == null) "Add meal" else "Edit meal",
                style = AppTextStyle.Subtitle,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.three)) {
                OptionPicker(
                    label = "Meal",
                    options = MealType.entries.toList(),
                    selected = type,
                    optionLabel = { it.label },
                    onSelect = { type = it },
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { ThemedText(text = "What is cooked", style = AppTextStyle.Default) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionPicker(
                    label = "Cook",
                    options = members,
                    selected = members.firstOrNull { it.id == cookId },
                    optionLabel = { it.name },
                    onSelect = { cookId = it.id },
                )
                AmountField(
                    label = "Total cost",
                    amount = cost,
                    onAmountChange = { cost = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(type, description, cookId, cost ?: 0.0) }) {
                ThemedText(text = "Save", style = AppTextStyle.LinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}

/** Sets what a member owes for the month. */
@Composable
fun DuesDialog(
    memberName: String,
    currentAmount: Double,
    onDismiss: () -> Unit,
    onSave: (amount: Double) -> Unit,
) {
    var amount by remember { mutableStateOf(currentAmount.takeIf { it > 0.0 }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ThemedText(text = "Dues of $memberName", style = AppTextStyle.Subtitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.three)) {
                ThemedText(
                    text = "How much does this member have to pay for the month?",
                    style = AppTextStyle.Default,
                    color = appColors.textSecondary,
                )
                AmountField(label = "Amount", amount = amount, onAmountChange = { amount = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(amount ?: 0.0) }) {
                ThemedText(text = "Save", style = AppTextStyle.LinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}

/** Records a payment of a member. */
@Composable
fun PaymentDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, paidAt: Long, note: String) -> Unit,
) {
    var amount by remember { mutableStateOf<Double?>(null) }
    var date by remember { mutableStateOf(DateUtils.startOfToday()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ThemedText(text = "Payment of $memberName", style = AppTextStyle.Subtitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.three)) {
                AmountField(label = "Amount", amount = amount, onAmountChange = { amount = it })
                DatePickerField(
                    label = DateUtils.formatLongDate(date),
                    millis = date,
                    onDateSelected = { date = it },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { ThemedText(text = "Note (optional)", style = AppTextStyle.Default) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(amount ?: 0.0, date, note) },
                enabled = (amount ?: 0.0) > 0.0,
            ) {
                ThemedText(text = "Save", style = AppTextStyle.LinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}

/** Adds a shared expense. */
@Composable
fun ExpenseDialog(
    members: List<MemberEntity>,
    onDismiss: () -> Unit,
    onSave: (
        description: String,
        amount: Double,
        category: ExpenseCategory,
        date: Long,
        paidByMemberId: Long?,
        shared: Boolean,
    ) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf<Double?>(null) }
    var category by remember { mutableStateOf(ExpenseCategory.Grocery) }
    var date by remember { mutableStateOf(DateUtils.startOfToday()) }
    var paidBy by remember { mutableStateOf<Long?>(null) }
    var shared by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ThemedText(text = "Add expense", style = AppTextStyle.Subtitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.three),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { ThemedText(text = "Description", style = AppTextStyle.Default) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField(label = "Amount", amount = amount, onAmountChange = { amount = it })
                OptionPicker(
                    label = "Category",
                    options = ExpenseCategory.entries.toList(),
                    selected = category,
                    optionLabel = { it.label },
                    onSelect = { category = it },
                )
                DatePickerField(
                    label = DateUtils.formatLongDate(date),
                    millis = date,
                    onDateSelected = { date = it },
                )
                OptionPicker(
                    label = "Paid by",
                    options = members,
                    selected = members.firstOrNull { it.id == paidBy },
                    optionLabel = { it.name },
                    onSelect = { paidBy = it.id },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { shared = !shared },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OptionDot(selected = shared)
                    Spacer(modifier = Modifier.width(Spacing.two))
                    ThemedText(text = "Split between everybody", style = AppTextStyle.Default)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(description, amount ?: 0.0, category, date, paidBy, shared) },
                enabled = description.isNotBlank() && (amount ?: 0.0) > 0.0,
            ) {
                ThemedText(text = "Save", style = AppTextStyle.LinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}

/** Adds an item to the shopping list. */
@Composable
fun ShoppingItemDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: String, estimatedCost: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf<Double?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ThemedText(text = "Add item", style = AppTextStyle.Subtitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.three)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { ThemedText(text = "Item", style = AppTextStyle.Default) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { ThemedText(text = "Quantity (optional)", style = AppTextStyle.Default) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField(label = "Estimated cost", amount = cost, onAmountChange = { cost = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, quantity, cost ?: 0.0) },
                enabled = name.isNotBlank(),
            ) {
                ThemedText(text = "Save", style = AppTextStyle.LinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}

/** Adds an announcement for the whole mess. */
@Composable
fun AnnouncementDialog(
    announcement: AnnouncementEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String) -> Unit,
) {
    var title by remember { mutableStateOf(announcement?.title.orEmpty()) }
    var body by remember { mutableStateOf(announcement?.body.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ThemedText(
                text = if (announcement == null) "New announcement" else "Edit announcement",
                style = AppTextStyle.Subtitle,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.three)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { ThemedText(text = "Title", style = AppTextStyle.Default) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { ThemedText(text = "Message", style = AppTextStyle.Default) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, body) },
                enabled = title.isNotBlank(),
            ) {
                ThemedText(text = "Save", style = AppTextStyle.LinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}
