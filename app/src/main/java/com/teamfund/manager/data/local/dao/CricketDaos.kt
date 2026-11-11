package com.teamfund.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.teamfund.manager.data.local.entity.DueScheduleEntity
import com.teamfund.manager.data.local.entity.FineEntity
import com.teamfund.manager.data.local.entity.InventoryItemEntity
import com.teamfund.manager.data.local.entity.InventoryMovementEntity
import com.teamfund.manager.data.local.entity.MatchPlayerEntity
import com.teamfund.manager.data.local.entity.MatchPlayerStatsEntity
import com.teamfund.manager.data.local.entity.SeasonEntity
import com.teamfund.manager.domain.model.FineReason
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchPlayerDao {
    @Query("SELECT * FROM match_players WHERE matchId = :matchId ORDER BY id ASC")
    fun getPlayersForMatchFlow(matchId: Long): Flow<List<MatchPlayerEntity>>

    @Query("SELECT * FROM match_players WHERE matchId = :matchId")
    suspend fun getPlayersForMatch(matchId: Long): List<MatchPlayerEntity>

    @Insert
    suspend fun insertAll(players: List<MatchPlayerEntity>)

    @Query("DELETE FROM match_players WHERE matchId = :matchId")
    suspend fun deleteForMatch(matchId: Long)

    @Transaction
    suspend fun replaceForMatch(matchId: Long, players: List<MatchPlayerEntity>) {
        deleteForMatch(matchId)
        if (players.isNotEmpty()) insertAll(players)
    }
}

@Dao
interface SeasonDao {
    @Query("SELECT * FROM seasons WHERE teamId = :teamId ORDER BY startDate DESC")
    fun getSeasonsForTeamFlow(teamId: Long): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons WHERE teamId = :teamId AND isActive = 1 LIMIT 1")
    fun getActiveSeasonFlow(teamId: Long): Flow<SeasonEntity?>

    @Upsert
    suspend fun insertSeason(season: SeasonEntity): Long

    @Update
    suspend fun updateSeason(season: SeasonEntity)

    @Query("UPDATE seasons SET isActive = 0 WHERE teamId = :teamId")
    suspend fun clearActiveForTeam(teamId: Long)

    @Query("UPDATE seasons SET isActive = 1 WHERE id = :seasonId")
    suspend fun setActiveSeason(seasonId: Long)
}

@Dao
interface MatchPlayerStatsDao {
    @Query("SELECT * FROM match_player_stats WHERE matchId = :matchId")
    fun getStatsForMatchFlow(matchId: Long): Flow<List<MatchPlayerStatsEntity>>

    @Query("SELECT * FROM match_player_stats WHERE matchId = :matchId AND playerId = :playerId LIMIT 1")
    suspend fun findStatForMatchPlayer(matchId: Long, playerId: Long): MatchPlayerStatsEntity?

    @Upsert
    suspend fun upsert(stat: MatchPlayerStatsEntity): Long

    @Transaction
    suspend fun upsertAll(stats: List<MatchPlayerStatsEntity>) {
        stats.forEach { stat ->
            val existing = findStatForMatchPlayer(stat.matchId, stat.playerId)
            upsert(if (existing != null) stat.copy(id = existing.id) else stat)
        }
    }
}

@Dao
interface FineDao {
    @Query("SELECT * FROM fines WHERE teamId = :teamId ORDER BY date DESC")
    fun getFinesForTeamFlow(teamId: Long): Flow<List<FineEntity>>

    @Query("SELECT * FROM fines WHERE playerId = :playerId ORDER BY date DESC")
    fun getFinesForPlayerFlow(playerId: Long): Flow<List<FineEntity>>

    @Query("SELECT * FROM fines WHERE matchId = :matchId ORDER BY date DESC")
    suspend fun getFinesForMatch(matchId: Long): List<FineEntity>

    @Upsert
    suspend fun insertFine(fine: FineEntity): Long

    @Query("SELECT * FROM fines WHERE id = :fineId")
    suspend fun getFineById(fineId: Long): FineEntity?

    @Query("SELECT COUNT(*) FROM fines WHERE matchId = :matchId AND playerId = :playerId AND reason = :reason")
    suspend fun countFinesForMatchPlayer(matchId: Long, playerId: Long, reason: FineReason): Int

    @Query("DELETE FROM fines WHERE id = :fineId")
    suspend fun deleteFine(fineId: Long)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE teamId = :teamId ORDER BY name ASC")
    fun getItemsForTeamFlow(teamId: Long): Flow<List<InventoryItemEntity>>

    @Upsert
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET quantity = quantity + :delta WHERE id = :itemId")
    suspend fun adjustQuantity(itemId: Long, delta: Int)

    @Insert
    suspend fun insertMovement(movement: InventoryMovementEntity): Long

    @Query("SELECT * FROM inventory_movements WHERE itemId = :itemId ORDER BY date DESC")
    fun getMovementsForItemFlow(itemId: Long): Flow<List<InventoryMovementEntity>>
}

@Dao
interface DueScheduleDao {
    @Query("SELECT * FROM due_schedules WHERE teamId = :teamId ORDER BY nextDueDate ASC")
    fun getSchedulesForTeamFlow(teamId: Long): Flow<List<DueScheduleEntity>>

    @Query("SELECT * FROM due_schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: Long): DueScheduleEntity?

    @Upsert
    suspend fun insertSchedule(schedule: DueScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: DueScheduleEntity)
}
