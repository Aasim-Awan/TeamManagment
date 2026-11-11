package com.teamfund.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for TeamFund.
 *
 * Current schema version: 10 (see exported schema JSON).
 */
object TeamFundMigrations {

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val hasPaidAmount = db.query("PRAGMA table_info(expense_shares)").use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameColumn) == "paidAmountMinorUnits") found = true
                }
                found
            }
            if (!hasPaidAmount) {
                db.execSQL("ALTER TABLE expense_shares ADD COLUMN paidAmountMinorUnits INTEGER NOT NULL DEFAULT 0")
            }
            db.execSQL("UPDATE expense_shares SET paidAmountMinorUnits = shareAmountMinorUnits WHERE isSettled = 1 AND paidAmountMinorUnits = 0")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE settlements ADD COLUMN adjustmentKind TEXT NOT NULL DEFAULT 'PLAYER_TO_TEAM'"
            )
            db.execSQL(
                "UPDATE settlements SET adjustmentKind = 'TEAM_TO_PLAYER' WHERE fromPlayerId = 0"
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE matches ADD COLUMN matchFeeMinorUnits INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS match_players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    matchId INTEGER NOT NULL,
                    playerId INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(matchId) REFERENCES matches(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_match_players_matchId ON match_players(matchId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_match_players_playerId ON match_players(playerId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_match_players_matchId_playerId ON match_players(matchId, playerId)"
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE players ADD COLUMN cricketRole TEXT NOT NULL DEFAULT 'UNSPECIFIED'"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS fines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    teamId INTEGER NOT NULL,
                    playerId INTEGER NOT NULL,
                    matchId INTEGER,
                    amountMinorUnits INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    note TEXT,
                    settlementId INTEGER,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(teamId) REFERENCES teams(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fines_teamId ON fines(teamId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fines_playerId ON fines(playerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fines_matchId ON fines(matchId)")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS seasons (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    teamId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    startDate INTEGER NOT NULL,
                    endDate INTEGER,
                    isActive INTEGER NOT NULL,
                    FOREIGN KEY(teamId) REFERENCES teams(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_seasons_teamId ON seasons(teamId)")
            db.execSQL("ALTER TABLE matches ADD COLUMN seasonId INTEGER")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS match_player_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    matchId INTEGER NOT NULL,
                    playerId INTEGER NOT NULL,
                    runs INTEGER NOT NULL,
                    balls INTEGER NOT NULL,
                    wickets INTEGER NOT NULL,
                    oversBowled TEXT,
                    catches INTEGER NOT NULL,
                    isPlayerOfMatch INTEGER NOT NULL,
                    FOREIGN KEY(matchId) REFERENCES matches(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_match_player_stats_matchId ON match_player_stats(matchId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_match_player_stats_playerId ON match_player_stats(playerId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_match_player_stats_matchId_playerId ON match_player_stats(matchId, playerId)"
            )
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE matches ADD COLUMN groundFeeMinorUnits INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS inventory_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    teamId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(teamId) REFERENCES teams(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_items_teamId ON inventory_items(teamId)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS inventory_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    itemId INTEGER NOT NULL,
                    delta INTEGER NOT NULL,
                    matchId INTEGER,
                    playerId INTEGER,
                    date INTEGER NOT NULL,
                    note TEXT,
                    FOREIGN KEY(itemId) REFERENCES inventory_items(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_itemId ON inventory_movements(itemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_matchId ON inventory_movements(matchId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_playerId ON inventory_movements(playerId)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS due_schedules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    teamId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    amountMinorUnits INTEGER NOT NULL,
                    period TEXT NOT NULL,
                    nextDueDate INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    FOREIGN KEY(teamId) REFERENCES teams(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_due_schedules_teamId ON due_schedules(teamId)")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                DELETE FROM match_players
                WHERE playerId NOT IN (SELECT id FROM players)
                """.trimIndent()
            )
            db.execSQL(
                """
                DELETE FROM match_player_stats
                WHERE playerId NOT IN (SELECT id FROM players)
                """.trimIndent()
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10
    )
}
