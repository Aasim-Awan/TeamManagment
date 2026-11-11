package com.teamfund.manager.feature.allexpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.ActivityItem
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Which kind of money record the ledger shows. */
enum class LedgerTypeFilter(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    CONTRIBUTIONS("Contributions"),
    SETTLEMENTS("Settlements"),
    CHARGES("Charges")
}

data class LedgerFilters(
    val type: LedgerTypeFilter = LedgerTypeFilter.ALL,
    val playerId: Long? = null,
    val month: YearMonth? = null
)

/** Totals for the selected month (or all time when no month is selected). */
data class AllExpensesSummary(
    val totalSpentMinorUnits: Long = 0L,
    val paidFromFundMinorUnits: Long = 0L,
    val paidByPlayersMinorUnits: Long = 0L,
    val contributionsMinorUnits: Long = 0L,
    val chargesMinorUnits: Long = 0L,
    val unpaidSharesMinorUnits: Long = 0L,
    val expenseCount: Int = 0
)

data class LedgerDay(
    val date: LocalDate,
    val items: List<ActivityItem>
)

data class LedgerMonth(
    val month: YearMonth,
    val expenseTotalMinorUnits: Long,
    val days: List<LedgerDay>
)

data class AllExpensesUiState(
    val isLoading: Boolean = true,
    val hasTeam: Boolean = true,
    val currencyCode: String = "USD",
    val filters: LedgerFilters = LedgerFilters(),
    val summary: AllExpensesSummary = AllExpensesSummary(),
    val months: List<LedgerMonth> = emptyList(),
    val availableMonths: List<YearMonth> = emptyList(),
    val players: List<Player> = emptyList(),
    val playerRows: List<PlayerFinancialSummary> = emptyList()
)

/** Raw team data the ledger is built from. */
private data class LedgerSource(
    val currencyCode: String,
    val players: List<Player>,
    val expenses: List<Expense>,
    val contributions: List<Contribution>,
    val settlements: List<Settlement>,
    val fines: List<Fine>,
    val playerRows: List<PlayerFinancialSummary>
)

@OptIn(ExperimentalCoroutinesApi::class)
class AllExpensesViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase
) : ViewModel() {

    private val filters = MutableStateFlow(LedgerFilters())

    private val _uiState = MutableStateFlow(AllExpensesUiState())
    val uiState: StateFlow<AllExpensesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sourceFlow(), filters) { source, currentFilters ->
                if (source == null) {
                    AllExpensesUiState(isLoading = false, hasTeam = false)
                } else {
                    buildState(source, currentFilters)
                }
            }.collect { _uiState.value = it }
        }
    }

    fun setTypeFilter(type: LedgerTypeFilter) {
        filters.value = filters.value.copy(type = type)
    }

    fun setPlayerFilter(playerId: Long?) {
        filters.value = filters.value.copy(playerId = playerId)
    }

    fun setMonthFilter(month: YearMonth?) {
        filters.value = filters.value.copy(month = month)
    }

    private fun sourceFlow(): Flow<LedgerSource?> =
        userPreferencesRepository.activeTeamIdFlow
            .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
            .flatMapLatest { team ->
                if (team == null) return@flatMapLatest flowOf(null)
                val teamId = team.id
                val moneyFlow = combine(
                    repository.getExpensesFlow(teamId),
                    repository.getContributionsFlow(teamId),
                    repository.getSettlementsFlow(teamId),
                    repository.getFinesForTeamFlow(teamId)
                ) { expenses, contributions, settlements, fines ->
                    MoneyData(expenses, contributions, settlements, fines)
                }
                combine(
                    repository.getAllPlayersFlow(teamId),
                    repository.getAllExpenseSharesFlow(teamId),
                    moneyFlow
                ) { players, shares, money ->
                    LedgerSource(
                        currencyCode = team.currencyCode,
                        players = players,
                        expenses = money.expenses,
                        contributions = money.contributions,
                        settlements = money.settlements,
                        fines = money.fines,
                        playerRows = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = money.contributions,
                            expenses = money.expenses,
                            expenseShares = shares,
                            settlements = money.settlements,
                            fines = money.fines
                        )
                    )
                }
            }

    private data class MoneyData(
        val expenses: List<Expense>,
        val contributions: List<Contribution>,
        val settlements: List<Settlement>,
        val fines: List<Fine>
    )

    private fun buildState(source: LedgerSource, currentFilters: LedgerFilters): AllExpensesUiState {
        val items = AllExpensesLedger.toActivities(
            players = source.players,
            expenses = source.expenses,
            contributions = source.contributions,
            settlements = source.settlements,
            fines = source.fines
        )
        val availableMonths = items.map { DateTimeUtils.toYearMonth(it.timestamp) }
            .distinct()
            .sortedDescending()
        val filtered = AllExpensesLedger.filter(items, currentFilters)
        return AllExpensesUiState(
            isLoading = false,
            hasTeam = true,
            currencyCode = source.currencyCode,
            filters = currentFilters,
            summary = AllExpensesLedger.summarize(items, currentFilters.month),
            months = AllExpensesLedger.group(filtered),
            availableMonths = availableMonths,
            players = source.players,
            playerRows = source.playerRows.sortedBy { it.player.name.lowercase() }
        )
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AllExpensesViewModel(repository, userPreferencesRepository, calculatePlayerBalancesUseCase) as T
        }
    }
}

/** Pure ledger logic, kept separate from the ViewModel so it can be unit tested. */
object AllExpensesLedger {

    fun toActivities(
        players: List<Player>,
        expenses: List<Expense>,
        contributions: List<Contribution>,
        settlements: List<Settlement>,
        fines: List<Fine>
    ): List<ActivityItem> {
        val names = players.associate { it.id to it.name }
        val expenseItems = expenses.map {
            ActivityItem.ExpenseActivity(
                id = it.id,
                expense = it,
                paidByPlayerName = it.paidByPlayerId?.let { id -> names[id] ?: "Member" }
            )
        }
        val contributionItems = contributions.map {
            ActivityItem.ContributionActivity(id = it.id, contribution = it, playerName = names[it.playerId] ?: "Member")
        }
        val settlementItems = settlements.map {
            ActivityItem.SettlementActivity(
                id = it.id,
                settlement = it,
                fromPlayerName = if (it.fromPlayerId == 0L) "Team Fund" else names[it.fromPlayerId] ?: "Member",
                toPlayerName = it.toPlayerId?.let { id -> names[id] ?: "Member" }
            )
        }
        val fineItems = fines.map {
            ActivityItem.FineActivity(id = it.id, fine = it, playerName = names[it.playerId] ?: "Member")
        }
        return (expenseItems + contributionItems + settlementItems + fineItems)
            .sortedByDescending { it.timestamp }
    }

    fun filter(items: List<ActivityItem>, filters: LedgerFilters): List<ActivityItem> = items.filter { item ->
        val typeMatches = when (filters.type) {
            LedgerTypeFilter.ALL -> true
            LedgerTypeFilter.EXPENSES -> item is ActivityItem.ExpenseActivity
            LedgerTypeFilter.CONTRIBUTIONS -> item is ActivityItem.ContributionActivity
            LedgerTypeFilter.SETTLEMENTS -> item is ActivityItem.SettlementActivity
            LedgerTypeFilter.CHARGES -> item is ActivityItem.FineActivity
        }
        val monthMatches = filters.month == null || DateTimeUtils.toYearMonth(item.timestamp) == filters.month
        val playerMatches = filters.playerId == null || involvesPlayer(item, filters.playerId)
        typeMatches && monthMatches && playerMatches
    }

    /** True when the player paid for, shares in, or is named on the record. */
    fun involvesPlayer(item: ActivityItem, playerId: Long): Boolean = when (item) {
        is ActivityItem.ExpenseActivity ->
            item.expense.paidByPlayerId == playerId || item.expense.shares.any { it.playerId == playerId }
        is ActivityItem.ContributionActivity -> item.contribution.playerId == playerId
        is ActivityItem.SettlementActivity ->
            item.settlement.fromPlayerId == playerId || item.settlement.toPlayerId == playerId
        is ActivityItem.FineActivity -> item.fine.playerId == playerId
        is ActivityItem.MatchActivity -> false
    }

    /** Groups newest first: month sections, then day sections inside each month. */
    fun group(items: List<ActivityItem>): List<LedgerMonth> =
        items.sortedByDescending { it.timestamp }
            .groupBy { DateTimeUtils.toYearMonth(it.timestamp) }
            .map { (month, monthItems) ->
                LedgerMonth(
                    month = month,
                    expenseTotalMinorUnits = monthItems
                        .filterIsInstance<ActivityItem.ExpenseActivity>()
                        .sumOf { it.expense.amountMinorUnits },
                    days = monthItems.groupBy { DateTimeUtils.toLocalDate(it.timestamp) }
                        .map { (date, dayItems) -> LedgerDay(date, dayItems) }
                )
            }

    fun summarize(items: List<ActivityItem>, month: YearMonth?): AllExpensesSummary {
        val scoped = if (month == null) items else items.filter { DateTimeUtils.toYearMonth(it.timestamp) == month }
        val expenses = scoped.filterIsInstance<ActivityItem.ExpenseActivity>().map { it.expense }
        return AllExpensesSummary(
            totalSpentMinorUnits = expenses.sumOf { it.amountMinorUnits },
            paidFromFundMinorUnits = expenses.filter { it.paidByPlayerId == null }.sumOf { it.amountMinorUnits },
            paidByPlayersMinorUnits = expenses.filter { it.paidByPlayerId != null }.sumOf { it.amountMinorUnits },
            contributionsMinorUnits = scoped.filterIsInstance<ActivityItem.ContributionActivity>()
                .sumOf { it.contribution.amountMinorUnits },
            chargesMinorUnits = scoped.filterIsInstance<ActivityItem.FineActivity>()
                .sumOf { it.fine.amountMinorUnits },
            // Player 0 is the Team Fund's own share, not money owed by a member.
            unpaidSharesMinorUnits = expenses.flatMap { it.shares }
                .filter { it.playerId != 0L }
                .sumOf { it.remainingAmountMinorUnits },
            expenseCount = expenses.size
        )
    }
}
