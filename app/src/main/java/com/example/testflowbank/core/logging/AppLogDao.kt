package com.example.testflowbank.core.logging

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AppLog)

    // For crash/ANR synchronous writes (if you use it)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(log: AppLog)

    // Latest logs across all sessions (optional but handy)
    @Query("SELECT * FROM app_logs ORDER BY id DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<AppLog>


    // ✅ Only logs newer than a given id (for fast incremental refresh)
    @Query(
        "SELECT * FROM app_logs " +
                "WHERE id > :afterId " +
                "ORDER BY id ASC"
    )
    suspend fun getNewerForSession(
        afterId: Long
    ): List<AppLog>
}