package com.aura.dashboard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aura.dashboard.domain.model.Priority
import com.aura.dashboard.domain.model.Repeat
import com.aura.dashboard.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "tasks",
    indices = [Index("due_date"), Index("is_completed"), Index("previous_occurrence_id")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String,
    /** Epoch day. */
    @ColumnInfo(name = "due_date") val dueDate: Long?,
    /** Minute of day. */
    @ColumnInfo(name = "due_time") val dueTime: Int?,
    val priority: String,
    val repeat: String,
    @ColumnInfo(name = "anchor_date") val anchorDate: Long?,
    val remind: Boolean,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "previous_occurrence_id") val previousOccurrenceId: Long?,
)

fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    notes = notes,
    dueDate = dueDate?.let(LocalDate::ofEpochDay),
    dueTime = dueTime?.let { LocalTime.of(it / 60, it % 60) },
    priority = enumOrDefault(priority, Priority.NORMAL),
    repeat = enumOrDefault(repeat, Repeat.NEVER),
    anchorDate = anchorDate?.let(LocalDate::ofEpochDay),
    remind = remind,
    isCompleted = isCompleted,
    completedAt = completedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    previousOccurrenceId = previousOccurrenceId,
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    notes = notes,
    dueDate = dueDate?.toEpochDay(),
    dueTime = dueTime?.let { it.hour * 60 + it.minute },
    priority = priority.name,
    repeat = repeat.name,
    anchorDate = anchorDate?.toEpochDay(),
    remind = remind,
    isCompleted = isCompleted,
    completedAt = completedAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    previousOccurrenceId = previousOccurrenceId,
)

internal inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
