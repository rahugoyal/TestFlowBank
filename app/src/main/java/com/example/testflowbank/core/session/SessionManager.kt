package com.example.testflowbank.core.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    // One logical session per app process by default.
    @Volatile
    private var currentId: Long = System.currentTimeMillis()

    fun currentSessionId(): Long = currentId

    fun startNewSession(): Long {
        currentId = System.currentTimeMillis()
        return currentId
    }
}