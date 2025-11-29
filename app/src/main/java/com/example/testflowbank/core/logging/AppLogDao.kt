package com.example.testflowbank.core.logging

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AppLogDao {

    // normal suspend logging (used everywhere else)
    @Insert
    suspend fun insert(log: AppLog)

    // synchronous logging — only for crash handler
    @Insert
    fun insertSync(log: AppLog)

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int = 400): List<AppLog>
}