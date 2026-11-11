package com.teamfund.manager.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.teamfund.manager.data.local.database.TeamFundDatabase
import com.teamfund.manager.data.local.entity.ContributionEntity
import com.teamfund.manager.data.local.entity.ExpenseEntity
import com.teamfund.manager.data.local.entity.MatchEntity
import com.teamfund.manager.data.local.entity.MatchPlayerEntity
import com.teamfund.manager.data.local.entity.MatchPlayerStatsEntity
import com.teamfund.manager.data.local.entity.PlayerEntity
import com.teamfund.manager.data.local.entity.SettlementEntity
import com.teamfund.manager.data.local.entity.TeamEntity
import com.teamfund.manager.data.repository.TeamFundRepositoryImpl
import com.teamfund.manager.domain.model.ContributionPurpose
import com.teamfund.manager.domain.model.CricketRole
import com.teamfund.manager.domain.model.ExpenseCategory
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.FineReason
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.MatchPlayer
import com.teamfund.manager.domain.model.MatchPlayerStatus
import com.teamfund.manager.domain.model.MatchResult
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.PlayerRole
import com.teamfund.manager.domain.model.PlayerSafeDeleteOutcome
import com.teamfund.manager.domain.model.SettlementAdjustmentKind
import com.teamfund.manager.domain.model.SportType
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TeamFundDaoTest {

    private lateinit var db: TeamFundDatabase
    private lateinit var repository: TeamFundRepositoryImpl
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TeamFundDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TeamFundRepositoryImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedTeam(): Long {
        return db.teamDao().insertTeam(
            TeamEntity(
                name = "Test FC",
                sport = SportType.CRICKET,
                logoUri = null,
                currencyCode = "USD",
                createdAt = now,
                updatedAt = now,
                isActive = true
            )
        )
    }

    private suspend fun seedPlayer(teamId: Long, name: String = "Player"): Long {
        return db.playerDao().insertPlayer(
            PlayerEntity(
                teamId = teamId,
                name = name,
                role = PlayerRole.PLAYER,
                cricketRole = CricketRole.UNSPECIFIED,
                phoneNumber = null,
                avatarUri = null,
                isActive = true,
                joinedAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    @Test
    fun upsertExpense_keepsSettlementExpenseId() = runBlocking {
        val teamId = seedTeam()
        val playerId = seedPlayer(teamId)
        val expenseId = db.expenseDao().insertExpense(
            ExpenseEntity(
                teamId = teamId,
                matchId = null,
                title = "Original",
                category = ExpenseCategory.GROUND,
                amountMinorUnits = 5000L,
                currencyCode = "USD",
                paidByPlayerId = null,
                date = now,
                notes = null,
                createdAt = now,
                updatedAt = now
            )
        )
        val settlementId = db.settlementDao().insertSettlement(
            SettlementEntity(
                teamId = teamId,
                fromPlayerId = playerId,
                toPlayerId = null,
                matchId = null,
                expenseId = expenseId,
                amountMinorUnits = 1000L,
                adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM,
                date = now,
                note = "Share payment",
                createdAt = now
            )
        )
        db.expenseDao().insertExpense(
            ExpenseEntity(
                id = expenseId,
                teamId = teamId,
                matchId = null,
                title = "Updated title",
                category = ExpenseCategory.GROUND,
                amountMinorUnits = 5000L,
                currencyCode = "USD",
                paidByPlayerId = null,
                date = now,
                notes = "edited",
                createdAt = now,
                updatedAt = now + 1
            )
        )
        val settlement = db.settlementDao().getSettlementById(settlementId)
        assertEquals(expenseId, settlement?.expenseId)
    }

    @Test
    fun upsertMatch_preservesPlayerStats() = runBlocking {
        val teamId = seedTeam()
        val playerId = seedPlayer(teamId)
        val matchId = db.matchDao().insertMatch(
            MatchEntity(
                teamId = teamId,
                opponentName = "Opponents",
                date = now,
                venue = null,
                result = MatchResult.WON,
                teamScore = "120/5",
                opponentScore = "119/8",
                overs = "20",
                tossWinner = null,
                playerOfTheMatchId = null,
                seasonId = null,
                matchFeeMinorUnits = 0L,
                groundFeeMinorUnits = 0L,
                notes = null,
                createdAt = now,
                updatedAt = now
            )
        )
        db.matchPlayerStatsDao().upsertAll(
            listOf(
                MatchPlayerStatsEntity(
                    matchId = matchId,
                    playerId = playerId,
                    runs = 42,
                    balls = 30,
                    wickets = 0,
                    oversBowled = null,
                    catches = 1,
                    isPlayerOfMatch = false
                )
            )
        )
        db.matchDao().insertMatch(
            MatchEntity(
                id = matchId,
                teamId = teamId,
                opponentName = "Opponents (edited)",
                date = now,
                venue = "Home",
                result = MatchResult.WON,
                teamScore = "120/5",
                opponentScore = "119/8",
                overs = "20",
                tossWinner = null,
                playerOfTheMatchId = null,
                seasonId = null,
                matchFeeMinorUnits = 0L,
                groundFeeMinorUnits = 0L,
                notes = null,
                createdAt = now,
                updatedAt = now + 1
            )
        )
        val list = db.matchPlayerStatsDao().getStatsForMatchFlow(matchId).first()
        assertEquals(1, list.size)
        assertEquals(42, list.first().runs)
    }

    @Test
    fun deletePlayerSafely_withContribution_deactivates() = runBlocking {
        val teamId = seedTeam()
        val playerId = seedPlayer(teamId)
        db.contributionDao().insertContribution(
            ContributionEntity(
                teamId = teamId,
                playerId = playerId,
                amountMinorUnits = 1000L,
                date = now,
                purpose = ContributionPurpose.GENERAL,
                notes = null,
                createdAt = now,
                updatedAt = now
            )
        )
        val outcome = repository.deletePlayerSafely(playerId)
        assertEquals(PlayerSafeDeleteOutcome.DEACTIVATED_HAS_HISTORY, outcome)
        val player = db.playerDao().getPlayerById(playerId)
        assertEquals(false, player?.isActive)
    }

    @Test
    fun deletePlayerSafely_noHistory_removesPlayerAndXi() = runBlocking {
        val teamId = seedTeam()
        val playerId = seedPlayer(teamId)
        val matchId = db.matchDao().insertMatch(
            MatchEntity(
                teamId = teamId,
                opponentName = "X",
                date = now,
                venue = null,
                result = MatchResult.UPCOMING,
                teamScore = null,
                opponentScore = null,
                overs = null,
                tossWinner = null,
                playerOfTheMatchId = null,
                seasonId = null,
                matchFeeMinorUnits = 0L,
                groundFeeMinorUnits = 0L,
                notes = null,
                createdAt = now,
                updatedAt = now
            )
        )
        db.matchPlayerDao().insertAll(
            listOf(
                MatchPlayerEntity(
                    matchId = matchId,
                    playerId = playerId,
                    status = MatchPlayerStatus.SELECTED,
                    createdAt = now
                )
            )
        )
        val outcome = repository.deletePlayerSafely(playerId)
        assertEquals(PlayerSafeDeleteOutcome.HARD_DELETED, outcome)
        assertNull(db.playerDao().getPlayerById(playerId))
        assertEquals(0, db.matchPlayerDao().getPlayersForMatch(matchId).size)
    }

    @Test
    fun balanceUseCase_includesInactivePlayerWithBalance() = runBlocking {
        val teamId = seedTeam()
        val activeId = seedPlayer(teamId, "Active")
        val inactiveId = db.playerDao().insertPlayer(
            PlayerEntity(
                teamId = teamId,
                name = "Inactive",
                role = PlayerRole.PLAYER,
                cricketRole = CricketRole.UNSPECIFIED,
                phoneNumber = null,
                avatarUri = null,
                isActive = false,
                joinedAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
        db.contributionDao().insertContribution(
            ContributionEntity(
                teamId = teamId,
                playerId = inactiveId,
                amountMinorUnits = 2000L,
                date = now,
                purpose = ContributionPurpose.GENERAL,
                notes = null,
                createdAt = now,
                updatedAt = now
            )
        )
        val players = listOf(
            Player(id = activeId, teamId = teamId, name = "Active"),
            Player(id = inactiveId, teamId = teamId, name = "Inactive", isActive = false)
        )
        val summaries = CalculatePlayerBalancesUseCase().execute(
            players = players,
            contributions = listOf(
                com.teamfund.manager.domain.model.Contribution(
                    teamId = teamId,
                    playerId = inactiveId,
                    amountMinorUnits = 2000L
                )
            ),
            expenses = emptyList(),
            expenseShares = emptyList(),
            settlements = emptyList()
        )
        assertEquals(2, summaries.size)
        assertEquals(2000L, summaries.find { it.player.id == inactiveId }?.netBalanceMinorUnits)
    }

    @Test
    fun saveMatchWithRoster_onEdit_keepsIdAndReplacesXi() = runBlocking {
        val teamId = seedTeam()
        val first = seedPlayer(teamId, "First")
        val second = seedPlayer(teamId, "Second")
        val match = Match(teamId = teamId, opponentName = "Rivals", date = now)
        val matchId = repository.saveMatchWithRoster(
            match,
            listOf(MatchPlayer(matchId = 0L, playerId = first, status = MatchPlayerStatus.SELECTED))
        )

        // Editing must return the same id (Room @Upsert alone returns -1 on update).
        val editedId = repository.saveMatchWithRoster(
            match.copy(id = matchId, opponentName = "Rivals (edited)"),
            listOf(MatchPlayer(matchId = matchId, playerId = second, status = MatchPlayerStatus.SELECTED))
        )

        assertEquals(matchId, editedId)
        assertEquals("Rivals (edited)", repository.getMatchById(matchId)?.opponentName)
        assertEquals(listOf(second), repository.getMatchPlayers(matchId).map { it.playerId })
    }

    @Test
    fun chargeMatchFeeToXi_twice_chargesOnce() = runBlocking {
        val teamId = seedTeam()
        val playerId = seedPlayer(teamId)
        val matchId = repository.saveMatch(Match(teamId = teamId, opponentName = "Rivals", date = now))

        repository.chargeMatchFeeToXi(teamId, matchId, 500L, listOf(playerId))
        repository.chargeMatchFeeToXi(teamId, matchId, 500L, listOf(playerId))

        assertEquals(1, repository.getFinesForMatch(matchId).size)
    }

    @Test
    fun deletePaidFine_removesItsSettlement() = runBlocking {
        val teamId = seedTeam()
        val playerId = seedPlayer(teamId)
        val fineId = repository.addFineWithSettlement(
            Fine(teamId = teamId, playerId = playerId, amountMinorUnits = 300L, reason = FineReason.LATE),
            recordPayment = true
        )
        assertEquals(1, repository.getSettlementsFlow(teamId).first().size)

        repository.deleteFine(fineId)

        assertEquals(0, repository.getSettlementsFlow(teamId).first().size)
        assertEquals(0, repository.getFinesForTeamFlow(teamId).first().size)
    }
}
