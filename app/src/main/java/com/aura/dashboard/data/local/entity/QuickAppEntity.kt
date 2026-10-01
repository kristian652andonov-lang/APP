package com.aura.dashboard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_apps")
data class QuickAppEntity(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "custom_label") val customLabel: String?,
    val position: Int,
    @ColumnInfo(name = "show_on_home") val showOnHome: Boolean,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
