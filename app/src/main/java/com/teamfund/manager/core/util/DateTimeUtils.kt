package com.teamfund.manager.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateTimeUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    fun formatDate(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> date.format(dateFormatter)
        }
    }

    fun formatAbsoluteDate(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }

    fun formatDateTime(timestamp: Long): String {
        val zdt = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
        val date = zdt.toLocalDate()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val timeStr = zdt.format(timeFormatter)

        return when (date) {
            today -> "Today, $timeStr"
            yesterday -> "Yesterday, $timeStr"
            else -> zdt.format(dateTimeFormatter)
        }
    }

    fun formatMonthYear(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(monthYearFormatter)
    }

    fun parseDate(dateStr: String): Long {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /** Material DatePicker uses UTC midnight; convert to local start-of-day millis. */
    fun utcPickerMillisToLocal(utcMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Convert local timestamp to UTC midnight for Material DatePicker. */
    fun localMillisToUtcPicker(localMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
