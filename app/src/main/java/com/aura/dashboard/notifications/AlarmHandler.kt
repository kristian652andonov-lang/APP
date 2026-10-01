package com.aura.dashboard.notifications

import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.data.repository.ReminderRepository
import com.aura.dashboard.data.repository.TaskRepository
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/** Business logic behind alarm and notification-action broadcasts. */
@Singleton
class AlarmHandler @Inject constructor(
    private val reminders: ReminderRepository,
    private val tasks: TaskRepository,
    private val notifier: ReminderNotifier,
    private val preferences: UserPreferencesRepository,
) {

    suspend fun onAlarm(target: AlarmTarget) {
        val enabled = preferences.current().remindersEnabled
        when (target) {
            is AlarmTarget.ReminderDue -> {
                val reminder = reminders.onReminderFired(target.id) ?: return
                if (enabled) notifier.showReminder(reminder)
            }
            is AlarmTarget.ReminderSnooze -> {
                val reminder = reminders.getReminder(target.id) ?: return
                if (enabled && reminder.isEnabled) notifier.showReminder(reminder)
            }
            is AlarmTarget.TaskDue -> {
                val task = tasks.getTask(target.id) ?: return
                if (enabled && !task.isCompleted && task.hasNotification) notifier.showTaskDue(task)
            }
        }
    }

    suspend fun onSnooze(target: AlarmTarget) {
        reminders.snooze(target.id, SNOOZE_DURATION)
    }

    suspend fun onCompleteTask(target: AlarmTarget) {
        tasks.setCompleted(target.id, completed = true)
        notifier.dismiss(target)
    }

    /** Restores every alarm. AlarmManager forgets alarms on reboot and force-stop. */
    suspend fun rescheduleAll() {
        reminders.rescheduleAll()
        tasks.rescheduleNotifications()
    }

    companion object {
        val SNOOZE_DURATION: Duration = Duration.ofMinutes(10)
    }
}
