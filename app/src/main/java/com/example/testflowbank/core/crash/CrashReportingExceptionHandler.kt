package com.example.testflowbank.core.crash

import android.content.Context
import com.example.testflowbank.core.logging.AppLog

class CrashReportingExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val appContext: Context
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val db = AppLogDatabaseBuilderHolder.getDb(appContext)
            val dao = db.appLogDao()

            val crashLog: AppLog = CrashLogFactory.fromThrowable(
                thread = thread,
                throwable = throwable,
                screen = null // if you ever track current screen, pass it here,
            )

            // IMPORTANT: synchronous write
            dao.insertSync(crashLog)

        } catch (_: Throwable) {
            // Always swallow errors here — you're crashing anyway
        }

        // let Android handle the crash normally
        defaultHandler?.uncaughtException(thread, throwable)
    }
}