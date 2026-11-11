package com.teamfund.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.teamfund.manager.data.local.entity.ContributionEntity
import com.teamfund.manager.data.local.entity.ExpenseEntity
import com.teamfund.manager.data.local.entity.ExpenseShareEntity
import com.teamfund.manager.data.local.entity.MatchEntity
import com.teamfund.manager.data.local.entity.PlayerEntity
import com.teamfund.manager.data.local.entity.SettlementEntity
import com.teamfund.manager.data.local.entity.TeamEntity
import com.teamfund.manager.data.local.relation.ExpenseWithShares
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.model.FineReason
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

    @Upsert
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

    @Query("SELECT * FROM players WHERE teamId = :teamId AND isActive = 1 ORDER BY name ASC")
    suspend fun getActivePlayersForTeam(teamId: Long): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY isActive DESC, name ASC")
    fun getAllPlayersForTeamFlow(teamId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getPlayerById(playerId: Long): PlayerEntity?

    @Upsert
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Query(
        """
        SELECT * FROM players
        WHERE teamId = :teamId AND LOWER(TRIM(name)) = LOWER(TRIM(:name))
        LIMIT 1
        """
    )
    suspend fun findPlayerByName(teamId: Long, name: String): PlayerEntity?

    @Query("UPDATE players SET isActive = 1, updatedAt = :updatedAt WHERE id = :playerId")
    suspend fun reactivatePlayer(playerId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM expense_shares WHERE playerId = :playerId")
    suspend fun countExpenseSharesForPlayer(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM contributions WHERE playerId = :playerId")
    suspend fun countContributionsForPlayer(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM settlements WHERE fromPlayerId = :playerId OR toPlayerId = :playerId")
    suspend fun countSettlementsForPlayer(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM fines WHERE playerId = :playerId")
    suspend fun countFinesForPlayer(playerId: Long): Int

    @Query("DELETE FROM match_players WHERE playerId = :playerId")
    suspend fun deleteMatchPlayersForPlayer(playerId: Long)

    @Query("DELETE FROM match_player_stats WHERE playerId = :playerId")
    suspend fun deleteMatchStatsForPlayer(playerId: Long)

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

    @Query("SELECT * FROM matches WHERE teamId = :teamId AND (:seasonId IS NULL OR seasonId = :seasonId) ORDER BY date DESC")
    fun getMatchesForTeamAndSeasonFlow(teamId: Long, seasonId: Long?): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatchById(matchId: Long): MatchEntity?

    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun getMatchByIdFlow(matchId: Long): Flow<MatchEntity?>

    @Upsert
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: Long)

    // Cleanup used when a match is deleted (expenses, fines, settlements and kit movements have no FK to matches).
    @Query("DELETE FROM fines WHERE matchId = :matchId AND reason = :reason")
    suspend fun deleteFinesForMatch(matchId: Long, reason: FineReason)

    @Query("DELETE FROM expenses WHERE matchId = :matchId AND category = :category AND title LIKE :titlePrefix || '%'")
    suspend fun deleteAutoExpensesForMatch(matchId: Long, category: ExpenseCategory, titlePrefix: String)

    @Query("UPDATE expenses SET matchId = NULL WHERE matchId = :matchId")
    suspend fun unlinkExpensesFromMatch(matchId: Long)

    @Query("UPDATE fines SET matchId = NULL WHERE matchId = :matchId")
    suspend fun unlinkFinesFromMatch(matchId: Long)

    @Query("UPDATE settlements SET matchId = NULL WHERE matchId = :matchId")
    suspend fun unlinkSettlementsFromMatch(matchId: Long)

    @Query("UPDATE inventory_movements SET matchId = NULL WHERE matchId = :matchId")
    suspend fun unlinkInventoryMovementsFromMatch(matchId: Long)
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

    @Upsert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert
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

    @Query("SELECT COUNT(*) FROM expenses WHERE matchId = :matchId AND category = :category")
    suspend fun countExpensesForMatch(matchId: Long, category: ExpenseCategory): Int

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
        SET paidAmountMinorUnits = MAX(0, paidAmountMinorUnits - :amount),
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

    @Upsert
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

    @Upsert
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlementById(settlementId: Long)
}
