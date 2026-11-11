package com.teamfund.manager.feature.expenses

import com.teamfund.manager.core.common.SaveGuard
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.PlayerRole
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.model.SettleAdjustmentMode
import com.teamfund.manager.domain.model.SettlementAdjustmentKind
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculateExpenseSharesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val selectedCategory: ExpenseCategory? = null,
    val allPlayers: List<Player> = emptyList(),
    val activePlayers: List<Player> = emptyList(),
    val matches: List<Match> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val currencyCode: String = "USD",
    val activeTeamId: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateExpenseSharesUseCase: CalculateExpenseSharesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private val saveGuard = SaveGuard()
    val isSaving: StateFlow<Boolean> = saveGuard.isSaving

    init {
        loadExpenses()
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collectLatest { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = ExpensesUiState(isLoading = false)
                        return@collectLatest
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getExpensesFlow(teamId),
                        repository.getAllPlayersFlow(teamId),
                        repository.getActivePlayersFlow(teamId),
                        repository.getMatchesFlow(teamId),
                        repository.getSettlementsFlow(teamId)
                    ) { flows: Array<Any> ->
                        val expensesList = flows[0] as List<Expense>
                        val allPlayers = flows[1] as List<Player>
                        val activePlayers = flows[2] as List<Player>
                        val matchesList = flows[3] as List<Match>
                        val settlementsList = flows[4] as List<Settlement>
                        ExpensesUiState(
                            isLoading = false,
                            expenses = expensesList,
                            filteredExpenses = filterExpenses(expensesList, _uiState.value.selectedCategory),
                            selectedCategory = _uiState.value.selectedCategory,
                            allPlayers = allPlayers,
                            activePlayers = activePlayers,
                            matches = matchesList,
                            settlements = settlementsList,
                            currencyCode = activeTeam.currencyCode,
                            activeTeamId = teamId
                        )
                    }.collect { newState ->
                        _uiState.value = newState
                    }
                }
        }
    }

    fun setCategoryFilter(category: ExpenseCategory?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            filteredExpenses = filterExpenses(_uiState.value.expenses, category)
        )
    }

    private fun filterExpenses(expenses: List<Expense>, category: ExpenseCategory?): List<Expense> {
        return if (category == null) expenses else expenses.filter { it.category == category }
    }

    fun addPlayerQuick(
        name: String,
        role: PlayerRole = PlayerRole.PLAYER,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || name.isBlank()) return@launch

            val playerId = repository.addPlayer(
                Player(
                    id = 0L,
                    teamId = teamId,
                    name = name.trim(),
                    role = role,
                    phoneNumber = null
                )
            )
            onCreated(playerId)
        }
    }

    fun saveExpense(
        expenseId: Long?,
        title: String,
        category: ExpenseCategory,
        amountMinorUnits: Long,
        paidByPlayerId: Long?,
        matchId: Long?,
        selectedPlayerIdsForSplit: List<Long>,
        manualPlayerAmounts: Map<Long, Long>? = null,
        notes: String?,
        date: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit
    ) {
        saveGuard.launch(viewModelScope) {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch

            val expense = Expense(
                id = expenseId ?: 0L,
                teamId = teamId,
                matchId = matchId,
                title = title,
                category = category,
                amountMinorUnits = amountMinorUnits,
                currencyCode = _uiState.value.currencyCode,
                paidByPlayerId = paidByPlayerId,
                date = date,
                notes = notes
            )

            val baseShares = if (manualPlayerAmounts != null) {
                calculateExpenseSharesUseCase.executeManual(
                    expenseId = expense.id,
                    playerAmounts = manualPlayerAmounts
                )
            } else {
                calculateExpenseSharesUseCase.execute(
                    expenseId = expense.id,
                    totalAmountMinorUnits = amountMinorUnits,
                    playerIds = selectedPlayerIdsForSplit
                )
            }

            // Check if there is an unallocated amount that should be covered by Team Fund
            val allocatedAmount = baseShares.sumOf { it.shareAmountMinorUnits }
            val remainingAmount = amountMinorUnits - allocatedAmount
            // Shares must never add up to more than the expense itself.
            if (remainingAmount < 0L) return@launch

            val finalShares = if (remainingAmount > 0) {
                // If Team Fund paid, this share is auto-settled. 
                // If a Player paid, this share is UNSETTLED (Team owes the player).
                val isTeamShareSettled = paidByPlayerId == null
                baseShares + ExpenseShare(
                    expenseId = expense.id,
                    playerId = 0L, // 0L represents Team Fund
                    shareAmountMinorUnits = remainingAmount,
                    paidAmountMinorUnits = if (isTeamShareSettled) remainingAmount else 0L,
                    isSettled = isTeamShareSettled,
                    settledAt = if (isTeamShareSettled) System.currentTimeMillis() else null
                )
            } else {
                baseShares
            }

            repository.saveExpenseWithShares(expense, finalShares)
            onSuccess()
        }
    }

    fun deleteExpense(expenseId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            onSuccess()
        }
    }

    fun settleAdjustment(
        playerId: Long,
        mode: SettleAdjustmentMode,
        amountMinorUnits: Long,
        note: String?,
        expenseId: Long?,
        shareId: Long? = null,
        onSuccess: () -> Unit
    ) {
        saveGuard.launch(viewModelScope) {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch

            when (mode) {
                SettleAdjustmentMode.PLAYER_OWES,
                SettleAdjustmentMode.TEAM_PAID_PLAYER -> return@launch
                SettleAdjustmentMode.PLAYER_PAID -> {
                    val settlement = Settlement(
                        id = 0L,
                        teamId = teamId,
                        fromPlayerId = playerId,
                        toPlayerId = null,
                        expenseId = expenseId,
                        amountMinorUnits = amountMinorUnits,
                        adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM,
                        date = System.currentTimeMillis(),
                        note = note ?: "Debt settlement"
                    )
                    repository.recordPlayerPayment(settlement, shareId = shareId, expenseId = expenseId)
                }
            }
            onSuccess()
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val calculateExpenseSharesUseCase: CalculateExpenseSharesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExpensesViewModel(repository, userPreferencesRepository, calculateExpenseSharesUseCase) as T
        }
    }
}
