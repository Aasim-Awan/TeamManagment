package com.teamfund.manager.domain.repository

import com.teamfund.manager.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TeamFundRepository {
    fun getActiveTeamFlow(activeTeamId: Long? = null): Flow<Team?>
    suspend fun getTeamById(teamId: Long): Team?
    fun getAllTeamsFlow(): Flow<List<Team>>
    suspend fun createTeam(team: Team): Long
    suspend fun updateTeam(team: Team)
    suspend fun setActiveTeam(teamId: Long)

    fun getActivePlayersFlow(teamId: Long): Flow<List<Player>>
    fun getAllPlayersFlow(teamId: Long): Flow<List<Player>>
    suspend fun getPlayerById(playerId: Long): Player?
    suspend fun addPlayer(player: Player): Long
    suspend fun updatePlayer(player: Player)
    suspend fun deactivatePlayer(playerId: Long)
    suspend fun reactivatePlayer(playerId: Long)
    suspend fun findPlayerByName(teamId: Long, name: String): Player?
    suspend fun deletePlayer(playerId: Long)
    suspend fun deletePlayerSafely(playerId: Long): PlayerSafeDeleteOutcome

    fun getMatchesFlow(teamId: Long): Flow<List<Match>>
    fun getMatchesFlow(teamId: Long, seasonId: Long?): Flow<List<Match>>
    suspend fun getMatchById(matchId: Long): Match?
    fun getMatchByIdFlow(matchId: Long): Flow<Match?>
    suspend fun saveMatch(match: Match): Long
    /** Saves the match and replaces its roster in one transaction. Returns the match id. */
    suspend fun saveMatchWithRoster(match: Match, players: List<MatchPlayer>): Long
    suspend fun deleteMatch(matchId: Long)

    fun getMatchPlayersFlow(matchId: Long): Flow<List<MatchPlayer>>
    suspend fun getMatchPlayers(matchId: Long): List<MatchPlayer>
    suspend fun replaceMatchPlayers(matchId: Long, players: List<MatchPlayer>)
    suspend fun chargeMatchFeeToXi(
        teamId: Long,
        matchId: Long,
        matchFeeMinorUnits: Long,
        selectedPlayerIds: List<Long>
    )

    fun getSeasonsFlow(teamId: Long): Flow<List<Season>>
    fun getActiveSeasonFlow(teamId: Long): Flow<Season?>
    suspend fun saveSeason(season: Season): Long
    suspend fun setActiveSeason(teamId: Long, seasonId: Long)

    fun getMatchPlayerStatsFlow(matchId: Long): Flow<List<MatchPlayerStats>>
    suspend fun saveMatchPlayerStats(stats: List<MatchPlayerStats>)

    fun getFinesForTeamFlow(teamId: Long): Flow<List<Fine>>
    fun getFinesForPlayerFlow(playerId: Long): Flow<List<Fine>>
    suspend fun getFinesForMatch(matchId: Long): List<Fine>
    suspend fun addFineWithSettlement(fine: Fine, recordPayment: Boolean): Long
    suspend fun addFine(fine: Fine): Long
    suspend fun deleteFine(fineId: Long)

    fun getInventoryItemsFlow(teamId: Long): Flow<List<InventoryItem>>
    suspend fun saveInventoryItem(item: InventoryItem): Long
    suspend fun adjustInventoryItem(itemId: Long, delta: Int, movement: InventoryMovement)

    fun getDueSchedulesFlow(teamId: Long): Flow<List<DueSchedule>>
    suspend fun saveDueSchedule(schedule: DueSchedule): Long
    suspend fun generateDueContributions(teamId: Long, scheduleId: Long): Int

    suspend fun createGroundFeeExpenseForMatch(teamId: Long, match: Match, currencyCode: String)

    fun getExpensesFlow(teamId: Long): Flow<List<Expense>>
    fun getExpensesForMatchFlow(matchId: Long): Flow<List<Expense>>
    suspend fun getExpenseById(expenseId: Long): Expense?
    suspend fun saveExpenseWithShares(expense: Expense, shares: List<ExpenseShare>): Long
    suspend fun deleteExpense(expenseId: Long)
    fun getAllExpenseSharesFlow(teamId: Long): Flow<List<ExpenseShare>>

    fun getContributionsFlow(teamId: Long): Flow<List<Contribution>>
    fun getContributionsForPlayerFlow(playerId: Long): Flow<List<Contribution>>
    suspend fun addContribution(contribution: Contribution): Long
    suspend fun deleteContribution(contributionId: Long)

    fun getSettlementsFlow(teamId: Long): Flow<List<Settlement>>
    fun getSettlementsForPlayerFlow(playerId: Long): Flow<List<Settlement>>
    suspend fun addSettlement(settlement: Settlement): Long
    /**
     * Records a player-to-team payment and, when linked to an expense share, applies it to that
     * share in the same transaction.
     */
    suspend fun recordPlayerPayment(settlement: Settlement, shareId: Long?, expenseId: Long?): Long
    suspend fun deleteSettlement(settlementId: Long)
    suspend fun markExpenseShareSettled(shareId: Long)
    suspend fun markPlayerExpenseShareSettled(expenseId: Long, playerId: Long)
    suspend fun recordExpenseSharePayment(expenseId: Long, playerId: Long, amountMinorUnits: Long)
    suspend fun recordExpenseSharePaymentById(shareId: Long, amountMinorUnits: Long)

    suspend fun clearAllData()
}
