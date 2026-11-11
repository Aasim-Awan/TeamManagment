package com.teamfund.manager.feature.team

import com.teamfund.manager.core.common.SaveGuard
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
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.FineReason
import com.teamfund.manager.domain.model.Settlement
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
    val expenses: List<Expense> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val fines: List<Fine> = emptyList(),
    val currencyCode: String = "USD",
    val activeTeamId: Long = 0L
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

    private val saveGuard = SaveGuard()
    val isSaving: StateFlow<Boolean> = saveGuard.isSaving

    init {
        loadRoster()
    }

    private fun loadRoster() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collectLatest { activeTeam ->
                    if (activeTeam == null) {
                        _rosterState.value = TeamRosterUiState(isLoading = false)
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
                        val fines = flows[5] as List<Fine>
                        val summaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements,
                            fines = fines
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
                .collectLatest { activeTeam ->
                    if (activeTeam == null) return@collectLatest

                    val teamId = activeTeam.id
                    combine(
                        combine(
                            repository.getAllPlayersFlow(teamId),
                            repository.getContributionsFlow(teamId),
                            repository.getExpensesFlow(teamId),
                            repository.getAllExpenseSharesFlow(teamId),
                            repository.getSettlementsFlow(teamId),
                            repository.getFinesForTeamFlow(teamId),
                        ) { flows: Array<Any> ->
                            TeamFinanceBundle(
                                players = flows[0] as List<Player>,
                                contributions = flows[1] as List<Contribution>,
                                expenses = flows[2] as List<Expense>,
                                expenseShares = flows[3] as List<ExpenseShare>,
                                settlements = flows[4] as List<Settlement>,
                                fines = flows[5] as List<Fine>
                            )
                        },
                        combine(
                            repository.getContributionsForPlayerFlow(playerId),
                            repository.getSettlementsForPlayerFlow(playerId),
                            repository.getFinesForPlayerFlow(playerId),
                            ::Triple
                        )
                    ) { teamFinance, (playerContribs, playerSettlements, playerFines) ->
                        val players = teamFinance.players
                        val contribs = teamFinance.contributions
                        val expenses = teamFinance.expenses
                        val shares = teamFinance.expenseShares
                        val settlements = teamFinance.settlements
                        val fines = teamFinance.fines
                        val summaries = calculatePlayerBalancesUseCase.execute(
                            players = players,
                            contributions = contribs,
                            expenses = expenses,
                            expenseShares = shares,
                            settlements = settlements,
                            fines = fines
                        )

                        val playerSummary = summaries.find { it.player.id == playerId }
                        val playerShares = shares.filter { it.playerId == playerId }
                        val expMap = expenses.associateBy { it.id }

                        PlayerDetailUiState(
                            isLoading = false,
                            summary = playerSummary,
                            contributions = playerContribs,
                            expenseShares = playerShares,
                            expensesMap = expMap,
                            expenses = expenses.sortedByDescending { it.date },
                            settlements = playerSettlements,
                            fines = playerFines,
                            currencyCode = activeTeam.currencyCode,
                            activeTeamId = teamId
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
        cricketRole: com.teamfund.manager.domain.model.CricketRole,
        phoneNumber: String?,
        onSuccess: () -> Unit,
        onDuplicateExisting: (Player) -> Unit = {}
    ) {
        saveGuard.launch(viewModelScope) {
            val teamId = _rosterState.value.activeTeamId
            if (teamId == 0L) return@launch

            val trimmedName = name.trim()
            val existing = if (playerId != null && playerId > 0L) repository.getPlayerById(playerId) else null
            val player = Player(
                id = playerId ?: 0L,
                teamId = teamId,
                name = trimmedName,
                role = role,
                cricketRole = cricketRole,
                phoneNumber = phoneNumber,
                avatarUri = existing?.avatarUri,
                isActive = existing?.isActive ?: true,
                joinedAt = existing?.joinedAt ?: System.currentTimeMillis(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            if (playerId == null || playerId == 0L) {
                val duplicate = repository.findPlayerByName(teamId, trimmedName)
                if (duplicate != null) {
                    onDuplicateExisting(duplicate)
                    return@launch
                }
                repository.addPlayer(player)
            } else {
                repository.updatePlayer(player)
            }
            onSuccess()
        }
    }

    fun reactivatePlayer(playerId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reactivatePlayer(playerId)
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

    fun settleAdjustment(
        playerId: Long,
        mode: SettleAdjustmentMode,
        amountMinorUnits: Long,
        note: String?,
        expenseId: Long?,
        onSuccess: () -> Unit
    ) {
        saveGuard.launch(viewModelScope) {
            val teamId = _detailState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch

            when (mode) {
                SettleAdjustmentMode.PLAYER_OWES -> {
                    repository.addFine(
                        Fine(
                            teamId = teamId,
                            playerId = playerId,
                            amountMinorUnits = amountMinorUnits,
                            reason = FineReason.CHARGE,
                            note = note ?: "Charge"
                        )
                    )
                }
                SettleAdjustmentMode.PLAYER_PAID -> {
                    val settlement = Settlement(
                        teamId = teamId,
                        fromPlayerId = playerId,
                        toPlayerId = null,
                        expenseId = expenseId,
                        amountMinorUnits = amountMinorUnits,
                        adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM,
                        note = note ?: "Balance adjustment"
                    )
                    repository.recordPlayerPayment(settlement, shareId = null, expenseId = expenseId)
                }
                SettleAdjustmentMode.TEAM_PAID_PLAYER -> {
                    val settlement = Settlement(
                        teamId = teamId,
                        fromPlayerId = 0L,
                        toPlayerId = playerId,
                        expenseId = expenseId,
                        amountMinorUnits = amountMinorUnits,
                        adjustmentKind = SettlementAdjustmentKind.TEAM_TO_PLAYER,
                        note = note ?: "Balance adjustment"
                    )
                    repository.addSettlement(settlement)
                }
            }
            onSuccess()
        }
    }

    fun addCharge(playerId: Long, amountMinorUnits: Long, note: String?) {
        viewModelScope.launch {
            val teamId = _detailState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch
            repository.addFine(
                Fine(
                    teamId = teamId,
                    playerId = playerId,
                    amountMinorUnits = amountMinorUnits,
                    reason = FineReason.CHARGE,
                    note = note
                )
            )
        }
    }

    fun deleteFine(fineId: Long) {
        viewModelScope.launch {
            repository.deleteFine(fineId)
        }
    }

    fun addFine(
        playerId: Long,
        amountMinorUnits: Long,
        reason: FineReason,
        note: String?,
        recordPayment: Boolean
    ) {
        viewModelScope.launch {
            val teamId = _detailState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch
            repository.addFineWithSettlement(
                Fine(
                    teamId = teamId,
                    playerId = playerId,
                    amountMinorUnits = amountMinorUnits,
                    reason = reason,
                    note = note
                ),
                recordPayment = recordPayment
            )
        }
    }

    private data class TeamFinanceBundle(
        val players: List<Player>,
        val contributions: List<Contribution>,
        val expenses: List<Expense>,
        val expenseShares: List<ExpenseShare>,
        val settlements: List<Settlement>,
        val fines: List<Fine>
    )

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
