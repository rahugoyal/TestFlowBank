package com.example.testflowbank.core.crash

import com.example.testflowbank.core.logging.AppLog

object CrashLogFactory {

    fun fromThrowable(
        thread: Thread,
        throwable: Throwable,
        screen: String? = null
    ): AppLog {
        val msg = buildCrashMessage(thread, throwable)

        return AppLog(
            timestamp = System.currentTimeMillis(),
            screen = screen,
            action = "UNCAUGHT_EXCEPTION",
            api = null,
            type = "CRASH",
            message = msg,
            sessionId = 0
        )
    }

    private fun buildCrashMessage(thread: Thread, throwable: Throwable): String {
        return """
            Thread: ${thread.name}
            Exception: ${throwable::class.java.name}
            Message: ${throwable.message}
            Stacktrace:
            ${throwable.stackTraceToString()}
        """.trimIndent()
    }
}