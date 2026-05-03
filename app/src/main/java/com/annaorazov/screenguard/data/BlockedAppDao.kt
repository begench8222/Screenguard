package com.annaorazov.screenguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlockedAppDao {
    @Insert
    suspend fun insertBlockedApp(blockedAppEntity: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAll(): List<BlockedAppEntity>

    @Query("DELETE FROM blocked_apps WHERE package_name = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)

    @Query("SELECT * FROM blocked_apps WHERE package_name = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): BlockedAppEntity?
}