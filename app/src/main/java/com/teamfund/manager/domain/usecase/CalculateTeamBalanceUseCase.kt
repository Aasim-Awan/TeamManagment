package com.teamfund.manager.domain.usecase

import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.model.TeamFinancialSummary

class CalculateTeamBalanceUseCase {

    /**
     * Calculates the overall team financial status:
     *
     * Total Money In = Contributions + Settlements from members (toPlayerId == null)
     * Total Money Out = Team Expenses Paid From Fund + Settlements to members (fromPlayerId == 0/null equivalent)
     * Team Fund Balance = Total Money In - Total Money Out
     * Total Outstanding Debt = Sum of negative net balances across players
     */
    fun execute(
        contributions: List<Contribution>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        playerSummaries: List<PlayerFinancialSummary>
    ): TeamFinancialSummary {
        val totalContributions = contributions.sumOf { it.amountMinorUnits }
        
        // Settlements into the team fund (usually from members clearing debt)
        val totalSettlementsIn = settlements
            .filter { it.toPlayerId == null }
            .sumOf { it.amountMinorUnits }

        val totalMoneyIn = totalContributions + totalSettlementsIn
        
        val expensesFromFund = expenses.filter { it.paidByPlayerId == null }
            .sumOf { it.amountMinorUnits }

        // Settlements out of team fund (e.g. paying back a member who paid extra)
        // In our system, fromPlayerId = 0L or similar might be used for 'Team'
        // But typically settlements out are recorded with toPlayerId = memberId and fromPlayerId as 'Team'
        // Let's assume fromPlayerId = 0L represents the Team Fund
        val totalSettlementsOut = settlements
            .filter { it.fromPlayerId == 0L }
            .sumOf { it.amountMinorUnits }

        val totalMoneyOut = expensesFromFund + totalSettlementsOut

        val expensesByPlayers = expenses.filter { it.paidByPlayerId != null }
            .sumOf { it.amountMinorUnits }

        val teamBalance = totalMoneyIn - totalMoneyOut

        val totalOutstanding = playerSummaries.sumOf { it.amountTeamShouldCollectMinorUnits }

        return TeamFinancialSummary(
            teamFundBalanceMinorUnits = teamBalance,
            totalContributionsMinorUnits = totalMoneyIn, // This maps to "Money In" in UI
            totalExpensesFromFundMinorUnits = totalMoneyOut, // This maps to "Money Out" in UI
            totalExpensesByPlayersMinorUnits = expensesByPlayers,
            totalOutstandingDebtMinorUnits = totalOutstanding
        )
    }
}
