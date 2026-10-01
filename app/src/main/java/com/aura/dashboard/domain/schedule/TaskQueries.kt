package com.aura.dashboard.domain.schedule

import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.domain.model.TaskSort
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class TaskFilter { ALL, TODAY, UPCOMING, COMPLETED }

/** Filtering and sorting rules shared by the dashboard and the Tasks screen. */
object TaskQueries {

    /**
     * "Today" contains tasks due today, overdue tasks that are still open, and undated tasks.
     * Tasks completed today stay visible so progress ("4 of 7 completed") is meaningful.
     */
    fun isForToday(task: Task, today: LocalDate, zone: ZoneId): Boolean {
        val completedToday = task.completedAt?.atZone(zone)?.toLocalDate() == today
        val due = task.dueDate
        return when {
            due == null -> !task.isCompleted || completedToday
            due == today -> true
            due.isBefore(today) -> !task.isCompleted || completedToday
            else -> false
        }
    }

    fun isUpcoming(task: Task, today: LocalDate): Boolean =
        !task.isCompleted && task.dueDate != null && task.dueDate.isAfter(today)

    fun isOverdue(task: Task, today: LocalDate, now: LocalTime): Boolean {
        if (task.isCompleted) return false
        val due = task.dueDate ?: return false
        return due.isBefore(today) || (due == today && task.dueTime != null && task.dueTime.isBefore(now))
    }

    fun filter(
        tasks: List<Task>,
        filter: TaskFilter,
        today: LocalDate,
        zone: ZoneId,
        showCompleted: Boolean,
    ): List<Task> = when (filter) {
        TaskFilter.ALL -> tasks.filter { showCompleted || !it.isCompleted }
        TaskFilter.TODAY -> tasks.filter { isForToday(it, today, zone) && (showCompleted || !it.isCompleted) }
        TaskFilter.UPCOMING -> tasks.filter { isUpcoming(it, today) }
        TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
    }

    fun sort(tasks: List<Task>, sort: TaskSort, filter: TaskFilter = TaskFilter.ALL): List<Task> {
        if (filter == TaskFilter.COMPLETED) {
            return tasks.sortedByDescending { it.completedAt }
        }
        val byDue = compareBy<Task, LocalDate?>(nullsLast()) { it.dueDate }
            .thenBy(nullsLast()) { it.dueTime }
        val comparator: Comparator<Task> = when (sort) {
            TaskSort.TIME -> byDue.thenByDescending { it.priority }.thenByDescending { it.createdAt }
            TaskSort.PRIORITY -> compareByDescending<Task> { it.priority }.then(byDue)
            TaskSort.CREATED -> compareByDescending { it.createdAt }
        }
        // Open tasks always come before completed ones.
        return tasks.sortedWith(compareBy<Task> { it.isCompleted }.then(comparator))
    }
}
