package com.example.testflowbank.core.crash

import com.example.testflowbank.core.logging.AppLog
object CrashLogFactory {
    fun fromThrowable(
        throwable: Throwable,
        screen: String?,
        action: String?
    ): AppLog {
        return AppLog(
            timestamp = System.currentTimeMillis(),
            message = throwable.message ?: throwable.toString(),
            stackTrace = throwable.stackTraceToString(),
            screen = screen,
            type = "ERROR",
            action = "$action DETECTED",
            api = null,
            exception = throwable.javaClass.name
        )
    }
}