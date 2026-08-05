package com.teamfund.manager.feature.matches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.ExpenseCard
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.StatusChip
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.DateTimeUtils

@Composable
fun MatchDetailScreen(
    matchId: Long,
    viewModel: MatchesViewModel,
    onNavigateToAddExpense: (Long) -> Unit,
    onNavigateToEditMatch: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(matchId) {
        viewModel.loadMatchDetail(matchId)
    }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = state.match?.opponentName?.let { "vs $it" } ?: "Match Details",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (state.isLoading || state.match == null) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else {
            val match = state.match!!
            val totalMatchExpense = state.expenses.sumOf { it.amountMinorUnits }

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
                            StatusChip(result = match.result)
                            Row {
                                IconButton(onClick = { onNavigateToEditMatch(match.id) }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Match")
                                }
                                IconButton(onClick = {
                                    viewModel.deleteMatch(match.id, onBackClick)
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Match", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "vs ${match.opponentName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${DateTimeUtils.formatDateTime(match.date)} • ${match.venue ?: "Local Ground"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (match.teamScore != null && match.opponentScore != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Our Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(match.teamScore!!, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Opponent Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(match.opponentScore!!, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (!match.notes.isNull_or_blank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Notes: ${match.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Match Expenses Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Match Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ground fee, balls, umpire, water",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MoneyText(
                        minorUnits = totalMatchExpense,
                        currencyCode = state.currencyCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = { onNavigateToAddExpense(match.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Match Expense")
                }

                if (state.expenses.isEmpty()) {
                    Text(
                        text = "No expenses recorded for this match yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    state.expenses.forEach { expense ->
                        ExpenseCard(
                            expense = expense,
                            currencyCode = state.currencyCode,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
