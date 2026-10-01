package com.aura.dashboard.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.TaskRepository
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TaskDraft(
    val title: String = "",
    val notes: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: Priority = Priority.NORMAL,
    val repeat: Repeat = Repeat.NEVER,
    val remind: Boolean = true,
)

data class TaskEditorState(
    val isOpen: Boolean = false,
    val original: Task? = null,
    val draft: TaskDraft = TaskDraft(),
    val today: LocalDate = LocalDate.now(),
) {
    val isNew: Boolean get() = original == null
    val canSave: Boolean get() = draft.title.isNotBlank()
}

sealed interface TaskEditorEvent {
    data class Saved(val isNew: Boolean) : TaskEditorEvent
    data class Deleted(val task: Task) : TaskEditorEvent
    data object Error : TaskEditorEvent
}

/** Shared add/edit task logic used by the Home and Tasks screens. */
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val preferences: UserPreferencesRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditorState())
    val state: StateFlow<TaskEditorState> = _state.asStateFlow()

    private val _events = Channel<TaskEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun openNew(dueToday: Boolean = true) {
        viewModelScope.launch {
            val today = clock.today()
            _state.value = TaskEditorState(
                isOpen = true,
                draft = TaskDraft(
                    dueDate = if (dueToday) today else null,
                    priority = preferences.current().defaultPriority,
                ),
                today = today,
            )
        }
    }

    fun openExisting(id: Long) {
        viewModelScope.launch {
            val task = tasks.getTask(id) ?: return@launch
            _state.value = TaskEditorState(
                isOpen = true,
                original = task,
                draft = TaskDraft(
                    title = task.title,
                    notes = task.notes,
                    dueDate = task.dueDate,
                    dueTime = task.dueTime,
                    priority = task.priority,
                    repeat = task.repeat,
                    remind = task.remind,
                ),
                today = clock.today(),
            )
        }
    }

    fun update(transform: (TaskDraft) -> TaskDraft) {
        _state.update { current ->
            val next = transform(current.draft)
            // Clearing the date also clears the time and repeat rule that depend on it.
            val consistent = if (next.dueDate == null) next.copy(dueTime = null, repeat = Repeat.NEVER) else next
            current.copy(draft = consistent)
        }
    }

    /** Persists the draft. Returns true when the sheet can close. */
    suspend fun save(): Boolean {
        val current = _state.value
        if (!current.canSave) return false
        val draft = current.draft
        return viewModelScope.async {
            runCatching {
                val base = current.original ?: Task(title = draft.title)
                tasks.save(
                    base.copy(
                        title = draft.title,
                        notes = draft.notes,
                        dueDate = draft.dueDate,
                        dueTime = draft.dueTime,
                        priority = draft.priority,
                        repeat = draft.repeat,
                        remind = draft.remind,
                    ),
                )
            }.fold(
                onSuccess = {
                    _events.send(TaskEditorEvent.Saved(current.isNew))
                    true
                },
                onFailure = {
                    _events.send(TaskEditorEvent.Error)
                    false
                },
            )
        }.await()
    }

    suspend fun delete(): Boolean {
        val task = _state.value.original ?: return false
        return viewModelScope.async {
            runCatching { tasks.delete(task.id) }.isSuccess.also { ok ->
                _events.send(if (ok) TaskEditorEvent.Deleted(task) else TaskEditorEvent.Error)
            }
        }.await()
    }

    fun undoDelete(task: Task) {
        viewModelScope.launch { runCatching { tasks.restore(task) } }
    }

    fun dismiss() {
        _state.update { it.copy(isOpen = false) }
    }
}
