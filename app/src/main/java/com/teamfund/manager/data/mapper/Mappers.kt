package com.teamfund.manager.data.mapper

import com.teamfund.manager.data.local.entity.ContributionEntity
import com.teamfund.manager.data.local.entity.ExpenseEntity
import com.teamfund.manager.data.local.entity.ExpenseShareEntity
import com.teamfund.manager.data.local.entity.MatchEntity
import com.teamfund.manager.data.local.entity.PlayerEntity
import com.teamfund.manager.data.local.entity.SettlementEntity
import com.teamfund.manager.data.local.entity.TeamEntity
import com.teamfund.manager.data.local.relation.ExpenseWithShares
import com.teamfund.manager.domain.model.Contribution
import com.teamfund.manager.domain.model.Expense
import com.teamfund.manager.domain.model.ExpenseShare
import com.teamfund.manager.domain.model.Match
import com.teamfund.manager.domain.model.Player
import com.teamfund.manager.domain.model.Settlement
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
    date = date,
    note = note,
    createdAt = createdAt
)
