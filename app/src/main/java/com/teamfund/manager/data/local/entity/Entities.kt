package com.teamfund.manager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teamfund.manager.domain.model.ContributionPurpose
import com.teamfund.manager.domain.model.CricketRole
import com.teamfund.manager.domain.model.DuePeriod
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.model.FineReason
import com.teamfund.manager.domain.model.InventoryCategory
import com.teamfund.manager.domain.model.MatchPlayerStatus
import com.teamfund.manager.domain.model.MatchResult
import com.teamfund.manager.domain.model.PlayerRole
import com.teamfund.manager.domain.model.SettlementAdjustmentKind
import com.teamfund.manager.domain.model.SportType

@Entity(
    tableName = "teams"
)
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sport: SportType,
    val logoUri: String?,
    val currencyCode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean
)

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId")]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val role: PlayerRole,
    val cricketRole: CricketRole,
    val phoneNumber: String?,
    val avatarUri: String?,
    val isActive: Boolean,
    val joinedAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId"), Index("date")]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val opponentName: String,
    val date: Long,
    val venue: String?,
    val result: MatchResult,
    val teamScore: String?,
    val opponentScore: String?,
    val overs: String?,
    val tossWinner: String?,
    val playerOfTheMatchId: Long?,
    val seasonId: Long?,
    val matchFeeMinorUnits: Long,
    val groundFeeMinorUnits: Long,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId"), Index("matchId"), Index("date")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val matchId: Long?,
    val title: String,
    val category: ExpenseCategory,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val paidByPlayerId: Long?,
    val date: Long,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "expense_shares",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId"), Index("playerId")]
)
data class ExpenseShareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val expenseId: Long,
    val playerId: Long,
    val shareAmountMinorUnits: Long,
    val paidAmountMinorUnits: Long,
    val isSettled: Boolean,
    val settledAt: Long?
)

@Entity(
    tableName = "contributions",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId"), Index("playerId"), Index("date")]
)
data class ContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val playerId: Long,
    val amountMinorUnits: Long,
    val date: Long,
    val purpose: ContributionPurpose,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("teamId"),
        Index("fromPlayerId"),
        Index("toPlayerId"),
        Index("matchId"),
        Index("expenseId"),
        Index("date")
    ]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val fromPlayerId: Long,
    val toPlayerId: Long?,
    val matchId: Long?,
    val expenseId: Long?,
    val amountMinorUnits: Long,
    val adjustmentKind: SettlementAdjustmentKind,
    val date: Long,
    val note: String?,
    val createdAt: Long
)

@Entity(
    tableName = "match_players",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("matchId"),
        Index("playerId"),
        Index(value = ["matchId", "playerId"], unique = true)
    ]
)
data class MatchPlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val matchId: Long,
    val playerId: Long,
    val status: MatchPlayerStatus,
    val createdAt: Long
)

@Entity(
    tableName = "seasons",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId")]
)
data class SeasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean
)

@Entity(
    tableName = "match_player_stats",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("matchId"),
        Index("playerId"),
        Index(value = ["matchId", "playerId"], unique = true)
    ]
)
data class MatchPlayerStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val matchId: Long,
    val playerId: Long,
    val runs: Int,
    val balls: Int,
    val wickets: Int,
    val oversBowled: String?,
    val catches: Int,
    val isPlayerOfMatch: Boolean
)

@Entity(
    tableName = "fines",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId"), Index("playerId"), Index("matchId")]
)
data class FineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val playerId: Long,
    val matchId: Long?,
    val amountMinorUnits: Long,
    val reason: FineReason,
    val date: Long,
    val note: String?,
    val settlementId: Long?,
    val createdAt: Long
)

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId")]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val category: InventoryCategory,
    val quantity: Int,
    val notes: String?
)

@Entity(
    tableName = "inventory_movements",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("matchId"), Index("playerId")]
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val itemId: Long,
    val delta: Int,
    val matchId: Long?,
    val playerId: Long?,
    val date: Long,
    val note: String?
)

@Entity(
    tableName = "due_schedules",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId")]
)
data class DueScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val amountMinorUnits: Long,
    val period: DuePeriod,
    val nextDueDate: Long,
    val isActive: Boolean
)
