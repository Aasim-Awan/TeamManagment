package com.teamfund.manager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MatchSportDefaultsTest {

    @Test
    fun addScreenTitle_usesSportName() {
        assertEquals("Add Football Match", MatchSportDefaults.addScreenTitle(SportType.FOOTBALL, isEdit = false))
        assertEquals("Edit Match", MatchSportDefaults.addScreenTitle(SportType.CRICKET, isEdit = true))
    }

    @Test
    fun defaultFormat_differsBySport() {
        assertEquals("20.0 T20", MatchSportDefaults.defaultFormat(SportType.CRICKET))
        assertEquals("90 min", MatchSportDefaults.defaultFormat(SportType.FOOTBALL))
        assertEquals("", MatchSportDefaults.defaultFormat(SportType.OTHER))
    }
}
