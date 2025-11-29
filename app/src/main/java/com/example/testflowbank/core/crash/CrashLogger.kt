package com.example.testflowbank.core.crash

import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.core.logging.AppLogDatabaseBuilderHolder
import com.example.testflowbank.core.util.AppContextProvider

object CrashLogger {
    fun log(
        throwable: Throwable,
        screen: String? = null,
        action: String? = null
    ) {
        try {
            val context = AppContextProvider.context
            val db = AppLogDatabaseBuilderHolder.getDb(context)
            val dao = db.appLogDao()

            val log: AppLog = CrashLogFactory.fromThrowable(
                throwable = throwable,
                screen = screen,
                action = action
            )
            dao.insertSync(log)
        } catch (_: Throwable) {
        }
    }
}