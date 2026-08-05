package com.teamfund.manager.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.core.util.CsvExporter
import com.teamfund.manager.core.util.CsvImporter
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.core.util.MoneyFormatter
import com.teamfund.manager.domain.model.*
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculateExpenseSharesUseCase
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import com.teamfund.manager.domain.usecase.CalculateTeamBalanceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryBreakdown(
    val category: ExpenseCategory,
    val amountMinorUnits: Long,
    val percentage: Float
)

data class ReportsUiState(
    val isLoading: Boolean = true,
    val teamName: String = "",
    val currencyCode: String = "USD",
    val financialSummary: TeamFinancialSummary = TeamFinancialSummary(),
    val playerSummaries: List<PlayerFinancialSummary> = emptyList(),
    val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val players: Map<Long, Player> = emptyMap(),
    val activeTeamId: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase,
    private val calculateTeamBalanceUseCase: CalculateTeamBalanceUseCase,
    private val calculateExpenseSharesUseCase: CalculateExpenseSharesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReportData()
    }

    private fun loadReportData() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collect { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = ReportsUiState(isLoading = false)
                        return@collect
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getActivePlayersFlow(teamId),
                        repository.getContributionsFlow(teamId),
                        repository.getExpensesFlow(teamId),
                        repository.getAllExpenseSharesFlow(teamId),
                        repository.getSettlementsFlow(teamId)
                    ) { players, contribs, expenses, shares, settlements ->
                        val playerSummaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements
                        )

                        val teamSummary = calculateTeamBalanceUseCase.execute(
                            contributions = contribs,
                            expenses = expenses,
                            settlements = settlements,
                            playerSummaries = playerSummaries
                        )

                        val totalExpenseAmount = expenses.sumOf { it.amountMinorUnits }
                        val catMap = expenses.groupBy { it.category }

                        val catBreakdowns = catMap.map { (cat, list) ->
                            val amt = list.sumOf { it.amountMinorUnits }
                            val pct = if (totalExpenseAmount > 0) (amt.toFloat() / totalExpenseAmount.toFloat()) * 100f else 0f
                            CategoryBreakdown(cat, amt, pct)
                        }.sortedByDescending { it.amountMinorUnits }

                        ReportsUiState(
                            isLoading = false,
                            teamName = activeTeam.name,
                            currencyCode = activeTeam.currencyCode,
                            financialSummary = teamSummary,
                            playerSummaries = playerSummaries,
                            categoryBreakdowns = catBreakdowns,
                            contributions = contribs,
                            expenses = expenses,
                            players = players.associateBy { it.id },
                            activeTeamId = teamId
                        )
                    }.collect { newState ->
                        _uiState.value = newState
                    }
                }
        }
    }

    fun exportMasterReport(): String {
        val state = uiState.value
        return CsvExporter.exportMasterReport(
            teamName = state.teamName,
            expenses = state.expenses,
            players = state.playerSummaries,
            currencyCode = state.currencyCode
        )
    }

    fun importContributionsFromCsv(csvContent: String) {
        val parsed = CsvImporter.parseContributions(csvContent)
        val playersByName = uiState.value.players.values.associateBy { it.name.lowercase() }

        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L) return@launch
            parsed.forEach { item ->
                val player = playersByName[item.playerName.lowercase()] ?: return@forEach
                val contribution = Contribution(
                    teamId = teamId,
                    playerId = player.id,
                    amountMinorUnits = MoneyFormatter.toMinorUnits(item.amount),
                    date = DateTimeUtils.parseDate(item.dateStr),
                    purpose = ContributionPurpose.entries.find { it.displayName == item.purposeStr } ?: ContributionPurpose.GENERAL,
                    notes = item.notes
                )
                repository.addContribution(contribution)
            }
        }
    }

    fun importExpensesFromCsv(csvContent: String) {
        val parsed = CsvImporter.parseExpenses(csvContent)
        val players = uiState.value.players.values.toList()
        val playersByName = players.associateBy { it.name.lowercase() }
        val playerIds = players.map { it.id }

        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L) return@launch
            val currencyCode = _uiState.value.currencyCode
            parsed.forEach { item ->
                val paidByPlayer = playersByName[item.paidByStr.lowercase()]
                val amountMinor = MoneyFormatter.toMinorUnits(item.amount)
                if (amountMinor <= 0L) return@forEach
                val expense = Expense(
                    teamId = teamId,
                    title = item.title,
                    category = ExpenseCategory.entries.find { it.displayName == item.categoryStr } ?: ExpenseCategory.OTHER,
                    amountMinorUnits = amountMinor,
                    currencyCode = currencyCode,
                    paidByPlayerId = paidByPlayer?.id,
                    date = DateTimeUtils.parseDate(item.dateStr),
                    notes = item.notes
                )
                val shares = calculateExpenseSharesUseCase.execute(
                    expenseId = 0L,
                    totalAmountMinorUnits = amountMinor,
                    playerIds = playerIds
                )
                repository.saveExpenseWithShares(expense, shares)
            }
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase,
        private val calculateTeamBalanceUseCase: CalculateTeamBalanceUseCase,
        private val calculateExpenseSharesUseCase: CalculateExpenseSharesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(
                repository,
                userPreferencesRepository,
                calculatePlayerBalancesUseCase,
                calculateTeamBalanceUseCase,
                calculateExpenseSharesUseCase
            ) as T
        }
    }
}
