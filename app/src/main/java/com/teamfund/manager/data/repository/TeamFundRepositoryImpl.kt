package com.teamfund.manager.data.repository

import com.teamfund.manager.data.local.database.TeamFundDatabase
import com.teamfund.manager.data.mapper.toDomain
import com.teamfund.manager.data.mapper.toEntity
import com.teamfund.manager.domain.model.*
import com.teamfund.manager.domain.repository.TeamFundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

/** Title prefix of the ground-fee expense created from a match; used to find it again on delete. */
private const val GROUND_FEE_TITLE_PREFIX = "Ground fee — "

class TeamFundRepositoryImpl(
    private val database: TeamFundDatabase
) : TeamFundRepository {

    private val teamDao = database.teamDao()
    private val playerDao = database.playerDao()
    private val matchDao = database.matchDao()
    private val expenseDao = database.expenseDao()
    private val contributionDao = database.contributionDao()
    private val settlementDao = database.settlementDao()
    private val matchPlayerDao = database.matchPlayerDao()
    private val seasonDao = database.seasonDao()
    private val matchPlayerStatsDao = database.matchPlayerStatsDao()
    private val fineDao = database.fineDao()
    private val inventoryDao = database.inventoryDao()
    private val dueScheduleDao = database.dueScheduleDao()

    override fun getActiveTeamFlow(activeTeamId: Long?): Flow<Team?> {
        return if (activeTeamId != null) {
            teamDao.getTeamByIdFlow(activeTeamId).map { it?.toDomain() }
        } else {
            teamDao.getActiveTeamFlow().map { it?.toDomain() }
        }
    }

    override suspend fun getTeamById(teamId: Long): Team? {
        return teamDao.getTeamById(teamId)?.toDomain()
    }

    override fun getAllTeamsFlow(): Flow<List<Team>> {
        return teamDao.getAllTeamsFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createTeam(team: Team): Long {
        val id = teamDao.insertTeam(team.toEntity())
        //  returns -1 when it updates an existing row.
        return if (team.id != 0L) team.id else id
    }

    override suspend fun updateTeam(team: Team) {
        teamDao.updateTeam(team.toEntity())
    }

    override suspend fun setActiveTeam(teamId: Long) {
        teamDao.setActiveTeamFlags(teamId)
    }

    override fun getActivePlayersFlow(teamId: Long): Flow<List<Player>> {
        return playerDao.getActivePlayersForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllPlayersFlow(teamId: Long): Flow<List<Player>> {
        return playerDao.getAllPlayersForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPlayerById(playerId: Long): Player? {
        return playerDao.getPlayerById(playerId)?.toDomain()
    }

    override suspend fun addPlayer(player: Player): Long {
        val id = playerDao.insertPlayer(player.toEntity())
        return if (player.id != 0L) player.id else id
    }

    override suspend fun updatePlayer(player: Player) {
        playerDao.updatePlayer(player.toEntity())
    }

    override suspend fun deactivatePlayer(playerId: Long) {
        playerDao.deactivatePlayer(playerId)
    }

    override suspend fun reactivatePlayer(playerId: Long) {
        playerDao.reactivatePlayer(playerId)
    }

    override suspend fun findPlayerByName(teamId: Long, name: String): Player? {
        return playerDao.findPlayerByName(teamId, name)?.toDomain()
    }

    override suspend fun deletePlayer(playerId: Long) {
        playerDao.deletePlayer(playerId)
    }

    override suspend fun deletePlayerSafely(playerId: Long): PlayerSafeDeleteOutcome = database.withTransaction {
        val hasHistory =
            playerDao.countExpenseSharesForPlayer(playerId) > 0 ||
                playerDao.countContributionsForPlayer(playerId) > 0 ||
                playerDao.countSettlementsForPlayer(playerId) > 0 ||
                playerDao.countFinesForPlayer(playerId) > 0
        if (hasHistory) {
            playerDao.deactivatePlayer(playerId)
            return@withTransaction PlayerSafeDeleteOutcome.DEACTIVATED_HAS_HISTORY
        }
        playerDao.deleteMatchPlayersForPlayer(playerId)
        playerDao.deleteMatchStatsForPlayer(playerId)
        playerDao.deletePlayer(playerId)
        PlayerSafeDeleteOutcome.HARD_DELETED
    }

    override fun getMatchesFlow(teamId: Long): Flow<List<Match>> {
        return matchDao.getMatchesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getMatchesFlow(teamId: Long, seasonId: Long?): Flow<List<Match>> {
        return matchDao.getMatchesForTeamAndSeasonFlow(teamId, seasonId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMatchById(matchId: Long): Match? {
        return matchDao.getMatchById(matchId)?.toDomain()
    }

    override fun getMatchByIdFlow(matchId: Long): Flow<Match?> {
        return matchDao.getMatchByIdFlow(matchId).map { it?.toDomain() }
    }

    override suspend fun saveMatch(match: Match): Long {
        val id = matchDao.insertMatch(match.toEntity())
        //  returns -1 when it updates an existing row, so keep the known id on edit.
        return if (match.id != 0L) match.id else id
    }

    override suspend fun saveMatchWithRoster(match: Match, players: List<MatchPlayer>): Long =
        database.withTransaction {
            val matchId = saveMatch(match)
            matchPlayerDao.replaceForMatch(matchId, players.map { it.copy(matchId = matchId).toEntity() })
            matchId
        }

    override suspend fun deleteMatch(matchId: Long) {
        database.withTransaction {
            // Remove charges the app created automatically for this match.
            matchDao.deleteFinesForMatch(matchId, FineReason.MATCH_FEE)
            matchDao.deleteAutoExpensesForMatch(matchId, ExpenseCategory.GROUND, GROUND_FEE_TITLE_PREFIX)
            // Keep records entered by hand, but clear their link to the deleted match.
            matchDao.unlinkExpensesFromMatch(matchId)
            matchDao.unlinkFinesFromMatch(matchId)
            matchDao.unlinkSettlementsFromMatch(matchId)
            matchDao.unlinkInventoryMovementsFromMatch(matchId)
            // Roster and player stats are removed by their ON DELETE CASCADE.
            matchDao.deleteMatchById(matchId)
        }
    }

    override fun getExpensesFlow(teamId: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesWithSharesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getExpensesForMatchFlow(matchId: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesForMatchFlow(matchId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExpenseById(expenseId: Long): Expense? {
        return expenseDao.getExpenseWithSharesById(expenseId)?.toDomain()
    }

    override suspend fun saveExpenseWithShares(expense: Expense, shares: List<ExpenseShare>): Long {
        val expenseEntity = expense.toEntity()
        val sharesEntities = shares.map { it.toEntity() }
        
        return if (expense.id == 0L) {
            expenseDao.insertExpenseWithShares(expenseEntity, sharesEntities)
        } else {
            // Reconcile payments: Preserve existing paid amounts when updating an expense
            val existingExpense = expenseDao.getExpenseWithSharesById(expense.id)
            val existingShares = existingExpense?.shares ?: emptyList()
            
            val reconciledShares = sharesEntities.map { newShare ->
                val matchingOldShare = existingShares.find { it.playerId == newShare.playerId }
                if (matchingOldShare != null) {
                    var paidAmount = matchingOldShare.paidAmountMinorUnits
                    
                    // IF the user was previously FULLY settled, and the expense was updated,
                    // we assume their "settled" status is stickier than the exact amount.
                    // We update their paidAmount to match the new share if they were already marked settled.
                    if (matchingOldShare.isSettled) {
                        paidAmount = newShare.shareAmountMinorUnits
                    }

                    val isNowSettled = paidAmount >= newShare.shareAmountMinorUnits
                    newShare.copy(
                        paidAmountMinorUnits = paidAmount,
                        isSettled = isNowSettled,
                        settledAt = if (isNowSettled) (matchingOldShare.settledAt ?: System.currentTimeMillis()) else null
                    )
                } else {
                    newShare
                }
            }
            
            expenseDao.updateExpenseWithShares(expenseEntity, reconciledShares)
            expense.id
        }
    }

    override suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteExpenseById(expenseId)
    }

    override fun getAllExpenseSharesFlow(teamId: Long): Flow<List<ExpenseShare>> {
        return expenseDao.getAllSharesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getContributionsFlow(teamId: Long): Flow<List<Contribution>> {
        return contributionDao.getContributionsForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getContributionsForPlayerFlow(playerId: Long): Flow<List<Contribution>> {
        return contributionDao.getContributionsForPlayerFlow(playerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addContribution(contribution: Contribution): Long {
        return contributionDao.insertContribution(contribution.toEntity())
    }

    override suspend fun deleteContribution(contributionId: Long) {
        contributionDao.deleteContributionById(contributionId)
    }

    override fun getSettlementsFlow(teamId: Long): Flow<List<Settlement>> {
        return settlementDao.getSettlementsForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getSettlementsForPlayerFlow(playerId: Long): Flow<List<Settlement>> {
        return settlementDao.getSettlementsForPlayerFlow(playerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addSettlement(settlement: Settlement): Long {
        return settlementDao.insertSettlement(settlement.toEntity())
    }

    override suspend fun recordPlayerPayment(settlement: Settlement, shareId: Long?, expenseId: Long?): Long =
        database.withTransaction {
            val settlementId = settlementDao.insertSettlement(settlement.toEntity())
            val now = System.currentTimeMillis()
            when {
                shareId != null -> expenseDao.incrementSharePaymentById(shareId, settlement.amountMinorUnits, now)
                expenseId != null -> expenseDao.incrementSharePayment(expenseId, settlement.fromPlayerId, settlement.amountMinorUnits, now)
            }
            settlementId
        }

    override suspend fun deleteSettlement(settlementId: Long) {
        database.withTransaction {
            val settlement = settlementDao.getSettlementById(settlementId) ?: return@withTransaction
            settlementDao.deleteSettlementById(settlementId)
            val expenseId = settlement.expenseId ?: return@withTransaction
            val playerId = when {
                settlement.fromPlayerId != 0L -> settlement.fromPlayerId
                settlement.toPlayerId != null -> settlement.toPlayerId
                else -> return@withTransaction
            }

            // Decrement the paid amount instead of full reset to handle partial settlements correctly
            expenseDao.decrementSharePayment(expenseId, playerId, settlement.amountMinorUnits)
        }
    }

    override suspend fun markExpenseShareSettled(shareId: Long) {
        expenseDao.markShareSettled(shareId, System.currentTimeMillis())
    }

    override suspend fun markPlayerExpenseShareSettled(expenseId: Long, playerId: Long) {
        expenseDao.markPlayerShareSettledForExpense(
            expenseId = expenseId,
            playerId = playerId,
            settledAt = System.currentTimeMillis()
        )
    }

    override suspend fun recordExpenseSharePayment(expenseId: Long, playerId: Long, amountMinorUnits: Long) {
        expenseDao.incrementSharePayment(
            expenseId = expenseId,
            playerId = playerId,
            amount = amountMinorUnits,
            settledAt = System.currentTimeMillis()
        )
    }

    override suspend fun recordExpenseSharePaymentById(shareId: Long, amountMinorUnits: Long) {
        expenseDao.incrementSharePaymentById(
            shareId = shareId,
            amount = amountMinorUnits,
            settledAt = System.currentTimeMillis()
        )
    }

    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }

    override fun getMatchPlayersFlow(matchId: Long): Flow<List<MatchPlayer>> {
        return matchPlayerDao.getPlayersForMatchFlow(matchId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMatchPlayers(matchId: Long): List<MatchPlayer> {
        return matchPlayerDao.getPlayersForMatch(matchId).map { it.toDomain() }
    }

    override suspend fun replaceMatchPlayers(matchId: Long, players: List<MatchPlayer>) {
        matchPlayerDao.replaceForMatch(matchId, players.map { it.toEntity() })
    }

    override suspend fun chargeMatchFeeToXi(
        teamId: Long,
        matchId: Long,
        matchFeeMinorUnits: Long,
        selectedPlayerIds: List<Long>
    ) {
        if (matchFeeMinorUnits <= 0L || selectedPlayerIds.isEmpty()) return
        database.withTransaction {
            selectedPlayerIds.forEach { playerId ->
                // Never charge the same player twice for one match (e.g. when the match is edited).
                if (fineDao.countFinesForMatchPlayer(matchId, playerId, FineReason.MATCH_FEE) > 0) return@forEach
                fineDao.insertFine(
                    Fine(
                        teamId = teamId,
                        playerId = playerId,
                        matchId = matchId,
                        amountMinorUnits = matchFeeMinorUnits,
                        reason = FineReason.MATCH_FEE,
                        note = "Match fee (match #$matchId)"
                    ).toEntity()
                )
            }
        }
    }

    override fun getSeasonsFlow(teamId: Long): Flow<List<Season>> {
        return seasonDao.getSeasonsForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveSeasonFlow(teamId: Long): Flow<Season?> {
        return seasonDao.getActiveSeasonFlow(teamId).map { it?.toDomain() }
    }

    override suspend fun saveSeason(season: Season): Long {
        val id = seasonDao.insertSeason(season.toEntity())
        return if (season.id != 0L) season.id else id
    }

    override suspend fun setActiveSeason(teamId: Long, seasonId: Long) {
        database.withTransaction {
            seasonDao.clearActiveForTeam(teamId)
            seasonDao.setActiveSeason(seasonId)
        }
    }

    override fun getMatchPlayerStatsFlow(matchId: Long): Flow<List<MatchPlayerStats>> {
        return matchPlayerStatsDao.getStatsForMatchFlow(matchId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveMatchPlayerStats(stats: List<MatchPlayerStats>) {
        if (stats.isNotEmpty()) {
            matchPlayerStatsDao.upsertAll(stats.map { it.toEntity() })
        }
    }

    override fun getFinesForTeamFlow(teamId: Long): Flow<List<Fine>> {
        return fineDao.getFinesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override fun getFinesForPlayerFlow(playerId: Long): Flow<List<Fine>> {
        return fineDao.getFinesForPlayerFlow(playerId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getFinesForMatch(matchId: Long): List<Fine> {
        return fineDao.getFinesForMatch(matchId).map { it.toDomain() }
    }

    override suspend fun addFineWithSettlement(fine: Fine, recordPayment: Boolean): Long = database.withTransaction {
        var settlementId: Long? = null
        if (recordPayment) {
            val settlement = Settlement(
                teamId = fine.teamId,
                fromPlayerId = fine.playerId,
                toPlayerId = null,
                matchId = fine.matchId,
                amountMinorUnits = fine.amountMinorUnits,
                adjustmentKind = SettlementAdjustmentKind.PLAYER_TO_TEAM,
                note = "Fine: ${fine.reason.displayName}",
                date = fine.date
            )
            settlementId = settlementDao.insertSettlement(settlement.toEntity())
        }
        fineDao.insertFine(fine.copy(settlementId = settlementId).toEntity())
    }

    override suspend fun addFine(fine: Fine): Long {
        return fineDao.insertFine(fine.toEntity())
    }

    override suspend fun deleteFine(fineId: Long) {
        database.withTransaction {
            val fine = fineDao.getFineById(fineId) ?: return@withTransaction
            fineDao.deleteFine(fineId)
            // A fine recorded as paid created a settlement; remove it so balances stay correct.
            fine.settlementId?.let { settlementDao.deleteSettlementById(it) }
        }
    }

    override fun getInventoryItemsFlow(teamId: Long): Flow<List<InventoryItem>> {
        return inventoryDao.getItemsForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveInventoryItem(item: InventoryItem): Long {
        val id = inventoryDao.insertItem(item.toEntity())
        return if (item.id != 0L) item.id else id
    }

    override suspend fun adjustInventoryItem(itemId: Long, delta: Int, movement: InventoryMovement) {
        database.withTransaction {
            inventoryDao.adjustQuantity(itemId, delta)
            inventoryDao.insertMovement(movement.toEntity())
        }
    }

    override fun getDueSchedulesFlow(teamId: Long): Flow<List<DueSchedule>> {
        return dueScheduleDao.getSchedulesForTeamFlow(teamId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveDueSchedule(schedule: DueSchedule): Long {
        val id = dueScheduleDao.insertSchedule(schedule.toEntity())
        return if (schedule.id != 0L) schedule.id else id
    }

    override suspend fun generateDueContributions(teamId: Long, scheduleId: Long): Int = database.withTransaction {
        val schedule = dueScheduleDao.getScheduleById(scheduleId) ?: return@withTransaction 0
        if (!schedule.isActive) return@withTransaction 0
        // Charge each period once: nothing to generate until the due date is reached.
        if (schedule.nextDueDate > System.currentTimeMillis()) return@withTransaction 0
        val players = playerDao.getActivePlayersForTeam(teamId)
        if (players.isEmpty()) return@withTransaction 0
        players.forEach { player ->
            fineDao.insertFine(
                Fine(
                    teamId = teamId,
                    playerId = player.id,
                    amountMinorUnits = schedule.amountMinorUnits,
                    reason = FineReason.DUES,
                    date = schedule.nextDueDate,
                    note = schedule.name
                ).toEntity()
            )
        }
        val nextMonth = nextDueDate(schedule.nextDueDate, schedule.period)
        dueScheduleDao.updateSchedule(schedule.copy(nextDueDate = nextMonth))
        players.size
    }

    /** Next due date one calendar period later (e.g. 31 Jan → 28/29 Feb for monthly). */
    private fun nextDueDate(current: Long, period: DuePeriod): Long {
        val zone = java.time.ZoneId.systemDefault()
        val date = java.time.Instant.ofEpochMilli(current).atZone(zone)
        val next = when (period) {
            DuePeriod.MONTHLY -> date.plusMonths(1)
        }
        return next.toInstant().toEpochMilli()
    }

    override suspend fun createGroundFeeExpenseForMatch(teamId: Long, match: Match, currencyCode: String) {
        if (match.groundFeeMinorUnits <= 0L || match.id <= 0L) return
        // Only one ground-fee expense per match, so re-saving a match does not charge again.
        if (expenseDao.countExpensesForMatch(match.id, ExpenseCategory.GROUND) > 0) return
        val players = playerDao.getActivePlayersForTeam(teamId)
        if (players.isEmpty()) return
        val perPlayer = match.groundFeeMinorUnits / players.size
        val remainder = match.groundFeeMinorUnits % players.size
        val expenseEntity = Expense(
            teamId = teamId,
            matchId = match.id,
            title = "$GROUND_FEE_TITLE_PREFIX${match.opponentName}",
            category = ExpenseCategory.GROUND,
            amountMinorUnits = match.groundFeeMinorUnits,
            currencyCode = currencyCode,
            date = match.date,
            notes = match.venue
        ).toEntity()
        val shares = players.mapIndexed { index, player ->
            val shareAmount = perPlayer + if (index == 0) remainder else 0L
            ExpenseShare(
                playerId = player.id,
                shareAmountMinorUnits = shareAmount
            ).toEntity()
        }
        expenseDao.insertExpenseWithShares(expenseEntity, shares)
    }
}
