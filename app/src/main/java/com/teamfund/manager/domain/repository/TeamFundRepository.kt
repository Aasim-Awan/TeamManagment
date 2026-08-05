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
    suspend fun deletePlayer(playerId: Long)

    fun getMatchesFlow(teamId: Long): Flow<List<Match>>
    suspend fun getMatchById(matchId: Long): Match?
    fun getMatchByIdFlow(matchId: Long): Flow<Match?>
    suspend fun saveMatch(match: Match): Long
    suspend fun deleteMatch(matchId: Long)

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
    suspend fun deleteSettlement(settlementId: Long)
    suspend fun markExpenseShareSettled(shareId: Long)
    suspend fun markPlayerExpenseShareSettled(expenseId: Long, playerId: Long)
    suspend fun recordExpenseSharePayment(expenseId: Long, playerId: Long, amountMinorUnits: Long)
    suspend fun recordExpenseSharePaymentById(shareId: Long, amountMinorUnits: Long)

    suspend fun clearAllData()
}
