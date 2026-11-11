package com.hussain.messmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hussain.messmobile.R
import com.hussain.messmobile.data.model.MemberRole
import com.hussain.messmobile.ui.theme.AppTextStyle
import com.hussain.messmobile.ui.theme.Spacing
import com.hussain.messmobile.ui.theme.appColors
import com.hussain.messmobile.util.DateUtils

/** A card with a title and an optional action on the right. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.backgroundElement),
        shape = RoundedCornerShape(Spacing.four),
    ) {
        Column(modifier = Modifier.padding(Spacing.four)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThemedText(text = title, style = AppTextStyle.Subtitle)
                action?.invoke()
            }
            Spacer(modifier = Modifier.height(Spacing.three))
            content()
        }
    }
}

/** A small box with a number and a caption. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(appColors.backgroundSelected, RoundedCornerShape(Spacing.three))
            .padding(horizontal = Spacing.three, vertical = Spacing.two),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThemedText(text = value, style = AppTextStyle.Subtitle, textAlign = TextAlign.Center)
        ThemedText(
            text = label,
            style = AppTextStyle.Small,
            color = appColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shown when a list is empty. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.six, horizontal = Spacing.four),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThemedText(text = title, style = AppTextStyle.Subtitle, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(Spacing.two))
        ThemedText(
            text = message,
            style = AppTextStyle.Default,
            color = appColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Asks the user to confirm something that cannot be undone. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ThemedText(text = title, style = AppTextStyle.Subtitle) },
        text = { ThemedText(text = text, style = AppTextStyle.Default) },
        confirmButton = {
            TextButton(onClick = onConfirm) { ThemedText(text = confirmLabel, style = AppTextStyle.LinkPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { ThemedText(text = "Cancel", style = AppTextStyle.Link) }
        },
    )
}

/** The round avatar with the first letter of a member. */
@Composable
fun MemberAvatar(name: String, modifier: Modifier = Modifier, size: Int = 40) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .size(size.dp)
            .background(appColors.backgroundSelected, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        ThemedText(text = initial, style = AppTextStyle.Subtitle)
    }
}

/** A small badge that shows the role of a member. */
@Composable
fun RoleBadge(role: MemberRole, modifier: Modifier = Modifier) {
    val color = when (role) {
        MemberRole.Admin -> appColors.text
        MemberRole.Cook -> appColors.textSecondary
        MemberRole.Member -> appColors.textSecondary
    }
    Box(
        modifier = modifier
            .background(appColors.backgroundSelected, RoundedCornerShape(Spacing.two))
            .padding(horizontal = Spacing.two, vertical = Spacing.half),
    ) {
        ThemedText(text = role.label, style = AppTextStyle.Small, color = color)
    }
}

/** A button that opens the date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    millis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showPicker = true }, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(Spacing.two))
        ThemedText(text = label, style = AppTextStyle.Default)
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = DateUtils.toUtcMillis(millis))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateSelected(DateUtils.fromUtcMillis(it)) }
                        showPicker = false
                    },
                ) {
                    ThemedText(text = "OK", style = AppTextStyle.LinkPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    ThemedText(text = "Cancel", style = AppTextStyle.Link)
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** A text field for an amount of money. */
@Composable
fun AmountField(
    label: String,
    amount: Double?,
    onAmountChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = amount?.toString() ?: "",
        onValueChange = { text ->
            onAmountChange(text.replace(',', '.').toDoubleOrNull())
        },
        label = { ThemedText(text = label, style = AppTextStyle.Default) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

/** An icon button that runs an action. */
@Composable
fun IconAction(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = appColors.text,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = tint,
        )
    }
}
