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
}

object CsvExporter {

    fun exportContributions(contributions: List<Contribution>, players: Map<Long, Player>): String {
        val sb = StringBuilder()
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
        val sb = StringBuilder()
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

    fun exportMasterReport(
        teamName: String,
        expenses: List<Expense>,
        players: List<PlayerFinancialSummary>,
        currencyCode: String
    ): String {
        val sb = StringBuilder()
        sb.append(Csv.row("MASTER FINANCIAL AUDIT", teamName))
        sb.append(Csv.row("Generated on", Csv.date(System.currentTimeMillis())))
        sb.append("\n")

        // 0. Actionable Summary
        sb.append("ACTIONABLE SUMMARY\n")

        val toPay = players.filter { it.positionMinorUnits > 0 }
        val toCollect = players.filter { it.positionMinorUnits < 0 }

        sb.append("MONEY TO GIVE BACK (Team owes these members)\n")
        if (toPay.isEmpty()) {
            sb.append("None - All payouts settled.\n")
        } else {
            toPay.forEach { p ->
                sb.append(Csv.row(p.player.name, MoneyFormatter.fromMinorUnits(p.positionMinorUnits), currencyCode))
            }
        }
        sb.append("\n")

        sb.append("MONEY TO COLLECT (Members owe the team)\n")
        if (toCollect.isEmpty()) {
            sb.append("None - All dues collected.\n")
        } else {
            toCollect.forEach { p ->
                sb.append(Csv.row(p.player.name, MoneyFormatter.fromMinorUnits(p.amountTeamShouldCollectMinorUnits), currencyCode))
            }
        }
        sb.append("\n---\n\n")

        // 1. Expense Distribution
        sb.append("EXPENSE DISTRIBUTION\n")
        sb.append(Csv.row("Date", "Title", "Category", "Amount ($currencyCode)", "Paid By", "Notes"))
        expenses.forEach { expense ->
            val paidByName = if (expense.paidByPlayerId == null) "Team Fund" else players.find { it.player.id == expense.paidByPlayerId }?.player?.name ?: "Unknown"
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
        sb.append("\n")

        // 2. Member Balances
        sb.append("MEMBER LEDGER & REMAINING BALANCES\n")
        sb.append(
            Csv.row(
                "Member Name", "Total Contributions", "Paid on Behalf", "Personal Expense Shares",
                "Settlements Paid", "Settlements Received", "Net Balance ($currencyCode)", "Status"
            )
        )
        players.forEach { p ->
            sb.append(
                Csv.row(
                    p.player.name,
                    MoneyFormatter.fromMinorUnits(p.totalContributionsMinorUnits),
                    MoneyFormatter.fromMinorUnits(p.totalPaidOnBehalfMinorUnits),
                    MoneyFormatter.fromMinorUnits(p.personalExpenseSharesMinorUnits),
                    MoneyFormatter.fromMinorUnits(p.settlementsPaidMinorUnits),
                    MoneyFormatter.fromMinorUnits(p.settlementsReceivedMinorUnits),
                    MoneyFormatter.fromMinorUnits(p.positionMinorUnits),
                    p.statusText
                )
            )
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
        val sb = StringBuilder()
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
