package com.teamfund.manager.feature.expenses

import com.teamfund.manager.core.designsystem.TeamFundFilterChip

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
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
import com.teamfund.manager.core.designsystem.ExpenseCard
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.domain.model.ExpenseCategory

@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToExpenseDetail: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = "Shared expenses",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
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
                // Category Filter Scroll Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamFundFilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.setCategoryFilter(null) },
                        label = { Text("All") }
                    )
                    ExpenseCategory.entries.forEach { cat ->
                        TeamFundFilterChip(
                            selected = state.selectedCategory == cat,
                            onClick = { viewModel.setCategoryFilter(cat) },
                            label = { Text(cat.displayName) }
                        )
                    }
                }

                if (state.filteredExpenses.isEmpty()) {
                    EmptyStateView(
                        title = "No Expenses Logged",
                        message = "Record ground fees, cricket balls, transport, and equipment expenses.",
                        icon = Icons.Default.Receipt,
                        actionLabel = "Log First Expense",
                        onActionClick = onNavigateToAddExpense
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredExpenses) { expense ->
                            ExpenseCard(
                                expense = expense,
                                currencyCode = state.currencyCode,
                                onClick = { onNavigateToExpenseDetail(expense.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}
