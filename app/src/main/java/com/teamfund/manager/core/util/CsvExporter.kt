package com.teamfund.manager.core.util

import com.teamfund.manager.domain.model.*

/** Safe CSV cell writing shared by every export. */
internal object Csv {

    private val numeric = Regex("^-?\\d+(\\.\\d+)?$")
    private val formulaStarts = setOf('=', '+', '-', '@', '\t', '\r')

    /**
     * Quotes one cell. Inner quotes are doubled, line breaks become spaces (the importer reads
     * one record per line), and text starting with = + - @ gets a leading ' so spreadsheet apps
     * do not run it as a formula. Plain numbers such as "-12.50" are left as numbers.
     */
    fun cell(value: Any?): String {
        var text = value?.toString().orEmpty().replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
        if (text.isNotEmpty() && text[0] in formulaStarts && !numeric.matches(text)) {
            text = "'$text"
        }
        return "\"" + text.replace("\"", "\"\"") + "\""
    }

    fun row(vararg values: Any?): String = values.joinToString(",") { cell(it) } + "\n"

    /** ISO date (yyyy-MM-dd), the same on every device and language, so files re-import cleanly. */
    fun date(timestamp: Long): String = DateTimeUtils.toLocalDate(timestamp).toString()

    /** UTF-8 byte-order mark so Excel opens accented names and currency symbols correctly. */
    const val BOM = "\uFEFF"
}

object CsvExporter {

    fun exportContributions(contributions: List<Contribution>, players: Map<Long, Player>): String {
        val sb = StringBuilder(Csv.BOM)
        sb.append("Date,Player,Amount,Purpose,Notes\n")
        contributions.forEach { contribution ->
            val playerName = players[contribution.playerId]?.name ?: "Unknown"
            sb.append(
                Csv.row(
                    Csv.date(contribution.date),
                    playerName,
                    MoneyFormatter.fromMinorUnits(contribution.amountMinorUnits),
                    contribution.purpose.displayName,
                    contribution.notes
                )
            )
        }
        return sb.toString()
    }

    fun exportExpenses(expenses: List<Expense>, players: Map<Long, Player>): String {
        val sb = StringBuilder(Csv.BOM)
        sb.append("Date,Title,Category,Amount,Paid By,Notes\n")
        expenses.forEach { expense ->
            val paidByName = if (expense.paidByPlayerId == null) "Team Fund" else players[expense.paidByPlayerId]?.name ?: "Unknown"
            sb.append(
                Csv.row(
                    Csv.date(expense.date),
                    expense.title,
                    expense.category.displayName,
                    MoneyFormatter.fromMinorUnits(expense.amountMinorUnits),
                    paidByName,
                    expense.notes
                )
            )
        }
        return sb.toString()
    }

    /** Columns of the full report. Every row has exactly these columns, so the file opens as one table. */
    val FULL_REPORT_HEADER = listOf("Section", "Date", "Description", "Player", "Category", "Amount", "Currency", "Notes")

    /**
     * Full team report as one fixed-column table, sorted newest first inside each section.
     * Amount sign = effect on the team: money in is positive, money out and expenses are negative.
     * Balance rows: positive = team owes the player, negative = collect from the player.
     * Charge rows are amounts a player owes (not cash moved), shown positive.
     */
    fun exportFullReport(
        teamName: String,
        summary: TeamFinancialSummary,
        playerSummaries: List<PlayerFinancialSummary>,
        players: Map<Long, Player>,
        expenses: List<Expense>,
        contributions: List<Contribution>,
        settlements: List<Settlement>,
        fines: List<Fine>,
        currencyCode: String,
        generatedAt: Long = System.currentTimeMillis()
    ): String {
        val sb = StringBuilder(Csv.BOM)
        sb.append(Csv.row(*FULL_REPORT_HEADER.toTypedArray()))
        fun add(section: String, date: Long?, description: String, player: String?, category: String?, amount: Long?, notes: String?) {
            sb.append(
                Csv.row(
                    section,
                    date?.let { Csv.date(it) },
                    description,
                    player,
                    category,
                    amount?.let { MoneyFormatter.fromMinorUnits(it) },
                    if (amount != null) currencyCode else null,
                    notes
                )
            )
        }
        fun name(id: Long?): String = when (id) {
            null, 0L -> "Team Fund"
            else -> players[id]?.name ?: "Unknown"
        }

        add("Summary", generatedAt, "Team", null, null, null, teamName)
        add("Summary", generatedAt, "Team fund balance", null, null, summary.teamFundBalanceMinorUnits, null)
        add("Summary", generatedAt, "Money in", null, null, summary.totalContributionsMinorUnits, "Contributions and payments into the fund")
        add("Summary", generatedAt, "Money out from fund", null, null, -summary.totalExpensesFromFundMinorUnits, "Expenses paid by the fund and payouts to players")
        add("Summary", generatedAt, "Paid by players", null, null, summary.totalExpensesByPlayersMinorUnits, "Expenses players paid for the team")
        add("Summary", generatedAt, "To collect from players", null, null, summary.totalOutstandingDebtMinorUnits, null)
        add("Summary", generatedAt, "Team owes players", null, null, playerSummaries.sumOf { it.amountTeamShouldPayMinorUnits }, null)

        playerSummaries.sortedBy { it.player.name.lowercase() }.forEach { p ->
            add("Balance", generatedAt, "Net position", p.player.name, if (p.player.isActive) "Active" else "Inactive", p.positionMinorUnits, p.statusText)
        }
        expenses.sortedByDescending { it.date }.forEach { e ->
            add("Expense", e.date, e.title, name(e.paidByPlayerId), e.category.displayName, -e.amountMinorUnits, e.notes)
        }
        contributions.sortedByDescending { it.date }.forEach { c ->
            add("Contribution", c.date, "Contribution received", name(c.playerId), c.purpose.displayName, c.amountMinorUnits, c.notes)
        }
        settlements.sortedByDescending { it.date }.forEach { s ->
            val teamPaid = s.adjustmentKind == SettlementAdjustmentKind.TEAM_TO_PLAYER
            add(
                "Settlement", s.date,
                if (teamPaid) "Team paid player" else "Player paid team",
                if (teamPaid) name(s.toPlayerId) else name(s.fromPlayerId),
                s.adjustmentKind.displayName,
                if (teamPaid) -s.amountMinorUnits else s.amountMinorUnits,
                s.note
            )
        }
        fines.sortedByDescending { it.date }.forEach { f ->
            add("Charge", f.date, f.reason.displayName, name(f.playerId), f.reason.displayName, f.amountMinorUnits, f.note)
        }
        return sb.toString()
    }

    fun exportMatchPack(
        match: Match,
        players: Map<Long, Player>,
        matchPlayers: List<MatchPlayer>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        fines: List<Fine>,
        stats: List<MatchPlayerStats>,
        currencyCode: String
    ): String {
        val sb = StringBuilder(Csv.BOM)
        sb.append("MATCH EXPORT\n")
        sb.append(Csv.row("Opponent", match.opponentName))
        sb.append(Csv.row("Date", Csv.date(match.date)))
        sb.append(Csv.row("Venue", match.venue))
        sb.append(Csv.row("Result", match.result.displayName))
        sb.append(Csv.row("Match fee", MoneyFormatter.fromMinorUnits(match.matchFeeMinorUnits), currencyCode))
        sb.append(Csv.row("Ground fee", MoneyFormatter.fromMinorUnits(match.groundFeeMinorUnits), currencyCode))
        sb.append("\n")

        sb.append("PLAYING XI\n")
        sb.append("Player,Status\n")
        matchPlayers.filter { it.status == MatchPlayerStatus.SELECTED }.forEach { mp ->
            val name = players[mp.playerId]?.name ?: "Unknown"
            sb.append(Csv.row(name, mp.status.displayName))
        }
        sb.append("\n")

        sb.append("EXPENSES\n")
        sb.append("Date,Title,Category,Amount,Paid By\n")
        expenses.forEach { expense ->
            val paidBy = if (expense.paidByPlayerId == null) "Team Fund" else players[expense.paidByPlayerId]?.name ?: "Unknown"
            sb.append(
                Csv.row(
                    Csv.date(expense.date),
                    expense.title,
                    expense.category.displayName,
                    MoneyFormatter.fromMinorUnits(expense.amountMinorUnits),
                    paidBy
                )
            )
        }
        sb.append("\n")

        sb.append("SETTLEMENTS\n")
        sb.append("Date,From,To,Amount,Note\n")
        settlements.forEach { s ->
            val from = if (s.fromPlayerId == 0L) "Team Fund" else players[s.fromPlayerId]?.name ?: "Unknown"
            val to = s.toPlayerId?.let { players[it]?.name } ?: "Team Fund"
            sb.append(Csv.row(Csv.date(s.date), from, to, MoneyFormatter.fromMinorUnits(s.amountMinorUnits), s.note))
        }
        sb.append("\n")

        sb.append("FINES\n")
        sb.append("Date,Player,Reason,Amount,Note\n")
        fines.forEach { fine ->
            val name = players[fine.playerId]?.name ?: "Unknown"
            sb.append(
                Csv.row(
                    Csv.date(fine.date),
                    name,
                    fine.reason.displayName,
                    MoneyFormatter.fromMinorUnits(fine.amountMinorUnits),
                    fine.note
                )
            )
        }
        sb.append("\n")

        sb.append("PLAYER STATS\n")
        sb.append("Player,Runs,Balls,Wickets,Overs,Catches,POTM\n")
        stats.forEach { st ->
            val name = players[st.playerId]?.name ?: "Unknown"
            sb.append(Csv.row(name, st.runs, st.balls, st.wickets, st.oversBowled, st.catches, st.isPlayerOfMatch))
        }

        return sb.toString()
    }
}
