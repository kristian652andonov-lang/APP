package com.aura.dashboard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aura.dashboard.data.local.dao.NewsDao
import com.aura.dashboard.data.local.dao.QuickAppDao
import com.aura.dashboard.data.local.dao.ReminderDao
import com.aura.dashboard.data.local.dao.TaskDao
import com.aura.dashboard.data.local.entity.NewsArticleEntity
import com.aura.dashboard.data.local.entity.NewsFetchStateEntity
import com.aura.dashboard.data.local.entity.QuickAppEntity
import com.aura.dashboard.data.local.entity.ReminderEntity
import com.aura.dashboard.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        QuickAppEntity::class,
        NewsArticleEntity::class,
        NewsFetchStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun quickAppDao(): QuickAppDao
    abstract fun newsDao(): NewsDao

    companion object {
        const val NAME = "aura.db"
    }
}
