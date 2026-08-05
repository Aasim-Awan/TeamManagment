package com.teamfund.manager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.core.util.CsvExporter
import com.teamfund.manager.core.util.CsvImporter
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.core.util.MoneyFormatter
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.ContributionPurpose
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.model.Team
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculateExpenseSharesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val activeTeam: Team? = null,
    val allTeams: List<Team> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val repository: TeamFundRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calculateExpenseSharesUseCase: CalculateExpenseSharesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.activeTeamIdFlow.flatMapLatest { activeId ->
                    repository.getActiveTeamFlow(activeId)
                },
                repository.getAllTeamsFlow(),
                userPreferencesRepository.notificationsEnabledFlow,
                userPreferencesRepository.themeModeFlow
            ) { team, teams, notifs, theme ->
                SettingsUiState(
                    isLoading = false,
                    activeTeam = team,
                    allTeams = teams,
                    notificationsEnabled = notifs,
                    themeMode = theme,
                    statusMessage = _uiState.value.statusMessage
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun updateTeamCurrency(currencyCode: String) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            repository.updateTeam(team.copy(currencyCode = currencyCode))
        }
    }

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun switchActiveTeam(teamId: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setActiveTeamId(teamId)
            repository.setActiveTeam(teamId)
        }
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            userPreferencesRepository.clearActiveTeamId()
            _uiState.value = _uiState.value.copy(statusMessage = "All data cleared")
            onDone()
        }
    }

    suspend fun exportContributionsCsv(): String? {
        val team = _uiState.value.activeTeam ?: return null
        val contributions = repository.getContributionsFlow(team.id).first()
        val players = repository.getActivePlayersFlow(team.id).first().associateBy { it.id }
        return CsvExporter.exportContributions(contributions, players)
    }

    suspend fun exportExpensesCsv(): String? {
        val team = _uiState.value.activeTeam ?: return null
        val expenses = repository.getExpensesFlow(team.id).first()
        val players = repository.getActivePlayersFlow(team.id).first().associateBy { it.id }
        return CsvExporter.exportExpenses(expenses, players)
    }

    fun importContributionsFromCsv(csvContent: String) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            val playersByName = repository.getActivePlayersFlow(team.id).first()
                .associateBy { it.name.lowercase() }
            val parsed = CsvImporter.parseContributions(csvContent)
            var count = 0
            parsed.forEach { item ->
                val player = playersByName[item.playerName.lowercase()] ?: return@forEach
                repository.addContribution(
                    Contribution(
                        teamId = team.id,
                        playerId = player.id,
                        amountMinorUnits = MoneyFormatter.toMinorUnits(item.amount),
                        date = DateTimeUtils.parseDate(item.dateStr),
                        purpose = ContributionPurpose.entries.find { it.displayName == item.purposeStr }
                            ?: ContributionPurpose.GENERAL,
                        notes = item.notes
                    )
                )
                count++
            }
            _uiState.value = _uiState.value.copy(statusMessage = "Imported $count contributions")
        }
    }

    fun importExpensesFromCsv(csvContent: String) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            val players = repository.getActivePlayersFlow(team.id).first()
            val playersByName = players.associateBy { it.name.lowercase() }
            val playerIds = players.map { it.id }
            val parsed = CsvImporter.parseExpenses(csvContent)
            var count = 0
            parsed.forEach { item ->
                val paidByPlayer = playersByName[item.paidByStr.lowercase()]
                val amountMinor = MoneyFormatter.toMinorUnits(item.amount)
                if (amountMinor <= 0L) return@forEach
                val expense = Expense(
                    teamId = team.id,
                    title = item.title,
                    category = ExpenseCategory.entries.find { it.displayName == item.categoryStr }
                        ?: ExpenseCategory.OTHER,
                    amountMinorUnits = amountMinor,
                    currencyCode = team.currencyCode,
                    paidByPlayerId = paidByPlayer?.id,
                    date = DateTimeUtils.parseDate(item.dateStr),
                    notes = item.notes
                )
                val shares = calculateExpenseSharesUseCase.execute(
                    expenseId = 0L,
                    totalAmountMinorUnits = amountMinor,
                    playerIds = playerIds
                )
                repository.saveExpenseWithShares(expense, shares)
                count++
            }
            _uiState.value = _uiState.value.copy(statusMessage = "Imported $count expenses")
        }
    }

    class Factory(
        private val repository: TeamFundRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val calculateExpenseSharesUseCase: CalculateExpenseSharesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, userPreferencesRepository, calculateExpenseSharesUseCase) as T
        }
    }
}
