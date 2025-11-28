package com.example.testflowbank.core.crash

import com.example.testflowbank.core.logging.AppLog

object CrashLogFactory {
    fun fromThrowable(
        throwable: Throwable,
        screen: String?
    ): AppLog {

        return AppLog(
            timestamp = System.currentTimeMillis(),
            message = throwable.message ?: "Unknown Crash",
            stackTrace = throwable.stackTraceToString(),
            screen = screen,
            type = "CRASH",
            sessionId = 0L,
            action = null,
            api = null
        )
    }
}