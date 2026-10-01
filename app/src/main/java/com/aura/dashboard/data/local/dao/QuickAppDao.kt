package com.aura.dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aura.dashboard.data.local.entity.QuickAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickAppDao {
    @Query("SELECT * FROM quick_apps ORDER BY position, added_at")
    fun observeAll(): Flow<List<QuickAppEntity>>

    @Query("SELECT * FROM quick_apps ORDER BY position, added_at")
    suspend fun getAll(): List<QuickAppEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM quick_apps")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: QuickAppEntity): Long

    @Query("DELETE FROM quick_apps WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE quick_apps SET custom_label = :label WHERE package_name = :packageName")
    suspend fun setCustomLabel(packageName: String, label: String?)

    @Query("UPDATE quick_apps SET show_on_home = :show WHERE package_name = :packageName")
    suspend fun setShowOnHome(packageName: String, show: Boolean)

    @Query("UPDATE quick_apps SET position = :position WHERE package_name = :packageName")
    suspend fun setPosition(packageName: String, position: Int)

    @Transaction
    suspend fun reorder(orderedPackages: List<String>) {
        orderedPackages.forEachIndexed { index, pkg -> setPosition(pkg, index) }
    }
}
