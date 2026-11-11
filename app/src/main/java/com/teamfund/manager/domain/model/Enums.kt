package com.teamfund.manager.domain.model

enum class SportType(val displayName: String) {
    CRICKET("Cricket"),
    FOOTBALL("Football"),
    BASKETBALL("Basketball"),
    BADMINTON("Badminton"),
    VOLLEYBALL("Volleyball"),
    OTHER("Other")
}

enum class PlayerRole(val displayName: String) {
    CAPTAIN("Captain"),
    VICE_CAPTAIN("Vice Captain"),
    PLAYER("Player"),
    MANAGER("Manager")
}

enum class MatchResult(val displayName: String) {
    WON("Won"),
    LOST("Lost"),
    DRAW("Draw"),
    NO_RESULT("No Result"),
    UPCOMING("Upcoming")
}

enum class ExpenseCategory(val displayName: String) {
    GROUND("Ground Fee"),
    BALL("Balls & Kits"),
    UMPIRE("Umpire Fee"),
    TRANSPORT("Transport"),
    FOOD("Food & Snacks"),
    WATER("Water & Drinks"),
    EQUIPMENT("Equipment"),
    OTHER("Other")
}

enum class ContributionPurpose(val displayName: String) {
    MONTHLY("Monthly Subscription"),
    MATCH("Match Fee"),
    GENERAL("General Pool"),
    OTHER("Other")
}

enum class SettlementAdjustmentKind(val displayName: String) {
    PLAYER_TO_TEAM("Player pays team"),
    TEAM_TO_PLAYER("Team pays player")
}

enum class MatchPlayerStatus(val displayName: String) {
    SELECTED("Playing XI"),
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    NO_SHOW("No show")
}

enum class CricketRole(val displayName: String) {
    BATTER("Batter"),
    BOWLER("Bowler"),
    WICKET_KEEPER("Wicket keeper"),
    ALL_ROUNDER("All-rounder"),
    UNSPECIFIED("Unspecified")
}

enum class FineReason(val displayName: String) {
    LATE("Late"),
    NO_SHOW("No show"),
    OTHER("Other"),
    MATCH_FEE("Match fee"),
    DUES("Monthly dues"),
    CHARGE("Charge")
}

enum class PlayerSafeDeleteOutcome {
    HARD_DELETED,
    DEACTIVATED_HAS_HISTORY
}

enum class SettleAdjustmentMode {
    PLAYER_PAID,
    PLAYER_OWES,
    TEAM_PAID_PLAYER
}

enum class InventoryCategory(val displayName: String) {
    BALL("Ball"),
    KIT("Kit"),
    OTHER("Other")
}

enum class DuePeriod(val displayName: String) {
    MONTHLY("Monthly")
}
