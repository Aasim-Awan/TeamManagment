package com.teamfund.manager.data.mapper

import com.teamfund.manager.data.local.entity.ContributionEntity
import com.teamfund.manager.data.local.entity.DueScheduleEntity
import com.teamfund.manager.data.local.entity.ExpenseEntity
import com.teamfund.manager.data.local.entity.ExpenseShareEntity
import com.teamfund.manager.data.local.entity.FineEntity
import com.teamfund.manager.data.local.entity.InventoryItemEntity
import com.teamfund.manager.data.local.entity.InventoryMovementEntity
import com.teamfund.manager.data.local.entity.MatchEntity
import com.teamfund.manager.data.local.entity.MatchPlayerEntity
import com.teamfund.manager.data.local.entity.MatchPlayerStatsEntity
import com.teamfund.manager.data.local.entity.PlayerEntity
import com.teamfund.manager.data.local.entity.SeasonEntity
import com.teamfund.manager.data.local.entity.SettlementEntity
import com.teamfund.manager.data.local.entity.TeamEntity
import com.teamfund.manager.data.local.relation.ExpenseWithShares
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.DueSchedule
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Fine
import com.teamfund.manager.domain.model.InventoryItem
import com.teamfund.manager.domain.model.InventoryMovement
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.MatchPlayer
import com.teamfund.manager.domain.model.MatchPlayerStats
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.Season
import com.teamfund.manager.domain.model.Settlement
import com.teamfund.manager.domain.model.SettlementAdjustmentKind
import com.teamfund.manager.domain.model.Team

fun TeamEntity.toDomain() = Team(
    id = id,
    name = name,
    sport = sport,
    logoUri = logoUri,
    currencyCode = currencyCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun Team.toEntity() = TeamEntity(
    id = id,
    name = name,
    sport = sport,
    logoUri = logoUri,
    currencyCode = currencyCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun PlayerEntity.toDomain() = Player(
    id = id,
    teamId = teamId,
    name = name,
    role = role,
    cricketRole = cricketRole,
    phoneNumber = phoneNumber,
    avatarUri = avatarUri,
    isActive = isActive,
    joinedAt = joinedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Player.toEntity() = PlayerEntity(
    id = id,
    teamId = teamId,
    name = name,
    role = role,
    cricketRole = cricketRole,
    phoneNumber = phoneNumber,
    avatarUri = avatarUri,
    isActive = isActive,
    joinedAt = joinedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MatchEntity.toDomain() = Match(
    id = id,
    teamId = teamId,
    opponentName = opponentName,
    date = date,
    venue = venue,
    result = result,
    teamScore = teamScore,
    opponentScore = opponentScore,
    overs = overs,
    tossWinner = tossWinner,
    playerOfTheMatchId = playerOfTheMatchId,
    seasonId = seasonId,
    matchFeeMinorUnits = matchFeeMinorUnits,
    groundFeeMinorUnits = groundFeeMinorUnits,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Match.toEntity() = MatchEntity(
    id = id,
    teamId = teamId,
    opponentName = opponentName,
    date = date,
    venue = venue,
    result = result,
    teamScore = teamScore,
    opponentScore = opponentScore,
    overs = overs,
    tossWinner = tossWinner,
    playerOfTheMatchId = playerOfTheMatchId,
    seasonId = seasonId,
    matchFeeMinorUnits = matchFeeMinorUnits,
    groundFeeMinorUnits = groundFeeMinorUnits,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ExpenseShareEntity.toDomain() = ExpenseShare(
    id = id,
    expenseId = expenseId,
    playerId = playerId,
    shareAmountMinorUnits = shareAmountMinorUnits,
    paidAmountMinorUnits = paidAmountMinorUnits,
    isSettled = isSettled,
    settledAt = settledAt
)

fun ExpenseShare.toEntity() = ExpenseShareEntity(
    id = id,
    expenseId = expenseId,
    playerId = playerId,
    shareAmountMinorUnits = shareAmountMinorUnits,
    paidAmountMinorUnits = paidAmountMinorUnits,
    isSettled = isSettled,
    settledAt = settledAt
)

fun ExpenseWithShares.toDomain() = Expense(
    id = expense.id,
    teamId = expense.teamId,
    matchId = expense.matchId,
    title = expense.title,
    category = expense.category,
    amountMinorUnits = expense.amountMinorUnits,
    currencyCode = expense.currencyCode,
    paidByPlayerId = expense.paidByPlayerId,
    date = expense.date,
    notes = expense.notes,
    createdAt = expense.createdAt,
    updatedAt = expense.updatedAt,
    shares = shares.map { it.toDomain() }
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    teamId = teamId,
    matchId = matchId,
    title = title,
    category = category,
    amountMinorUnits = amountMinorUnits,
    currencyCode = currencyCode,
    paidByPlayerId = paidByPlayerId,
    date = date,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ContributionEntity.toDomain() = Contribution(
    id = id,
    teamId = teamId,
    playerId = playerId,
    amountMinorUnits = amountMinorUnits,
    date = date,
    purpose = purpose,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Contribution.toEntity() = ContributionEntity(
    id = id,
    teamId = teamId,
    playerId = playerId,
    amountMinorUnits = amountMinorUnits,
    date = date,
    purpose = purpose,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SettlementEntity.toDomain() = Settlement(
    id = id,
    teamId = teamId,
    fromPlayerId = fromPlayerId,
    toPlayerId = toPlayerId,
    matchId = matchId,
    expenseId = expenseId,
    amountMinorUnits = amountMinorUnits,
    adjustmentKind = adjustmentKind,
    date = date,
    note = note,
    createdAt = createdAt
)

fun Settlement.toEntity() = SettlementEntity(
    id = id,
    teamId = teamId,
    fromPlayerId = fromPlayerId,
    toPlayerId = toPlayerId,
    matchId = matchId,
    expenseId = expenseId,
    amountMinorUnits = amountMinorUnits,
    adjustmentKind = adjustmentKind,
    date = date,
    note = note,
    createdAt = createdAt
)

fun settlementKindFor(fromPlayerId: Long): SettlementAdjustmentKind =
    if (fromPlayerId == 0L) SettlementAdjustmentKind.TEAM_TO_PLAYER
    else SettlementAdjustmentKind.PLAYER_TO_TEAM

fun MatchPlayerEntity.toDomain() = MatchPlayer(
    id = id,
    matchId = matchId,
    playerId = playerId,
    status = status,
    createdAt = createdAt
)

fun MatchPlayer.toEntity() = MatchPlayerEntity(
    id = id,
    matchId = matchId,
    playerId = playerId,
    status = status,
    createdAt = createdAt
)

fun SeasonEntity.toDomain() = Season(
    id = id,
    teamId = teamId,
    name = name,
    startDate = startDate,
    endDate = endDate,
    isActive = isActive
)

fun Season.toEntity() = SeasonEntity(
    id = id,
    teamId = teamId,
    name = name,
    startDate = startDate,
    endDate = endDate,
    isActive = isActive
)

fun MatchPlayerStatsEntity.toDomain() = MatchPlayerStats(
    id = id,
    matchId = matchId,
    playerId = playerId,
    runs = runs,
    balls = balls,
    wickets = wickets,
    oversBowled = oversBowled,
    catches = catches,
    isPlayerOfMatch = isPlayerOfMatch
)

fun MatchPlayerStats.toEntity() = MatchPlayerStatsEntity(
    id = id,
    matchId = matchId,
    playerId = playerId,
    runs = runs,
    balls = balls,
    wickets = wickets,
    oversBowled = oversBowled,
    catches = catches,
    isPlayerOfMatch = isPlayerOfMatch
)

fun FineEntity.toDomain() = Fine(
    id = id,
    teamId = teamId,
    playerId = playerId,
    matchId = matchId,
    amountMinorUnits = amountMinorUnits,
    reason = reason,
    date = date,
    note = note,
    settlementId = settlementId,
    createdAt = createdAt
)

fun Fine.toEntity() = FineEntity(
    id = id,
    teamId = teamId,
    playerId = playerId,
    matchId = matchId,
    amountMinorUnits = amountMinorUnits,
    reason = reason,
    date = date,
    note = note,
    settlementId = settlementId,
    createdAt = createdAt
)

fun InventoryItemEntity.toDomain() = InventoryItem(
    id = id,
    teamId = teamId,
    name = name,
    category = category,
    quantity = quantity,
    notes = notes
)

fun InventoryItem.toEntity() = InventoryItemEntity(
    id = id,
    teamId = teamId,
    name = name,
    category = category,
    quantity = quantity,
    notes = notes
)

fun InventoryMovementEntity.toDomain() = InventoryMovement(
    id = id,
    itemId = itemId,
    delta = delta,
    matchId = matchId,
    playerId = playerId,
    date = date,
    note = note
)

fun InventoryMovement.toEntity() = InventoryMovementEntity(
    id = id,
    itemId = itemId,
    delta = delta,
    matchId = matchId,
    playerId = playerId,
    date = date,
    note = note
)

fun DueScheduleEntity.toDomain() = DueSchedule(
    id = id,
    teamId = teamId,
    name = name,
    amountMinorUnits = amountMinorUnits,
    period = period,
    nextDueDate = nextDueDate,
    isActive = isActive
)

fun DueSchedule.toEntity() = DueScheduleEntity(
    id = id,
    teamId = teamId,
    name = name,
    amountMinorUnits = amountMinorUnits,
    period = period,
    nextDueDate = nextDueDate,
    isActive = isActive
)
