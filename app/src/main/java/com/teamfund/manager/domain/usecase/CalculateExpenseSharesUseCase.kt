package com.teamfund.manager.domain.usecase

import com.teamfund.manager.domain.model.ExpenseShare

class CalculateExpenseSharesUseCase {

    /**
     * Splits [totalAmountMinorUnits] equally among [playerIds].
     *
     * Handles rounding safely by distributing remaining minor units deterministically.
     * Example: $10.00 (1000 cents) shared by 3 players:
     * Player 1: 334 cents ($3.34)
     * Player 2: 333 cents ($3.33)
     * Player 3: 333 cents ($3.33)
     * Sum = 1000 cents ($10.00).
     */
    fun execute(
        expenseId: Long,
        totalAmountMinorUnits: Long,
        playerIds: List<Long>
    ): List<ExpenseShare> {
        if (playerIds.isEmpty() || totalAmountMinorUnits <= 0L) {
            return emptyList()
        }

        val count = playerIds.size.toLong()
        val baseShare = totalAmountMinorUnits / count
        val remainder = (totalAmountMinorUnits % count).toInt()

        // Sort player IDs to ensure deterministic assignment of remainder
        val sortedPlayerIds = playerIds.sorted()

        return sortedPlayerIds.mapIndexed { index, playerId ->
            val extra = if (index < remainder) 1L else 0L
            val shareAmount = baseShare + extra
            ExpenseShare(
                expenseId = expenseId,
                playerId = playerId,
                shareAmountMinorUnits = shareAmount,
                isSettled = false,
                settledAt = null
            )
        }
    }

    /**
     * Creates shares based on manual amounts.
     */
    fun executeManual(
        expenseId: Long,
        playerAmounts: Map<Long, Long>
    ): List<ExpenseShare> {
        return playerAmounts.map { (playerId, amount) ->
            ExpenseShare(
                expenseId = expenseId,
                playerId = playerId,
                shareAmountMinorUnits = amount,
                isSettled = false,
                settledAt = null
            )
        }
    }
}
