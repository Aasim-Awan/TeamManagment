package com.teamfund.manager.feature.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.StatCard
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Settlement

@Composable
fun PlayerDetailScreen(
    playerId: Long,
    viewModel: TeamViewModel,
    onNavigateToAddContribution: (Long) -> Unit,
    onNavigateToEditPlayer: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    
    var contributionToDelete by remember { mutableStateOf<Contribution?>(null) }
    var settlementToRevoke by remember { mutableStateOf<Settlement?>(null) }

    LaunchedEffect(playerId) {
        viewModel.loadPlayerDetail(playerId)
    }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = state.summary?.player?.name ?: "Player Ledger",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (state.isLoading || state.summary == null) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else {
            val summary = state.summary!!
            val player = summary.player

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = player.role.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(onClick = { onNavigateToEditPlayer(player.id) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Player")
                                }
                                IconButton(onClick = { viewModel.deactivatePlayer(player.id, onBackClick) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deactivate Player", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (!player.phoneNumber.isNull_or_blank()) {
                            Text(
                                text = player.phoneNumber!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Net Balance Display
                        Text(
                            text = "NET FINANCIAL BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MoneyText(
                            minorUnits = summary.netBalanceMinorUnits,
                            currencyCode = state.currencyCode,
                            includeSign = true,
                            style = MaterialTheme.typography.displayLarge,
                            color = when {
                                summary.netBalanceMinorUnits > 0 -> MaterialTheme.colorScheme.primary
                                summary.netBalanceMinorUnits < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = summary.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                summary.netBalanceMinorUnits > 0 -> MaterialTheme.colorScheme.primary
                                summary.netBalanceMinorUnits < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Contributed",
                        value = com.teamfund.manager.core.util.MoneyFormatter.format(summary.totalContributionsMinorUnits, state.currencyCode),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Paid for Team",
                        value = com.teamfund.manager.core.util.MoneyFormatter.format(summary.totalPaidOnBehalfMinorUnits, state.currencyCode),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Personal Share",
                        value = com.teamfund.manager.core.util.MoneyFormatter.format(summary.personalExpenseSharesMinorUnits, state.currencyCode),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = { onNavigateToAddContribution(player.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record Contribution from ${player.name}")
                }

                // Ledger History
                Text(
                    text = "Player Activity Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (state.contributions.isEmpty() && state.expenseShares.isEmpty() && state.settlements.isEmpty()) {
                    Text("No transactions recorded for this player yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    // Contributions List
                    state.contributions.forEach { contrib ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Contribution (${contrib.purpose.displayName})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = DateTimeUtils.formatDate(contrib.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyText(minorUnits = contrib.amountMinorUnits, currencyCode = state.currencyCode, includeSign = true, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { contributionToDelete = contrib }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Settlements List
                    state.settlements.forEach { settlement ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isOutgoing = settlement.fromPlayerId == playerId
                            Column(modifier = Modifier.weight(1f)) {
                                val text = if (isOutgoing) "Paid Team / Player" else "Received from Team / Player"
                                Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = DateTimeUtils.formatDate(settlement.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!settlement.note.isNullOrBlank()) {
                                    Text(text = settlement.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyText(
                                    minorUnits = settlement.amountMinorUnits, 
                                    currencyCode = state.currencyCode, 
                                    includeSign = true,
                                    color = if (isOutgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                IconButton(onClick = { settlementToRevoke = settlement }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Expense Shares List
                    state.expenseShares.forEach { share ->
                        val exp = state.expensesMap[share.expenseId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Expense Share: ${exp?.title ?: "Team Expense"}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = exp?.date?.let { DateTimeUtils.formatDate(it) } ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            MoneyText(minorUnits = -share.shareAmountMinorUnits, currencyCode = state.currencyCode, color = MaterialTheme.colorScheme.error)
                            // Spacer to align with others that have delete icons
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                }
            }
        }
    }

    contributionToDelete?.let { contrib ->
        AlertDialog(
            onDismissRequest = { contributionToDelete = null },
            title = { Text("Delete Contribution") },
            text = { Text("Are you sure you want to delete this contribution?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteContribution(contrib.id)
                    contributionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contributionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    settlementToRevoke?.let { settlement ->
        AlertDialog(
            onDismissRequest = { settlementToRevoke = null },
            title = { Text("Revoke Settlement") },
            text = { Text("Are you sure you want to revoke this settlement?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSettlement(settlement.id)
                    settlementToRevoke = null
                }) {
                    Text("Revoke", color = MaterialTheme.colorScheme.error)
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

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
