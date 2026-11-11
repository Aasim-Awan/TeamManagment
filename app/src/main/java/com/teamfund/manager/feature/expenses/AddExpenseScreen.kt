package com.teamfund.manager.feature.expenses

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.saveable.rememberSaveable

import com.teamfund.manager.core.designsystem.TeamFundTextField
import com.teamfund.manager.core.designsystem.TeamFundButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.DatePickerField
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.MoneyFormatter
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.model.PlayerRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpensesViewModel,
    matchId: Long? = null,
    expenseId: Long? = null,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val splitPlayers = remember(state.allPlayers, state.activePlayers, expenseId, state.expenses) {
        if (expenseId != null) {
            val onExpense = state.expenses.find { it.id == expenseId }
                ?.shares?.map { it.playerId }?.toSet().orEmpty()
            state.allPlayers.filter { it.isActive || onExpense.contains(it.id) }
        } else {
            state.activePlayers
        }
    }
    val payerPlayers = state.activePlayers

    var title by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ExpenseCategory.GROUND) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var paidByPlayerId by rememberSaveable { mutableStateOf<Long?>(null) } // null = Team Fund
    var selectedMatchId by rememberSaveable { mutableStateOf<Long?>(matchId) }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    val selectedPlayerIds = remember { mutableStateListOf<Long>() }
    var perPersonAmountText by rememberSaveable { mutableStateOf("") }
    var isManualSplit by rememberSaveable { mutableStateOf(false) }

    var catExpanded by rememberSaveable { mutableStateOf(false) }
    var payerExpanded by rememberSaveable { mutableStateOf(false) }
    var matchExpanded by rememberSaveable { mutableStateOf(false) }

    var showAddPlayerDialog by rememberSaveable { mutableStateOf(false) }
    var pendingSelectPlayerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var didInitialPlayerSelect by rememberSaveable { mutableStateOf(false) }

    // Load existing expense if editing
    LaunchedEffect(expenseId, state.expenses) {
        if (expenseId != null && state.expenses.isNotEmpty()) {
            state.expenses.find { it.id == expenseId }?.let { expense ->
                title = expense.title
                selectedCategory = expense.category
                amountText = MoneyFormatter.fromMinorUnits(expense.amountMinorUnits)
                paidByPlayerId = expense.paidByPlayerId
                selectedMatchId = expense.matchId
                notes = expense.notes ?: ""
                selectedDateMillis = expense.date

                selectedPlayerIds.clear()
                selectedPlayerIds.addAll(expense.shares.map { it.playerId })
                
                // Determine if it was a manual split (if shares aren't roughly equal to total/count)
                val shareAmounts = expense.shares.map { it.shareAmountMinorUnits }
                if (shareAmounts.isNotEmpty()) {
                    val count = expense.shares.size
                    val total = expense.amountMinorUnits
                    val autoShare = total / count
                    
                    isManualSplit = shareAmounts.any { Math.abs(it - autoShare) > 1L }
                    
                    if (isManualSplit) {
                        perPersonAmountText = MoneyFormatter.fromMinorUnits(shareAmounts.first())
                    }
                }
                
                didInitialPlayerSelect = true
            }
        }
    }

    LaunchedEffect(state.activePlayers) {
        if (expenseId == null && !didInitialPlayerSelect && state.activePlayers.isNotEmpty()) {
            selectedPlayerIds.clear()
            selectedPlayerIds.addAll(state.activePlayers.map { it.id })
            didInitialPlayerSelect = true
        }
    }

    LaunchedEffect(pendingSelectPlayerId, state.allPlayers) {
        val newId = pendingSelectPlayerId ?: return@LaunchedEffect
        if (state.allPlayers.any { it.id == newId }) {
            if (!selectedPlayerIds.contains(newId)) {
                selectedPlayerIds.add(newId)
            }
            pendingSelectPlayerId = null
            didInitialPlayerSelect = true
        }
    }

    if (showAddPlayerDialog) {
        QuickAddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { name, role ->
                viewModel.addPlayerQuick(name = name, role = role) { newId ->
                    pendingSelectPlayerId = newId
                    if (!selectedPlayerIds.contains(newId)) {
                        selectedPlayerIds.add(newId)
                    }
                    didInitialPlayerSelect = true
                    showAddPlayerDialog = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = if (expenseId == null) "Log Shared Expense" else "Edit Expense",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Expense Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            TeamFundTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title *") },
                placeholder = { Text("e.g. Ground Booking Fee") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_title_input"),
                singleLine = true
            )

            TeamFundTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (${state.currencyCode}) *") },
                placeholder = { Text("50.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                singleLine = true
            )

            DatePickerField(
                selectedDateMillis = selectedDateMillis,
                onDateSelected = { selectedDateMillis = it },
                label = "Expense Date",
                testTag = "expense_date_picker"
            )

            // Category Selector
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                TeamFundTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    ExpenseCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            onClick = { selectedCategory = cat; catExpanded = false }
                        )
                    }
                }
            }

            // Paid By Selector
            ExposedDropdownMenuBox(expanded = payerExpanded, onExpandedChange = { payerExpanded = !payerExpanded }) {
                val payerName = if (paidByPlayerId == null) "Paid directly from Team Fund"
                else payerPlayers.find { it.id == paidByPlayerId }?.name ?: "Member"

                TeamFundTextField(
                    value = payerName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paid By") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = payerExpanded, onDismissRequest = { payerExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Team Fund Pool") },
                        onClick = { paidByPlayerId = null; payerExpanded = false }
                    )
                    payerPlayers.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.name} (Player)") },
                            onClick = { paidByPlayerId = p.id; payerExpanded = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("+ Add player") },
                        onClick = {
                            payerExpanded = false
                            showAddPlayerDialog = true
                        }
                    )
                }
            }

            // Associate Match
            ExposedDropdownMenuBox(expanded = matchExpanded, onExpandedChange = { matchExpanded = !matchExpanded }) {
                val matchName = if (selectedMatchId == null) "General Team Expense (No Match)"
                else state.matches.find { it.id == selectedMatchId }?.let { "vs ${it.opponentName}" } ?: "Match"

                TeamFundTextField(
                    value = matchName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Associate Match (Optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = matchExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = matchExpanded, onDismissRequest = { matchExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("None (General Expense)") },
                        onClick = { selectedMatchId = null; matchExpanded = false }
                    )
                    state.matches.forEach { m ->
                        DropdownMenuItem(
                            text = { Text("vs ${m.opponentName}") },
                            onClick = { selectedMatchId = m.id; matchExpanded = false }
                        )
                    }
                }
            }

            // Split Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Split among players",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedPlayerIds.size} selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Manual", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = isManualSplit,
                                onCheckedChange = { isManualSplit = it }
                            )
                        }
                    }

                    if (splitPlayers.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    selectedPlayerIds.clear()
                                    selectedPlayerIds.addAll(splitPlayers.map { it.id })
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("Select All", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = { selectedPlayerIds.clear() },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("None", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { showAddPlayerDialog = true },
                                modifier = Modifier.testTag("add_player_from_expense"),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+ Add", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (splitPlayers.isEmpty()) {
                        Text(
                            text = "No players on roster yet. Add a player to split this expense.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (isManualSplit) {
                            TeamFundTextField(
                                value = perPersonAmountText,
                                onValueChange = { perPersonAmountText = it },
                                label = { Text("Share Amount Per Person") },
                                placeholder = { Text("0.00") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                prefix = { Text(state.currencyCode + " ") }
                            )
                        }

                        splitPlayers.forEach { player ->
                            val isChecked = selectedPlayerIds.contains(player.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedPlayerIds.add(player.id)
                                        else selectedPlayerIds.remove(player.id)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Text(
                                    text = if (!player.isActive) "${player.name} (inactive)" else player.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (!player.isActive) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    val totalAmountMinor = MoneyFormatter.toMinorUnits(amountText)
                    val allocatedMinor = if (isManualSplit) {
                        selectedPlayerIds.size * MoneyFormatter.toMinorUnits(perPersonAmountText)
                    } else {
                        if (selectedPlayerIds.isNotEmpty()) totalAmountMinor else 0L
                    }
                    val remainingMinor = totalAmountMinor - allocatedMinor

                    if (totalAmountMinor > 0L) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Allocated:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                MoneyFormatter.format(allocatedMinor, state.currencyCode),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (remainingMinor > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining (Covered by Team Fund):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    MoneyFormatter.format(remainingMinor, state.currencyCode),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (remainingMinor < 0L) {
                             Text(
                                "Over-allocated by ${MoneyFormatter.format(-remainingMinor, state.currencyCode)}!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            TeamFundTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            val isOverAllocated = isManualSplit &&
                selectedPlayerIds.size * MoneyFormatter.toMinorUnits(perPersonAmountText) > MoneyFormatter.toMinorUnits(amountText)

            TeamFundButton(
                onClick = {
                    val amountMinor = MoneyFormatter.toMinorUnits(amountText)
                    val manualAmounts = if (isManualSplit) {
                        val fixedAmount = MoneyFormatter.toMinorUnits(perPersonAmountText)
                        selectedPlayerIds.associateWith { fixedAmount }
                    } else null
                    
                    if (title.isNotBlank() && amountMinor > 0L && (selectedPlayerIds.isNotEmpty() || !isManualSplit)) {
                        viewModel.saveExpense(
                            expenseId = expenseId,
                            title = title,
                            category = selectedCategory,
                            amountMinorUnits = amountMinor,
                            paidByPlayerId = paidByPlayerId,
                            matchId = selectedMatchId,
                            selectedPlayerIdsForSplit = selectedPlayerIds.toList(),
                            manualPlayerAmounts = manualAmounts,
                            notes = notes.ifBlank { null },
                            date = selectedDateMillis,
                            onSuccess = onBackClick
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_expense_button"),
                enabled = title.isNotBlank() && MoneyFormatter.toMinorUnits(amountText) > 0L && !isOverAllocated && !isSaving
            ) {
                Text(if (expenseId == null) "Save Expense" else "Update Expense")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddPlayerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: PlayerRole) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf(PlayerRole.PLAYER) }
    var roleExpanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Player") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TeamFundTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player Name *") },
                    placeholder = { Text("e.g. John Smith") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_add_player_name"),
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    TeamFundTextField(
                        value = selectedRole.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        PlayerRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.displayName) },
                                onClick = {
                                    selectedRole = role
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedRole)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("quick_add_player_confirm")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
