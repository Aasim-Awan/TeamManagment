package com.teamfund.manager.feature.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.feature.settlements.SettleDebtSheet

@Composable
fun ExpenseDetailScreen(
    expenseId: Long,
    viewModel: ExpensesViewModel,
    onNavigateToEditExpense: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val expense = state.expenses.find { it.id == expenseId }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var settlingShare by remember { mutableStateOf<Pair<Player, ExpenseShare>?>(null) }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Expense Details",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else if (expense == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Expense not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = expense.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = expense.category.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row {
                                IconButton(onClick = { onNavigateToEditExpense(expenseId) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(
                                    minorUnits = expense.amountMinorUnits,
                                    currencyCode = expense.currencyCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = DateTimeUtils.formatDate(expense.date),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val payerName = if (expense.paidByPlayerId == null) "Team Fund" 
                                        else state.allPlayers.find { it.id == expense.paidByPlayerId }?.name ?: "Unknown"
                        
                        Text("Paid By", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = payerName, style = MaterialTheme.typography.titleMedium)
                        
                        if (!expense.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = expense.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Split Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(expense.shares) { share ->
                        val player = if (share.playerId == 0L) {
                            Player(
                                id = 0L,
                                teamId = expense.teamId,
                                name = "Team Fund",
                                role = com.teamfund.manager.domain.model.PlayerRole.MANAGER
                            )
                        } else {
                            state.allPlayers.find { it.id == share.playerId }
                        }

                        if (player != null) {
                            ExpenseShareItem(
                                player = player,
                                share = share,
                                currencyCode = expense.currencyCode,
                                onSettleClick = {
                                    settlingShare = player to share
                                }
                            )
                        }
                    }

                    val expenseSettlements = state.settlements.filter { it.expenseId == expenseId }
                    if (expenseSettlements.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Settlement History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(expenseSettlements) { settlement ->
                            val fromPlayer = if (settlement.fromPlayerId == 0L) "Team Fund" 
                                            else state.allPlayers.find { it.id == settlement.fromPlayerId }?.name ?: "Member"
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "$fromPlayer paid", style = MaterialTheme.typography.bodySmall)
                                        Text(text = DateTimeUtils.formatDate(settlement.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (!settlement.note.isNullOrBlank()) {
                                            Text(text = settlement.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    MoneyText(minorUnits = settlement.amountMinorUnits, currencyCode = expense.currencyCode, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expenseId) {
                            showDeleteDialog = false
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    settlingShare?.let { (player, share) ->
        val playerSummary = PlayerFinancialSummary(
            player = player,
            unpaidExpenseSharesMinorUnits = share.remainingAmountMinorUnits
        )
        SettleDebtSheet(
            playerSummary = playerSummary,
            currencyCode = expense?.currencyCode ?: "USD",
            expenses = state.expenses,
            onDismiss = { settlingShare = null },
            isSaving = isSaving,
            initialExpenseId = expenseId,
            onSettle = { amount, note, mode, _ ->
                viewModel.settleAdjustment(
                    playerId = player.id,
                    mode = mode,
                    amountMinorUnits = amount,
                    note = note,
                    expenseId = expenseId,
                    shareId = share.id,
                    onSuccess = { settlingShare = null }
                )
            }
        )
    }
}

@Composable
fun ExpenseShareItem(
    player: Player,
    share: ExpenseShare,
    currencyCode: String,
    onSettleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val isActuallySettled = share.remainingAmountMinorUnits <= 0L
                Text(text = player.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (isActuallySettled) {
                    Text(text = "Settled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else if (share.paidAmountMinorUnits > 0) {
                    Text(
                        text = "Paid: ${com.teamfund.manager.core.util.MoneyFormatter.fromMinorUnits(share.paidAmountMinorUnits)} $currencyCode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val isActuallySettled = share.remainingAmountMinorUnits <= 0L
                if (!isActuallySettled && share.paidAmountMinorUnits > 0) {
                    Text(text = "Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(
                        minorUnits = share.remainingAmountMinorUnits,
                        currencyCode = currencyCode,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    MoneyText(
                        minorUnits = share.shareAmountMinorUnits,
                        currencyCode = currencyCode,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (!isActuallySettled) {
                    TextButton(
                        onClick = onSettleClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Settle", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
