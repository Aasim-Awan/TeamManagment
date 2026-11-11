package com.teamfund.manager.core.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Matches : Screen("matches")
    object AddMatch : Screen("add_match?matchId={matchId}") {
        fun createRoute(matchId: Long? = null) = if (matchId != null) "add_match?matchId=$matchId" else "add_match"
    }
    object MatchDetail : Screen("match_detail/{matchId}") {
        fun createRoute(matchId: Long) = "match_detail/$matchId"
    }
    object TeamRoster : Screen("team_roster")
    object AddPlayer : Screen("add_player?playerId={playerId}") {
        fun createRoute(playerId: Long? = null) = if (playerId != null) "add_player?playerId=$playerId" else "add_player"
    }
    object PlayerDetail : Screen("player_detail/{playerId}") {
        fun createRoute(playerId: Long) = "player_detail/$playerId"
    }
    object Expenses : Screen("expenses")
    object AllExpenses : Screen("all_expenses")
    object AddExpense : Screen("add_expense?matchId={matchId}&expenseId={expenseId}") {
        fun createRoute(matchId: Long? = null, expenseId: Long? = null) = buildString {
            append("add_expense")
            val params = mutableListOf<String>()
            if (matchId != null) params.add("matchId=$matchId")
            if (expenseId != null) params.add("expenseId=$expenseId")
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }
    object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: Long) = "expense_detail/$expenseId"
    }
    object Contributions : Screen("contributions")
    object AddContribution : Screen("add_contribution?playerId={playerId}") {
        fun createRoute(playerId: Long? = null) = if (playerId != null) "add_contribution?playerId=$playerId" else "add_contribution"
    }
    object Settlements : Screen("settlements")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object Kit : Screen("kit")
    object CreateTeam : Screen("create_team")
}
