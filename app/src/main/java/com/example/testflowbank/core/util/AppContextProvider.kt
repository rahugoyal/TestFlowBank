package com.example.testflowbank.core.util

import android.annotation.SuppressLint
import android.content.Context

object AppContextProvider {

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context
        private set

    fun init(appContext: Context) {
        context = appContext
    }
}