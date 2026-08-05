package com.teamfund.manager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teamfund.manager.domain.model.ContributionPurpose
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.model.MatchResult
import com.teamfund.manager.domain.model.PlayerRole
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
    val date: Long,
    val note: String?,
    val createdAt: Long
)
