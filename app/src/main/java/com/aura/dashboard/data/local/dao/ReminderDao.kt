package com.aura.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aura.dashboard.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY next_trigger_at IS NULL, next_trigger_at, created_at DESC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query(
        "SELECT * FROM reminders WHERE is_enabled = 1 AND next_trigger_at IS NOT NULL " +
            "ORDER BY next_trigger_at LIMIT 1",
    )
    fun observeNext(): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)
}
