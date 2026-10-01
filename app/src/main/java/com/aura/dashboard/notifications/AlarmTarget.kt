package com.aura.dashboard.notifications

import android.net.Uri
import androidx.core.net.toUri

/**
 * Identifies what an alarm or notification refers to. Encoded as a URI so every target gets
 * its own distinct PendingIntent (PendingIntents are matched by Intent data, not extras).
 */
sealed interface AlarmTarget {
    val id: Long

    data class ReminderDue(override val id: Long) : AlarmTarget
    data class ReminderSnooze(override val id: Long) : AlarmTarget
    data class TaskDue(override val id: Long) : AlarmTarget

    fun toUri(): Uri = "$SCHEME://$HOST/${kind()}/$id".toUri()

    private fun kind(): String = when (this) {
        is ReminderDue -> KIND_REMINDER
        is ReminderSnooze -> KIND_SNOOZE
        is TaskDue -> KIND_TASK
    }

    companion object {
        private const val SCHEME = "aura"
        private const val HOST = "alarm"
        private const val KIND_REMINDER = "reminder"
        private const val KIND_SNOOZE = "snooze"
        private const val KIND_TASK = "task"

        fun fromUri(uri: Uri?): AlarmTarget? {
            if (uri == null || uri.scheme != SCHEME || uri.host != HOST) return null
            val segments = uri.pathSegments
            if (segments.size != 2) return null
            val id = segments[1].toLongOrNull() ?: return null
            return when (segments[0]) {
                KIND_REMINDER -> ReminderDue(id)
                KIND_SNOOZE -> ReminderSnooze(id)
                KIND_TASK -> TaskDue(id)
                else -> null
            }
        }
    }
}
