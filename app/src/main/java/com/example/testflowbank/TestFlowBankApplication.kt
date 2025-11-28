package com.example.testflowbank

import android.app.Application
import com.example.testflowbank.core.crash.GlobalCrashHandler
import com.example.testflowbank.core.util.AppContextProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TestFlowBankApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Provide global app context
        AppContextProvider.init(applicationContext)

        // Global Java/Kotlin crash handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalCrashHandler(
                defaultHandler = defaultHandler
            )
        )

        // 👇 ANR Watchdog to log UI stalls
        com.example.testflowbank.core.anr.AnrWatchdog(
            timeoutMs = 5000L,
            appContext = this
        ).start()
    }
}