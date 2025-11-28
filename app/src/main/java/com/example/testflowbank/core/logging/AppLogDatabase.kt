package com.example.testflowbank.core.logging

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppLogDatabase : RoomDatabase() {
    abstract fun appLogDao(): AppLogDao
}