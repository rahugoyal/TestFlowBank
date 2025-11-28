package com.example.testflowbank.core.crash

import com.example.testflowbank.core.util.CurrentScreenTracker

class GlobalCrashHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {

        // Log the crash safely
        CrashLogger.log(
            throwable = throwable,
            screen = CurrentScreenTracker.currentScreen
        )

        // Allow Android to crash normally
        defaultHandler?.uncaughtException(thread, throwable)
    }
}