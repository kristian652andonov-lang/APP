package com.aura.dashboard.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.ReminderRepository
import com.aura.dashboard.data.repository.ReminderSaveResult
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.ui.util.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class RemindersUiState(
    val isLoading: Boolean = true,
    val upcoming: List<Reminder> = emptyList(),
    val paused: List<Reminder> = emptyList(),
    val past: List<Reminder> = emptyList(),
    val remindersEnabled: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val now: Instant = Instant.now(),
) {
    val isEmpty: Boolean get() = upcoming.isEmpty() && paused.isEmpty() && past.isEmpty()
}

data class ReminderDraft(
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val repeat: Repeat = Repeat.NEVER,
)

data class ReminderEditorState(
    val isOpen: Boolean = false,
    val original: Reminder? = null,
    val draft: ReminderDraft = ReminderDraft(),
    val timeInPast: Boolean = false,
    val today: LocalDate = LocalDate.now(),
) {
    val isNew: Boolean get() = original == null
    val canSave: Boolean get() = draft.title.isNotBlank()
}

sealed interface ReminderEvent {
    data class Saved(val reminder: Reminder?, val exact: Boolean) : ReminderEvent
    data class Deleted(val reminder: Reminder) : ReminderEvent
    data object SchedulingFailed : ReminderEvent
    data object Error : ReminderEvent
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminders: ReminderRepository,
    preferences: UserPreferencesRepository,
    private val clock: AppClock,
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = combine(
        reminders.observeReminders(),
        preferences.preferences,
        minuteTicker(clock),
    ) { all, prefs, now ->
        RemindersUiState(
            isLoading = false,
            upcoming = all.filter { it.isEnabled && !it.isFinished }.sortedBy { it.nextTriggerAt },
            paused = all.filter { !it.isEnabled },
            past = all.filter { it.isEnabled && it.isFinished }.sortedByDescending { it.lastTriggeredAt },
            remindersEnabled = prefs.remindersEnabled,
            today = now.atZone(clock.zone()).toLocalDate(),
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    private val _editor = MutableStateFlow(ReminderEditorState())
    val editor: StateFlow<ReminderEditorState> = _editor.asStateFlow()

    private val _events = Channel<ReminderEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun openNew() {
        val now = clock.now().atZone(clock.zone())
        // Default to the next full hour.
        val next = now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
        _editor.value = ReminderEditorState(
            isOpen = true,
            draft = ReminderDraft(date = next.toLocalDate(), time = next.toLocalTime()),
            today = now.toLocalDate(),
        )
    }

    fun openExisting(id: Long) {
        viewModelScope.launch { loadEditor(id, timeInPast = false) }
    }

    private suspend fun loadEditor(id: Long, timeInPast: Boolean) {
        val reminder = reminders.getReminder(id) ?: return
        val today = clock.today()
        // A finished one-time reminder re-opens on today's date for easy re-use.
        val date = if (reminder.isFinished && reminder.repeat == Repeat.NEVER) maxOf(reminder.startDate, today) else reminder.startDate
        _editor.value = ReminderEditorState(
            isOpen = true,
            original = reminder,
            draft = ReminderDraft(reminder.title, reminder.description, date, reminder.time, reminder.repeat),
            timeInPast = timeInPast,
            today = today,
        )
    }

    fun updateDraft(transform: (ReminderDraft) -> ReminderDraft) {
        _editor.update { it.copy(draft = transform(it.draft), timeInPast = false) }
    }

    suspend fun save(): Boolean {
        val state = _editor.value
        if (!state.canSave) return false
        val draft = state.draft
        val reminder = (state.original ?: Reminder(title = draft.title, startDate = draft.date, time = draft.time)).copy(
            title = draft.title,
            description = draft.description,
            startDate = draft.date,
            time = draft.time,
            repeat = draft.repeat,
            isEnabled = true,
        )
        return viewModelScope.async {
            when (val result = runCatching { reminders.save(reminder) }.getOrNull()) {
                is ReminderSaveResult.Saved -> {
                    _events.send(ReminderEvent.Saved(reminders.getReminder(result.id), result.exact))
                    true
                }
                ReminderSaveResult.TimeInPast -> {
                    _editor.update { it.copy(timeInPast = true) }
                    false
                }
                is ReminderSaveResult.SchedulingFailed -> {
                    _events.send(ReminderEvent.SchedulingFailed)
                    true
                }
                null -> {
                    _events.send(ReminderEvent.Error)
                    false
                }
            }
        }.await()
    }

    suspend fun delete(): Boolean {
        val reminder = _editor.value.original ?: return false
        return viewModelScope.async {
            runCatching { reminders.delete(reminder.id) }.isSuccess.also { ok ->
                _events.send(if (ok) ReminderEvent.Deleted(reminder) else ReminderEvent.Error)
            }
        }.await()
    }

    fun undoDelete(reminder: Reminder) {
        viewModelScope.launch { runCatching { reminders.restore(reminder) } }
    }

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch {
            when (runCatching { reminders.setEnabled(reminder.id, enabled) }.getOrNull()) {
                ReminderSaveResult.TimeInPast -> loadEditor(reminder.id, timeInPast = true)
                is ReminderSaveResult.SchedulingFailed -> _events.send(ReminderEvent.SchedulingFailed)
                else -> Unit
            }
        }
    }

    fun dismissEditor() {
        _editor.update { it.copy(isOpen = false) }
    }
}
