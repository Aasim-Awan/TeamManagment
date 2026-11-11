package com.teamfund.manager.domain.model

data class Money(
    val amountMinorUnits: Long = 0L,
    val currencyCode: String = "USD"
) {
    companion object {
        fun zero(currencyCode: String = "USD") = Money(0L, currencyCode)
        fun fromMajor(amountMajor: Double, currencyCode: String = "USD"): Money {
            return Money(
                amountMinorUnits = com.teamfund.manager.core.util.MoneyFormatter.toMinorUnits(amountMajor),
                currencyCode = currencyCode
            )
        }
    }
}

data class Team(
    val id: Long = 0L,
    val name: String,
    val sport: SportType = SportType.CRICKET,
    val logoUri: String? = null,
    val currencyCode: String = "USD",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class Player(
    val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val role: PlayerRole = PlayerRole.PLAYER,
    val cricketRole: CricketRole = CricketRole.UNSPECIFIED,
    val phoneNumber: String? = null,
    val avatarUri: String? = null,
    val isActive: Boolean = true,
    val joinedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Match(
    val id: Long = 0L,
    val teamId: Long,
    val opponentName: String,
    val date: Long = System.currentTimeMillis(),
    val venue: String? = null,
    val result: MatchResult = MatchResult.UPCOMING,
    val teamScore: String? = null,
    val opponentScore: String? = null,
    val overs: String? = null,
    val tossWinner: String? = null,
    val playerOfTheMatchId: Long? = null,
    val seasonId: Long? = null,
    val matchFeeMinorUnits: Long = 0L,
    val groundFeeMinorUnits: Long = 0L,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Expense(
    val id: Long = 0L,
    val teamId: Long,
    val matchId: Long? = null,
    val title: String,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amountMinorUnits: Long,
    val currencyCode: String = "USD",
    val paidByPlayerId: Long? = null, // null means paid directly from Team Fund
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val shares: List<ExpenseShare> = emptyList()
)

data class ExpenseShare(
    val id: Long = 0L,
    val expenseId: Long = 0L,
    val playerId: Long,
    val shareAmountMinorUnits: Long,
    val paidAmountMinorUnits: Long = 0L,
    val isSettled: Boolean = false,
    val settledAt: Long? = null
) {
    val remainingAmountMinorUnits: Long
        get() = (shareAmountMinorUnits - paidAmountMinorUnits).coerceAtLeast(0L)
}

data class Contribution(
    val id: Long = 0L,
    val teamId: Long,
    val playerId: Long,
    val amountMinorUnits: Long,
    val date: Long = System.currentTimeMillis(),
    val purpose: ContributionPurpose = ContributionPurpose.GENERAL,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Settlement(
    val id: Long = 0L,
    val teamId: Long,
    val fromPlayerId: Long,
    val toPlayerId: Long? = null, // null = paid into team fund
    val matchId: Long? = null,
    val expenseId: Long? = null,
    val amountMinorUnits: Long,
    val adjustmentKind: SettlementAdjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class MatchPlayer(
    val id: Long = 0L,
    val matchId: Long,
    val playerId: Long,
    val status: MatchPlayerStatus = MatchPlayerStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis()
)

data class Season(
    val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = false
)

data class MatchPlayerStats(
    val id: Long = 0L,
    val matchId: Long,
    val playerId: Long,
    val runs: Int = 0,
    val balls: Int = 0,
    val wickets: Int = 0,
    val oversBowled: String? = null,
    val catches: Int = 0,
    val isPlayerOfMatch: Boolean = false
)

data class Fine(
    val id: Long = 0L,
    val teamId: Long,
    val playerId: Long,
    val matchId: Long? = null,
    val amountMinorUnits: Long,
    val reason: FineReason = FineReason.OTHER,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val settlementId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class InventoryItem(
    val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val category: InventoryCategory = InventoryCategory.OTHER,
    val quantity: Int = 0,
    val notes: String? = null
)

data class InventoryMovement(
    val id: Long = 0L,
    val itemId: Long,
    val delta: Int,
    val matchId: Long? = null,
    val playerId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null
)

data class DueSchedule(
    val id: Long = 0L,
    val teamId: Long,
    val name: String,
    val amountMinorUnits: Long,
    val period: DuePeriod = DuePeriod.MONTHLY,
    val nextDueDate: Long,
    val isActive: Boolean = true
)

data class PlayerFinancialSummary(
    val player: Player,
    val totalContributionsMinorUnits: Long = 0L,
    val totalPaidOnBehalfMinorUnits: Long = 0L,
    val personalExpenseSharesMinorUnits: Long = 0L,
    val settlementsPaidMinorUnits: Long = 0L,
    val settlementsReceivedMinorUnits: Long = 0L,
    val totalChargesMinorUnits: Long = 0L,
    val netBalanceMinorUnits: Long = 0L,
    val unpaidExpenseSharesMinorUnits: Long = 0L
) {
    /** Single owed/credit line: net cash position minus unpaid shares (counted once). */
    val positionMinorUnits: Long
        get() = netBalanceMinorUnits - unpaidExpenseSharesMinorUnits

    val statusText: String
        get() = when {
            positionMinorUnits > 0 -> "The team owes this player."
            positionMinorUnits < 0 -> "Collect from this player."
            unpaidExpenseSharesMinorUnits > 0 -> "Unpaid shares included in collect total."
            else -> "Settled."
        }

    val amountTeamShouldPayMinorUnits: Long
        get() = positionMinorUnits.coerceAtLeast(0L)

    val amountTeamShouldCollectMinorUnits: Long
        get() = (-positionMinorUnits).coerceAtLeast(0L)

    fun balanceActionLabel(currencyCode: String): String? = when {
        positionMinorUnits > 0 ->
            "Pay player: ${com.teamfund.manager.core.util.MoneyFormatter.format(amountTeamShouldPayMinorUnits, currencyCode)}"
        positionMinorUnits < 0 ->
            "Collect: ${com.teamfund.manager.core.util.MoneyFormatter.format(amountTeamShouldCollectMinorUnits, currencyCode)}"
        else -> null
    }

    fun sharesBreakdownLabel(currencyCode: String): String? =
        if (unpaidExpenseSharesMinorUnits > 0L) {
            "incl. shares ${com.teamfund.manager.core.util.MoneyFormatter.format(unpaidExpenseSharesMinorUnits, currencyCode)}"
        } else null
}

data class TeamFinancialSummary(
    val teamFundBalanceMinorUnits: Long = 0L,
    val totalContributionsMinorUnits: Long = 0L,
    val totalExpensesFromFundMinorUnits: Long = 0L,
    val totalExpensesByPlayersMinorUnits: Long = 0L,
    val totalOutstandingDebtMinorUnits: Long = 0L
)

data class MatchStatsSummary(
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val noResults: Int = 0
) {
    val winRate: Float
        get() = if (matchesPlayed > 0) (wins.toFloat() / matchesPlayed.toFloat()) * 100f else 0f
}

sealed class ActivityItem {
    abstract val timestamp: Long
    abstract val id: Long

    data class ExpenseActivity(
        override val id: Long,
        val expense: Expense,
        val paidByPlayerName: String?
    ) : ActivityItem() {
        override val timestamp: Long get() = expense.date
    }

    data class ContributionActivity(
        override val id: Long,
        val contribution: Contribution,
        val playerName: String
    ) : ActivityItem() {
        override val timestamp: Long get() = contribution.date
    }

    data class MatchActivity(
        override val id: Long,
        val match: Match
    ) : ActivityItem() {
        override val timestamp: Long get() = match.date
    }

    data class SettlementActivity(
        override val id: Long,
        val settlement: Settlement,
        val fromPlayerName: String,
        val toPlayerName: String?
    ) : ActivityItem() {
        override val timestamp: Long get() = settlement.date
    }

    data class FineActivity(
        override val id: Long,
        val fine: Fine,
        val playerName: String
    ) : ActivityItem() {
        override val timestamp: Long get() = fine.date
    }
}
