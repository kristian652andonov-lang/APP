package com.aura.dashboard.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aura.dashboard.core.di.ApplicationScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun alarmHandler(): AlarmHandler

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}

/**
 * Runs [block] off the main thread while keeping the broadcast alive (goAsync), bounded so we
 * never exceed the system's broadcast time limit.
 */
private fun BroadcastReceiver.handleAsync(context: Context, block: suspend (AlarmHandler) -> Unit) {
    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
    val pendingResult = goAsync()
    entryPoint.applicationScope().launch {
        try {
            withTimeout(BROADCAST_TIMEOUT_MS) { block(entryPoint.alarmHandler()) }
        } catch (error: Exception) {
            Log.e("AuraReceiver", "Broadcast handling failed", error)
        } finally {
            pendingResult.finish()
        }
    }
}

private const val BROADCAST_TIMEOUT_MS = 8_000L

/** Fires when a reminder/task alarm goes off. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM) return
        val target = AlarmTarget.fromUri(intent.data) ?: return
        handleAsync(context) { it.onAlarm(target) }
    }

    companion object {
        const val ACTION_ALARM = "com.aura.dashboard.action.ALARM"
    }
}

/** Handles notification buttons ("Snooze", "Mark as done"). */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val target = AlarmTarget.fromUri(intent.data) ?: return
        when (intent.action) {
            ACTION_SNOOZE -> handleAsync(context) { it.onSnooze(target) }
            ACTION_COMPLETE_TASK -> handleAsync(context) { it.onCompleteTask(target) }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.aura.dashboard.action.SNOOZE"
        const val ACTION_COMPLETE_TASK = "com.aura.dashboard.action.COMPLETE_TASK"
    }
}

/** Restores alarms after reboot, app update, clock/time zone changes or permission changes. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in HANDLED_ACTIONS) {
            handleAsync(context) { it.rescheduleAll() }
        }
    }

    private companion object {
        // The exact-alarm action constant is inlined at compile time; older devices never send it.
        @SuppressLint("InlinedApi")
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
