package com.aura.dashboard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aura.dashboard.domain.model.Reminder
import com.aura.dashboard.domain.model.Repeat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "reminders", indices = [Index("next_trigger_at")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    /** Epoch day. */
    @ColumnInfo(name = "start_date") val startDate: Long,
    /** Minute of day. */
    val time: Int,
    val repeat: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
    @ColumnInfo(name = "next_trigger_at") val nextTriggerAt: Long?,
    @ColumnInfo(name = "last_triggered_at") val lastTriggeredAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun ReminderEntity.toDomain() = Reminder(
    id = id,
    title = title,
    description = description,
    startDate = LocalDate.ofEpochDay(startDate),
    time = LocalTime.of(time / 60, time % 60),
    repeat = enumOrDefault(repeat, Repeat.NEVER),
    isEnabled = isEnabled,
    nextTriggerAt = nextTriggerAt?.let(Instant::ofEpochMilli),
    lastTriggeredAt = lastTriggeredAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    title = title,
    description = description,
    startDate = startDate.toEpochDay(),
    time = time.hour * 60 + time.minute,
    repeat = repeat.name,
    isEnabled = isEnabled,
    nextTriggerAt = nextTriggerAt?.toEpochMilli(),
    lastTriggeredAt = lastTriggeredAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
)
