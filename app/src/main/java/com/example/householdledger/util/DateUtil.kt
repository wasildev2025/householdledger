package com.example.householdledger.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Robust date parsing for Supabase-sourced strings.
 *
 * Supabase / Postgres returns timestamps in several forms:
 *   - `2026-04-17T14:49:00.000Z`       (Instant / UTC, with millis)
 *   - `2026-04-17T14:49:00+00:00`      (OffsetDateTime)
 *   - `2026-04-17T14:49:00`            (LocalDateTime, no zone)
 *   - `2026-04-17`                     (LocalDate)
 *
 * `LocalDateTime.parse()` alone rejects the first two because of the zone suffix,
 * which is why every "this month" filter silently produced 0 rows.
 */
object DateUtil {

    fun parseDateTime(raw: String): LocalDateTime? {
        if (raw.isBlank()) return null
        // 1) Full ISO with zone (most common from Supabase)
        runCatching { Instant.parse(raw) }.getOrNull()?.let {
            return LocalDateTime.ofInstant(it, ZoneId.systemDefault())
        }
        // 2) ISO with offset
        runCatching { OffsetDateTime.parse(raw) }.getOrNull()?.let {
            return it.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        }
        // 3) ISO without zone
        runCatching { LocalDateTime.parse(raw) }.getOrNull()?.let { return it }
        // 4) Date only
        runCatching { LocalDate.parse(raw) }.getOrNull()?.let { return it.atStartOfDay() }
        return null
    }

    fun parseDate(raw: String): LocalDate? = parseDateTime(raw)?.toLocalDate()

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun formatLocalTime(raw: String): String =
        parseDateTime(raw)?.format(timeFormatter) ?: raw
}
