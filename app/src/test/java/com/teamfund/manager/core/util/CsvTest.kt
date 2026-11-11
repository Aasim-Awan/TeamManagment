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
}
