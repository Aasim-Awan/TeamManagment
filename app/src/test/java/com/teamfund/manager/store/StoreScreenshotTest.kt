package com.teamfund.manager.store

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.teamfund.manager.MainActivity
import com.teamfund.manager.TeamFundApplication
import com.teamfund.manager.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Real application screens, isolated Robolectric storage and fictional records only. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun captureStoreScreens() {
        val container = (compose.activity.application as TeamFundApplication).appContainer
        val repository = container.repository
        // Dates are relative to today so Home's "Spent this month" shows real figures.
        val today = LocalDate.now()
        val date = maxOf(today.withDayOfMonth(1), today.minusDays(1)).atTime(16, 30)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runBlocking {
            val teamId = repository.createTeam(Team(name = "Northside Cricket", currencyCode = "USD"))
            container.userPreferencesRepository.setActiveTeamId(teamId)
            container.userPreferencesRepository.setThemeMode("LIGHT")
            val players = listOf("Alex Morgan", "Sam Patel", "Jordan Lee", "Taylor Brooks", "Casey Ellis", "Riley Khan")
                .mapIndexed { index, name ->
                    repository.addPlayer(Player(teamId = teamId, name = name,
                        role = if (index == 0) PlayerRole.CAPTAIN else PlayerRole.PLAYER))
                }
            players.forEach { playerId ->
                repository.addContribution(Contribution(teamId = teamId, playerId = playerId,
                    amountMinorUnits = 10000, date = date, purpose = ContributionPurpose.GENERAL))
            }
            repository.saveExpenseWithShares(Expense(teamId = teamId, title = "Weekend ground booking",
                category = ExpenseCategory.GROUND, amountMinorUnits = 12000, date = date), emptyList())
            repository.saveExpenseWithShares(Expense(teamId = teamId, title = "Training equipment",
                category = ExpenseCategory.EQUIPMENT, amountMinorUnits = 6000, paidByPlayerId = players[0], date = date),
                players.map { ExpenseShare(playerId = it, shareAmountMinorUnits = 1000) })
            val seasonId = repository.saveSeason(Season(teamId = teamId, name = "Summer League ${today.year}", startDate = date))
            repository.setActiveSeason(teamId, seasonId)
            repository.saveDueSchedule(DueSchedule(teamId = teamId, name = "Monthly subscription", amountMinorUnits = 2500,
                nextDueDate = today.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            repository.saveMatch(Match(teamId = teamId, seasonId = seasonId, opponentName = "Riverside XI", venue = "Northside Sports Ground", date = date + 172800000))
            repository.saveMatch(Match(teamId = teamId, seasonId = seasonId, opponentName = "City Strikers", venue = "Central Park", date = date + 604800000))
            repository.saveMatch(Match(teamId = teamId, seasonId = seasonId, opponentName = "Valley United", date = date - 604800000,
                result = MatchResult.WON, teamScore = "168/5", opponentScore = "142/8", overs = "20"))
        }
        awaitText("Available team fund")
        capture("01-home")
        compose.onNodeWithTag("nav_Players").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("6 active", substring = true)
        capture("02-players")
        compose.onNodeWithTag("nav_Matches").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Riverside XI", substring = true)
        capture("03-matches")
        compose.onNodeWithTag("nav_Reports").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("TEAM FUND AUDIT SUMMARY")
        capture("04-reports")
        compose.onNodeWithTag("nav_Expenses").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Total spent", substring = true)
        capture("07-all-expenses")
        compose.onNodeWithTag("nav_Home").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Available team fund")
        compose.onNodeWithTag("quick_add_expense").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Log Shared Expense")
        compose.onNodeWithTag("expense_title_input").performTextInput("Weekend training")
        compose.onNodeWithTag("expense_amount_input").performTextInput("90.00")
        compose.activityRule.scenario.onActivity { activity ->
            val keyboard = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            keyboard.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
            activity.window.decorView.clearFocus()
        }
        compose.waitForIdle()
        capture("05-add-expense")
        compose.onNodeWithTag("expense_date_picker").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("OK").assertExists()
        compose.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("top_bar_back_button").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Available team fund")
        compose.onNodeWithTag("quick_settle").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Dues")
        capture("08-settlements")
        compose.onNodeWithTag("top_bar_back_button").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Available team fund")
        compose.onNodeWithTag("settings_button").performSemanticsAction(SemanticsActions.OnClick)
        awaitText("Settings", substring = true)
        capture("06-settings")
    }

    private fun awaitText(text: String, substring: Boolean = false) {
        compose.waitUntil(20000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitUntil(20000) {
            compose.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }
        // Let press ripples on the clicked tab fade out so they do not appear in store images.
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
        val directory = File("../store-assets/raw")
        directory.mkdirs()
        compose.onRoot().captureRoboImage(File(directory, "$name.png").absolutePath)
    }
}
