package com.teamfund.manager.feature.matches

import com.teamfund.manager.core.common.SaveGuard
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.core.util.CsvExporter
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.MatchPlayer
import com.teamfund.manager.domain.model.MatchPlayerStats
import com.teamfund.manager.domain.model.MatchPlayerStatus
import com.teamfund.manager.domain.model.MatchResult
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.Season
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.model.SportType
import com.teamfund.manager.domain.model.Team
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class MatchesUiState(
    val isLoading: Boolean = true,
    val matches: List<Match> = emptyList(),
    val filteredMatches: List<Match> = emptyList(),
    val selectedFilter: MatchFilter = MatchFilter.ALL,
    val currencyCode: String = "USD",
    val activeTeamId: Long = 0L,
    val sport: SportType = SportType.CRICKET,
    val players: List<Player> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val activeSeason: Season? = null
)

enum class MatchFilter(val displayName: String) {
    ALL("All Matches"),
    PLAYED("Played"),
    UPCOMING("Upcoming")
}

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val match: Match? = null,
    val expenses: List<Expense> = emptyList(),
    val playerMap: Map<Long, Player> = emptyMap(),
    val playerOfTheMatch: Player? = null,
    val matchPlayers: List<MatchPlayer> = emptyList(),
    val playerStats: List<MatchPlayerStats> = emptyList(),
    val fines: List<Fine> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val currencyCode: String = "USD"
)

@OptIn(ExperimentalCoroutinesApi::class)
class MatchesViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private val saveGuard = SaveGuard()
    val isSaving: StateFlow<Boolean> = saveGuard.isSaving

    private val _detailState = MutableStateFlow(MatchDetailUiState())
    val detailState: StateFlow<MatchDetailUiState> = _detailState.asStateFlow()

    init {
        loadMatches()
    }

    private fun loadMatches() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collectLatest { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = MatchesUiState(isLoading = false)
                        return@collectLatest
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getActiveSeasonFlow(teamId),
                        repository.getSeasonsFlow(teamId),
                        repository.getActivePlayersFlow(teamId)
                    ) { activeSeason, seasons, players ->
                        Triple(activeSeason, seasons, players)
                    }.flatMapLatest { (activeSeason, seasons, players) ->
                        repository.getMatchesFlow(teamId, activeSeason?.id).map { matchesList ->
                            val filter = _uiState.value.selectedFilter
                            MatchesUiState(
                                isLoading = false,
                                matches = matchesList,
                                filteredMatches = filterMatches(matchesList, filter),
                                selectedFilter = filter,
                                currencyCode = activeTeam.currencyCode,
                                activeTeamId = teamId,
                                sport = activeTeam.sport,
                                players = players,
                                seasons = seasons,
                                activeSeason = activeSeason
                            )
                        }
                    }.collect { newState ->
                        _uiState.value = newState
                    }
                }
        }
    }

    fun setFilter(filter: MatchFilter) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredMatches = filterMatches(_uiState.value.matches, filter)
        )
    }

    fun setActiveSeason(seasonId: Long) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L) return@launch
            repository.setActiveSeason(teamId, seasonId)
        }
    }

    fun createSeason(name: String, startDate: Long) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || name.isBlank()) return@launch
            val id = repository.saveSeason(
                Season(teamId = teamId, name = name.trim(), startDate = startDate, isActive = false)
            )
            repository.setActiveSeason(teamId, id)
        }
    }

    private fun filterMatches(matches: List<Match>, filter: MatchFilter): List<Match> {
        return when (filter) {
            MatchFilter.ALL -> matches
            MatchFilter.PLAYED -> matches.filter { it.result != MatchResult.UPCOMING }
            MatchFilter.UPCOMING -> matches.filter { it.result == MatchResult.UPCOMING }
        }
    }

    fun loadMatchDetail(matchId: Long) {
        viewModelScope.launch {
            _detailState.value = MatchDetailUiState(isLoading = true)

            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .flatMapLatest { team ->
                    if (team == null) {
                        return@flatMapLatest kotlinx.coroutines.flow.flowOf(
                            MatchDetailUiState(isLoading = false)
                        )
                    }
                    combine(
                        combine(
                            repository.getMatchByIdFlow(matchId),
                            repository.getMatchPlayersFlow(matchId),
                            repository.getMatchPlayerStatsFlow(matchId),
                            ::Triple
                        ),
                        combine(
                            repository.getExpensesForMatchFlow(matchId),
                            repository.getAllPlayersFlow(team.id),
                            repository.getSettlementsFlow(team.id),
                            ::Triple
                        )
                    ) { (match, matchPlayers, stats), (expenses, players, settlements) ->
                        val map = players.associateBy { it.id }
                        MatchDetailUiState(
                            isLoading = false,
                            match = match,
                            expenses = expenses,
                            playerMap = map,
                            playerOfTheMatch = match?.playerOfTheMatchId?.let { map[it] },
                            matchPlayers = matchPlayers,
                            playerStats = stats,
                            fines = emptyList(),
                            settlements = settlements.filter { it.matchId == matchId },
                            currencyCode = team.currencyCode
                        )
                    }
                }
                .collectLatest { state ->
                    if (state.match != null) {
                        val fines = repository.getFinesForMatch(matchId)
                        _detailState.value = state.copy(fines = fines)
                    } else {
                        _detailState.value = state
                    }
                }
        }
    }

    fun saveMatch(
        matchId: Long?,
        opponentName: String,
        date: Long,
        venue: String?,
        result: MatchResult,
        teamScore: String?,
        opponentScore: String?,
        overs: String?,
        tossWinner: String?,
        playerOfTheMatchId: Long?,
        notes: String?,
        seasonId: Long?,
        matchFeeMinorUnits: Long,
        groundFeeMinorUnits: Long,
        selectedXiPlayerIds: Set<Long>,
        chargeMatchFee: Boolean,
        createGroundExpense: Boolean,
        onSuccess: () -> Unit
    ) {
        saveGuard.launch(viewModelScope) {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L) return@launch

            val existing = matchId?.let { repository.getMatchById(it) }
            val matchToSave = Match(
                id = matchId ?: 0L,
                teamId = teamId,
                opponentName = opponentName,
                date = date,
                venue = venue,
                result = result,
                teamScore = teamScore,
                opponentScore = opponentScore,
                overs = overs,
                tossWinner = tossWinner,
                playerOfTheMatchId = playerOfTheMatchId,
                seasonId = seasonId ?: _uiState.value.activeSeason?.id,
                matchFeeMinorUnits = matchFeeMinorUnits,
                groundFeeMinorUnits = groundFeeMinorUnits,
                notes = notes,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val rosterIds = _uiState.value.players.map { it.id }.toSet()
            val matchPlayers = rosterIds.map { pid ->
                MatchPlayer(
                    matchId = matchToSave.id,
                    playerId = pid,
                    status = if (selectedXiPlayerIds.contains(pid)) {
                        MatchPlayerStatus.SELECTED
                    } else {
                        MatchPlayerStatus.AVAILABLE
                    }
                )
            }
            // Saves the match and its roster together; returns the real id on both create and edit.
            val savedId = repository.saveMatchWithRoster(matchToSave, matchPlayers)

            if (chargeMatchFee && matchFeeMinorUnits > 0L && selectedXiPlayerIds.isNotEmpty()) {
                repository.chargeMatchFeeToXi(teamId, savedId, matchFeeMinorUnits, selectedXiPlayerIds.toList())
            }

            val savedMatch = matchToSave.copy(id = savedId)
            if (createGroundExpense && groundFeeMinorUnits > 0L) {
                repository.createGroundFeeExpenseForMatch(teamId, savedMatch, _uiState.value.currencyCode)
            }

            onSuccess()
        }
    }

    suspend fun fetchSelectedXi(matchId: Long): Set<Long> {
        return repository.getMatchPlayers(matchId)
            .filter { it.status == MatchPlayerStatus.SELECTED }
            .map { it.playerId }
            .toSet()
    }

    fun savePlayerStats(matchId: Long, stats: List<MatchPlayerStats>) {
        viewModelScope.launch {
            repository.saveMatchPlayerStats(stats)
        }
    }

    fun exportMatchCsv(): String? {
        val detail = _detailState.value
        val match = detail.match ?: return null
        return CsvExporter.exportMatchPack(
            match = match,
            players = detail.playerMap,
            matchPlayers = detail.matchPlayers,
            expenses = detail.expenses,
            settlements = detail.settlements,
            fines = detail.fines,
            stats = detail.playerStats,
            currencyCode = detail.currencyCode
        )
    }

    fun deleteMatch(matchId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
            onSuccess()
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MatchesViewModel(repository, userPreferencesRepository) as T
        }
    }
}
