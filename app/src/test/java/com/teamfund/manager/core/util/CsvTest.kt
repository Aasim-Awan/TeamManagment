package com.teamfund.manager.core.util

import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CsvTest {

    @Test
    fun cell_escapesQuotesAndLineBreaks() {
        assertEquals("\"Sam \"\"The Wall\"\" Lee\"", Csv.cell("Sam \"The Wall\" Lee"))
        assertEquals("\"line one line two\"", Csv.cell("line one\nline two"))
    }

    @Test
    fun cell_guardsFormulasButKeepsNumbers() {
        assertEquals("\"'=HYPERLINK(1)\"", Csv.cell("=HYPERLINK(1)"))
        assertEquals("\"'+cmd\"", Csv.cell("+cmd"))
        assertEquals("\"-12.50\"", Csv.cell("-12.50"))
    }

    @Test
    fun parseCsvLine_roundTripsExportedCells() {
        val line = Csv.row("2026-11-10", "Sam \"The Wall\", Lee", "=SUM(A1)", "12.50").trimEnd()
        assertEquals(
            listOf("2026-11-10", "Sam \"The Wall\", Lee", "=SUM(A1)", "12.50"),
            CsvImporter.parseCsvLine(line)
        )
    }

    @Test
    fun parseDate_acceptsIsoAndLegacyEnglish() {
        assertNotNull(CsvImporter.parseDate("2026-11-10"))
        assertNotNull(CsvImporter.parseDate("Nov 10, 2026"))
        assertNull(CsvImporter.parseDate("not a date"))
    }

    @Test
    fun exportThenParse_contributions() {
        val player = Player(id = 1L, teamId = 1L, name = "Alex, Jr.")
        val csv = CsvExporter.exportContributions(
            listOf(Contribution(teamId = 1L, playerId = 1L, amountMinorUnits = 1250L, notes = "Paid \"cash\"")),
            mapOf(1L to player)
        )
        val parsed = CsvImporter.parseContributions(csv)

        assertEquals(0, parsed.invalidRows)
        val row = parsed.rows.single()
        assertEquals("Alex, Jr.", row.playerName)
        assertEquals(1250L, row.amountMinorUnits)
        assertEquals("Paid \"cash\"", row.notes)
    }

    @Test
    fun fullReport_everyRowHasTheSameColumns() {
        val tricky = Player(id = 1L, teamId = 1L, name = "Sam \"The Wall\", Lee")
        val summary = com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase().execute(
            players = listOf(tricky),
            contributions = listOf(Contribution(teamId = 1L, playerId = 1L, amountMinorUnits = 1000L)),
            expenses = emptyList(),
            expenseShares = emptyList(),
            settlements = emptyList()
        )
        val csv = CsvExporter.exportFullReport(
            teamName = "Northside, Cricket",
            summary = com.teamfund.manager.domain.model.TeamFinancialSummary(teamFundBalanceMinorUnits = 1000L),
            playerSummaries = summary,
            players = mapOf(1L to tricky),
            expenses = listOf(com.teamfund.manager.domain.model.Expense(teamId = 1L, title = "=cmd()", amountMinorUnits = 500L, notes = "line one\nline two")),
            contributions = listOf(Contribution(teamId = 1L, playerId = 1L, amountMinorUnits = 1000L)),
            settlements = emptyList(),
            fines = emptyList(),
            currencyCode = "EUR"
        )

        assertEquals(true, csv.startsWith(Csv.BOM))
        val lines = csv.removePrefix(Csv.BOM).trimEnd().lines()
        val rows = lines.map { CsvImporter.parseCsvLine(it) }
        assertEquals(CsvExporter.FULL_REPORT_HEADER, rows.first())
        rows.forEach { assertEquals(CsvExporter.FULL_REPORT_HEADER.size, it.size) }
        val expense = rows.single { it[0] == "Expense" }
        assertEquals("=cmd()", expense[2])
        assertEquals("-5.00", expense[5])
        assertEquals("line one line two", expense[7])
        assertEquals("Sam \"The Wall\", Lee", rows.single { it[0] == "Balance" }[3])
    }
}
