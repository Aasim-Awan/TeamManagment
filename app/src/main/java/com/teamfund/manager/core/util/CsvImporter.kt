package com.teamfund.manager.core.util

import com.teamfund.manager.domain.model.*

object CsvImporter {

    fun parseContributions(csvContent: String): List<ParsedContribution> {
        val lines = csvContent.lines()
        if (lines.isEmpty()) return emptyList()
        
        val header = lines[0]
        // Expecting: Date,Player,Amount,Purpose,Notes
        
        return lines.drop(1).filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                val parts = parseCsvLine(line)
                if (parts.size >= 4) {
                    ParsedContribution(
                        dateStr = parts[0],
                        playerName = parts[1],
                        amount = parts[2].toDoubleOrNull() ?: 0.0,
                        purposeStr = parts[3],
                        notes = parts.getOrNull(4) ?: ""
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun parseExpenses(csvContent: String): List<ParsedExpense> {
        val lines = csvContent.lines()
        if (lines.isEmpty()) return emptyList()
        
        return lines.drop(1).filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                val parts = parseCsvLine(line)
                if (parts.size >= 5) {
                    ParsedExpense(
                        dateStr = parts[0],
                        title = parts[1],
                        categoryStr = parts[2],
                        amount = parts[3].toDoubleOrNull() ?: 0.0,
                        paidByStr = parts[4],
                        notes = parts.getOrNull(5) ?: ""
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result.map { it.trim() }
    }

    data class ParsedContribution(
        val dateStr: String,
        val playerName: String,
        val amount: Double,
        val purposeStr: String,
        val notes: String
    )

    data class ParsedExpense(
        val dateStr: String,
        val title: String,
        val categoryStr: String,
        val amount: Double,
        val paidByStr: String,
        val notes: String
    )
}
