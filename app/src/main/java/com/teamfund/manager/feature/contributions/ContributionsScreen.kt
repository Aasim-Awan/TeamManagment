package com.teamfund.manager.feature.contributions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.ContributionCard
import com.teamfund.manager.core.designsystem.EmptyStateView
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.Contribution

@Composable
fun ContributionsScreen(
    viewModel: ContributionsViewModel,
    onNavigateToAddContribution: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var contributionToDelete by remember { mutableStateOf<Contribution?>(null) }

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Fund Contributions",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddContribution,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_contribution_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contribution")
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

                if (state.contributions.isEmpty()) {
                    EmptyStateView(
                        title = "No Contributions Logged",
                        message = "Track member monthly subscriptions and match fee deposits into the team pool.",
                        icon = Icons.Default.Payments,
                        actionLabel = "Record Contribution",
                        onActionClick = onNavigateToAddContribution
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.contributions) { contribution ->
                            val playerName = state.playersMap[contribution.playerId]?.name ?: "Member"
                            ContributionCard(
                                contribution = contribution,
                                playerName = playerName,
                                currencyCode = state.currencyCode,
                                onClick = { contributionToDelete = contribution }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    contributionToDelete?.let { contrib ->
        AlertDialog(
            onDismissRequest = { contributionToDelete = null },
            title = { Text("Delete Contribution") },
            text = { Text("Are you sure you want to delete this contribution? This will revert the team balance.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContribution(contrib.id)
                        contributionToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { contributionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
