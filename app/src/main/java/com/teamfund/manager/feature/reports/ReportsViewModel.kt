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
import com.teamfund.manager.domain.usecase.ImportCsvUseCase
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import com.teamfund.manager.domain.usecase.CalculateTeamBalanceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

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
                .collectLatest { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = ReportsUiState(isLoading = false)
                        return@collectLatest
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getAllPlayersFlow(teamId),
                        repository.getContributionsFlow(teamId),
                        repository.getExpensesFlow(teamId),
                        repository.getAllExpenseSharesFlow(teamId),
                        repository.getSettlementsFlow(teamId),
                        repository.getFinesForTeamFlow(teamId)
                    ) { flows: Array<Any> ->
                        val players = flows[0] as List<Player>
                        val contribs = flows[1] as List<Contribution>
                        val expenses = flows[2] as List<Expense>
                        val shares = flows[3] as List<ExpenseShare>
                        val settlements = flows[4] as List<Settlement>
                        val fines = flows[5] as List<com.teamfund.manager.domain.model.Fine>
                        val playerSummaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements,
                            fines = fines
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

    private val importCsvUseCase = ImportCsvUseCase(repository)

    fun importContributionsFromCsv(csvContent: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L) return@launch
            val result = importCsvUseCase.importContributions(teamId, csvContent)
            onDone(result.message("contributions"))
        }
    }

    fun importExpensesFromCsv(csvContent: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L) return@launch
            val result = importCsvUseCase.importExpenses(teamId, _uiState.value.currencyCode, csvContent)
            onDone(result.message("expenses (splits are not in the file)"))
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
