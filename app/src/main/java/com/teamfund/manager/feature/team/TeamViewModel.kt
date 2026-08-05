package com.teamfund.manager.feature.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.domain.model.PlayerRole
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

data class TeamRosterUiState(
    val isLoading: Boolean = true,
    val playerSummaries: List<PlayerFinancialSummary> = emptyList(),
    val currencyCode: String = "USD",
    val activeTeamId: Long = 0L
)

data class PlayerDetailUiState(
    val isLoading: Boolean = true,
    val summary: PlayerFinancialSummary? = null,
    val contributions: List<Contribution> = emptyList(),
    val expenseShares: List<ExpenseShare> = emptyList(),
    val expensesMap: Map<Long, Expense> = emptyMap(),
    val settlements: List<Settlement> = emptyList(),
    val currencyCode: String = "USD"
)

@OptIn(ExperimentalCoroutinesApi::class)
class TeamViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase
) : ViewModel() {

    private val _rosterState = MutableStateFlow(TeamRosterUiState())
    val rosterState: StateFlow<TeamRosterUiState> = _rosterState.asStateFlow()

    private val _detailState = MutableStateFlow(PlayerDetailUiState())
    val detailState: StateFlow<PlayerDetailUiState> = _detailState.asStateFlow()

    init {
        loadRoster()
    }

    private fun loadRoster() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collect { activeTeam ->
                    if (activeTeam == null) {
                        _rosterState.value = TeamRosterUiState(isLoading = false)
                        return@collect
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getAllPlayersFlow(teamId),
                        repository.getContributionsFlow(teamId),
                        repository.getExpensesFlow(teamId),
                        repository.getAllExpenseSharesFlow(teamId),
                        repository.getSettlementsFlow(teamId)
                    ) { players, contribs, expenses, shares, settlements ->
                        val summaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements
                        )

                        TeamRosterUiState(
                            isLoading = false,
                            playerSummaries = summaries,
                            currencyCode = activeTeam.currencyCode,
                            activeTeamId = teamId
                        )
                    }.collect { newState ->
                        _rosterState.value = newState
                    }
                }
        }
    }

    fun loadPlayerDetail(playerId: Long) {
        viewModelScope.launch {
            _detailState.value = PlayerDetailUiState(isLoading = true)

            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collect { activeTeam ->
                    if (activeTeam == null) return@collect

                    val teamId = activeTeam.id
                    combine(
                        repository.getAllPlayersFlow(teamId),
                        repository.getContributionsForPlayerFlow(playerId),
                        repository.getExpensesFlow(teamId),
                        repository.getAllExpenseSharesFlow(teamId),
                        repository.getSettlementsForPlayerFlow(playerId)
                    ) { players, contribs, expenses, shares, settlements ->
                        val summaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements
                        )

                        val playerSummary = summaries.find { it.player.id == playerId }
                        val playerShares = shares.filter { it.playerId == playerId }
                        val expMap = expenses.associateBy { it.id }

                        PlayerDetailUiState(
                            isLoading = false,
                            summary = playerSummary,
                            contributions = contribs,
                            expenseShares = playerShares,
                            expensesMap = expMap,
                            settlements = settlements,
                            currencyCode = activeTeam.currencyCode
                        )
                    }.collect { newState ->
                        _detailState.value = newState
                    }
                }
        }
    }

    fun addOrUpdatePlayer(
        playerId: Long?,
        name: String,
        role: PlayerRole,
        phoneNumber: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val teamId = _rosterState.value.activeTeamId
            if (teamId == 0L) return@launch

            val player = Player(
                id = playerId ?: 0L,
                teamId = teamId,
                name = name,
                role = role,
                phoneNumber = phoneNumber
            )

            if (playerId == null || playerId == 0L) {
                repository.addPlayer(player)
            } else {
                repository.updatePlayer(player)
            }
            onSuccess()
        }
    }

    fun deactivatePlayer(playerId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deactivatePlayer(playerId)
            onSuccess()
        }
    }

    fun deleteContribution(contributionId: Long) {
        viewModelScope.launch {
            repository.deleteContribution(contributionId)
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
            return TeamViewModel(repository, userPreferencesRepository, calculatePlayerBalancesUseCase) as T
        }
    }
}
