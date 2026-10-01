package com.aura.dashboard.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.aura.dashboard.R

/**
 * Android does not let apps change a channel's sound/vibration after creation, so each
 * combination of the in-app toggles maps to its own channel id. Switching a toggle deletes the
 * old channel and creates the new one; the user only ever sees one "Reminders" channel.
 */
object NotificationChannels {

    private const val REMINDERS_PREFIX = "reminders"

    fun remindersChannelId(sound: Boolean, vibration: Boolean): String =
        "${REMINDERS_PREFIX}_s${if (sound) 1 else 0}_v${if (vibration) 1 else 0}"

    /** Ensures the reminders channel matching the settings exists and returns its id. */
    fun ensureRemindersChannel(context: Context, sound: Boolean, vibration: Boolean): String {
        val manager = NotificationManagerCompat.from(context)
        val id = remindersChannelId(sound, vibration)
        manager.notificationChannelsCompat
            .filter { it.id.startsWith(REMINDERS_PREFIX) && it.id != id }
            .forEach { manager.deleteNotificationChannel(it.id) }

        if (manager.getNotificationChannelCompat(id) == null) {
            val builder = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.notification_channel_reminders))
                .setDescription(context.getString(R.string.notification_channel_reminders_description))
                .setVibrationEnabled(vibration)
                .setLightsEnabled(true)
            if (sound) {
                builder.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            } else {
                builder.setSound(null, null)
            }
            manager.createNotificationChannel(builder.build())
        }
        return id
    }
}
