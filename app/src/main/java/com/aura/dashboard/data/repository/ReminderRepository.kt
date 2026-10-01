package com.aura.dashboard.data.repository

import com.aura.dashboard.core.time.AppClock
import com.aura.dashboard.data.local.AuraDatabase
import com.aura.dashboard.data.local.entity.toDomain
import com.aura.dashboard.data.local.entity.toEntity
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.schedule.Recurrence
import com.aura.dashboard.notifications.AlarmScheduler
import com.aura.dashboard.notifications.AlarmTarget
import com.aura.dashboard.notifications.NotificationDismisser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ReminderSaveResult {
    data class Saved(val id: Long, val exact: Boolean) : ReminderSaveResult

    /** A one-time reminder was set in the past. */
    data object TimeInPast : ReminderSaveResult

    /** Stored, but the system refused to schedule the alarm. */
    data class SchedulingFailed(val id: Long) : ReminderSaveResult
}

interface ReminderRepository {
    fun observeReminders(): Flow<List<Reminder>>
    fun observeNextReminder(): Flow<Reminder?>
    suspend fun getReminder(id: Long): Reminder?
    suspend fun save(reminder: Reminder): ReminderSaveResult
    suspend fun delete(id: Long)

    /** Re-inserts a deleted reminder with its original id (used by "Undo"). */
    suspend fun restore(reminder: Reminder)
    suspend fun setEnabled(id: Long, enabled: Boolean): ReminderSaveResult?

    /**
     * Called when a reminder's alarm fires. Advances repeating reminders to their next
     * occurrence and returns the reminder to notify about, or null if it should stay silent.
     */
    suspend fun onReminderFired(id: Long): Reminder?
    suspend fun snooze(id: Long, duration: Duration)

    /** Re-creates all alarms, recomputing trigger times (e.g. after a reboot or zone change). */
    suspend fun rescheduleAll()
}

@Singleton
class OfflineFirstReminderRepository @Inject constructor(
    database: AuraDatabase,
    private val alarmScheduler: AlarmScheduler,
    private val notificationDismisser: NotificationDismisser,
    private val preferences: UserPreferencesRepository,
    private val clock: AppClock,
) : ReminderRepository {

    private val dao = database.reminderDao()

    override fun observeReminders(): Flow<List<Reminder>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeNextReminder(): Flow<Reminder?> = dao.observeNext().map { it?.toDomain() }

    override suspend fun getReminder(id: Long): Reminder? = dao.getById(id)?.toDomain()

    override suspend fun save(reminder: Reminder): ReminderSaveResult {
        val now = clock.now()
        val next = Recurrence.nextOccurrence(reminder.startDate, reminder.time, reminder.repeat, now, clock.zone())
        if (next == null && reminder.isEnabled) return ReminderSaveResult.TimeInPast

        val normalized = reminder.copy(
            title = reminder.title.trim(),
            description = reminder.description.trim(),
            nextTriggerAt = next,
        )
        val id = if (normalized.id == 0L) {
            dao.insert(normalized.copy(createdAt = now).toEntity())
        } else {
            dao.update(normalized.toEntity())
            normalized.id
        }
        return syncAlarm(normalized.copy(id = id))
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
        alarmScheduler.cancel(AlarmTarget.ReminderDue(id))
        alarmScheduler.cancel(AlarmTarget.ReminderSnooze(id))
        notificationDismisser.dismiss(AlarmTarget.ReminderDue(id))
    }

    override suspend fun restore(reminder: Reminder) {
        if (dao.getById(reminder.id) != null) return
        val next = if (reminder.nextTriggerAt == null) {
            null
        } else {
            Recurrence.nextOccurrence(reminder.startDate, reminder.time, reminder.repeat, clock.now(), clock.zone())
        }
        val restored = reminder.copy(nextTriggerAt = next)
        dao.insert(restored.toEntity())
        syncAlarm(restored)
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean): ReminderSaveResult? {
        val reminder = dao.getById(id)?.toDomain() ?: return null
        if (!enabled) {
            dao.update(reminder.copy(isEnabled = false).toEntity())
            alarmScheduler.cancel(AlarmTarget.ReminderDue(id))
            alarmScheduler.cancel(AlarmTarget.ReminderSnooze(id))
            return ReminderSaveResult.Saved(id, alarmScheduler.canScheduleExactAlarms())
        }
        return save(reminder.copy(isEnabled = true))
    }

    override suspend fun onReminderFired(id: Long): Reminder? {
        val reminder = dao.getById(id)?.toDomain() ?: return null
        if (!reminder.isEnabled) return null
        val now = clock.now()
        // Never schedule the same instant twice, even if the alarm fired a moment early.
        val after = listOfNotNull(now, reminder.nextTriggerAt).max()
        val next = Recurrence.nextOccurrence(reminder.startDate, reminder.time, reminder.repeat, after, clock.zone())
        val updated = reminder.copy(nextTriggerAt = next, lastTriggeredAt = now)
        dao.update(updated.toEntity())
        syncAlarm(updated)
        return updated
    }

    override suspend fun snooze(id: Long, duration: Duration) {
        if (dao.getById(id) == null) return
        alarmScheduler.schedule(AlarmTarget.ReminderSnooze(id), clock.now().plus(duration))
        notificationDismisser.dismiss(AlarmTarget.ReminderDue(id))
    }

    override suspend fun rescheduleAll() {
        val now = clock.now()
        dao.getAll().map { it.toDomain() }.forEach { reminder ->
            if (!reminder.isEnabled) {
                alarmScheduler.cancel(AlarmTarget.ReminderDue(reminder.id))
                return@forEach
            }
            val next = reminder.nextTriggerAt?.let { stored ->
                if (!stored.isAfter(now)) {
                    // Missed while the device was off or the app was force-stopped: keep the
                    // past time so the alarm fires immediately and the user still gets notified.
                    stored
                } else {
                    // Recompute so a time zone change keeps the reminder at the same local time.
                    Recurrence.nextOccurrence(reminder.startDate, reminder.time, reminder.repeat, now, clock.zone())
                        ?: stored
                }
            }
            val updated = reminder.copy(nextTriggerAt = next)
            if (updated != reminder) dao.update(updated.toEntity())
            syncAlarm(updated)
        }
    }

    private suspend fun syncAlarm(reminder: Reminder): ReminderSaveResult {
        val target = AlarmTarget.ReminderDue(reminder.id)
        val next = reminder.nextTriggerAt
        if (!reminder.isEnabled || next == null || !preferences.current().remindersEnabled) {
            alarmScheduler.cancel(target)
            return ReminderSaveResult.Saved(reminder.id, alarmScheduler.canScheduleExactAlarms())
        }
        return if (alarmScheduler.schedule(target, next)) {
            ReminderSaveResult.Saved(reminder.id, alarmScheduler.canScheduleExactAlarms())
        } else {
            ReminderSaveResult.SchedulingFailed(reminder.id)
        }
    }
}
