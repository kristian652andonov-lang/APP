package com.aura.dashboard.domain.schedule

import com.aura.dashboard.domain.model.Repeat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Pure date math for repeating tasks and reminders. */
object Recurrence {

    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    /** Upper bound for scanning forward; every rule matches at least once within a month. */
    private const val MAX_SCAN_DAYS = 400L

    /** Whether [date] is an occurrence day for [repeat], given the series [anchor] date. */
    fun matches(repeat: Repeat, date: LocalDate, anchor: LocalDate): Boolean = when (repeat) {
        Repeat.NEVER -> date == anchor
        Repeat.DAILY -> true
        Repeat.WEEKDAYS -> date.dayOfWeek !in WEEKEND
        Repeat.WEEKENDS -> date.dayOfWeek in WEEKEND
        Repeat.WEEKLY -> date.dayOfWeek == anchor.dayOfWeek
        // Day 31 maps to the last day of shorter months.
        Repeat.MONTHLY -> date.dayOfMonth == minOf(anchor.dayOfMonth, date.lengthOfMonth())
    }

    /**
     * The first occurrence strictly after [after], on or after [startDate], at [time] local time.
     * Returns null for a one-time schedule whose moment has already passed.
     */
    fun nextOccurrence(
        startDate: LocalDate,
        time: LocalTime,
        repeat: Repeat,
        after: Instant,
        zone: ZoneId,
    ): Instant? {
        if (repeat == Repeat.NEVER) {
            val at = startDate.atTime(time).atZone(zone).toInstant()
            return at.takeIf { it.isAfter(after) }
        }
        var date = maxOf(startDate, after.atZone(zone).toLocalDate())
        var scanned = 0L
        while (scanned <= MAX_SCAN_DAYS) {
            if (matches(repeat, date, startDate)) {
                val at = date.atTime(time).atZone(zone).toInstant()
                if (at.isAfter(after)) return at
            }
            date = date.plusDays(1)
            scanned++
        }
        return null
    }

    /**
     * Due date of the next occurrence of a recurring task after the occurrence due on
     * [dueDate] was completed. The result is always after both [dueDate] and [today], so
     * completing an overdue task never creates another overdue copy.
     */
    fun nextTaskDueDate(dueDate: LocalDate, anchor: LocalDate, repeat: Repeat, today: LocalDate): LocalDate? {
        if (repeat == Repeat.NEVER) return null
        var date = maxOf(dueDate, today).plusDays(1)
        var scanned = 0L
        while (scanned <= MAX_SCAN_DAYS) {
            if (matches(repeat, date, anchor)) return date
            date = date.plusDays(1)
            scanned++
        }
        return null
    }
}
