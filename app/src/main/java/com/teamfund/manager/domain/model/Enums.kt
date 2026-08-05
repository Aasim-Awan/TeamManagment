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
