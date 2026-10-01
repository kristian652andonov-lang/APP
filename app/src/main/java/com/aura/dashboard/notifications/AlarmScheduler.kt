package com.aura.dashboard.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface AlarmScheduler {
    /** True when alarms fire at the exact minute (otherwise Android may delay them slightly). */
    fun canScheduleExactAlarms(): Boolean

    /** Schedules (or replaces) the alarm for [target]. Returns false if scheduling failed. */
    fun schedule(target: AlarmTarget, at: Instant): Boolean

    fun cancel(target: AlarmTarget)
}

@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AlarmScheduler {

    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    override fun schedule(target: AlarmTarget, at: Instant): Boolean {
        val manager = alarmManager ?: return false
        val operation = pendingIntent(target)
        val triggerAt = at.toEpochMilli()
        return try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                // Without the "Alarms & reminders" permission we fall back to an inexact alarm,
                // which still fires in Doze but may be deferred by a few minutes.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
            true
        } catch (security: SecurityException) {
            // The exact-alarm permission can be revoked between the check and the call.
            Log.w(TAG, "Exact alarm not permitted, falling back to inexact", security)
            runCatching {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }.isSuccess
        } catch (error: RuntimeException) {
            // E.g. the system limit of 500 pending alarms per app was reached.
            Log.e(TAG, "Failed to schedule alarm for $target", error)
            false
        }
    }

    override fun cancel(target: AlarmTarget) {
        alarmManager?.cancel(pendingIntent(target))
    }

    private fun pendingIntent(target: AlarmTarget): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_ALARM)
            .setData(target.toUri())
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
