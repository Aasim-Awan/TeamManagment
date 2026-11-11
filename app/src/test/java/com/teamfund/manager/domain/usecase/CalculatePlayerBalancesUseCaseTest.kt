package com.teamfund.manager.domain.usecase

import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.FineReason
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.model.SettlementAdjustmentKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatePlayerBalancesUseCaseTest {

    private val useCase = CalculatePlayerBalancesUseCase()

    private val player = Player(id = 1L, teamId = 1L, name = "Test")

    @Test
    fun teamPaysPlayer_settlementOnly_netDecreasesOnce() {
        val expenses = listOf(
            Expense(
                id = 10L,
                teamId = 1L,
                title = "Trip",
                amountMinorUnits = 40000L,
                paidByPlayerId = 1L
            )
        )
        val settlements = listOf(
            Settlement(
                teamId = 1L,
                fromPlayerId = 0L,
                toPlayerId = 1L,
                expenseId = 10L,
                amountMinorUnits = 20000L,
                adjustmentKind = SettlementAdjustmentKind.TEAM_TO_PLAYER
            )
        )

        val summary = useCase.execute(
            players = listOf(player),
            contributions = emptyList(),
            expenses = expenses,
            expenseShares = emptyList(),
            settlements = settlements
        ).single()

        assertEquals(40000L, summary.totalPaidOnBehalfMinorUnits)
        assertEquals(20000L, summary.settlementsReceivedMinorUnits)
        assertEquals(20000L, summary.netBalanceMinorUnits)
        assertEquals(20000L, summary.positionMinorUnits)
    }

    @Test
    fun playerPaysTeam_onExpense_settlementAndSharePaid_offset() {
        val shares = listOf(
            ExpenseShare(
                id = 1L,
                expenseId = 10L,
                playerId = 1L,
                shareAmountMinorUnits = 20000L,
                paidAmountMinorUnits = 20000L
            )
        )
        val settlements = listOf(
            Settlement(
                teamId = 1L,
                fromPlayerId = 1L,
                toPlayerId = null,
                expenseId = 10L,
                amountMinorUnits = 20000L,
                adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM
            )
        )

        val summary = useCase.execute(
            players = listOf(player),
            contributions = emptyList(),
            expenses = emptyList(),
            expenseShares = shares,
            settlements = settlements
        ).single()

        assertEquals(20000L, summary.settlementsPaidMinorUnits)
        assertEquals(0L, summary.netBalanceMinorUnits)
        assertEquals(0L, summary.positionMinorUnits)
    }

    @Test
    fun unpaidShareOnly_positionIsCollectOnce() {
        val shares = listOf(
            ExpenseShare(
                id = 1L,
                expenseId = 10L,
                playerId = 1L,
                shareAmountMinorUnits = 20000L,
                paidAmountMinorUnits = 0L
            )
        )

        val summary = useCase.execute(
            players = listOf(player),
            contributions = emptyList(),
            expenses = emptyList(),
            expenseShares = shares,
            settlements = emptyList()
        ).single()

        assertEquals(20000L, summary.unpaidExpenseSharesMinorUnits)
        assertEquals(0L, summary.netBalanceMinorUnits)
        assertEquals(-20000L, summary.positionMinorUnits)
        assertEquals(20000L, summary.amountTeamShouldCollectMinorUnits)
    }

    @Test
    fun chargeOnly_positionIsCollect() {
        val fines = listOf(
            Fine(
                teamId = 1L,
                playerId = 1L,
                amountMinorUnits = 20000L,
                reason = FineReason.CHARGE
            )
        )

        val summary = useCase.execute(
            players = listOf(player),
            contributions = emptyList(),
            expenses = emptyList(),
            expenseShares = emptyList(),
            settlements = emptyList(),
            fines = fines
        ).single()

        assertEquals(20000L, summary.totalChargesMinorUnits)
        assertEquals(-20000L, summary.netBalanceMinorUnits)
        assertEquals(-20000L, summary.positionMinorUnits)
    }

    @Test
    fun finePaidOnSpot_positionZero() {
        val fines = listOf(
            Fine(
                teamId = 1L,
                playerId = 1L,
                amountMinorUnits = 20000L,
                reason = FineReason.LATE
            )
        )
        val settlements = listOf(
            Settlement(
                teamId = 1L,
                fromPlayerId = 1L,
                toPlayerId = null,
                amountMinorUnits = 20000L,
                adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM
            )
        )

        val summary = useCase.execute(
            players = listOf(player),
            contributions = emptyList(),
            expenses = emptyList(),
            expenseShares = emptyList(),
            settlements = settlements,
            fines = fines
        ).single()

        assertEquals(0L, summary.netBalanceMinorUnits)
        assertEquals(0L, summary.positionMinorUnits)
    }

    @Test
    fun teamPaidPlayer_afterPaidOnBehalf_positionPositive() {
        val expenses = listOf(
            Expense(
                id = 10L,
                teamId = 1L,
                title = "Trip",
                amountMinorUnits = 40000L,
                paidByPlayerId = 1L
            )
        )
        val settlements = listOf(
            Settlement(
                teamId = 1L,
                fromPlayerId = 0L,
                toPlayerId = 1L,
                amountMinorUnits = 20000L,
                adjustmentKind = SettlementAdjustmentKind.TEAM_TO_PLAYER
            )
        )

        val summary = useCase.execute(
            players = listOf(player),
            contributions = emptyList(),
            expenses = expenses,
            expenseShares = emptyList(),
            settlements = settlements
        ).single()

        assertEquals(20000L, summary.netBalanceMinorUnits)
        assertEquals(20000L, summary.positionMinorUnits)
        assertEquals(20000L, summary.amountTeamShouldPayMinorUnits)
    }

    @Test
    fun overpaidShare_showsAsCredit() {
        val shares = listOf(
            ExpenseShare(
                id = 1L,
                expenseId = 10L,
                playerId = 1L,
                shareAmountMinorUnits = 10000L,
                paidAmountMinorUnits = 15000L
            )
        )
        val settlements = listOf(
            Settlement(
                teamId = 1L,
                fromPlayerId = 1L,
                toPlayerId = null,
                expenseId = 10L,
                amountMinorUnits = 15000L,
                adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM
            )
        )

        val summary = useCase.execute(
            players = listOf(player),
            contributions = emptyList(),
            expenses = emptyList(),
            expenseShares = shares,
            settlements = settlements
        ).single()

        // Paid 150 against a 100 share: the extra 50 is credit the team owes back.
        assertEquals(0L, summary.unpaidExpenseSharesMinorUnits)
        assertEquals(5000L, summary.positionMinorUnits)
    }
}
