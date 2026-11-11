package com.teamfund.manager.feature.allexpenses

import com.teamfund.manager.core.util.DateTimeUtils
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class AllExpensesLedgerTest {

    private val zone = ZoneId.systemDefault()
    private fun millis(year: Int, month: Int, day: Int): Long =
        java.time.LocalDate.of(year, month, day).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private val alice = Player(id = 1L, teamId = 1L, name = "Alice")
    private val bob = Player(id = 2L, teamId = 1L, name = "Bob")

    private val items = AllExpensesLedger.toActivities(
        players = listOf(alice, bob),
        expenses = listOf(
            Expense(
                id = 10L, teamId = 1L, title = "Ground", amountMinorUnits = 2000L,
                date = millis(2026, 11, 3),
                shares = listOf(
                    ExpenseShare(expenseId = 10L, playerId = 1L, shareAmountMinorUnits = 1000L, paidAmountMinorUnits = 400L),
                    ExpenseShare(expenseId = 10L, playerId = 2L, shareAmountMinorUnits = 1000L)
                )
            ),
            Expense(
                id = 11L, teamId = 1L, title = "Balls", amountMinorUnits = 500L,
                paidByPlayerId = 2L, date = millis(2026, 10, 20)
            )
        ),
        contributions = listOf(
            Contribution(id = 20L, teamId = 1L, playerId = 1L, amountMinorUnits = 3000L, date = millis(2026, 11, 3))
        ),
        settlements = emptyList(),
        fines = listOf(Fine(id = 30L, teamId = 1L, playerId = 2L, amountMinorUnits = 100L, date = millis(2026, 10, 21)))
    )

    @Test
    fun group_splitsByMonthThenDay_newestFirst() {
        val months = AllExpensesLedger.group(items)

        assertEquals(listOf("2026-11", "2026-10"), months.map { it.month.toString() })
        assertEquals(2000L, months[0].expenseTotalMinorUnits)
        assertEquals(1, months[0].days.size)
        assertEquals(2, months[0].days[0].items.size)
        assertEquals(listOf(21, 20), months[1].days.map { it.date.dayOfMonth })
    }

    @Test
    fun filter_byPlayer_includesSharesPaymentsAndCharges() {
        val bobItems = AllExpensesLedger.filter(items, LedgerFilters(playerId = 2L))

        // Bob shares in "Ground", paid for "Balls" and has a fine; Alice's contribution is excluded.
        assertEquals(3, bobItems.size)
    }

    @Test
    fun filter_byTypeAndMonth() {
        val novExpenses = AllExpensesLedger.filter(
            items,
            LedgerFilters(type = LedgerTypeFilter.EXPENSES, month = DateTimeUtils.toYearMonth(millis(2026, 11, 1)))
        )
        assertEquals(1, novExpenses.size)
    }

    @Test
    fun summarize_allTime() {
        val summary = AllExpensesLedger.summarize(items, month = null)

        assertEquals(2500L, summary.totalSpentMinorUnits)
        assertEquals(2000L, summary.paidFromFundMinorUnits)
        assertEquals(500L, summary.paidByPlayersMinorUnits)
        assertEquals(1600L, summary.unpaidSharesMinorUnits)
        assertEquals(3000L, summary.contributionsMinorUnits)
        assertEquals(100L, summary.chargesMinorUnits)
        assertEquals(2, summary.expenseCount)
    }
}
