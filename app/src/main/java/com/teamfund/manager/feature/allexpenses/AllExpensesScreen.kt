package com.teamfund.manager.feature.allexpenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamfund.manager.core.designsystem.ActivityRow
import com.teamfund.manager.core.designsystem.EmptyStateView
import com.teamfund.manager.core.designsystem.LoadingView
import com.teamfund.manager.core.designsystem.MoneyText
import com.teamfund.manager.core.designsystem.TeamFundFilterChip
import com.teamfund.manager.core.designsystem.TeamFundTopBar
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.ActivityItem
import com.teamfund.manager.domain.model.PlayerFinancialSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExpensesScreen(
    viewModel: AllExpensesViewModel,
    onNavigateToExpenseDetail: (Long) -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TeamFundTopBar(title = "All expenses")
        }
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(modifier = Modifier.padding(innerPadding))
            !state.hasTeam -> EmptyStateView(
                title = "No team selected",
                message = "Create or select a team to see its expenses.",
                modifier = Modifier.padding(innerPadding)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Timeline") },
                        modifier = Modifier.testTag("all_expenses_tab_timeline")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("By player") },
                        modifier = Modifier.testTag("all_expenses_tab_players")
                    )
                }
                if (selectedTab == 0) {
                    TimelineTab(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToExpenseDetail = onNavigateToExpenseDetail
                    )
                } else {
                    PlayersTab(
                        rows = state.playerRows,
                        currencyCode = state.currencyCode,
                        onPlayerClick = onNavigateToPlayerDetail
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineTab(
    state: AllExpensesUiState,
    viewModel: AllExpensesViewModel,
    onNavigateToExpenseDetail: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            SummaryCard(
                summary = state.summary,
                currencyCode = state.currencyCode,
                periodLabel = state.filters.month?.let { DateTimeUtils.formatMonthYear(it) } ?: "All time",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        item {
            FilterBar(state = state, viewModel = viewModel)
        }

        if (state.months.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Nothing to show",
                    message = "No records match these filters.",
                    icon = Icons.Default.Receipt
                )
            }
        }

        state.months.forEach { month ->
            stickyHeader(key = "month_${month.month}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateTimeUtils.formatMonthYear(month.month),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (month.expenseTotalMinorUnits > 0L) {
                        Text(
                            text = "Spent ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MoneyText(
                            minorUnits = month.expenseTotalMinorUnits,
                            currencyCode = state.currencyCode,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            month.days.forEach { day ->
                item(key = "day_${day.date}") {
                    Text(
                        text = DateTimeUtils.formatDayHeader(day.date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(day.items, key = { activityKey(it) }) { activity ->
                    ActivityRow(
                        activity = activity,
                        currencyCode = state.currencyCode,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onClick = (activity as? ActivityItem.ExpenseActivity)?.let { expense ->
                            { onNavigateToExpenseDetail(expense.id) }
                        }
                    )
                }
            }
        }
    }
}

private fun activityKey(item: ActivityItem): String = when (item) {
    is ActivityItem.ExpenseActivity -> "expense_${item.id}"
    is ActivityItem.ContributionActivity -> "contribution_${item.id}"
    is ActivityItem.SettlementActivity -> "settlement_${item.id}"
    is ActivityItem.FineActivity -> "fine_${item.id}"
    is ActivityItem.MatchActivity -> "match_${item.id}"
}

@Composable
private fun SummaryCard(
    summary: AllExpensesSummary,
    currencyCode: String,
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
            Text(
                text = "Total spent • $periodLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoneyText(
                minorUnits = summary.totalSpentMinorUnits,
                currencyCode = currencyCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
                }
                Text(
                    text = "${summary.expenseCount} expense${if (summary.expenseCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            // Compact two-column grid keeps the timeline visible below the summary.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Team fund paid", summary.paidFromFundMinorUnits, currencyCode, Modifier.weight(1f))
                StatCell("Players paid", summary.paidByPlayersMinorUnits, currencyCode, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Unpaid", summary.unpaidSharesMinorUnits, currencyCode, Modifier.weight(1f))
                StatCell("Contributions", summary.contributionsMinorUnits, currencyCode, Modifier.weight(1f))
                StatCell("Charges", summary.chargesMinorUnits, currencyCode, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, minorUnits: Long, currencyCode: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MoneyText(
                minorUnits = minorUnits,
                currencyCode = currencyCode,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, minorUnits: Long, currencyCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            minorUnits = minorUnits,
            currencyCode = currencyCode,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FilterBar(state: AllExpensesUiState, viewModel: AllExpensesViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LedgerTypeFilter.entries.forEach { type ->
                TeamFundFilterChip(
                    selected = state.filters.type == type,
                    onClick = { viewModel.setTypeFilter(type) },
                    label = { Text(type.label) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val selectedMonth = state.filters.month
            DropdownFilterChip(
                label = selectedMonth?.let { DateTimeUtils.formatMonthYear(it) } ?: "All months",
                selected = selectedMonth != null,
                options = listOf("All months" to null) +
                    state.availableMonths.map { DateTimeUtils.formatMonthYear(it) to it },
                onSelected = { viewModel.setMonthFilter(it) },
                testTag = "all_expenses_month_filter"
            )
            val selectedPlayer = state.players.find { it.id == state.filters.playerId }
            DropdownFilterChip(
                label = selectedPlayer?.name ?: "All players",
                selected = selectedPlayer != null,
                options = listOf("All players" to null) + state.players.map { it.name to it.id },
                onSelected = { viewModel.setPlayerFilter(it) },
                testTag = "all_expenses_player_filter"
            )
        }
    }
}

@Composable
private fun <T> DropdownFilterChip(
    label: String,
    selected: Boolean,
    options: List<Pair<String, T?>>,
    onSelected: (T?) -> Unit,
    testTag: String
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        TeamFundFilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.testTag(testTag)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayersTab(
    rows: List<PlayerFinancialSummary>,
    currencyCode: String,
    onPlayerClick: (Long) -> Unit
) {
    if (rows.isEmpty()) {
        EmptyStateView(
            title = "No players yet",
            message = "Add players to see how expenses are shared.",
            icon = Icons.Default.Group
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "All-time expense shares and balances per player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(rows, key = { it.player.id }) { row ->
            PlayerExpenseRow(row = row, currencyCode = currencyCode, onClick = { onPlayerClick(row.player.id) })
        }
    }
}

@Composable
private fun PlayerExpenseRow(
    row: PlayerFinancialSummary,
    currencyCode: String,
    onClick: () -> Unit
) {
    val sharePaid = row.personalExpenseSharesMinorUnits - row.unpaidExpenseSharesMinorUnits
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.player.name + if (row.player.isActive) "" else " (inactive)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val position = row.positionMinorUnits
                Text(
                    text = when {
                        position > 0L -> "Team owes "
                        position < 0L -> "To collect "
                        else -> "Settled"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (position != 0L) {
                    MoneyText(
                        minorUnits = if (position > 0L) position else -position,
                        currencyCode = currencyCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (position > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SummaryLine("Expense share", row.personalExpenseSharesMinorUnits, currencyCode)
            SummaryLine("Share paid", sharePaid, currencyCode)
            SummaryLine("Share unpaid", row.unpaidExpenseSharesMinorUnits, currencyCode)
            SummaryLine("Paid for the team", row.totalPaidOnBehalfMinorUnits, currencyCode)
            SummaryLine("Contributions", row.totalContributionsMinorUnits, currencyCode)
            SummaryLine("Charges & fines", row.totalChargesMinorUnits, currencyCode)
        }
    }
}
