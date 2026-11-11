package com.teamfund.manager.feature.dashboard

import com.teamfund.manager.core.util.DateTimeUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.domain.model.ActivityItem
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.MatchResult
import com.teamfund.manager.domain.model.MatchStatsSummary
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.domain.model.Team
import com.teamfund.manager.domain.model.TeamFinancialSummary
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import com.teamfund.manager.domain.usecase.CalculateTeamBalanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val team: Team? = null,
    val allTeams: List<Team> = emptyList(),
    val financialSummary: TeamFinancialSummary = TeamFinancialSummary(),
    val playerSummaries: List<PlayerFinancialSummary> = emptyList(),
    val matchStats: MatchStatsSummary = MatchStatsSummary(),
    val upcomingMatch: Match? = null,
    val recentActivities: List<ActivityItem> = emptyList(),
    /** Sum of all expenses dated in the current calendar month (shown on the Home top card). */
    val thisMonthExpensesMinorUnits: Long = 0L,
    val currencyCode: String = "USD"
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase,
    private val calculateTeamBalanceUseCase: CalculateTeamBalanceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId ->
                    combine(
                        repository.getActiveTeamFlow(activeId),
                        repository.getAllTeamsFlow()
                    ) { activeTeam, allTeams ->
                        activeTeam to allTeams
                    }
                }
                .flatMapLatest { (activeTeam, allTeams) ->
                    if (activeTeam == null) {
                        MutableStateFlow(
                            DashboardUiState(
                                isLoading = false,
                                team = null,
                                allTeams = allTeams
                            )
                        )
                    } else {
                        val teamId = activeTeam.id
                        val financialFlow = combine(
                            repository.getAllPlayersFlow(teamId),
                            repository.getContributionsFlow(teamId),
                            repository.getExpensesFlow(teamId)
                        ) { players, contribs, expenses ->
                            Triple(players, contribs, expenses)
                        }

                        val activityFlow = combine(
                            repository.getAllExpenseSharesFlow(teamId),
                            repository.getSettlementsFlow(teamId),
                            repository.getMatchesFlow(teamId),
                            repository.getFinesForTeamFlow(teamId)
                        ) { shares, settlements, matches, fines ->
                            listOf(shares, settlements, matches, fines)
                        }

                        combine(financialFlow, activityFlow) { (players, contribs, expenses), activity ->
                            val shares = activity[0] as List<com.teamfund.manager.domain.model.ExpenseShare>
                            val settlements = activity[1] as List<com.teamfund.manager.domain.model.Settlement>
                            val matches = activity[2] as List<com.teamfund.manager.domain.model.Match>
                            val fines = activity[3] as List<com.teamfund.manager.domain.model.Fine>
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

                            val playedMatches = matches.filter { it.result != MatchResult.UPCOMING }
                            val wins = playedMatches.count { it.result == MatchResult.WON }
                            val losses = playedMatches.count { it.result == MatchResult.LOST }
                            val draws = playedMatches.count { it.result == MatchResult.DRAW }
                            val noResults = playedMatches.count { it.result == MatchResult.NO_RESULT }

                            val stats = MatchStatsSummary(
                                matchesPlayed = playedMatches.size,
                                wins = wins,
                                losses = losses,
                                draws = draws,
                                noResults = noResults
                            )

                            val upcoming = matches.filter { it.result == MatchResult.UPCOMING }
                                .minByOrNull { it.date }

                            val playerMap = players.associateBy { it.id }

                            val monthRange = DateTimeUtils.currentMonthRange()
                            val thisMonthExpenses = expenses
                                .filter { it.date in monthRange }
                                .sumOf { it.amountMinorUnits }

                            val expActivities = expenses.take(5).map { exp ->
                                ActivityItem.ExpenseActivity(
                                    id = exp.id,
                                    expense = exp,
                                    paidByPlayerName = exp.paidByPlayerId?.let { playerMap[it]?.name }
                                )
                            }

                            val contribActivities = contribs.take(5).map { c ->
                                ActivityItem.ContributionActivity(
                                    id = c.id,
                                    contribution = c,
                                    playerName = playerMap[c.playerId]?.name ?: "Member"
                                )
                            }

                            val matchActivities = matches.take(3).map { m ->
                                ActivityItem.MatchActivity(id = m.id, match = m)
                            }

                            val settlementActivities = settlements.take(5).map { s ->
                                ActivityItem.SettlementActivity(
                                    id = s.id,
                                    settlement = s,
                                    fromPlayerName = playerMap[s.fromPlayerId]?.name ?: "Member",
                                    toPlayerName = s.toPlayerId?.let { playerMap[it]?.name }
                                )
                            }

                            val allActivities = (expActivities + contribActivities + matchActivities + settlementActivities)
                                .sortedByDescending { it.timestamp }
                                .take(10)

                            DashboardUiState(
                                isLoading = false,
                                team = activeTeam,
                                allTeams = allTeams,
                                financialSummary = teamSummary,
                                playerSummaries = playerSummaries,
                                matchStats = stats,
                                upcomingMatch = upcoming,
                                recentActivities = allActivities,
                                thisMonthExpensesMinorUnits = thisMonthExpenses,
                                currencyCode = activeTeam.currencyCode
                            )
                        }
                    }
                }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    fun switchTeam(teamId: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setActiveTeamId(teamId)
            repository.setActiveTeam(teamId)
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val calculatePlayerBalancesUseCase: CalculatePlayerBalancesUseCase,
        private val calculateTeamBalanceUseCase: CalculateTeamBalanceUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, userPreferencesRepository, calculatePlayerBalancesUseCase, calculateTeamBalanceUseCase) as T
        }
    }
}
