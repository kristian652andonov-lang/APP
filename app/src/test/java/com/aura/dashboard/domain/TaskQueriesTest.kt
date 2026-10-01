package com.aura.dashboard.domain

import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.domain.model.TaskSort
import com.aura.dashboard.domain.schedule.TaskFilter
import com.aura.dashboard.domain.schedule.TaskQueries
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskQueriesTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.parse("2026-10-01")
    private val todayInstant = Instant.parse("2026-10-01T10:00:00Z")
    private val yesterdayInstant = Instant.parse("2026-09-30T10:00:00Z")

    private fun task(
        id: Long,
        due: LocalDate? = null,
        time: LocalTime? = null,
        done: Boolean = false,
        completedAt: Instant? = null,
        priority: Priority = Priority.NORMAL,
        created: Long = id,
    ) = Task(
        id = id,
        title = "Task $id",
        dueDate = due,
        dueTime = time,
        isCompleted = done,
        completedAt = completedAt,
        priority = priority,
        createdAt = Instant.ofEpochSecond(created),
    )

    @Test
    fun `today includes due today, open overdue and undated tasks`() {
        assertTrue(TaskQueries.isForToday(task(1, due = today), today, zone))
        assertTrue(TaskQueries.isForToday(task(2, due = today.minusDays(2)), today, zone))
        assertTrue(TaskQueries.isForToday(task(3), today, zone))
        assertFalse(TaskQueries.isForToday(task(4, due = today.plusDays(1)), today, zone))
    }

    @Test
    fun `today keeps tasks completed today but drops older completions`() {
        assertTrue(TaskQueries.isForToday(task(1, due = today.minusDays(1), done = true, completedAt = todayInstant), today, zone))
        assertFalse(TaskQueries.isForToday(task(2, due = today.minusDays(1), done = true, completedAt = yesterdayInstant), today, zone))
        assertFalse(TaskQueries.isForToday(task(3, done = true, completedAt = yesterdayInstant), today, zone))
    }

    @Test
    fun `overdue considers the due time on the current day`() {
        val now = LocalTime.of(12, 0)
        assertTrue(TaskQueries.isOverdue(task(1, due = today, time = LocalTime.of(9, 0)), today, now))
        assertFalse(TaskQueries.isOverdue(task(2, due = today, time = LocalTime.of(18, 0)), today, now))
        assertFalse(TaskQueries.isOverdue(task(3, due = today), today, now))
        assertTrue(TaskQueries.isOverdue(task(4, due = today.minusDays(1)), today, now))
    }

    @Test
    fun `upcoming only contains open future tasks`() {
        val tasks = listOf(task(1, due = today), task(2, due = today.plusDays(3)), task(3, due = today.plusDays(1), done = true))
        val result = TaskQueries.filter(tasks, TaskFilter.UPCOMING, today, zone, showCompleted = true)
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `hiding completed removes them from all`() {
        val tasks = listOf(task(1), task(2, done = true, completedAt = todayInstant))
        assertEquals(listOf(1L), TaskQueries.filter(tasks, TaskFilter.ALL, today, zone, showCompleted = false).map { it.id })
        assertEquals(listOf(2L), TaskQueries.filter(tasks, TaskFilter.COMPLETED, today, zone, showCompleted = false).map { it.id })
    }

    @Test
    fun `time sort puts dated tasks first, ordered by date and time, completed last`() {
        val tasks = listOf(
            task(1),
            task(2, due = today, time = LocalTime.of(18, 0)),
            task(3, due = today, time = LocalTime.of(9, 0)),
            task(4, due = today.minusDays(1), done = true, completedAt = todayInstant),
            task(5, due = today.plusDays(1)),
        )
        assertEquals(listOf(3L, 2L, 5L, 1L, 4L), TaskQueries.sort(tasks, TaskSort.TIME).map { it.id })
    }

    @Test
    fun `priority sort puts high priority first`() {
        val tasks = listOf(
            task(1, priority = Priority.LOW),
            task(2, priority = Priority.HIGH),
            task(3, priority = Priority.NORMAL),
        )
        assertEquals(listOf(2L, 3L, 1L), TaskQueries.sort(tasks, TaskSort.PRIORITY).map { it.id })
    }

    @Test
    fun `creation sort shows newest first`() {
        val tasks = listOf(task(1, created = 10), task(2, created = 30), task(3, created = 20))
        assertEquals(listOf(2L, 3L, 1L), TaskQueries.sort(tasks, TaskSort.CREATED).map { it.id })
    }
}
