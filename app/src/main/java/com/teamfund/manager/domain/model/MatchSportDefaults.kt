package com.teamfund.manager.domain.model

object MatchSportDefaults {

    fun addScreenTitle(sport: SportType, isEdit: Boolean): String {
        return if (isEdit) "Edit Match" else "Add ${sport.displayName} Match"
    }

    fun defaultFormat(sport: SportType): String = when (sport) {
        SportType.CRICKET -> "20.0 T20"
        SportType.FOOTBALL -> "90 min"
        SportType.BASKETBALL -> "4 x 12 min"
        SportType.BADMINTON -> "Best of 3"
        SportType.VOLLEYBALL -> "Best of 5"
        SportType.OTHER -> ""
    }

    fun formatFieldLabel(sport: SportType): String = when (sport) {
        SportType.CRICKET -> "Match Format / Overs"
        SportType.FOOTBALL -> "Match Format / Duration"
        SportType.BASKETBALL -> "Match Format / Quarters"
        SportType.BADMINTON, SportType.VOLLEYBALL -> "Match Format / Sets"
        SportType.OTHER -> "Match Format"
    }

    fun formatPlaceholder(sport: SportType): String = when (sport) {
        SportType.CRICKET -> "20.0 T20"
        SportType.FOOTBALL -> "90 min"
        SportType.BASKETBALL -> "4 x 12 min"
        SportType.BADMINTON -> "Best of 3"
        SportType.VOLLEYBALL -> "Best of 5"
        SportType.OTHER -> "e.g. Friendly"
    }

    fun venuePlaceholder(sport: SportType): String = when (sport) {
        SportType.CRICKET -> "e.g. Shalimar Cricket Ground"
        SportType.FOOTBALL -> "e.g. City Stadium"
        SportType.BASKETBALL -> "e.g. Indoor Court"
        SportType.BADMINTON -> "e.g. Sports Complex Court 2"
        SportType.VOLLEYBALL -> "e.g. Beach / Indoor Court"
        SportType.OTHER -> "e.g. Local Ground"
    }
}
