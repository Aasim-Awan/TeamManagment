package com.teamfund.manager.core.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.teamfund.manager.core.common.AppContainer
import com.teamfund.manager.feature.contributions.AddContributionScreen
import com.teamfund.manager.feature.contributions.ContributionsScreen
import com.teamfund.manager.feature.contributions.ContributionsViewModel
import com.teamfund.manager.feature.dashboard.DashboardScreen
import com.teamfund.manager.feature.dashboard.DashboardViewModel
import com.teamfund.manager.feature.expenses.AddExpenseScreen
import com.teamfund.manager.feature.expenses.ExpenseDetailScreen
import com.teamfund.manager.feature.expenses.ExpensesScreen
import com.teamfund.manager.feature.expenses.ExpensesViewModel
import com.teamfund.manager.feature.matches.AddMatchScreen
import com.teamfund.manager.feature.matches.MatchDetailScreen
import com.teamfund.manager.feature.matches.MatchesScreen
import com.teamfund.manager.feature.matches.MatchesViewModel
import com.teamfund.manager.feature.reports.ReportsScreen
import com.teamfund.manager.feature.reports.ReportsViewModel
import com.teamfund.manager.feature.settings.SettingsScreen
import com.teamfund.manager.feature.settings.SettingsViewModel
import com.teamfund.manager.feature.settlements.SettlementsScreen
import com.teamfund.manager.feature.settlements.SettlementsViewModel
import com.teamfund.manager.feature.team.AddPlayerScreen
import com.teamfund.manager.feature.team.CreateTeamScreen
import com.teamfund.manager.feature.team.PlayerDetailScreen
import com.teamfund.manager.feature.team.TeamRosterScreen
import com.teamfund.manager.feature.team.TeamViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    appContainer: AppContainer
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        // 1. Dashboard
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(
                    appContainer.repository,
                    appContainer.userPreferencesRepository,
                    appContainer.calculatePlayerBalancesUseCase,
                    appContainer.calculateTeamBalanceUseCase
                )
            )
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAddMatch = { navController.navigate(Screen.AddMatch.createRoute()) },
                onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.createRoute()) },
                onNavigateToAddContribution = { navController.navigate(Screen.AddContribution.createRoute()) },
                onNavigateToSettlements = { navController.navigate(Screen.Settlements.route) },
                onNavigateToMatches = { navController.navigate(Screen.Matches.route) },
                onNavigateToTeam = { navController.navigate(Screen.TeamRoster.route) },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onNavigateToContributions = { navController.navigate(Screen.Contributions.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToMatchDetail = { matchId -> navController.navigate(Screen.MatchDetail.createRoute(matchId)) }
            )
        }

        // 2. Matches Screen
        composable(Screen.Matches.route) {
            val viewModel: MatchesViewModel = viewModel(
                factory = MatchesViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository)
            )
            MatchesScreen(
                viewModel = viewModel,
                onNavigateToAddMatch = { navController.navigate(Screen.AddMatch.createRoute()) },
                onNavigateToMatchDetail = { matchId -> navController.navigate(Screen.MatchDetail.createRoute(matchId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 3. Add / Edit Match Screen
        composable(
            route = Screen.AddMatch.route,
            arguments = listOf(navArgument("matchId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val matchIdArg = backStackEntry.arguments?.getLong("matchId")
            val matchId = if (matchIdArg != null && matchIdArg > 0L) matchIdArg else null
            val viewModel: MatchesViewModel = viewModel(
                factory = MatchesViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository)
            )
            AddMatchScreen(
                viewModel = viewModel,
                matchId = matchId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 4. Match Detail Screen
        composable(
            route = Screen.MatchDetail.route,
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: 0L
            val viewModel: MatchesViewModel = viewModel(
                factory = MatchesViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository)
            )
            MatchDetailScreen(
                matchId = matchId,
                viewModel = viewModel,
                onNavigateToAddExpense = { mId -> navController.navigate(Screen.AddExpense.createRoute(mId)) },
                onNavigateToEditMatch = { mId -> navController.navigate(Screen.AddMatch.createRoute(mId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 5. Team Roster Screen
        composable(Screen.TeamRoster.route) {
            val viewModel: TeamViewModel = viewModel(
                factory = TeamViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository, appContainer.calculatePlayerBalancesUseCase)
            )
            TeamRosterScreen(
                viewModel = viewModel,
                onNavigateToAddPlayer = { navController.navigate(Screen.AddPlayer.createRoute()) },
                onNavigateToPlayerDetail = { playerId -> navController.navigate(Screen.PlayerDetail.createRoute(playerId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 6. Add / Edit Player Screen
        composable(
            route = Screen.AddPlayer.route,
            arguments = listOf(navArgument("playerId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val playerIdArg = backStackEntry.arguments?.getLong("playerId")
            val playerId = if (playerIdArg != null && playerIdArg > 0L) playerIdArg else null
            val viewModel: TeamViewModel = viewModel(
                factory = TeamViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository, appContainer.calculatePlayerBalancesUseCase)
            )
            AddPlayerScreen(
                viewModel = viewModel,
                playerId = playerId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 7. Player Detail Screen
        composable(
            route = Screen.PlayerDetail.route,
            arguments = listOf(navArgument("playerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playerId = backStackEntry.arguments?.getLong("playerId") ?: 0L
            val viewModel: TeamViewModel = viewModel(
                factory = TeamViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository, appContainer.calculatePlayerBalancesUseCase)
            )
            PlayerDetailScreen(
                playerId = playerId,
                viewModel = viewModel,
                onNavigateToAddContribution = { pId -> navController.navigate(Screen.AddContribution.createRoute(pId)) },
                onNavigateToEditPlayer = { pId -> navController.navigate(Screen.AddPlayer.createRoute(pId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 8. Expenses Log Screen
        composable(Screen.Expenses.route) {
            val viewModel: ExpensesViewModel = viewModel(
                factory = ExpensesViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository, appContainer.calculateExpenseSharesUseCase)
            )
            ExpensesScreen(
                viewModel = viewModel,
                onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.createRoute()) },
                onNavigateToExpenseDetail = { expenseId -> navController.navigate(Screen.ExpenseDetail.createRoute(expenseId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 9. Add Expense Screen
        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("matchId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("expenseId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val matchIdArg = backStackEntry.arguments?.getLong("matchId")
            val matchId = if (matchIdArg != null && matchIdArg > 0L) matchIdArg else null
            
            val expenseIdArg = backStackEntry.arguments?.getLong("expenseId")
            val expenseId = if (expenseIdArg != null && expenseIdArg > 0L) expenseIdArg else null

            val viewModel: ExpensesViewModel = viewModel(
                factory = ExpensesViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository, appContainer.calculateExpenseSharesUseCase)
            )
            AddExpenseScreen(
                viewModel = viewModel,
                matchId = matchId,
                expenseId = expenseId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 9.1 Expense Detail Screen
        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
            val viewModel: ExpensesViewModel = viewModel(
                factory = ExpensesViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository, appContainer.calculateExpenseSharesUseCase)
            )
            ExpenseDetailScreen(
                expenseId = expenseId,
                viewModel = viewModel,
                onNavigateToEditExpense = { id -> navController.navigate(Screen.AddExpense.createRoute(expenseId = id)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 10. Contributions Screen
        composable(Screen.Contributions.route) {
            val viewModel: ContributionsViewModel = viewModel(
                factory = ContributionsViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository)
            )
            ContributionsScreen(
                viewModel = viewModel,
                onNavigateToAddContribution = { navController.navigate(Screen.AddContribution.createRoute()) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 11. Add Contribution Screen
        composable(
            route = Screen.AddContribution.route,
            arguments = listOf(navArgument("playerId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val playerIdArg = backStackEntry.arguments?.getLong("playerId")
            val playerId = if (playerIdArg != null && playerIdArg > 0L) playerIdArg else null
            val viewModel: ContributionsViewModel = viewModel(
                factory = ContributionsViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository)
            )
            AddContributionScreen(
                viewModel = viewModel,
                preselectedPlayerId = playerId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 12. Settlements Screen
        composable(Screen.Settlements.route) {
            val viewModel: SettlementsViewModel = viewModel(
                factory = SettlementsViewModel.Factory(appContainer.repository, appContainer.userPreferencesRepository, appContainer.calculatePlayerBalancesUseCase)
            )
            SettlementsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 13. Reports Screen
        composable(Screen.Reports.route) {
            val viewModel: ReportsViewModel = viewModel(
                factory = ReportsViewModel.Factory(
                    appContainer.repository,
                    appContainer.userPreferencesRepository,
                    appContainer.calculatePlayerBalancesUseCase,
                    appContainer.calculateTeamBalanceUseCase,
                    appContainer.calculateExpenseSharesUseCase
                )
            )
            ReportsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 14. Settings Screen
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(
                    appContainer.repository,
                    appContainer.userPreferencesRepository,
                    appContainer.calculateExpenseSharesUseCase
                )
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToCreateTeam = { navController.navigate(Screen.CreateTeam.route) },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 15. Create Team Screen
        composable(Screen.CreateTeam.route) {
            CreateTeamScreen(
                repository = appContainer.repository,
                userPreferencesRepository = appContainer.userPreferencesRepository,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
