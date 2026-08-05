package com.teamfund.manager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for TeamFund.
 *
 * Current schema version: 3 (see exported schema JSON).
 * Pre-v3 installs have no exported migrations → allowed to wipe via
 * [fallbackToDestructiveMigrationFrom] for versions 1–2 only.
 *
 * When bumping @Database version:
 * 1. Add Migration(from, to) here
 * 2. Register it in [ALL]
 * 3. Never re-enable blanket fallbackToDestructiveMigration
 */
object TeamFundMigrations {

    /** Versions that may wipe on upgrade (no schema export / migration history). */
    val DESTRUCTIVE_FROM_VERSIONS = intArrayOf(1, 2)

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE expense_shares ADD COLUMN paidAmountMinorUnits INTEGER NOT NULL DEFAULT 0")
            // Backfill paidAmount for already settled shares
            db.execSQL("UPDATE expense_shares SET paidAmountMinorUnits = shareAmountMinorUnits WHERE isSettled = 1")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_3_4)
}
