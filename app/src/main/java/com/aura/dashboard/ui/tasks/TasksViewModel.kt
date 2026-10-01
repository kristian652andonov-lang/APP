package com.aura.dashboard.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.TaskRepository
import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.domain.model.TaskSort
import com.aura.dashboard.domain.schedule.TaskFilter
import com.aura.dashboard.domain.schedule.TaskQueries
import com.aura.dashboard.ui.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

sealed interface TaskSectionKey {
    data object Overdue : TaskSectionKey
    data object Today : TaskSectionKey
    data object Tomorrow : TaskSectionKey
    data class Day(val date: LocalDate) : TaskSectionKey
    data object NoDate : TaskSectionKey
    data object Completed : TaskSectionKey
}

/** [key] is null for an un-headed (flat) list. */
data class TaskSection(val key: TaskSectionKey?, val tasks: List<Task>)

data class TasksUiState(
    val isLoading: Boolean = true,
    val filter: TaskFilter = TaskFilter.ALL,
    val sort: TaskSort = TaskSort.TIME,
    val showCompleted: Boolean = true,
    val sections: List<TaskSection> = emptyList(),
    val openCount: Int = 0,
    val completedTodayCount: Int = 0,
    val hasCompleted: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val now: LocalTime = LocalTime.now(),
) {
    val isEmpty: Boolean get() = sections.all { it.tasks.isEmpty() }
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val preferences: UserPreferencesRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val filter = MutableStateFlow(TaskFilter.ALL)

    val uiState: StateFlow<TasksUiState> = combine(
        tasks.observeTasks(),
        preferences.preferences,
        filter,
        minuteTicker(clock),
    ) { all, prefs, filter, now ->
        val zone = clock.zone()
        val zoned = now.atZone(zone)
        val today = zoned.toLocalDate()
        val visible = TaskQueries.filter(all, filter, today, zone, prefs.showCompletedTasks)
        val sorted = TaskQueries.sort(visible, prefs.taskSort, filter)
        TasksUiState(
            isLoading = false,
            filter = filter,
            sort = prefs.taskSort,
            showCompleted = prefs.showCompletedTasks,
            sections = sectionsFor(sorted, filter, prefs.taskSort, today),
            openCount = all.count { !it.isCompleted },
            completedTodayCount = all.count { it.completedAt?.atZone(zone)?.toLocalDate() == today },
            hasCompleted = all.any { it.isCompleted },
            today = today,
            now = zoned.toLocalTime(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setFilter(value: TaskFilter) {
        filter.value = value
    }

    fun setSort(sort: TaskSort) = viewModelScope.launch { preferences.setTaskSort(sort) }

    fun setShowCompleted(show: Boolean) = viewModelScope.launch { preferences.setShowCompletedTasks(show) }

    fun toggle(task: Task, completed: Boolean) = viewModelScope.launch {
        runCatching { tasks.setCompleted(task.id, completed) }
    }

    fun clearCompleted() = viewModelScope.launch { runCatching { tasks.clearCompleted() } }

    private fun sectionsFor(sorted: List<Task>, filter: TaskFilter, sort: TaskSort, today: LocalDate): List<TaskSection> {
        val grouped = sort == TaskSort.TIME && (filter == TaskFilter.ALL || filter == TaskFilter.UPCOMING)
        if (!grouped) return listOf(TaskSection(null, sorted))

        val (completed, open) = sorted.partition { it.isCompleted }
        val sections = open
            .groupBy { task ->
                val due = task.dueDate
                when {
                    due == null -> TaskSectionKey.NoDate
                    due.isBefore(today) -> TaskSectionKey.Overdue
                    due == today -> TaskSectionKey.Today
                    due == today.plusDays(1) -> TaskSectionKey.Tomorrow
                    else -> TaskSectionKey.Day(due)
                }
            }
            .map { (key, list) -> TaskSection(key, list) }
            .sortedBy { section ->
                when (val key = section.key) {
                    TaskSectionKey.Overdue -> Long.MIN_VALUE
                    TaskSectionKey.Today -> today.toEpochDay()
                    TaskSectionKey.Tomorrow -> today.toEpochDay() + 1
                    is TaskSectionKey.Day -> key.date.toEpochDay()
                    TaskSectionKey.NoDate -> Long.MAX_VALUE - 1
                    else -> Long.MAX_VALUE
                }
            }
        return if (completed.isEmpty()) sections else sections + TaskSection(TaskSectionKey.Completed, completed)
    }
}
