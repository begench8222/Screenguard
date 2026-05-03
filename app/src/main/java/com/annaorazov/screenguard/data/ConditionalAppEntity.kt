package com.annaorazov.screenguard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conditional_apps")
data class ConditionalAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_name") val appName: String,
    @ColumnInfo(name = "condition") val condition: String,
    @ColumnInfo(name = "limit_time") val limitTime: Long,
    @ColumnInfo(name = "usage_time") var usageTime: Long = 0
)