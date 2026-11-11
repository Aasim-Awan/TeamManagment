package com.teamfund.manager.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.ActivityRow
import com.teamfund.manager.core.designsystem.EmptyStateView
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MatchCard
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.QuickActionButton
import com.teamfund.manager.core.designsystem.StatCard
import com.teamfund.manager.core.designsystem.TeamBalanceCard
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.ActivityItem

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddMatch: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAddContribution: () -> Unit,
    onNavigateToSettlements: () -> Unit,
    onNavigateToMatches: () -> Unit,
    onNavigateToTeam: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToAllExpenses: () -> Unit,
    onNavigateToContributions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMatchDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TeamFundTopBar(
                title = state.team?.name ?: "TeamFund",
                onSettingsClick = onNavigateToSettings,
                teams = state.allTeams,
                activeTeamId = state.team?.id,
                onTeamSelected = { viewModel.switchTeam(it) }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingView(modifier = Modifier.padding(innerPadding))
        } else if (state.team == null) {
            EmptyStateView(
                title = "No Active Team Found",
                message = "Create or select a team to manage players, matches and shared expenses.",
                actionLabel = "Setup Team",
                onActionClick = onNavigateToSettings,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Team Balance Card
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    TeamBalanceCard(
                        balanceMinorUnits = state.financialSummary.teamFundBalanceMinorUnits,
                        totalMoneyInMinorUnits = state.financialSummary.totalContributionsMinorUnits,
                        totalMoneyOutMinorUnits = state.thisMonthExpensesMinorUnits,
                        moneyInLabel = "Money In (all time)",
                        moneyOutLabel = "Spent this month",
                        currencyCode = state.currencyCode,
                        onAddContribution = onNavigateToAddContribution
                    )
                }

                // 2. Quick Action Buttons
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Quick actions",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                QuickActionButton(
                                    icon = Icons.Default.SportsCricket,
                                    label = "Match",
                                    onClick = onNavigateToAddMatch,
                                    testTag = "quick_add_match"
                                )
                                QuickActionButton(
                                    icon = Icons.Default.Receipt,
                                    label = "Expense",
                                    onClick = onNavigateToAddExpense,
                                    testTag = "quick_add_expense"
                                )
                                QuickActionButton(
                                    icon = Icons.Default.Payments,
                                    label = "Funds",
                                    onClick = onNavigateToAddContribution,
                                    testTag = "quick_add_contribution"
                                )
                                QuickActionButton(
                                    icon = Icons.Default.SwapHoriz,
                                    label = "Settle",
                                    onClick = onNavigateToSettlements,
                                    testTag = "quick_settle"
                                )
                            }
                        }
                    }
                }

                // 3. Match Performance & Outstanding Debt
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Match performance",
                            value = "${state.matchStats.matchesPlayed} (${String.format("%.0f", state.matchStats.winRate)}%)",
                            subtitle = "W: ${state.matchStats.wins} | L: ${state.matchStats.losses}",
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToMatches() }
                        )
                        StatCard(
                            title = "Pending dues",
                            value = MoneyTextString(state.financialSummary.totalOutstandingDebtMinorUnits, state.currencyCode),
                            subtitle = "Owed by members",
                            valueColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToSettlements() }
                        )
                    }
                }

                // 4. Next Match Card (if any)
                if (state.upcomingMatch != null) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Upcoming Match",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onNavigateToMatches) {
                                    Text("View All")
                                }
                            }
                            MatchCard(
                                match = state.upcomingMatch!!,
                                onClick = { onNavigateToMatchDetail(state.upcomingMatch!!.id) }
                            )
                        }
                    }
                }

                // 5. Navigation Hub Shortcuts
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Manage your team",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            NavigationRow(
                                icon = Icons.Default.Receipt,
                                title = "Shared expenses",
                                subtitle = "Venue, equipment and match costs",
                                onClick = onNavigateToExpenses
                            )
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            NavigationRow(
                                icon = Icons.Default.Payments,
                                title = "Fund Contributions",
                                subtitle = "Monthly dues & match fees",
                                onClick = onNavigateToContributions
                            )
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            NavigationRow(
                                icon = Icons.Default.BarChart,
                                title = "Reports & summaries",
                                subtitle = "Balances and CSV exports",
                                onClick = onNavigateToReports
                            )
                        }
                    }
                }

                // 6. Recent Activities Feed
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = onNavigateToAllExpenses,
                            modifier = Modifier.testTag("recent_activity_view_all")
                        ) {
                            Text("View All")
                        }
                    }
                }

                if (state.recentActivities.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No Recent Activity",
                            message = "Start by adding players, matches, or team expenses."
                        )
                    }
                } else {
                    items(state.recentActivities) { item ->
                        ActivityRow(activity = item, currencyCode = state.currencyCode)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun NavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}


@Composable
private fun MoneyTextString(minorUnits: Long, currencyCode: String): String {
    return com.teamfund.manager.core.util.MoneyFormatter.format(minorUnits, currencyCode)
}
