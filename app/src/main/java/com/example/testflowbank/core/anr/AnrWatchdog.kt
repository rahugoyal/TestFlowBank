package com.example.testflowbank.core.anr

import android.os.Handler
import android.os.Looper
import com.example.testflowbank.core.logging.AppLogDatabaseBuilderHolder
import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.core.util.CurrentScreenTracker

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

            val anrLog = AppLog(
                timestamp = detectedAt,
                screen = CurrentScreenTracker.currentScreen,
                action = "ANR DETECTED",
                api = null,
                type = "ERROR",
                message = "Main thread unresponsive for >= ${timeoutMs}ms",
                stackTrace = stacktrace,
                exception = "ANR Exception"
            )

            dao.insertSync(anrLog)
        } catch (_: Throwable) {
        }
    }
}