package com.aura.dashboard.data.repository

import androidx.room.withTransaction
import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.local.AuraDatabase
import com.aura.dashboard.data.local.entity.toDomain
import com.aura.dashboard.data.local.entity.toEntity
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.domain.model.Task
import com.aura.dashboard.domain.schedule.Recurrence
import com.aura.dashboard.notifications.AlarmScheduler
import com.aura.dashboard.notifications.AlarmTarget
import com.aura.dashboard.notifications.NotificationDismisser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun getTask(id: Long): Task?

    /** Inserts a new task (id == 0) or updates an existing one. Returns the task id. */
    suspend fun save(task: Task): Long
    suspend fun delete(id: Long)

    /** Re-inserts a deleted task with its original id (used by "Undo"). */
    suspend fun restore(task: Task)
    suspend fun setCompleted(id: Long, completed: Boolean)
    suspend fun clearCompleted()

    /** Re-creates due-time alarms for all open tasks, e.g. after a reboot. */
    suspend fun rescheduleNotifications()
}

@Singleton
class OfflineFirstTaskRepository @Inject constructor(
    private val database: AuraDatabase,
    private val alarmScheduler: AlarmScheduler,
    private val notificationDismisser: NotificationDismisser,
    private val preferences: UserPreferencesRepository,
    private val clock: AppClock,
) : TaskRepository {

    private val dao = database.taskDao()

    override fun observeTasks(): Flow<List<Task>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTask(id: Long): Task? = dao.getById(id)?.toDomain()

    override suspend fun save(task: Task): Long {
        val now = clock.now()
        val normalized = task.copy(
            title = task.title.trim(),
            notes = task.notes.trim(),
            // Times and repeats only make sense with a date.
            dueTime = if (task.dueDate == null) null else task.dueTime,
            repeat = if (task.dueDate == null) Repeat.NEVER else task.repeat,
            anchorDate = if (task.dueDate == null) null else (task.anchorDate.takeIf { task.id != 0L && it != null } ?: task.dueDate),
            updatedAt = now,
        )
        val id = if (normalized.id == 0L) {
            dao.insert(normalized.copy(createdAt = now).toEntity())
        } else {
            dao.update(normalized.toEntity())
            normalized.id
        }
        syncAlarm(normalized.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        database.withTransaction {
            dao.clearPreviousOccurrence(id)
            dao.delete(id)
        }
        alarmScheduler.cancel(AlarmTarget.TaskDue(id))
        notificationDismisser.dismiss(AlarmTarget.TaskDue(id))
    }

    override suspend fun restore(task: Task) {
        if (dao.getById(task.id) == null) dao.insert(task.toEntity())
        syncAlarm(task)
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) {
        val now = clock.now()
        val changed = mutableListOf<Task>()
        val removedIds = mutableListOf<Long>()
        database.withTransaction {
            val task = dao.getById(id)?.toDomain() ?: return@withTransaction
            if (task.isCompleted == completed) return@withTransaction
            val updated = task.copy(
                isCompleted = completed,
                completedAt = if (completed) now else null,
                updatedAt = now,
            )
            dao.update(updated.toEntity())
            changed += updated

            val existingNext = dao.getNextOccurrence(id)?.toDomain()
            if (completed && task.isRecurring && existingNext == null) {
                val due = task.dueDate!!
                val nextDate = Recurrence.nextTaskDueDate(
                    dueDate = due,
                    anchor = task.anchorDate ?: due,
                    repeat = task.repeat,
                    today = clock.today(),
                )
                if (nextDate != null) {
                    val next = task.copy(
                        id = 0,
                        dueDate = nextDate,
                        anchorDate = task.anchorDate ?: due,
                        isCompleted = false,
                        completedAt = null,
                        createdAt = now,
                        updatedAt = now,
                        previousOccurrenceId = id,
                    )
                    val nextId = dao.insert(next.toEntity())
                    changed += next.copy(id = nextId)
                }
            } else if (!completed && existingNext != null && !existingNext.isCompleted) {
                // Un-checking a recurring task removes the occurrence that completing it created.
                dao.delete(existingNext.id)
                removedIds += existingNext.id
            }
        }
        changed.forEach { syncAlarm(it) }
        removedIds.forEach { alarmScheduler.cancel(AlarmTarget.TaskDue(it)) }
        if (completed) notificationDismisser.dismiss(AlarmTarget.TaskDue(id))
    }

    override suspend fun clearCompleted() {
        val ids = database.withTransaction {
            val completedIds = dao.getCompletedIds()
            dao.clearReferencesToCompleted()
            dao.deleteCompleted()
            completedIds
        }
        ids.forEach { alarmScheduler.cancel(AlarmTarget.TaskDue(it)) }
    }

    override suspend fun rescheduleNotifications() {
        dao.getOpenWithNotification().forEach { syncAlarm(it.toDomain()) }
    }

    private suspend fun syncAlarm(task: Task) {
        val target = AlarmTarget.TaskDue(task.id)
        val date = task.dueDate
        val time = task.dueTime
        if (task.isCompleted || !task.hasNotification || date == null || time == null ||
            !preferences.current().remindersEnabled
        ) {
            alarmScheduler.cancel(target)
            return
        }
        val at = date.atTime(time).atZone(clock.zone()).toInstant()
        if (at.isAfter(clock.now())) alarmScheduler.schedule(target, at) else alarmScheduler.cancel(target)
    }
}
