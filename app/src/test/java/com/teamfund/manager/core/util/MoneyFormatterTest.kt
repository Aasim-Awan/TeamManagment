package com.teamfund.manager.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun toMinorUnits_parsesDecimalText() {
        assertEquals(1999L, MoneyFormatter.toMinorUnits("19.99"))
        assertEquals(1000L, MoneyFormatter.toMinorUnits("10"))
        assertEquals(1L, MoneyFormatter.toMinorUnits("0.01"))
        assertEquals(0L, MoneyFormatter.toMinorUnits(""))
        assertEquals(0L, MoneyFormatter.toMinorUnits("abc"))
    }

    @Test
    fun toMinorUnits_avoidsFloatOffByOne() {
        // Classic float bug: 19.99 * 100 → 1998.999... → 1998L
        assertEquals(1999L, MoneyFormatter.toMinorUnits(19.99))
        assertEquals(299L, MoneyFormatter.toMinorUnits(2.99))
    }

    @Test
    fun fromMinorUnits_formatsTwoDecimals() {
        assertEquals("19.99", MoneyFormatter.fromMinorUnits(1999L))
        assertEquals("10.00", MoneyFormatter.fromMinorUnits(1000L))
        assertEquals("0.01", MoneyFormatter.fromMinorUnits(1L))
    }

    @Test
    fun roundTrip_majorText() {
        val minor = MoneyFormatter.toMinorUnits("123.45")
        assertEquals("123.45", MoneyFormatter.fromMinorUnits(minor))
    }

    @Test
    fun toMinorUnits_acceptsCommaDecimal() {
        assertEquals(1250L, MoneyFormatter.toMinorUnits("12,50"))
        assertEquals(1250L, MoneyFormatter.toMinorUnits("12,5"))
        assertEquals(123450L, MoneyFormatter.toMinorUnits("1.234,50"))
    }

    @Test
    fun toMinorUnits_keepsThousandsSeparators() {
        assertEquals(100000L, MoneyFormatter.toMinorUnits("1,000"))
        assertEquals(123450L, MoneyFormatter.toMinorUnits("1,234.50"))
        assertEquals(100000000L, MoneyFormatter.toMinorUnits("1,000,000"))
    }
}
