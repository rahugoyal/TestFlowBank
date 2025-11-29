package com.example.testflowbank.core.crash

import com.example.testflowbank.core.util.CurrentScreenTracker

class GlobalCrashHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        CrashLogger.log(
            throwable = throwable,
            screen = CurrentScreenTracker.currentScreen,
            action = "UncaughtException"
        )
        defaultHandler?.uncaughtException(thread, throwable)
    }
}