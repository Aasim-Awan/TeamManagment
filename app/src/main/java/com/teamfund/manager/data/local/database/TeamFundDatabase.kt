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
    ],
    version = 4,
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
                    // Only wipe when upgrading from pre-v3 installs (no migration history).
                    // From v3 onward, missing migrations fail loudly instead of deleting data.
                    .fallbackToDestructiveMigrationFrom(
                        dropAllTables = true,
                        *TeamFundMigrations.DESTRUCTIVE_FROM_VERSIONS
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
