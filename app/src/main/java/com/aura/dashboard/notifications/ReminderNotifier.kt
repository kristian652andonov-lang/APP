package com.aura.dashboard.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aura.dashboard.MainActivity
import com.aura.dashboard.R
import com.aura.dashboard.data.preferences.UserPreferencesRepository
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

/** Removes notifications that are no longer relevant (task completed, reminder deleted…). */
interface NotificationDismisser {
    fun dismiss(target: AlarmTarget)
}

@Singleton
class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
) : NotificationDismisser {

    private val manager = NotificationManagerCompat.from(context)

    fun canPostNotifications(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && manager.areNotificationsEnabled()
    }

    suspend fun showReminder(reminder: Reminder) {
        val target = AlarmTarget.ReminderDue(reminder.id)
        val builder = baseBuilder(target, DeepLink.Reminder(reminder.id))
            .setContentTitle(reminder.title)
            .setContentText(reminder.description.ifBlank { context.getString(R.string.notification_reminder_fallback_text) })
            .addAction(
                R.drawable.ic_stat_snooze,
                context.getString(R.string.notification_action_snooze),
                actionIntent(NotificationActionReceiver.ACTION_SNOOZE, target),
            )
        if (reminder.description.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(reminder.description))
        }
        post(target, builder)
    }

    suspend fun showTaskDue(task: Task) {
        val target = AlarmTarget.TaskDue(task.id)
        val time = task.dueTime?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        val text = task.notes.ifBlank {
            if (time != null) context.getString(R.string.notification_task_due_at, time)
            else context.getString(R.string.notification_task_due)
        }
        val builder = baseBuilder(target, DeepLink.Task(task.id))
            .setContentTitle(task.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .addAction(
                R.drawable.ic_stat_check,
                context.getString(R.string.notification_action_complete),
                actionIntent(NotificationActionReceiver.ACTION_COMPLETE_TASK, target),
            )
        post(target, builder)
    }

    override fun dismiss(target: AlarmTarget) {
        manager.cancel(tagFor(target), notificationId(target))
    }

    private suspend fun baseBuilder(target: AlarmTarget, deepLink: DeepLink): NotificationCompat.Builder {
        val prefs = preferences.current()
        val channelId = NotificationChannels.ensureRemindersChannel(
            context,
            sound = prefs.notificationSound,
            vibration = prefs.notificationVibration,
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_aura)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(contentIntent(target, deepLink))
            // Pre-O fallback; on O+ the channel controls sound and vibration.
            .setSilent(!prefs.notificationSound && !prefs.notificationVibration)
    }

    private fun post(target: AlarmTarget, builder: NotificationCompat.Builder) {
        if (!canPostNotifications()) return
        try {
            manager.notify(tagFor(target), notificationId(target), builder.build())
        } catch (security: SecurityException) {
            // Permission revoked concurrently; nothing else we can do.
        }
    }

    private fun contentIntent(target: AlarmTarget, deepLink: DeepLink): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(deepLink.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            notificationId(target),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun actionIntent(action: String, target: AlarmTarget): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .setData(target.toUri())
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun tagFor(target: AlarmTarget): String = when (target) {
        is AlarmTarget.ReminderDue, is AlarmTarget.ReminderSnooze -> TAG_REMINDER
        is AlarmTarget.TaskDue -> TAG_TASK
    }

    /** Snoozed and regular reminders share one notification slot. */
    private fun notificationId(target: AlarmTarget): Int = (target.id % Int.MAX_VALUE).toInt()

    private companion object {
        const val TAG_REMINDER = "reminder"
        const val TAG_TASK = "task"
    }
}
