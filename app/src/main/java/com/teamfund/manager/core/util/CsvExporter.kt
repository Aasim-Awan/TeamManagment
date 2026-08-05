package com.teamfund.manager.core.util

import com.teamfund.manager.domain.model.*

object CsvExporter {

    fun exportContributions(contributions: List<Contribution>, players: Map<Long, Player>): String {
        val sb = StringBuilder()
        sb.append("Date,Player,Amount,Purpose,Notes\n")
        contributions.forEach { contribution ->
            val playerName = players[contribution.playerId]?.name ?: "Unknown"
            val date = DateTimeUtils.formatDate(contribution.date)
            sb.append("\"$date\",\"$playerName\",\"${MoneyFormatter.fromMinorUnits(contribution.amountMinorUnits)}\",\"${contribution.purpose.displayName}\",\"${contribution.notes ?: ""}\"\n")
        }
        return sb.toString()
    }

    fun exportExpenses(expenses: List<Expense>, players: Map<Long, Player>): String {
        val sb = StringBuilder()
        sb.append("Date,Title,Category,Amount,Paid By,Notes\n")
        expenses.forEach { expense ->
            val paidByName = if (expense.paidByPlayerId == null) "Team Fund" else players[expense.paidByPlayerId]?.name ?: "Unknown"
            val date = DateTimeUtils.formatDate(expense.date)
            sb.append("\"$date\",\"${expense.title}\",\"${expense.category.displayName}\",\"${MoneyFormatter.fromMinorUnits(expense.amountMinorUnits)}\",\"$paidByName\",\"${expense.notes ?: ""}\"\n")
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
        sb.append("MASTER FINANCIAL AUDIT: $teamName\n")
        sb.append("Generated on: ${DateTimeUtils.formatDate(System.currentTimeMillis())}\n\n")

        // 0. Actionable Summary
        sb.append("ACTIONABLE SUMMARY\n")
        
        val toPay = players.filter { it.netBalanceMinorUnits > 0 }
        val toCollect = players.filter { it.netBalanceMinorUnits < 0 }

        sb.append("MONEY TO GIVE BACK (Team owes these members):\n")
        if (toPay.isEmpty()) {
            sb.append("None - All payouts settled.\n")
        } else {
            toPay.forEach { p ->
                sb.append("${p.player.name}: ${MoneyFormatter.fromMinorUnits(p.netBalanceMinorUnits)} $currencyCode\n")
            }
        }
        sb.append("\n")

        sb.append("MONEY TO COLLECT (Members owe the team):\n")
        if (toCollect.isEmpty()) {
            sb.append("None - All dues collected.\n")
        } else {
            toCollect.forEach { p ->
                sb.append("${p.player.name}: ${MoneyFormatter.fromMinorUnits(Math.abs(p.netBalanceMinorUnits))} $currencyCode\n")
            }
        }
        sb.append("\n---\n\n")

        // 1. Expense Distribution
        sb.append("EXPENSE DISTRIBUTION\n")
        sb.append("Date,Title,Category,Amount ($currencyCode),Paid By,Notes\n")
        expenses.forEach { expense ->
            val paidByName = if (expense.paidByPlayerId == null) "Team Fund" else players.find { it.player.id == expense.paidByPlayerId }?.player?.name ?: "Unknown"
            val date = DateTimeUtils.formatDate(expense.date)
            sb.append("\"$date\",\"${expense.title}\",\"${expense.category.displayName}\",\"${MoneyFormatter.fromMinorUnits(expense.amountMinorUnits)}\",\"$paidByName\",\"${expense.notes ?: ""}\"\n")
        }
        sb.append("\n")

        // 2. Member Balances
        sb.append("MEMBER LEDGER & REMAINING BALANCES\n")
        sb.append("Member Name,Total Contributions,Paid on Behalf,Personal Expense Shares,Settlements Paid,Settlements Received,Net Balance ($currencyCode),Status\n")
        players.forEach { p ->
            sb.append("\"${p.player.name}\",")
            sb.append("\"${MoneyFormatter.fromMinorUnits(p.totalContributionsMinorUnits)}\",")
            sb.append("\"${MoneyFormatter.fromMinorUnits(p.totalPaidOnBehalfMinorUnits)}\",")
            sb.append("\"${MoneyFormatter.fromMinorUnits(p.personalExpenseSharesMinorUnits)}\",")
            sb.append("\"${MoneyFormatter.fromMinorUnits(p.settlementsPaidMinorUnits)}\",")
            sb.append("\"${MoneyFormatter.fromMinorUnits(p.settlementsReceivedMinorUnits)}\",")
            sb.append("\"${MoneyFormatter.fromMinorUnits(p.netBalanceMinorUnits)}\",")
            sb.append("\"${p.statusText}\"\n")
        }

        return sb.toString()
    }
}
