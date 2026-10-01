package com.aura.dashboard.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class Priority { LOW, NORMAL, HIGH }

/** Repeat schedule shared by tasks and reminders. */
enum class Repeat { NEVER, DAILY, WEEKDAYS, WEEKENDS, WEEKLY, MONTHLY }

data class Task(
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: Priority = Priority.NORMAL,
    val repeat: Repeat = Repeat.NEVER,
    /** The original due date of a recurring series; keeps monthly schedules from drifting. */
    val anchorDate: LocalDate? = null,
    /** Whether to post a notification at the due date/time. */
    val remind: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    /** For generated occurrences of a recurring task: the occurrence that produced this one. */
    val previousOccurrenceId: Long? = null,
) {
    val isRecurring: Boolean get() = repeat != Repeat.NEVER && dueDate != null
    val hasNotification: Boolean get() = remind && dueDate != null && dueTime != null
}
