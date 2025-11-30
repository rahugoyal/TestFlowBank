package com.example.testflowbank.data.logs

import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.core.logging.AppLogDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val dao: AppLogDao
) {
    suspend fun getLatest(limit: Int = 400): List<AppLog> = dao.getLatest(limit)

    suspend fun getNewerForSession(
        afterId: Long
    ): List<AppLog> =
        dao.getNewerForSession(afterId)
}
