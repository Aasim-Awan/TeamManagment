package com.teamfund.manager.feature.kit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.domain.model.InventoryCategory
import com.teamfund.manager.domain.model.InventoryItem
import com.teamfund.manager.domain.model.InventoryMovement
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class KitUiState(
    val isLoading: Boolean = true,
    val items: List<InventoryItem> = emptyList(),
    val activeTeamId: Long = 0L
)

@OptIn(ExperimentalCoroutinesApi::class)
class KitViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KitUiState())
    val uiState: StateFlow<KitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .collectLatest { team ->
                    if (team == null) {
                        _uiState.value = KitUiState(isLoading = false)
                        return@collectLatest
                    }
                    repository.getInventoryItemsFlow(team.id).collect { items ->
                        _uiState.value = KitUiState(
                            isLoading = false,
                            items = items,
                            activeTeamId = team.id
                        )
                    }
                }
        }
    }

    fun addItem(name: String, category: InventoryCategory, quantity: Int) {
        viewModelScope.launch {
            val teamId = _uiState.value.activeTeamId
            if (teamId == 0L || name.isBlank()) return@launch
            repository.saveInventoryItem(
                InventoryItem(teamId = teamId, name = name.trim(), category = category, quantity = quantity)
            )
        }
    }

    fun adjustStock(itemId: Long, delta: Int, note: String?) {
        viewModelScope.launch {
            if (delta == 0) return@launch
            repository.adjustInventoryItem(
                itemId,
                delta,
                InventoryMovement(itemId = itemId, delta = delta, note = note)
            )
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KitViewModel(repository, userPreferencesRepository) as T
        }
    }
}
