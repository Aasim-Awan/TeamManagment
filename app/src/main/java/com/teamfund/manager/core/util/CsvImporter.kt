package com.teamfund.manager.core.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvImporter {

    /** Rows of a Contributions export: Date,Player,Amount,Purpose,Notes. Rows with a bad date or amount are dropped. */
    fun parseContributions(csvContent: String): ParseResult<ParsedContribution> =
        parseRows(csvContent, minColumns = 4) { parts ->
            val date = parseDate(parts[0]) ?: return@parseRows null
            val amount = MoneyFormatter.toMinorUnits(parts[2])
            if (amount <= 0L || parts[1].isBlank()) return@parseRows null
            ParsedContribution(
                date = date,
                playerName = parts[1],
                amountMinorUnits = amount,
                purposeStr = parts[3],
                notes = parts.getOrNull(4).orEmpty()
            )
        }

    /** Rows of an Expenses export: Date,Title,Category,Amount,Paid By,Notes. */
    fun parseExpenses(csvContent: String): ParseResult<ParsedExpense> =
        parseRows(csvContent, minColumns = 5) { parts ->
            val date = parseDate(parts[0]) ?: return@parseRows null
            val amount = MoneyFormatter.toMinorUnits(parts[3])
            if (amount <= 0L || parts[1].isBlank()) return@parseRows null
            ParsedExpense(
                date = date,
                title = parts[1],
                categoryStr = parts[2],
                amountMinorUnits = amount,
                paidByStr = parts[4],
                notes = parts.getOrNull(5).orEmpty()
            )
        }

    private fun <T> parseRows(csvContent: String, minColumns: Int, map: (List<String>) -> T?): ParseResult<T> {
        val dataLines = csvContent.removePrefix("\uFEFF").lines().drop(1).filter { it.isNotBlank() }
        val rows = mutableListOf<T>()
        var invalid = 0
        dataLines.forEach { line ->
            val parts = parseCsvLine(line)
            val row = if (parts.size >= minColumns) runCatching { map(parts) }.getOrNull() else null
            if (row != null) rows += row else invalid++
        }
        return ParseResult(rows, invalid)
    }

    /** Splits one CSV line. Handles quoted cells, doubled quotes ("") and the ' formula guard the exporter adds. */
    internal fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result.map { cell ->
            val trimmed = cell.trim()
            if (trimmed.length > 1 && trimmed[0] == '\'' && trimmed[1] in "=+-@") trimmed.substring(1) else trimmed
        }
    }

    private val legacyDateFormats = listOf(
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM dd, yyyy"),
    )

    /** Accepts ISO dates (current exports) and the older "Nov 10, 2026" format. Returns local start of day, or null. */
    internal fun parseDate(text: String): Long? {
        val trimmed = text.trim()
        val date = runCatching { LocalDate.parse(trimmed) }.getOrNull()
            ?: legacyDateFormats.firstNotNullOfOrNull { format -> runCatching { LocalDate.parse(trimmed, format) }.getOrNull() }
            ?: return null
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    data class ParseResult<T>(val rows: List<T>, val invalidRows: Int)

    data class ParsedContribution(
        val date: Long,
        val playerName: String,
        val amountMinorUnits: Long,
        val purposeStr: String,
        val notes: String
    )

    data class ParsedExpense(
        val date: Long,
        val title: String,
        val categoryStr: String,
        val amountMinorUnits: Long,
        val paidByStr: String,
        val notes: String
    )
}
