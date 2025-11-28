package com.example.testflowbank.core.util

object CurrentScreenTracker {
    @Volatile
    var currentScreen: String? = null
}