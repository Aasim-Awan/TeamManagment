package com.teamfund.manager.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormatter {

    fun format(minorUnits: Long, currencyCode: String = "USD", includeSign: Boolean = false): String {
        val majorAmount = BigDecimal.valueOf(minorUnits).movePointLeft(2)
        val isNegative = minorUnits < 0
        val absAmount = majorAmount.abs().toDouble()

        val formatted = try {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = Currency.getInstance(currencyCode.uppercase())
            format.format(absAmount)
        } catch (e: Exception) {
            val symbol = getCurrencySymbol(currencyCode)
            String.format(Locale.getDefault(), "%s%.2f", symbol, absAmount)
        }

        return when {
            isNegative -> "-$formatted"
            includeSign && minorUnits > 0 -> "+$formatted"
            else -> formatted
        }
    }

    /** Parse major-unit text (e.g. "19.99") to minor units. Invalid → 0. */
    fun toMinorUnits(majorText: String): Long {
        val cleaned = majorText.trim().replace(",", "")
        if (cleaned.isBlank()) return 0L
        return try {
            BigDecimal(cleaned)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (_: Exception) {
            0L
        }
    }

    fun toMinorUnits(major: Double): Long {
        if (!major.isFinite()) return 0L
        return BigDecimal.valueOf(major)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    /** Plain major string for form fields. */
    fun fromMinorUnits(minorUnits: Long): String {
        return BigDecimal.valueOf(minorUnits)
            .movePointLeft(2)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }

    fun getCurrencySymbol(currencyCode: String): String {
        return try {
            Currency.getInstance(currencyCode.uppercase()).symbol
        } catch (e: Exception) {
            when (currencyCode.uppercase()) {
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                "PKR" -> "₨"
                "INR" -> "₹"
                "AUD" -> "A$"
                "CAD" -> "C$"
                else -> "$"
            }
        }
    }
}
