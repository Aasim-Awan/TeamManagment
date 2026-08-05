package com.teamfund.manager.domain.usecase

import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.PlayerFinancialSummary
import com.teamfund.manager.domain.model.Settlement

class CalculatePlayerBalancesUseCase {

    /**
     * Calculates financial summary for each player:
     *
     * Net Balance =
     *   Contributions
     *   + Expenses Paid on Behalf of Team (paidByPlayerId == playerId)
     *   - Personal Expense Shares (sum of shareAmountMinorUnits where playerId == player)
     *   + Settlements Paid (fromPlayerId == playerId)
     *   - Settlements Received (toPlayerId == playerId)
     */
    fun execute(
        players: List<Player>,
        contributions: List<Contribution>,
        expenses: List<Expense>,
        expenseShares: List<ExpenseShare>,
        settlements: List<Settlement>
    ): List<PlayerFinancialSummary> {
        val contributionMap = contributions.groupBy { it.playerId }
        val expensesPaidMap = expenses.filter { it.paidByPlayerId != null }.groupBy { it.paidByPlayerId!! }
        val sharesMap = expenseShares.groupBy { it.playerId }
        val settlementsPaidMap = settlements.groupBy { it.fromPlayerId }
        val settlementsReceivedMap = settlements.filter { it.toPlayerId != null }.groupBy { it.toPlayerId!! }

        return players.map { player ->
            val totalContrib = contributionMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L
            val totalPaidOnBehalf = expensesPaidMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L
            
            val playerShares = sharesMap[player.id] ?: emptyList()
            val personalSharesTotal = playerShares.sumOf { it.shareAmountMinorUnits }
            val personalSharesPaid = playerShares.sumOf { it.paidAmountMinorUnits }
            val unpaidDues = (personalSharesTotal - personalSharesPaid).coerceAtLeast(0L)

            val setPaid = settlementsPaidMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L
            val setReceived = settlementsReceivedMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L

            // REDEFINED Net Balance: Total Pool Contribution (Contributions + Money given to fund)
            // It no longer automatically subtracts UNPAID expense shares.
            // Balance = Contributions + Money Paid on Behalf + Settlements Paid - Settlements Received - (Paid Shares)
            val netBalance = totalContrib + totalPaidOnBehalf + setPaid - setReceived - personalSharesPaid

            PlayerFinancialSummary(
                player = player,
                totalContributionsMinorUnits = totalContrib,
                totalPaidOnBehalfMinorUnits = totalPaidOnBehalf,
                personalExpenseSharesMinorUnits = personalSharesTotal,
                settlementsPaidMinorUnits = setPaid,
                settlementsReceivedMinorUnits = setReceived,
                netBalanceMinorUnits = netBalance,
                unpaidExpenseSharesMinorUnits = unpaidDues
            )
        }
    }
}
