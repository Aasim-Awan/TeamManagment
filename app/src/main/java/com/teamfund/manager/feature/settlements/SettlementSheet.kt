package com.teamfund.manager.feature.settlements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.core.util.MoneyFormatter
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleDebtSheet(
    playerSummary: PlayerFinancialSummary,
    currencyCode: String,
    expenses: List<Expense>,
    onDismiss: () -> Unit,
    onSettle: (Long, String, Boolean, Long?) -> Unit,
    initialExpenseId: Long? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialAmount = if (playerSummary.unpaidExpenseSharesMinorUnits > 0L) 
        playerSummary.unpaidExpenseSharesMinorUnits 
    else 
        kotlin.math.abs(playerSummary.netBalanceMinorUnits)

    var amountText by remember {
        mutableStateOf(MoneyFormatter.fromMinorUnits(initialAmount))
    }
    var note by remember { mutableStateOf("Settlement") }

    // true = player pays team fund; false = team fund pays player
    var isIncoming by remember { 
        mutableStateOf(playerSummary.unpaidExpenseSharesMinorUnits > 0L || playerSummary.netBalanceMinorUnits < 0) 
    }

    var selectedExpenseId by remember {
        mutableStateOf<Long?>(
            initialExpenseId ?: expenses.firstOrNull()?.id
        )
    }
    var expenseExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedExpenseId) {
        val selectedExpense = expenses.find { it.id == selectedExpenseId }
        val playerShare = selectedExpense?.shares?.find { it.playerId == playerSummary.player.id }

        if (selectedExpense != null) {
            note = "Settled: ${selectedExpense.title}"
            if (playerShare != null) {
                amountText = MoneyFormatter.fromMinorUnits(playerShare.remainingAmountMinorUnits)
            }
        } else {
            note = "General Settlement"
            amountText =
                MoneyFormatter.fromMinorUnits(kotlin.math.abs(playerSummary.netBalanceMinorUnits))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Settle with ${playerSummary.player.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (playerSummary.netBalanceMinorUnits < 0) "Member owes team fund" else "Team fund owes member",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Flow Direction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isIncoming,
                    onClick = { isIncoming = true },
                    label = { Text("Player → Team") },
                    leadingIcon = if (isIncoming) {
                        {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null)
                FilterChip(
                    selected = !isIncoming,
                    onClick = { isIncoming = false },
                    label = { Text("Team → Player") },
                    leadingIcon = if (!isIncoming) {
                        {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null)
            }

            // Expense Selector
            ExposedDropdownMenuBox(
                expanded = expenseExpanded, onExpandedChange = {
                    if (expenses.isNotEmpty()) expenseExpanded = !expenseExpanded
                }, modifier = Modifier.fillMaxWidth()
            ) {
                val expenseName = when {
                    expenses.isEmpty() -> "General (No Expenses)"
                    selectedExpenseId == null -> "General Settlement (None)"
                    else -> expenses.find { it.id == selectedExpenseId }?.title ?: "Expense"
                }

                OutlinedTextField(
                    value = expenseName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expense to Settle") },
                    trailingIcon = {
                        if (expenses.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    supportingText = {
                        if (expenses.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("No expenses recorded. This will be a general settlement.")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                if (expenses.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expenseExpanded,
                        onDismissRequest = { expenseExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("General / No Expense", fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedExpenseId = null
                                expenseExpanded = false
                            })
                        HorizontalDivider()
                        for (expense in expenses) {
                            val playerShare =
                                expense.shares.find { it.playerId == playerSummary.player.id }
                            val remainingText = if (playerShare != null) {
                                " (Remaining: ${MoneyFormatter.fromMinorUnits(playerShare.remainingAmountMinorUnits)} $currencyCode)"
                            } else ""

                            DropdownMenuItem(text = {
                                Column {
                                    Text(
                                        text = expense.title + remainingText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${DateTimeUtils.formatDate(expense.date)} • ${expense.category.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }, onClick = {
                                selectedExpenseId = expense.id
                                expenseExpanded = false
                            })
                        }
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Settlement Amount ($currencyCode)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = {
                        val current = amountText.toDoubleOrNull() ?: 0.0
                        if (current > 0) amountText =
                            String.format(Locale.US, "%.2f", (current - 1.0).coerceAtLeast(0.0))
                    }) {
                        Text(
                            "-",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    IconButton(onClick = {
                        val current = amountText.toDoubleOrNull() ?: 0.0
                        amountText = String.format(Locale.US, "%.2f", (current + 1.0))
                    }) {
                        Text(
                            "+",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Reference Note") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Button(
                onClick = {
                    val amountMinor = MoneyFormatter.toMinorUnits(amountText)
                    if (amountMinor > 0) {
                        onSettle(amountMinor, note, isIncoming, selectedExpenseId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amountText.isNotEmpty(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Confirm Settlement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onDismiss, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
