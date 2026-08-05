package com.teamfund.manager.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateExpenseSharesUseCaseTest {

    private val useCase = CalculateExpenseSharesUseCase()

    @Test
    fun equalSplit_threePlayers_distributesRemainder() {
        // $10.00 = 1000 cents among 3 → 334 + 333 + 333
        val shares = useCase.execute(
            expenseId = 1L,
            totalAmountMinorUnits = 1000L,
            playerIds = listOf(3L, 1L, 2L)
        )

        assertEquals(3, shares.size)
        assertEquals(1000L, shares.sumOf { it.shareAmountMinorUnits })
        // Sorted by player id: 1,2,3 — first `remainder` get +1
        assertEquals(334L, shares[0].shareAmountMinorUnits) // player 1
        assertEquals(333L, shares[1].shareAmountMinorUnits) // player 2
        assertEquals(333L, shares[2].shareAmountMinorUnits) // player 3
        assertEquals(1L, shares[0].playerId)
        assertEquals(2L, shares[1].playerId)
        assertEquals(3L, shares[2].playerId)
    }

    @Test
    fun equalSplit_twoPlayers_even() {
        val shares = useCase.execute(1L, 1000L, listOf(10L, 20L))
        assertEquals(2, shares.size)
        assertEquals(500L, shares[0].shareAmountMinorUnits)
        assertEquals(500L, shares[1].shareAmountMinorUnits)
    }

    @Test
    fun emptyPlayers_returnsEmpty() {
        assertTrue(useCase.execute(1L, 1000L, emptyList()).isEmpty())
    }

    @Test
    fun zeroAmount_returnsEmpty() {
        assertTrue(useCase.execute(1L, 0L, listOf(1L, 2L)).isEmpty())
    }

    @Test
    fun sharesStartUnsettled() {
        val shares = useCase.execute(5L, 100L, listOf(1L))
        assertEquals(1, shares.size)
        assertEquals(false, shares[0].isSettled)
        assertEquals(null, shares[0].settledAt)
        assertEquals(5L, shares[0].expenseId)
    }
}
