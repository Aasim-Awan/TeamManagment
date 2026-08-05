package com.teamfund.manager.feature.contributions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.ContributionPurpose
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class ContributionsUiState(
    val isLoading: Boolean = true,
    val contributions: List<Contribution> = emptyList(),
    val playersMap: Map<Long, Player> = emptyMap(),
    val players: List<Player> = emptyList(),
    val currencyCode: String = "USD",
    val activeTeamId: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
class ContributionsViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContributionsUiState())
    val uiState: StateFlow<ContributionsUiState> = _uiState.asStateFlow()

    init {
        loadContributions()
    }

    private fun loadContributions() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collect { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = ContributionsUiState(isLoading = false)
                        return@collect
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getContributionsFlow(teamId),
                        repository.getActivePlayersFlow(teamId)
                    ) { contribList, playerList ->
                        val pMap = playerList.associateBy { it.id }
                        ContributionsUiState(
                            isLoading = false,
                            contributions = contribList,
                            playersMap = pMap,
                            players = playerList,
                            currencyCode = activeTeam.currencyCode,
                            activeTeamId = teamId
                        )
                    }.collect { newState ->
                        _uiState.value = newState
                    }
                }
        }
    }

    fun addContribution(
        playerId: Long,
        amountMinorUnits: Long,
        purpose: ContributionPurpose,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || amountMinorUnits <= 0L) return@launch

            val contrib = Contribution(
                id = 0L,
                teamId = teamId,
                playerId = playerId,
                amountMinorUnits = amountMinorUnits,
                date = System.currentTimeMillis(),
                purpose = purpose,
                notes = notes
            )

            repository.addContribution(contrib)
            onSuccess()
        }
    }

    fun deleteContribution(contributionId: Long) {
        viewModelScope.launch {
            repository.deleteContribution(contributionId)
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContributionsViewModel(repository, userPreferencesRepository) as T
        }
    }
}
