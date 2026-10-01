package com.aura.dashboard.domain

import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.domain.schedule.Recurrence
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecurrenceTest {

    private val zone = ZoneId.of("Europe/Sofia")
    private fun at(dateTime: String) = LocalDateTime.parse(dateTime).atZone(zone).toInstant()

    @Test
    fun `one-time reminder in the future fires at its time`() {
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-10-01"), LocalTime.of(18, 30), Repeat.NEVER, at("2026-10-01T09:00"), zone)
        assertEquals(at("2026-10-01T18:30"), next)
    }

    @Test
    fun `one-time reminder in the past has no next occurrence`() {
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-10-01"), LocalTime.of(8, 0), Repeat.NEVER, at("2026-10-01T09:00"), zone)
        assertNull(next)
    }

    @Test
    fun `daily reminder moves to tomorrow once today's time has passed`() {
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-09-01"), LocalTime.of(8, 0), Repeat.DAILY, at("2026-10-01T09:00"), zone)
        assertEquals(at("2026-10-02T08:00"), next)
    }

    @Test
    fun `daily reminder fires later today when the time is still ahead`() {
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-09-01"), LocalTime.of(14, 30), Repeat.DAILY, at("2026-10-01T09:00"), zone)
        assertEquals(at("2026-10-01T14:30"), next)
    }

    @Test
    fun `repeating reminder never fires before its start date`() {
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-10-10"), LocalTime.of(7, 0), Repeat.DAILY, at("2026-10-01T09:00"), zone)
        assertEquals(at("2026-10-10T07:00"), next)
    }

    @Test
    fun `weekdays skip the weekend`() {
        // 2026-10-02 is a Friday.
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-09-01"), LocalTime.of(7, 0), Repeat.WEEKDAYS, at("2026-10-02T08:00"), zone)
        assertEquals(at("2026-10-05T07:00"), next)
    }

    @Test
    fun `weekends only fire on Saturday and Sunday`() {
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-09-01"), LocalTime.of(10, 0), Repeat.WEEKENDS, at("2026-10-01T12:00"), zone)
        assertEquals(at("2026-10-03T10:00"), next)
    }

    @Test
    fun `weekly keeps the weekday of the start date`() {
        // Started on Thursday 2026-09-03.
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-09-03"), LocalTime.of(9, 0), Repeat.WEEKLY, at("2026-10-01T10:00"), zone)
        assertEquals(at("2026-10-08T09:00"), next)
    }

    @Test
    fun `monthly on the 31st falls back to the last day of shorter months`() {
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-01-31"), LocalTime.of(9, 0), Repeat.MONTHLY, at("2026-02-01T00:00"), zone)
        assertEquals(at("2026-02-28T09:00"), next)
        val after = Recurrence.nextOccurrence(LocalDate.parse("2026-01-31"), LocalTime.of(9, 0), Repeat.MONTHLY, at("2026-03-01T00:00"), zone)
        assertEquals(at("2026-03-31T09:00"), after)
    }

    @Test
    fun `daylight saving gap still produces a valid future time`() {
        // Clocks jump from 03:00 to 04:00 in Sofia on 2026-03-29.
        val next = Recurrence.nextOccurrence(LocalDate.parse("2026-03-01"), LocalTime.of(3, 30), Repeat.DAILY, at("2026-03-28T23:00"), zone)
        assertEquals(LocalDate.parse("2026-03-29"), next!!.atZone(zone).toLocalDate())
    }

    @Test
    fun `completing an overdue daily task schedules tomorrow, not another overdue day`() {
        val today = LocalDate.parse("2026-10-01")
        val next = Recurrence.nextTaskDueDate(LocalDate.parse("2026-09-27"), LocalDate.parse("2026-09-01"), Repeat.DAILY, today)
        assertEquals(LocalDate.parse("2026-10-02"), next)
    }

    @Test
    fun `completing a weekly task early moves it one week after its due date`() {
        val today = LocalDate.parse("2026-10-01")
        val next = Recurrence.nextTaskDueDate(LocalDate.parse("2026-10-03"), LocalDate.parse("2026-09-26"), Repeat.WEEKLY, today)
        assertEquals(LocalDate.parse("2026-10-10"), next)
    }

    @Test
    fun `monthly task keeps its anchor day instead of drifting`() {
        val next = Recurrence.nextTaskDueDate(LocalDate.parse("2026-02-28"), LocalDate.parse("2026-01-31"), Repeat.MONTHLY, LocalDate.parse("2026-02-28"))
        assertEquals(LocalDate.parse("2026-03-31"), next)
    }

    @Test
    fun `non repeating task has no next due date`() {
        assertNull(Recurrence.nextTaskDueDate(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01"), Repeat.NEVER, LocalDate.parse("2026-10-01")))
    }
}
