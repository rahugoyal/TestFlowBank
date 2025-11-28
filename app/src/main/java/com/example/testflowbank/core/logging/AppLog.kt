package com.example.testflowbank.core.logging

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class AppLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val sessionId: Long,
    val screen: String?,
    val action: String?,
    val api: String?,
    val type: String,
    val message: String,
    val stackTrace: String?
)