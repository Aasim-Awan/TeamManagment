package com.teamfund.manager.feature.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.MatchResult
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.SportType
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class MatchesUiState(
    val isLoading: Boolean = true,
    val matches: List<Match> = emptyList(),
    val filteredMatches: List<Match> = emptyList(),
    val selectedFilter: MatchFilter = MatchFilter.ALL,
    val currencyCode: String = "USD",
    val activeTeamId: Long = 0L,
    val sport: SportType = SportType.CRICKET
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
    val currencyCode: String = "USD"
)

@OptIn(ExperimentalCoroutinesApi::class)
class MatchesViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(MatchDetailUiState())
    val detailState: StateFlow<MatchDetailUiState> = _detailState.asStateFlow()

    init {
        loadMatches()
    }

    private fun loadMatches() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collect { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = MatchesUiState(isLoading = false)
                        return@collect
                    }

                    repository.getMatchesFlow(activeTeam.id).collect { matchesList ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            matches = matchesList,
                            filteredMatches = filterMatches(matchesList, _uiState.value.selectedFilter),
                            currencyCode = activeTeam.currencyCode,
                            activeTeamId = activeTeam.id,
                            sport = activeTeam.sport
                        )
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
                .flatMapLatest { activeId ->
                    combine(
                        repository.getActiveTeamFlow(activeId),
                        repository.getMatchByIdFlow(matchId)
                    ) { team, match -> team to match }
                }
                .collect { (team, match) ->
                    if (match == null || team == null) {
                        _detailState.value = MatchDetailUiState(isLoading = false)
                        return@collect
                    }

                    val players = repository.getActivePlayersFlow(team.id)
                    val expensesFlow = repository.getExpensesForMatchFlow(matchId)

                    combine(players, expensesFlow) { playerList, expenseList ->
                        val map = playerList.associateBy { it.id }
                        val potm = match.playerOfTheMatchId?.let { map[it] }

                        MatchDetailUiState(
                            isLoading = false,
                            match = match,
                            expenses = expenseList,
                            playerMap = map,
                            playerOfTheMatch = potm,
                            currencyCode = team.currencyCode
                        )
                    }.collect { newState ->
                        _detailState.value = newState
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L) return@launch

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
                notes = notes
            )

            repository.saveMatch(matchToSave)
            onSuccess()
        }
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
