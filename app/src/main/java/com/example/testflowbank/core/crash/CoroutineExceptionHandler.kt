package com.example.testflowbank.core.crash

import com.example.testflowbank.core.util.CurrentScreenTracker
import kotlinx.coroutines.CoroutineExceptionHandler

val GlobalCoroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
    CrashLogger.log(
        throwable = throwable,
        screen = CurrentScreenTracker.currentScreen,
        action = "CoroutineException"
    )
}