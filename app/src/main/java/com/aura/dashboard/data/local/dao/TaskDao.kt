package com.aura.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aura.dashboard.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE previous_occurrence_id = :id LIMIT 1")
    suspend fun getNextOccurrence(id: Long): TaskEntity?

    @Query(
        "SELECT * FROM tasks WHERE is_completed = 0 AND remind = 1 " +
            "AND due_date IS NOT NULL AND due_time IS NOT NULL",
    )
    suspend fun getOpenWithNotification(): List<TaskEntity>

    @Query("SELECT id FROM tasks WHERE is_completed = 1")
    suspend fun getCompletedIds(): List<Long>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasks WHERE is_completed = 1")
    suspend fun deleteCompleted()

    /** Detach generated occurrences from a deleted predecessor so they stay valid. */
    @Query("UPDATE tasks SET previous_occurrence_id = NULL WHERE previous_occurrence_id = :id")
    suspend fun clearPreviousOccurrence(id: Long)

    @Query("UPDATE tasks SET previous_occurrence_id = NULL WHERE previous_occurrence_id IN (SELECT id FROM tasks WHERE is_completed = 1)")
    suspend fun clearReferencesToCompleted()
}
