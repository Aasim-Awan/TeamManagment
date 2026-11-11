package com.teamfund.manager.feature.contributions

import com.teamfund.manager.core.common.SaveGuard
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
import kotlinx.coroutines.flow.collectLatest

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

    private val saveGuard = SaveGuard()
    val isSaving: StateFlow<Boolean> = saveGuard.isSaving

    init {
        loadContributions()
    }

    private fun loadContributions() {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collectLatest { activeTeam ->
                    if (activeTeam == null) {
                        _uiState.value = ContributionsUiState(isLoading = false)
                        return@collectLatest
                    }

                    val teamId = activeTeam.id
                    combine(
                        repository.getContributionsFlow(teamId),
                        repository.getAllPlayersFlow(teamId),
                        repository.getActivePlayersFlow(teamId)
                    ) { flows: Array<Any> ->
                        val contribList = flows[0] as List<com.teamfund.manager.domain.model.Contribution>
                        val allPlayers = flows[1] as List<Player>
                        val activePlayers = flows[2] as List<Player>
                        val pMap = allPlayers.associateBy { it.id }
                        ContributionsUiState(
                            isLoading = false,
                            contributions = contribList,
                            playersMap = pMap,
                            players = activePlayers,
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
        saveGuard.launch(viewModelScope) {
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
