package com.teamfund.manager.feature.settlements

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.BalanceStatusCard
import com.teamfund.manager.core.designsystem.EmptyStateView
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.domain.model.Settlement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementsScreen(
    viewModel: SettlementsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    var showSettleDialog by remember { mutableStateOf<PlayerFinancialSummary?>(null) }
    var settlementToRevoke by remember { mutableStateOf<Settlement?>(null) }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Settlements & Dues",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Dues") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("History") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTabIndex) {
                    0 -> {
                        // Filter players who either owe/are owed overall, OR have specific unpaid expense dues
                        val pendingPlayers = state.playerSummaries.filter { 
                            it.netBalanceMinorUnits != 0L || it.unpaidExpenseSharesMinorUnits > 0L 
                        }

                        if (pendingPlayers.isEmpty()) {
                            EmptyStateView(
                                title = "All Debts Settled!",
                                message = "No member currently owes or is owed money."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(pendingPlayers) { summary ->
                                    BalanceStatusCard(
                                        playerSummary = summary,
                                        currencyCode = state.currencyCode,
                                        onSettleClick = {
                                            showSettleDialog = summary
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (state.settlementsHistory.isEmpty()) {
                            EmptyStateView(
                                title = "No Past Settlements",
                                message = "Settlement records will appear here."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.settlementsHistory) { settlement ->
                                    val fromPlayer = if (settlement.fromPlayerId == 0L) {
                                        "Team Fund"
                                    } else {
                                        state.playerSummaries.find { it.player.id == settlement.fromPlayerId }?.player?.name
                                            ?: "Member"
                                    }
                                    val toPlayer = if (settlement.toPlayerId != null) 
                                        state.playerSummaries.find { it.player.id == settlement.toPlayerId }?.player?.name 
                                        else null
                                        
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            val detailText = if (toPlayer != null) "$fromPlayer paid $toPlayer" else "$fromPlayer cleared debt"
                                            Text(text = detailText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            
                                            val matchName = state.matches.find { it.id == settlement.matchId }?.opponentName
                                            if (matchName != null) {
                                                Text(text = "Match vs $matchName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            
                                            Text(text = DateTimeUtils.formatDate(settlement.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (!settlement.note.isNullOrBlank()) {
                                                Text(text = settlement.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            MoneyText(minorUnits = settlement.amountMinorUnits, currencyCode = state.currencyCode, color = MaterialTheme.colorScheme.primary)
                                            IconButton(onClick = { settlementToRevoke = settlement }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showSettleDialog?.let { summary ->
        SettleDebtSheet(
            playerSummary = summary,
            currencyCode = state.currencyCode,
            expenses = state.expenses,
            onDismiss = { showSettleDialog = null },
            onSettle = { amount, note, isIncoming, expenseId ->
                viewModel.settleDebt(
                    fromPlayerId = if (isIncoming) summary.player.id else 0L,
                    toPlayerId = if (isIncoming) null else summary.player.id,
                    expenseId = expenseId,
                    amountMinorUnits = amount,
                    note = note,
                    onSuccess = { showSettleDialog = null }
                )
            }
        )
    }

    settlementToRevoke?.let { settlement ->
        AlertDialog(
            onDismissRequest = { settlementToRevoke = null },
            title = { Text("Revoke Settlement") },
            text = { Text("Are you sure you want to revoke this settlement? This will revert the balance changes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSettlement(settlement.id)
                        settlementToRevoke = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Revoke")
                }
            },
            dismissButton = {
                TextButton(onClick = { settlementToRevoke = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
