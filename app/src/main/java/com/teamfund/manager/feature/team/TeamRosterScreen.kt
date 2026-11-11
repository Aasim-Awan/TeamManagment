package com.teamfund.manager.feature.team

import com.teamfund.manager.core.designsystem.TeamFundTextField

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.EmptyStateView
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.PlayerCard
import com.teamfund.manager.core.designsystem.TeamFundTopBar

@Composable
fun TeamRosterScreen(
    viewModel: TeamViewModel,
    onNavigateToAddPlayer: () -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.rosterState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showInactive by rememberSaveable { mutableStateOf(false) }
    val q = query.trim()
    fun matchesQuery(summary: com.teamfund.manager.domain.model.PlayerFinancialSummary) =
        summary.player.name.contains(q, ignoreCase = true) ||
            summary.player.role.displayName.contains(q, ignoreCase = true) ||
            summary.player.cricketRole.displayName.contains(q, ignoreCase = true)
    val activeSummaries = state.playerSummaries.filter { it.player.isActive && matchesQuery(it) }
    val inactiveSummaries = state.playerSummaries.filter { !it.player.isActive && matchesQuery(it) }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Players & balances",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPlayer,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_player_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Player")
            }
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${state.playerSummaries.count { it.player.isActive }} active · ${inactiveSummaries.size} inactive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TeamFundTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search players or roles") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("roster_search")
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (state.playerSummaries.isEmpty()) {
                    EmptyStateView(
                        title = "No Players Added",
                        message = "Add team players to divide match ground fees, balls, and equipment costs.",
                        icon = Icons.Default.Group,
                        actionLabel = "Add First Player",
                        onActionClick = onNavigateToAddPlayer
                    )
                } else if (activeSummaries.isEmpty() && inactiveSummaries.isEmpty()) {
                    EmptyStateView(
                        title = "No matching players",
                        message = "Try another name or role."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeSummaries, key = { it.player.id }) { summary ->
                            PlayerCard(
                                playerSummary = summary,
                                currencyCode = state.currencyCode,
                                onClick = { onNavigateToPlayerDetail(summary.player.id) }
                            )
                        }
                        if (inactiveSummaries.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Inactive (${inactiveSummaries.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { showInactive = !showInactive }) {
                                        Text(if (showInactive) "Hide" else "Show")
                                    }
                                }
                            }
                            if (showInactive) {
                                items(inactiveSummaries, key = { it.player.id }) { summary ->
                                    PlayerCard(
                                        playerSummary = summary,
                                        currencyCode = state.currencyCode,
                                        onClick = { onNavigateToPlayerDetail(summary.player.id) }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}
