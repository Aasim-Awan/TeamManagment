package com.teamfund.manager.domain.usecase

import com.teamfund.manager.core.util.CsvImporter
import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.ContributionPurpose
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.flow.first

/** Outcome of one CSV import, shown to the user after the import finishes. */
data class CsvImportResult(
    val imported: Int,
    val duplicates: Int,
    val unknownPlayers: Int,
    val invalidRows: Int
) {
    fun message(kind: String): String = buildString {
        append("Imported $imported $kind")
        val skipped = listOfNotNull(
            duplicates.takeIf { it > 0 }?.let { "$it already recorded" },
            unknownPlayers.takeIf { it > 0 }?.let { "$it with unknown player" },
            invalidRows.takeIf { it > 0 }?.let { "$it unreadable" }
        )
        if (skipped.isNotEmpty()) append(". Skipped: ${skipped.joinToString(", ")}")
    }
}

/**
 * Imports TeamFund CSV exports. Rows already in the team (same player/title, amount and day)
 * are skipped, so importing the same file twice does not double the records.
 */
class ImportCsvUseCase(private val repository: TeamFundRepository) {

    suspend fun importContributions(teamId: Long, csvContent: String): CsvImportResult {
        val parsed = CsvImporter.parseContributions(csvContent)
        val playersByName = repository.getAllPlayersFlow(teamId).first().associateBy { it.name.trim().lowercase() }
        val existing = repository.getContributionsFlow(teamId).first()
            .map { Triple(it.playerId, it.amountMinorUnits, DateTimeUtils.toLocalDate(it.date)) }
            .toSet()
        var imported = 0
        var duplicates = 0
        var unknown = 0
        parsed.rows.forEach { row ->
            val player = playersByName[row.playerName.trim().lowercase()]
            if (player == null) {
                unknown++
                return@forEach
            }
            if (Triple(player.id, row.amountMinorUnits, DateTimeUtils.toLocalDate(row.date)) in existing) {
                duplicates++
                return@forEach
            }
            repository.addContribution(
                Contribution(
                    teamId = teamId,
                    playerId = player.id,
                    amountMinorUnits = row.amountMinorUnits,
                    date = row.date,
                    purpose = ContributionPurpose.entries.find { it.displayName == row.purposeStr }
                        ?: ContributionPurpose.GENERAL,
                    notes = row.notes.ifBlank { null }
                )
            )
            imported++
        }
        return CsvImportResult(imported, duplicates, unknown, parsed.invalidRows)
    }

    /**
     * Expense exports do not contain the per-player split, so imported expenses have no player
     * shares. Edit an imported expense to split it again.
     */
    suspend fun importExpenses(teamId: Long, currencyCode: String, csvContent: String): CsvImportResult {
        val parsed = CsvImporter.parseExpenses(csvContent)
        val playersByName = repository.getAllPlayersFlow(teamId).first().associateBy { it.name.trim().lowercase() }
        val existing = repository.getExpensesFlow(teamId).first()
            .map { Triple(it.title.trim().lowercase(), it.amountMinorUnits, DateTimeUtils.toLocalDate(it.date)) }
            .toSet()
        var imported = 0
        var duplicates = 0
        var unknown = 0
        parsed.rows.forEach { row ->
            val paidByTeam = row.paidByStr.isBlank() || row.paidByStr.equals("Team Fund", ignoreCase = true)
            val paidBy = if (paidByTeam) null else playersByName[row.paidByStr.trim().lowercase()]
            if (!paidByTeam && paidBy == null) {
                unknown++
                return@forEach
            }
            if (Triple(row.title.trim().lowercase(), row.amountMinorUnits, DateTimeUtils.toLocalDate(row.date)) in existing) {
                duplicates++
                return@forEach
            }
            repository.saveExpenseWithShares(
                Expense(
                    teamId = teamId,
                    title = row.title,
                    category = ExpenseCategory.entries.find { it.displayName == row.categoryStr }
                        ?: ExpenseCategory.OTHER,
                    amountMinorUnits = row.amountMinorUnits,
                    currencyCode = currencyCode,
                    paidByPlayerId = paidBy?.id,
                    date = row.date,
                    notes = row.notes.ifBlank { null }
                ),
                emptyList()
            )
            imported++
        }
        return CsvImportResult(imported, duplicates, unknown, parsed.invalidRows)
    }
}
