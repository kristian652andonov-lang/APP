package com.aura.dashboard.notifications

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/** In-app destinations that can be opened from outside (notifications). */
sealed interface DeepLink {
    data class Reminder(val id: Long) : DeepLink
    data class Task(val id: Long) : DeepLink

    fun toUri(): Uri = when (this) {
        is Reminder -> "$SCHEME://$HOST/reminder/$id"
        is Task -> "$SCHEME://$HOST/task/$id"
    }.toUri()

    companion object {
        private const val SCHEME = "aura"
        private const val HOST = "open"

        fun from(intent: Intent?): DeepLink? {
            val uri = intent?.data ?: return null
            if (uri.scheme != SCHEME || uri.host != HOST) return null
            val segments = uri.pathSegments
            if (segments.size != 2) return null
            val id = segments[1].toLongOrNull() ?: return null
            return when (segments[0]) {
                "reminder" -> Reminder(id)
                "task" -> Task(id)
                else -> null
            }
        }
    }
}
