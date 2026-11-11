package com.teamfund.manager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.teamfund.manager.data.local.dao.*
import com.teamfund.manager.data.local.entity.*
import com.teamfund.manager.domain.model.*

class Converters {
    @TypeConverter
    fun fromSportType(value: SportType): String = value.name

    @TypeConverter
    fun toSportType(value: String): SportType = try {
        SportType.valueOf(value)
    } catch (_: Exception) {
        SportType.CRICKET
    }

    @TypeConverter
    fun fromPlayerRole(value: PlayerRole): String = value.name

    @TypeConverter
    fun toPlayerRole(value: String): PlayerRole = try {
        PlayerRole.valueOf(value)
    } catch (_: Exception) {
        PlayerRole.PLAYER
    }

    @TypeConverter
    fun fromMatchResult(value: MatchResult): String = value.name

    @TypeConverter
    fun toMatchResult(value: String): MatchResult = try {
        MatchResult.valueOf(value)
    } catch (_: Exception) {
        MatchResult.UPCOMING
    }

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = try {
        ExpenseCategory.valueOf(value)
    } catch (_: Exception) {
        ExpenseCategory.OTHER
    }

    @TypeConverter
    fun fromContributionPurpose(value: ContributionPurpose): String = value.name

    @TypeConverter
    fun toContributionPurpose(value: String): ContributionPurpose = try {
        ContributionPurpose.valueOf(value)
    } catch (_: Exception) {
        ContributionPurpose.GENERAL
    }

    @TypeConverter
    fun fromSettlementAdjustmentKind(value: SettlementAdjustmentKind): String = value.name

    @TypeConverter
    fun toSettlementAdjustmentKind(value: String): SettlementAdjustmentKind = try {
        SettlementAdjustmentKind.valueOf(value)
    } catch (_: Exception) {
        SettlementAdjustmentKind.PLAYER_TO_TEAM
    }

    @TypeConverter
    fun fromMatchPlayerStatus(value: MatchPlayerStatus): String = value.name

    @TypeConverter
    fun toMatchPlayerStatus(value: String): MatchPlayerStatus = try {
        MatchPlayerStatus.valueOf(value)
    } catch (_: Exception) {
        MatchPlayerStatus.AVAILABLE
    }

    @TypeConverter
    fun fromCricketRole(value: CricketRole): String = value.name

    @TypeConverter
    fun toCricketRole(value: String): CricketRole = try {
        CricketRole.valueOf(value)
    } catch (_: Exception) {
        CricketRole.UNSPECIFIED
    }

    @TypeConverter
    fun fromFineReason(value: FineReason): String = value.name

    @TypeConverter
    fun toFineReason(value: String): FineReason = try {
        FineReason.valueOf(value)
    } catch (_: Exception) {
        FineReason.OTHER
    }

    @TypeConverter
    fun fromInventoryCategory(value: InventoryCategory): String = value.name

    @TypeConverter
    fun toInventoryCategory(value: String): InventoryCategory = try {
        InventoryCategory.valueOf(value)
    } catch (_: Exception) {
        InventoryCategory.OTHER
    }

    @TypeConverter
    fun fromDuePeriod(value: DuePeriod): String = value.name

    @TypeConverter
    fun toDuePeriod(value: String): DuePeriod = try {
        DuePeriod.valueOf(value)
    } catch (_: Exception) {
        DuePeriod.MONTHLY
    }
}

@Database(
    entities = [
        TeamEntity::class,
        PlayerEntity::class,
        MatchEntity::class,
        ExpenseEntity::class,
        ExpenseShareEntity::class,
        ContributionEntity::class,
        SettlementEntity::class,
        MatchPlayerEntity::class,
        SeasonEntity::class,
        MatchPlayerStatsEntity::class,
        FineEntity::class,
        InventoryItemEntity::class,
        InventoryMovementEntity::class,
        DueScheduleEntity::class,
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TeamFundDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun contributionDao(): ContributionDao
    abstract fun settlementDao(): SettlementDao
    abstract fun matchPlayerDao(): MatchPlayerDao
    abstract fun seasonDao(): SeasonDao
    abstract fun matchPlayerStatsDao(): MatchPlayerStatsDao
    abstract fun fineDao(): FineDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun dueScheduleDao(): DueScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: TeamFundDatabase? = null

        fun getInstance(context: Context): TeamFundDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeamFundDatabase::class.java,
                    "teamfund_app_db"
                )
                    .addMigrations(*TeamFundMigrations.ALL)
                    // Never erase financial history when a migration is missing.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
