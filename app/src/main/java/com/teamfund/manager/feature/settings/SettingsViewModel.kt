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
import com.teamfund.manager.domain.model.DuePeriod
import com.teamfund.manager.domain.model.DueSchedule
import com.teamfund.manager.domain.model.Season
import com.teamfund.manager.domain.model.Team
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculateExpenseSharesUseCase
import com.teamfund.manager.domain.usecase.ImportCsvUseCase
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
    val notificationsEnabled: Boolean = false,
    val themeMode: String = "SYSTEM",
    val statusMessage: String? = null,
    val seasons: List<Season> = emptyList(),
    val activeSeason: Season? = null,
    val dueSchedules: List<DueSchedule> = emptyList()
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
            userPreferencesRepository.activeTeamIdFlow
                .flatMapLatest { activeId -> repository.getActiveTeamFlow(activeId) }
                .flatMapLatest { team ->
                    if (team == null) {
                        return@flatMapLatest combine(
                            repository.getAllTeamsFlow(),
                            userPreferencesRepository.notificationsEnabledFlow,
                            userPreferencesRepository.themeModeFlow
                        ) { teams, notifs, theme ->
                            SettingsUiState(
                                isLoading = false,
                                allTeams = teams,
                                notificationsEnabled = notifs,
                                themeMode = theme,
                                statusMessage = _uiState.value.statusMessage
                            )
                        }
                    }
                    combine(
                        combine(
                            repository.getAllTeamsFlow(),
                            userPreferencesRepository.notificationsEnabledFlow,
                            userPreferencesRepository.themeModeFlow,
                            ::Triple
                        ),
                        combine(
                            repository.getSeasonsFlow(team.id),
                            repository.getActiveSeasonFlow(team.id),
                            repository.getDueSchedulesFlow(team.id),
                            ::Triple
                        )
                    ) { (teams, notifs, theme), (seasons, activeSeason, dueSchedules) ->
                        SettingsUiState(
                            isLoading = false,
                            activeTeam = team,
                            allTeams = teams,
                            notificationsEnabled = notifs,
                            themeMode = theme,
                            statusMessage = _uiState.value.statusMessage,
                            seasons = seasons,
                            activeSeason = activeSeason,
                            dueSchedules = dueSchedules
                        )
                    }
                }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    fun createSeason(name: String) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            if (name.isBlank()) return@launch
            val id = repository.saveSeason(
                Season(teamId = team.id, name = name.trim(), startDate = System.currentTimeMillis(), isActive = false)
            )
            repository.setActiveSeason(team.id, id)
            _uiState.value = _uiState.value.copy(statusMessage = "Season created")
        }
    }

    fun setActiveSeason(seasonId: Long) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            repository.setActiveSeason(team.id, seasonId)
        }
    }

    fun addMonthlyDue(name: String, amountMinorUnits: Long) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            if (name.isBlank() || amountMinorUnits <= 0L) return@launch
            repository.saveDueSchedule(
                DueSchedule(
                    teamId = team.id,
                    name = name.trim(),
                    amountMinorUnits = amountMinorUnits,
                    period = DuePeriod.MONTHLY,
                    nextDueDate = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(statusMessage = "Due schedule saved")
        }
    }

    fun generateDueContributions(scheduleId: Long) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            val schedule = _uiState.value.dueSchedules.find { it.id == scheduleId }
            val count = repository.generateDueContributions(team.id, scheduleId)
            val message = when {
                count > 0 -> "Charged $count players"
                schedule != null && schedule.nextDueDate > System.currentTimeMillis() ->
                    "Not due yet. Next due ${DateTimeUtils.formatAbsoluteDate(schedule.nextDueDate)}"
                else -> "No active players to charge"
            }
            _uiState.value = _uiState.value.copy(statusMessage = message)
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
        val players = repository.getAllPlayersFlow(team.id).first().associateBy { it.id }
        return CsvExporter.exportContributions(contributions, players)
    }

    suspend fun exportExpensesCsv(): String? {
        val team = _uiState.value.activeTeam ?: return null
        val expenses = repository.getExpensesFlow(team.id).first()
        val players = repository.getAllPlayersFlow(team.id).first().associateBy { it.id }
        return CsvExporter.exportExpenses(expenses, players)
    }

    private val importCsvUseCase = ImportCsvUseCase(repository)

    fun importContributionsFromCsv(csvContent: String) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            val result = importCsvUseCase.importContributions(team.id, csvContent)
            _uiState.value = _uiState.value.copy(statusMessage = result.message("contributions"))
        }
    }

    fun importExpensesFromCsv(csvContent: String) {
        viewModelScope.launch {
            val team = _uiState.value.activeTeam ?: return@launch
            val result = importCsvUseCase.importExpenses(team.id, team.currencyCode, csvContent)
            _uiState.value = _uiState.value.copy(statusMessage = result.message("expenses (splits are not in the file)"))
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
