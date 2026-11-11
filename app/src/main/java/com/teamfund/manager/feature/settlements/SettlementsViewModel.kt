package com.teamfund.manager.feature.settlements

import com.teamfund.manager.core.common.SaveGuard
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
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.FineReason
import com.teamfund.manager.domain.model.SettleAdjustmentMode
import com.teamfund.manager.domain.model.SettlementAdjustmentKind
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

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

    private val saveGuard = SaveGuard()
    val isSaving: StateFlow<Boolean> = saveGuard.isSaving

    init {
        loadSettlements()
    }

    private fun loadSettlements() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collectLatest { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = SettlementsUiState(isLoading = false)
                        return@collectLatest
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getAllPlayersFlow(teamId),
                        repository.getContributionsFlow(teamId),
                        repository.getExpensesFlow(teamId),
                        repository.getAllExpenseSharesFlow(teamId),
                        repository.getSettlementsFlow(teamId),
                        repository.getMatchesFlow(teamId),
                        repository.getFinesForTeamFlow(teamId)
                    ) { flows: Array<Any> ->
                        val players = flows[0] as List<Player>
                        val contribs = flows[1] as List<Contribution>
                        val expenses = flows[2] as List<Expense>
                        val shares = flows[3] as List<ExpenseShare>
                        val settlements = flows[4] as List<Settlement>
                        val matches = flows[5] as List<Match>
                        val fines = flows[6] as List<com.teamfund.manager.domain.model.Fine>

                        val summaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements,
                            fines = fines
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

    fun settleAdjustment(
        playerId: Long,
        mode: SettleAdjustmentMode,
        amountMinorUnits: Long,
        note: String?,
        expenseId: Long?,
        matchId: Long? = null,
        onSuccess: () -> Unit
    ) {
        saveGuard.launch(viewModelScope) {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch

            when (mode) {
                SettleAdjustmentMode.PLAYER_OWES -> {
                    repository.addFine(
                        Fine(
                            teamId = teamId,
                            playerId = playerId,
                            matchId = matchId,
                            amountMinorUnits = amountMinorUnits,
                            reason = FineReason.CHARGE,
                            note = note ?: "Charge"
                        )
                    )
                }
                SettleAdjustmentMode.PLAYER_PAID -> {
                    val settlement = Settlement(
                        id = 0L,
                        teamId = teamId,
                        fromPlayerId = playerId,
                        toPlayerId = null,
                        matchId = matchId,
                        expenseId = expenseId,
                        amountMinorUnits = amountMinorUnits,
                        adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM,
                        date = System.currentTimeMillis(),
                        note = note ?: "Debt settlement"
                    )
                    repository.recordPlayerPayment(settlement, shareId = null, expenseId = expenseId)
                }
                SettleAdjustmentMode.TEAM_PAID_PLAYER -> {
                    val settlement = Settlement(
                        id = 0L,
                        teamId = teamId,
                        fromPlayerId = 0L,
                        toPlayerId = playerId,
                        matchId = matchId,
                        expenseId = expenseId,
                        amountMinorUnits = amountMinorUnits,
                        adjustmentKind = SettlementAdjustmentKind.TEAM_TO_PLAYER,
                        date = System.currentTimeMillis(),
                        note = note ?: "Debt settlement"
                    )
                    repository.addSettlement(settlement)
                }
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
