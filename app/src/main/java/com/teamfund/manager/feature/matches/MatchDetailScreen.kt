package com.teamfund.manager.feature.matches

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import com.teamfund.manager.core.designsystem.SectionHeading
import com.teamfund.manager.core.designsystem.StatusChip
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.MatchPlayerStats
import com.teamfund.manager.domain.model.MatchPlayerStatus
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateMapOf

@Composable
fun MatchDetailScreen(
    matchId: Long,
    viewModel: MatchesViewModel,
    onNavigateToAddExpense: (Long) -> Unit,
    onNavigateToEditMatch: (Long) -> Unit,
    onNavigateToExpenseDetail: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val runsDraft = remember { mutableStateMapOf<Long, String>() }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete match?") },
            text = { Text("This removes the match, its Playing XI and stats, the match-fee charges and the ground-fee expense created for it. Expenses, payments and fines you added yourself are kept but no longer linked to this match. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteMatch(matchId, onBackClick)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

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
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    confirmDelete = true
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Match", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "vs ${match.opponentName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${DateTimeUtils.formatDateTime(match.date)} · ${match.venue ?: "TBC"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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

                val xi = state.matchPlayers.filter { it.status == MatchPlayerStatus.SELECTED }
                if (xi.isNotEmpty()) {
                    SectionHeading("Playing XI")
                    Text(
                        xi.mapNotNull { state.playerMap[it.playerId]?.name }.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SectionHeading("Runs")
                    xi.forEach { mp ->
                        val name = state.playerMap[mp.playerId]?.name ?: "Player"
                        val existing = state.playerStats.find { it.playerId == mp.playerId }
                        if (!runsDraft.containsKey(mp.playerId)) {
                            runsDraft[mp.playerId] = (existing?.runs ?: 0).toString()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            OutlinedTextField(
                                value = runsDraft[mp.playerId] ?: "0",
                                onValueChange = { runsDraft[mp.playerId] = it },
                                modifier = Modifier.widthIn(min = 64.dp, max = 88.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val stats = xi.map { mp ->
                                MatchPlayerStats(
                                    matchId = matchId,
                                    playerId = mp.playerId,
                                    runs = runsDraft[mp.playerId]?.toIntOrNull() ?: 0,
                                    isPlayerOfMatch = match.playerOfTheMatchId == mp.playerId
                                )
                            }
                            viewModel.savePlayerStats(matchId, stats)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save stats") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val csv = viewModel.exportMatchCsv()
                            if (csv != null) {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                }, "Export CSV"))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("CSV", style = MaterialTheme.typography.labelMedium) }

                    OutlinedButton(
                        onClick = {
                            val message = buildString {
                                appendLine("Match vs ${match.opponentName}")
                                appendLine(DateTimeUtils.formatDateTime(match.date))
                                appendLine("Venue: ${match.venue ?: "TBC"}")
                                appendLine("Status: ${match.result.displayName}")
                            }
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }, "Share"))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Share", style = MaterialTheme.typography.labelMedium) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeading("Expenses")
                    MoneyText(
                        minorUnits = totalMatchExpense,
                        currencyCode = state.currencyCode,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedButton(
                    onClick = { onNavigateToAddExpense(match.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add expense")
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
                            onClick = { onNavigateToExpenseDetail(expense.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
