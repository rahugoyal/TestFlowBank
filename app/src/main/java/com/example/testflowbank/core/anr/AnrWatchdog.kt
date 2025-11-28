// com/example/testflowbank/core/anr/AnrWatchdog.kt
package com.example.testflowbank.core.anr

import android.os.Handler
import android.os.Looper
import com.example.testflowbank.core.crash.AppLogDatabaseBuilderHolder
import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.core.session.SessionManagerEntryPoint
import dagger.hilt.android.EntryPointAccessors

class AnrWatchdog(
    private val timeoutMs: Long = 5000L,  // 5s threshold
    private val appContext: android.content.Context
) : Thread("ANR-Watchdog") {

    @Volatile
    private var lastTick: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        isDaemon = true
    }

    override fun run() {
        while (!isInterrupted) {
            val start = System.currentTimeMillis()
            lastTick = start

            // Post a runnable to main thread; if it runs, we clear lastTick
            mainHandler.post {
                lastTick = 0L
            }

            try {
                sleep(timeoutMs)
            } catch (_: InterruptedException) {
                return
            }

            // If lastTick wasn't reset, main thread didn't process the runnable in time
            if (lastTick == start) {
                reportAnr(start)
            }
        }
    }

    private fun reportAnr(detectedAt: Long) {
        try {
            val db = AppLogDatabaseBuilderHolder.getDb(appContext)
            val dao = db.appLogDao()

            val mainThread = Looper.getMainLooper().thread
            val stacktrace = mainThread.stackTrace.joinToString("\n")
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                SessionManagerEntryPoint::class.java
            )
            val sessionId = entryPoint.sessionManager().currentSessionId()
            val anrLog = AppLog(
                timestamp = detectedAt,
                screen = null,
                action = "ANR_DETECTED",
                api = null,
                type = "ANR",
                sessionId = sessionId,
                message = "Main thread unresponsive for >= ${timeoutMs}ms.\nStacktrace:\n$stacktrace"
            )

            dao.insertSync(anrLog)
        } catch (_: Throwable) {
            // don't crash the watchdog
        }
    }
}