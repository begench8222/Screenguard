package com.annaorazov.screenguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConditionalAppDao {
    @Insert
    suspend fun insertConditionalApp(conditionalAppEntity: ConditionalAppEntity)

    @Update
    suspend fun updateConditionalApp(conditionalAppEntity: ConditionalAppEntity)

    @Query("SELECT * FROM conditional_apps")
    suspend fun getAll(): List<ConditionalAppEntity>

    @Query("DELETE FROM conditional_apps WHERE package_name = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)

    @Query("SELECT * FROM conditional_apps WHERE package_name = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): ConditionalAppEntity?
}