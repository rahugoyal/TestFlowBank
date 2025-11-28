package com.example.testflowbank.core.logging

import android.content.Context
import androidx.room.Room

object AppLogDatabaseBuilderHolder {

    @Volatile
    private var db: AppLogDatabase? = null

    fun getDb(context: Context): AppLogDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppLogDatabase::class.java,
                "logs.db"
            )
                .allowMainThreadQueries()   // REQUIRED for crash handler
                .build()
                .also { db = it }
        }
    }
}