package com.example.testflowbank.core.logging

import com.example.testflowbank.core.session.SessionManager
import com.example.testflowbank.core.util.CurrentScreenTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppLogger @Inject constructor(
    private val dao: AppLogDao,
    private val sessionManager: SessionManager
) {

    /**
     * Base logging helper – all logs go through this.
     */
    private suspend fun log(
        type: String,
        message: String,
        action: String? = null,
        api: String? = null,
        throwable: Throwable? = null
    ) = withContext(Dispatchers.IO) {
        dao.insert(
            AppLog(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionManager.currentSessionId(),
                screen = CurrentScreenTracker.currentScreen,
                action = action,
                api = api,
                type = type,
                message = throwable?.message ?: message,
                stackTrace = throwable?.stackTraceToString()
            )
        )
    }

    // ---------- Generic log helpers ----------

    suspend fun info(
        message: String,
        action: String? = null,
        api: String? = null
    ) = log(type = "INFO", message = message, action = action, api = api)

    suspend fun error(
        message: String,
        api: String? = null,
        throwable: Throwable? = null
    ) = log(
        type = "ERROR",
        message = message,
        api = api,
        throwable = throwable
    )

    suspend fun api(
        message: String,
        api: String? = null
    ) = log(type = "API", message = message, api = api)


    suspend fun screenView() =
        info(
            message = "Screen viewed",
            action = "SCREEN_VIEW"
        )

    suspend fun journeyStep(
        step: String,
        detail: String? = null
    ) = info(
        message = buildString {
            append("JOURNEY_STEP; step=")
            append(step)
            if (!detail.isNullOrBlank()) {
                append("; detail=")
                append(detail)
            }
        },
        action = "JOURNEY_STEP"
    )

    suspend fun paymentResult(
        paymentType: String,     // e.g. DTH, CREDIT_CARD
        paymentStatus: String,   // e.g. SUCCESS, FAILED
        paymentAmount: String?,  // e.g. "499"
        title: String,           // human label
        scenario: String,        // e.g. SERVER_ERROR, SUCCESS
        httpCode: Int?          // 200, 400, 500...

    ) = log(
        type = "PAYMENT",
        message = buildString {
            append("PAYMENT_RESULT; ")
            append("payment_type=")
            append(paymentType)
            append("; payment_status=")
            append(paymentStatus)
            append("; payment_title=")
            append(title)
            if (!paymentAmount.isNullOrBlank()) {
                append("; payment_amount=")
                append(paymentAmount)
            }
            append("; scenario=")
            append(scenario)
            append("; http_code=")
            append(httpCode ?: -1)
        },
        action = "PAYMENT_RESULT",
        api = "simulatePayment"
    )
}