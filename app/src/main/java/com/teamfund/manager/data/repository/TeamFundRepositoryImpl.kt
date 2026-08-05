package com.teamfund.manager.data.repository

import com.teamfund.manager.data.local.database.TeamFundDatabase
import com.teamfund.manager.data.mapper.toDomain
import com.teamfund.manager.data.mapper.toEntity
import com.teamfund.manager.domain.model.*
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TeamFundRepositoryImpl(
    private val database: TeamFundDatabase
) : TeamFundRepository {

    private val teamDao = database.teamDao()
    private val playerDao = database.playerDao()
    private val matchDao = database.matchDao()
    private val expenseDao = database.expenseDao()
    private val contributionDao = database.contributionDao()
    private val settlementDao = database.settlementDao()

    override fun getActiveTeamFlow(activeTeamId: Long?): Flow<Team?> {
        return if (activeTeamId != null) {
            teamDao.getTeamByIdFlow(activeTeamId).map { it?.toDomain() }
        } else {
            teamDao.getActiveTeamFlow().map { it?.toDomain() }
        }
    }

    override suspend fun getTeamById(teamId: Long): Team? {
        return teamDao.getTeamById(teamId)?.toDomain()
    }

    override fun getAllTeamsFlow(): Flow<List<Team>> {
        return teamDao.getAllTeamsFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createTeam(team: Team): Long {
        return teamDao.insertTeam(team.toEntity())
    }

    override suspend fun updateTeam(team: Team) {
        teamDao.updateTeam(team.toEntity())
    }

    override suspend fun setActiveTeam(teamId: Long) {
        teamDao.setActiveTeamFlags(teamId)
    }

    override fun getActivePlayersFlow(teamId: Long): Flow<List<Player>> {
        return playerDao.getActivePlayersForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllPlayersFlow(teamId: Long): Flow<List<Player>> {
        return playerDao.getAllPlayersForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPlayerById(playerId: Long): Player? {
        return playerDao.getPlayerById(playerId)?.toDomain()
    }

    override suspend fun addPlayer(player: Player): Long {
        return playerDao.insertPlayer(player.toEntity())
    }

    override suspend fun updatePlayer(player: Player) {
        playerDao.updatePlayer(player.toEntity())
    }

    override suspend fun deactivatePlayer(playerId: Long) {
        playerDao.deactivatePlayer(playerId)
    }

    override suspend fun deletePlayer(playerId: Long) {
        playerDao.deletePlayer(playerId)
    }

    override fun getMatchesFlow(teamId: Long): Flow<List<Match>> {
        return matchDao.getMatchesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMatchById(matchId: Long): Match? {
        return matchDao.getMatchById(matchId)?.toDomain()
    }

    override fun getMatchByIdFlow(matchId: Long): Flow<Match?> {
        return matchDao.getMatchByIdFlow(matchId).map { it?.toDomain() }
    }

    override suspend fun saveMatch(match: Match): Long {
        return matchDao.insertMatch(match.toEntity())
    }

    override suspend fun deleteMatch(matchId: Long) {
        matchDao.deleteMatchById(matchId)
    }

    override fun getExpensesFlow(teamId: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesWithSharesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getExpensesForMatchFlow(matchId: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesForMatchFlow(matchId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExpenseById(expenseId: Long): Expense? {
        return expenseDao.getExpenseWithSharesById(expenseId)?.toDomain()
    }

    override suspend fun saveExpenseWithShares(expense: Expense, shares: List<ExpenseShare>): Long {
        val expenseEntity = expense.toEntity()
        val sharesEntities = shares.map { it.toEntity() }
        
        return if (expense.id == 0L) {
            expenseDao.insertExpenseWithShares(expenseEntity, sharesEntities)
        } else {
            // Reconcile payments: Preserve existing paid amounts when updating an expense
            val existingExpense = expenseDao.getExpenseWithSharesById(expense.id)
            val existingShares = existingExpense?.shares ?: emptyList()
            
            val reconciledShares = sharesEntities.map { newShare ->
                val matchingOldShare = existingShares.find { it.playerId == newShare.playerId }
                if (matchingOldShare != null) {
                    var paidAmount = matchingOldShare.paidAmountMinorUnits
                    
                    // IF the user was previously FULLY settled, and the expense was updated,
                    // we assume their "settled" status is stickier than the exact amount.
                    // We update their paidAmount to match the new share if they were already marked settled.
                    if (matchingOldShare.isSettled) {
                        paidAmount = newShare.shareAmountMinorUnits
                    }

                    val isNowSettled = paidAmount >= newShare.shareAmountMinorUnits
                    newShare.copy(
                        paidAmountMinorUnits = paidAmount,
                        isSettled = isNowSettled,
                        settledAt = if (isNowSettled) (matchingOldShare.settledAt ?: System.currentTimeMillis()) else null
                    )
                } else {
                    newShare
                }
            }
            
            expenseDao.updateExpenseWithShares(expenseEntity, reconciledShares)
            expense.id
        }
    }

    override suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteExpenseById(expenseId)
    }

    override fun getAllExpenseSharesFlow(teamId: Long): Flow<List<ExpenseShare>> {
        return expenseDao.getAllSharesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getContributionsFlow(teamId: Long): Flow<List<Contribution>> {
        return contributionDao.getContributionsForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getContributionsForPlayerFlow(playerId: Long): Flow<List<Contribution>> {
        return contributionDao.getContributionsForPlayerFlow(playerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addContribution(contribution: Contribution): Long {
        return contributionDao.insertContribution(contribution.toEntity())
    }

    override suspend fun deleteContribution(contributionId: Long) {
        contributionDao.deleteContributionById(contributionId)
    }

    override fun getSettlementsFlow(teamId: Long): Flow<List<Settlement>> {
        return settlementDao.getSettlementsForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getSettlementsForPlayerFlow(playerId: Long): Flow<List<Settlement>> {
        return settlementDao.getSettlementsForPlayerFlow(playerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addSettlement(settlement: Settlement): Long {
        return settlementDao.insertSettlement(settlement.toEntity())
    }

    override suspend fun deleteSettlement(settlementId: Long) {
        val settlement = settlementDao.getSettlementById(settlementId)
        settlementDao.deleteSettlementById(settlementId)
        val expenseId = settlement?.expenseId ?: return
        val playerId = when {
            settlement.fromPlayerId != 0L -> settlement.fromPlayerId
            settlement.toPlayerId != null -> settlement.toPlayerId
            else -> return
        }
        
        // Decrement the paid amount instead of full reset to handle partial settlements correctly
        expenseDao.decrementSharePayment(expenseId, playerId, settlement.amountMinorUnits)
    }

    override suspend fun markExpenseShareSettled(shareId: Long) {
        expenseDao.markShareSettled(shareId, System.currentTimeMillis())
    }

    override suspend fun markPlayerExpenseShareSettled(expenseId: Long, playerId: Long) {
        expenseDao.markPlayerShareSettledForExpense(
            expenseId = expenseId,
            playerId = playerId,
            settledAt = System.currentTimeMillis()
        )
    }

    override suspend fun recordExpenseSharePayment(expenseId: Long, playerId: Long, amountMinorUnits: Long) {
        expenseDao.incrementSharePayment(
            expenseId = expenseId,
            playerId = playerId,
            amount = amountMinorUnits,
            settledAt = System.currentTimeMillis()
        )
    }

    override suspend fun recordExpenseSharePaymentById(shareId: Long, amountMinorUnits: Long) {
        expenseDao.incrementSharePaymentById(
            shareId = shareId,
            amount = amountMinorUnits,
            settledAt = System.currentTimeMillis()
        )
    }

    override suspend fun clearAllData() {
        database.clearAllTables()
    }
}
