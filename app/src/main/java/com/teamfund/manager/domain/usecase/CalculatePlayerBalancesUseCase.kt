package com.teamfund.manager.domain.usecase

import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Fine
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
        settlements: List<Settlement>,
        fines: List<Fine> = emptyList()
    ): List<PlayerFinancialSummary> {
        val contributionMap = contributions.groupBy { it.playerId }
        val expensesPaidMap = expenses.filter { it.paidByPlayerId != null }.groupBy { it.paidByPlayerId!! }
        val sharesMap = expenseShares.groupBy { it.playerId }
        val settlementsPaidMap = settlements.groupBy { it.fromPlayerId }
        val settlementsReceivedMap = settlements.filter { it.toPlayerId != null }.groupBy { it.toPlayerId!! }
        val finesMap = fines.groupBy { it.playerId }

        return players.map { player ->
            val totalContrib = contributionMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L
            val totalPaidOnBehalf = expensesPaidMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L
            
            val playerShares = sharesMap[player.id] ?: emptyList()
            val personalSharesTotal = playerShares.sumOf { it.shareAmountMinorUnits }
            // Count a share as paid only up to its own amount. Any extra stays in the
            // settlement total, so an overpayment shows as credit instead of vanishing.
            val personalSharesPaid = playerShares.sumOf { minOf(it.paidAmountMinorUnits, it.shareAmountMinorUnits) }
            val unpaidDues = (personalSharesTotal - personalSharesPaid).coerceAtLeast(0L)

            val setPaid = settlementsPaidMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L
            val setReceived = settlementsReceivedMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L
            val totalCharges = finesMap[player.id]?.sumOf { it.amountMinorUnits } ?: 0L

            val netBalance = totalContrib + totalPaidOnBehalf + setPaid - setReceived - personalSharesPaid - totalCharges

            PlayerFinancialSummary(
                player = player,
                totalContributionsMinorUnits = totalContrib,
                totalPaidOnBehalfMinorUnits = totalPaidOnBehalf,
                personalExpenseSharesMinorUnits = personalSharesTotal,
                settlementsPaidMinorUnits = setPaid,
                settlementsReceivedMinorUnits = setReceived,
                totalChargesMinorUnits = totalCharges,
                netBalanceMinorUnits = netBalance,
                unpaidExpenseSharesMinorUnits = unpaidDues
            )
        }
    }
}
