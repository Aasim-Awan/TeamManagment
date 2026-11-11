package com.teamfund.manager.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.teamfund.manager.data.local.database.TeamFundDatabase
import com.teamfund.manager.data.local.database.TeamFundMigrations
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TeamFundMigrationTest {
    @Test fun legacyV3AddsPaymentColumnAndKeepsRecords() = verifyUpgrade(false)
    @Test fun developmentV3KeepsExistingPartialPayments() = verifyUpgrade(true)

    private fun verifyUpgrade(hasPaymentColumn: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${if (hasPaymentColumn) "partial" else "legacy"}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = JSONObject(File("schemas/com.teamfund.manager.data.local.database.TeamFundDatabase/3.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                var sql = entity.getString("createSql").replace("\${TABLE_NAME}", table)
                if (!hasPaymentColumn && table == "expense_shares") sql = sql.replace(", `paidAmountMinorUnits` INTEGER NOT NULL", "")
                old.execSQL(sql)
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) {
                    old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO teams (id,name,sport,currencyCode,createdAt,updatedAt,isActive) VALUES (1,'Migration Team','CRICKET','USD',0,0,1)")
            old.execSQL("INSERT INTO expenses (id,teamId,title,category,amountMinorUnits,currencyCode,date,createdAt,updatedAt) VALUES (1,1,'Equipment','EQUIPMENT',3000,'USD',0,0,0)")
            val column = if (hasPaymentColumn) ",paidAmountMinorUnits" else ""
            old.execSQL("INSERT INTO expense_shares (id,expenseId,playerId,shareAmountMinorUnits,isSettled$column) VALUES (1,1,1,1000,1${if (hasPaymentColumn) ",0" else ""})")
            old.execSQL("INSERT INTO expense_shares (id,expenseId,playerId,shareAmountMinorUnits,isSettled$column) VALUES (2,1,2,1000,0${if (hasPaymentColumn) ",300" else ""})")
            old.version = 3
        }
        val database = Room.databaseBuilder(context, TeamFundDatabase::class.java, name)
            .addMigrations(*TeamFundMigrations.ALL).allowMainThreadQueries().build()
        try {
            val upgraded = database.openHelper.writableDatabase
            upgraded.query("SELECT paidAmountMinorUnits FROM expense_shares ORDER BY id").use {
                assertEquals(2, it.count)
                it.moveToFirst(); assertEquals(1000L, it.getLong(0))
                it.moveToNext(); assertEquals(if (hasPaymentColumn) 300L else 0L, it.getLong(0))
            }
            upgraded.query("SELECT name FROM teams").use {
                assertEquals(1, it.count); it.moveToFirst(); assertEquals("Migration Team", it.getString(0))
            }
        } finally { database.close() }
    }
}
