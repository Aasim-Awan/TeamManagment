package com.teamfund.manager.feature.settlements

import com.teamfund.manager.core.designsystem.TeamFundTextField
import com.teamfund.manager.core.designsystem.TeamFundButton
import com.teamfund.manager.core.designsystem.TeamFundFilterChip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
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
import com.teamfund.manager.domain.model.SettleAdjustmentMode
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleDebtSheet(
    playerSummary: PlayerFinancialSummary,
    currencyCode: String,
    expenses: List<Expense>,
    onDismiss: () -> Unit,
    onSettle: (Long, String, SettleAdjustmentMode, Long?) -> Unit,
    initialExpenseId: Long? = null,
    isSaving: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialAmount = playerSummary.amountTeamShouldCollectMinorUnits
        .takeIf { it > 0L }
        ?: playerSummary.amountTeamShouldPayMinorUnits

    var amountText by remember {
        mutableStateOf(MoneyFormatter.fromMinorUnits(initialAmount))
    }
    var note by remember { mutableStateOf("Settlement") }

    var adjustmentMode by remember {
        mutableStateOf(
            when {
                playerSummary.positionMinorUnits < 0L -> SettleAdjustmentMode.PLAYER_PAID
                playerSummary.positionMinorUnits > 0L -> SettleAdjustmentMode.TEAM_PAID_PLAYER
                else -> SettleAdjustmentMode.PLAYER_PAID
            }
        )
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
            val fallback = playerSummary.amountTeamShouldCollectMinorUnits
                .takeIf { it > 0L }
                ?: playerSummary.amountTeamShouldPayMinorUnits
            amountText = MoneyFormatter.fromMinorUnits(fallback)
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
                        text = when {
                            playerSummary.positionMinorUnits < 0 -> "Collect ${MoneyFormatter.format(playerSummary.amountTeamShouldCollectMinorUnits, currencyCode)}"
                            playerSummary.positionMinorUnits > 0 -> "Pay player ${MoneyFormatter.format(playerSummary.amountTeamShouldPayMinorUnits, currencyCode)}"
                            else -> "Settled"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamFundFilterChip(
                        selected = adjustmentMode == SettleAdjustmentMode.PLAYER_PAID,
                        onClick = { adjustmentMode = SettleAdjustmentMode.PLAYER_PAID },
                        label = { Text("Player paid") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (adjustmentMode == SettleAdjustmentMode.PLAYER_PAID) {
                            {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                    TeamFundFilterChip(
                        selected = adjustmentMode == SettleAdjustmentMode.PLAYER_OWES,
                        onClick = { adjustmentMode = SettleAdjustmentMode.PLAYER_OWES },
                        label = { Text("Player owes") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (adjustmentMode == SettleAdjustmentMode.PLAYER_OWES) {
                            {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
                TeamFundFilterChip(
                    selected = adjustmentMode == SettleAdjustmentMode.TEAM_PAID_PLAYER,
                    onClick = { adjustmentMode = SettleAdjustmentMode.TEAM_PAID_PLAYER },
                    label = { Text("Team paid player") },
                    leadingIcon = if (adjustmentMode == SettleAdjustmentMode.TEAM_PAID_PLAYER) {
                        {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
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

                TeamFundTextField(
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
            TeamFundTextField(
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
            TeamFundTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Reference Note") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            val amountMinorPreview = MoneyFormatter.toMinorUnits(amountText)
            val positionDelta = when {
                amountMinorPreview <= 0L -> 0L
                adjustmentMode == SettleAdjustmentMode.PLAYER_PAID -> amountMinorPreview
                adjustmentMode == SettleAdjustmentMode.PLAYER_OWES -> -amountMinorPreview
                else -> -amountMinorPreview
            }
            val projectedPosition = playerSummary.positionMinorUnits + positionDelta

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Collect / pay after this adjustment",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            projectedPosition > 0 ->
                                "Pay player: ${MoneyFormatter.format(projectedPosition, currencyCode)}"
                            projectedPosition < 0 ->
                                "Collect: ${MoneyFormatter.format(-projectedPosition, currencyCode)}"
                            else -> "Settled"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (adjustmentMode) {
                            SettleAdjustmentMode.PLAYER_PAID -> "Player → team fund"
                            SettleAdjustmentMode.PLAYER_OWES -> "Charge (no cash moved)"
                            SettleAdjustmentMode.TEAM_PAID_PLAYER -> "Team fund → player"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            TeamFundButton(
                onClick = {
                    val amountMinor = MoneyFormatter.toMinorUnits(amountText)
                    if (amountMinor > 0) {
                        onSettle(amountMinor, note, adjustmentMode, selectedExpenseId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amountText.isNotEmpty() && !isSaving,
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
