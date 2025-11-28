package com.example.testflowbank

import android.app.Application
import com.example.testflowbank.core.crash.CrashReportingExceptionHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TestFlowBankApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global Java/Kotlin crash handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            CrashReportingExceptionHandler(
                defaultHandler = defaultHandler,
                appContext = this
            )
        )

        // 👇 ANR Watchdog to log UI stalls
        com.example.testflowbank.core.anr.AnrWatchdog(
            timeoutMs = 5000L,
            appContext = this
        ).start()
    }
}