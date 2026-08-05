package com.teamfund.manager.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.teamfund.manager.core.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    label: String = "Date",
    modifier: Modifier = Modifier,
    testTag: String = "date_picker_field"
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = DateTimeUtils.formatAbsoluteDate(selectedDateMillis),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable { showPicker = true },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.localMillisToUtcPicker(selectedDateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utcMillis ->
                            onDateSelected(DateTimeUtils.utcPickerMillisToLocal(utcMillis))
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
