package com.teamfund.manager.domain.usecase

import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.Settlement
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateTeamBalanceUseCaseTest {

    private val useCase = CalculateTeamBalanceUseCase()

    @Test
    fun fixedSplitOverExpense_extraStaysInTeamFund() {
        // Expense 1000 paid from the fund; 11 players each pay a fixed 100 share.
        val expense = Expense(id = 10L, teamId = 1L, title = "Ground", amountMinorUnits = 1000L, paidByPlayerId = null)
        val payments = (1L..11L).map { playerId ->
            Settlement(teamId = 1L, fromPlayerId = playerId, toPlayerId = null, expenseId = 10L, amountMinorUnits = 100L)
        }

        val summary = useCase.execute(
            contributions = emptyList(),
            expenses = listOf(expense),
            settlements = payments,
            playerSummaries = emptyList()
        )

        assertEquals(100L, summary.teamFundBalanceMinorUnits)
    }
}
