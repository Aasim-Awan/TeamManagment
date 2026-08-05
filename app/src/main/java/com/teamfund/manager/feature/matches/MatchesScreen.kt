package com.teamfund.manager.feature.matches

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
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.EmptyStateView
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MatchCard
import com.teamfund.manager.core.designsystem.TeamFundTopBar

@Composable
fun MatchesScreen(
    viewModel: MatchesViewModel,
    onNavigateToAddMatch: () -> Unit,
    onNavigateToMatchDetail: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Cricket Matches",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddMatch,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_match_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Match")
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
                // Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MatchFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.displayName) },
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }

                if (state.filteredMatches.isEmpty()) {
                    EmptyStateView(
                        title = "No Matches Found",
                        message = "Log your played or upcoming cricket fixtures to track match expenses and stats.",
                        icon = Icons.Default.SportsCricket,
                        actionLabel = "Schedule Match",
                        onActionClick = onNavigateToAddMatch
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredMatches) { match ->
                            MatchCard(
                                match = match,
                                onClick = { onNavigateToMatchDetail(match.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}
