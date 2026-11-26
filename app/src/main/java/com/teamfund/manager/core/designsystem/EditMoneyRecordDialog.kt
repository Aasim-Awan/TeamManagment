package com.teamfund.manager.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teamfund.manager.core.util.MoneyFormatter

/**
 * Edits the amount, note and date of an existing money record (contribution, payment or charge).
 * The player and record type stay the same; delete and re-add to change those.
 */
@Composable
fun EditMoneyRecordDialog(
    title: String,
    initialAmountMinorUnits: Long,
    initialNote: String?,
    initialDate: Long,
    currencyCode: String,
    onSave: (amountMinorUnits: Long, note: String?, date: Long) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "Delete"
) {
    var amountText by rememberSaveable { mutableStateOf(MoneyFormatter.fromMinorUnits(initialAmountMinorUnits)) }
    var note by rememberSaveable { mutableStateOf(initialNote.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(initialDate) }
    val amount = MoneyFormatter.toMinorUnits(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TeamFundTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amount <= 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_record_amount")
                )
                DatePickerField(
                    selectedDateMillis = date,
                    onDateSelected = { date = it },
                    testTag = "edit_record_date"
                )
                TeamFundTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(amount, note.ifBlank { null }, date) }, enabled = amount > 0L) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(deleteLabel) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
