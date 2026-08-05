package com.teamfund.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.teamfund.manager.data.local.entity.ContributionEntity
import com.teamfund.manager.data.local.entity.ExpenseEntity
import com.teamfund.manager.data.local.entity.ExpenseShareEntity
import com.teamfund.manager.data.local.entity.MatchEntity
import com.teamfund.manager.data.local.entity.PlayerEntity
import com.teamfund.manager.data.local.entity.SettlementEntity
import com.teamfund.manager.data.local.entity.TeamEntity
import com.teamfund.manager.data.local.relation.ExpenseWithShares
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveTeamFlow(): Flow<TeamEntity?>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: Long): TeamEntity?

    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun getTeamByIdFlow(teamId: Long): Flow<TeamEntity?>

    @Query("SELECT * FROM teams ORDER BY createdAt DESC")
    fun getAllTeamsFlow(): Flow<List<TeamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Query("UPDATE teams SET isActive = CASE WHEN id = :teamId THEN 1 ELSE 0 END")
    suspend fun setActiveTeamFlags(teamId: Long)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE teamId = :teamId AND isActive = 1 ORDER BY name ASC")
    fun getActivePlayersForTeamFlow(teamId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY isActive DESC, name ASC")
    fun getAllPlayersForTeamFlow(teamId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getPlayerById(playerId: Long): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Query("UPDATE players SET isActive = 0, updatedAt = :updatedAt WHERE id = :playerId")
    suspend fun deactivatePlayer(playerId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayer(playerId: Long)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE teamId = :teamId ORDER BY date DESC")
    fun getMatchesForTeamFlow(teamId: Long): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatchById(matchId: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun getMatchByIdFlow(matchId: Long): Flow<MatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Long)
}

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses WHERE teamId = :teamId ORDER BY date DESC")
    fun getExpensesWithSharesForTeamFlow(teamId: Long): Flow<List<ExpenseWithShares>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE matchId = :matchId ORDER BY date DESC")
    fun getExpensesForMatchFlow(matchId: Long): Flow<List<ExpenseWithShares>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseWithSharesById(expenseId: Long): ExpenseWithShares?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseShares(shares: List<ExpenseShareEntity>)

    @Query("DELETE FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun deleteExpenseShares(expenseId: Long)

    @Transaction
    suspend fun insertExpenseWithShares(expense: ExpenseEntity, shares: List<ExpenseShareEntity>): Long {
        val expenseId = insertExpense(expense)
        val updatedShares = shares.map { it.copy(expenseId = expenseId) }
        insertExpenseShares(updatedShares)
        return expenseId
    }

    @Transaction
    suspend fun updateExpenseWithShares(expense: ExpenseEntity, shares: List<ExpenseShareEntity>) {
        insertExpense(expense)
        deleteExpenseShares(expense.id)
        val updatedShares = shares.map { it.copy(expenseId = expense.id) }
        insertExpenseShares(updatedShares)
    }

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    @Query("SELECT * FROM expense_shares WHERE playerId = :playerId")
    fun getSharesForPlayerFlow(playerId: Long): Flow<List<ExpenseShareEntity>>

    @Query("SELECT * FROM expense_shares WHERE expenseId IN (SELECT id FROM expenses WHERE teamId = :teamId)")
    fun getAllSharesForTeamFlow(teamId: Long): Flow<List<ExpenseShareEntity>>

    @Query(
        """
        UPDATE expense_shares
        SET paidAmountMinorUnits = shareAmountMinorUnits, isSettled = 1, settledAt = :settledAt
        WHERE id = :shareId
        """
    )
    suspend fun markShareSettled(shareId: Long, settledAt: Long)

    @Query(
        """
        UPDATE expense_shares
        SET paidAmountMinorUnits = shareAmountMinorUnits, isSettled = 1, settledAt = :settledAt
        WHERE expenseId = :expenseId AND playerId = :playerId
        """
    )
    suspend fun markPlayerShareSettledForExpense(expenseId: Long, playerId: Long, settledAt: Long)

    @Query(
        """
        UPDATE expense_shares
        SET paidAmountMinorUnits = paidAmountMinorUnits + :amount,
            isSettled = CASE WHEN (paidAmountMinorUnits + :amount) >= shareAmountMinorUnits THEN 1 ELSE 0 END,
            settledAt = CASE WHEN (paidAmountMinorUnits + :amount) >= shareAmountMinorUnits THEN :settledAt ELSE settledAt END
        WHERE id = :shareId
        """
    )
    suspend fun incrementSharePaymentById(shareId: Long, amount: Long, settledAt: Long)

    @Query(
        """
        UPDATE expense_shares
        SET paidAmountMinorUnits = paidAmountMinorUnits + :amount,
            isSettled = CASE WHEN (paidAmountMinorUnits + :amount) >= shareAmountMinorUnits THEN 1 ELSE 0 END,
            settledAt = CASE WHEN (paidAmountMinorUnits + :amount) >= shareAmountMinorUnits THEN :settledAt ELSE settledAt END
        WHERE expenseId = :expenseId AND playerId = :playerId
        """
    )
    suspend fun incrementSharePayment(expenseId: Long, playerId: Long, amount: Long, settledAt: Long)

    @Query(
        """
        UPDATE expense_shares
        SET paidAmountMinorUnits = (paidAmountMinorUnits - :amount),
            isSettled = CASE WHEN (paidAmountMinorUnits - :amount) >= shareAmountMinorUnits THEN 1 ELSE 0 END,
            settledAt = CASE WHEN (paidAmountMinorUnits - :amount) >= shareAmountMinorUnits THEN settledAt ELSE NULL END
        WHERE expenseId = :expenseId AND playerId = :playerId
        """
    )
    suspend fun decrementSharePayment(expenseId: Long, playerId: Long, amount: Long)
}

@Dao
interface ContributionDao {
    @Query("SELECT * FROM contributions WHERE teamId = :teamId ORDER BY date DESC")
    fun getContributionsForTeamFlow(teamId: Long): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE playerId = :playerId ORDER BY date DESC")
    fun getContributionsForPlayerFlow(playerId: Long): Flow<List<ContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: ContributionEntity): Long

    @Update
    suspend fun updateContribution(contribution: ContributionEntity)

    @Query("DELETE FROM contributions WHERE id = :contributionId")
    suspend fun deleteContributionById(contributionId: Long)
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE teamId = :teamId ORDER BY date DESC")
    fun getSettlementsForTeamFlow(teamId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE fromPlayerId = :playerId OR toPlayerId = :playerId ORDER BY date DESC")
    fun getSettlementsForPlayerFlow(playerId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE id = :settlementId")
    suspend fun getSettlementById(settlementId: Long): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlementById(settlementId: Long)
}
