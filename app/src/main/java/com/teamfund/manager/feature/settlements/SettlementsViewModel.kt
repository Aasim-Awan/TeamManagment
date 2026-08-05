package com.teamfund.manager.feature.settlements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class SettlementsUiState(
    val isLoading: Boolean = true,
    val playerSummaries: List<PlayerFinancialSummary> = emptyList(),
    val settlementsHistory: List<Settlement> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val matches: List<Match> = emptyList(),
    val currencyCode: String = "USD",
    val activeTeamId: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettlementsViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementsUiState())
    val uiState: StateFlow<SettlementsUiState> = _uiState.asStateFlow()

    init {
        loadSettlements()
    }

    private fun loadSettlements() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collect { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = SettlementsUiState(isLoading = false)
                        return@collect
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getActivePlayersFlow(teamId),
                        repository.getContributionsFlow(teamId),
                        repository.getExpensesFlow(teamId),
                        repository.getAllExpenseSharesFlow(teamId),
                        repository.getSettlementsFlow(teamId),
                        repository.getMatchesFlow(teamId)
                    ) { flows: Array<Any> ->
                        val players = flows[0] as List<Player>
                        val contribs = flows[1] as List<Contribution>
                        val expenses = flows[2] as List<Expense>
                        val shares = flows[3] as List<ExpenseShare>
                        val settlements = flows[4] as List<Settlement>
                        val matches = flows[5] as List<Match>

                        val summaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements
                        )

                        SettlementsUiState(
                            isLoading = false,
                            playerSummaries = summaries,
                            settlementsHistory = settlements.sortedByDescending { it.date },
                            expenses = expenses.sortedByDescending { it.date },
                            matches = matches.sortedByDescending { it.date },
                            currencyCode = activeTeam.currencyCode,
                            activeTeamId = teamId
                        )
                    }.collect { newState ->
                        _uiState.value = newState
                    }
                }
        }
    }

    fun settleDebt(
        fromPlayerId: Long,
        toPlayerId: Long? = null,
        matchId: Long? = null,
        expenseId: Long? = null,
        amountMinorUnits: Long,
        note: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch

            val settlement = Settlement(
                id = 0L,
                teamId = teamId,
                fromPlayerId = fromPlayerId,
                toPlayerId = toPlayerId,
                matchId = matchId,
                expenseId = expenseId,
                amountMinorUnits = amountMinorUnits,
                date = System.currentTimeMillis(),
                note = note ?: "Debt settlement"
            )

            repository.addSettlement(settlement)

            if (expenseId != null) {
                // Update: The payment is recorded for the person/entity sending money
                repository.recordExpenseSharePayment(expenseId, fromPlayerId, amountMinorUnits)
            }

            onSuccess()
        }
    }

    fun deleteSettlement(settlementId: Long) {
        viewModelScope.launch {
            repository.deleteSettlement(settlementId)
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettlementsViewModel(repository, userPreferencesRepository, calculatePlayerBalancesUseCase) as T
        }
    }
}
