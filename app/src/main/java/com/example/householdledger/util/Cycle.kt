package com.example.householdledger.util

import java.time.LocalDate
import java.time.YearMonth

/**
 * A half-open date range representing the user's transaction cycle:
 * transactions with `start <= date < end` belong to this cycle.
 */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    fun contains(d: LocalDate): Boolean = !d.isBefore(start) && d.isBefore(endExclusive)
}

object Cycle {
    /**
     * Compute the cycle containing [reference] given a 1..31 `startDay`.
     * If the month has fewer days than `startDay`, the cycle boundary is clamped
     * to the last day of that month.
     *
     * Examples with startDay = 28:
     *   reference = Apr 19  → [Mar 28, Apr 28)
     *   reference = Apr 28  → [Apr 28, May 28)
     *   reference = Feb 10  → [Jan 28, Feb 28)   // Feb has only 28 days, so end clamps to Feb 28
     */
    fun current(reference: LocalDate, startDay: Int): DateRange {
        val day = startDay.coerceIn(1, 31)
        val thisBoundary = clampToMonth(YearMonth.from(reference), day)
        return if (!reference.isBefore(thisBoundary)) {
            DateRange(thisBoundary, clampToMonth(YearMonth.from(reference).plusMonths(1), day))
        } else {
            DateRange(clampToMonth(YearMonth.from(reference).minusMonths(1), day), thisBoundary)
        }
    }

    fun previous(reference: LocalDate, startDay: Int): DateRange {
        val cur = current(reference, startDay)
        return DateRange(
            clampToMonth(YearMonth.from(cur.start).minusMonths(1), startDay),
            cur.start
        )
    }

    /** Calendar-month range (always 1st of month → 1st of next month). */
    fun calendarMonth(reference: LocalDate): DateRange {
        val ym = YearMonth.from(reference)
        return DateRange(ym.atDay(1), ym.plusMonths(1).atDay(1))
    }

    private fun clampToMonth(ym: YearMonth, day: Int): LocalDate =
        ym.atDay(day.coerceAtMost(ym.lengthOfMonth()))
}
