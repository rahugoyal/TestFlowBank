package com.example.testflowbank.core.logging

import com.example.testflowbank.core.session.SessionManager
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
        screen: String? = null,
        action: String? = null,
        api: String? = null
    ) = withContext(Dispatchers.IO) {
        dao.insert(
            AppLog(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionManager.currentSessionId(),   // 👈 current session
                screen = screen,
                action = action,
                api = api,
                type = type,
                message = message
            )
        )
    }

    // ---------- Generic log helpers ----------

    suspend fun info(
        message: String,
        screen: String? = null,
        action: String? = null,
        api: String? = null
    ) = log("INFO", message, screen, action, api)

    suspend fun error(
        message: String,
        screen: String? = null,
        api: String? = null
    ) = log("ERROR", message, screen, api = api)

    suspend fun api(
        message: String,
        screen: String? = null,
        api: String? = null
    ) = log("API", message, screen, api = api)

    // ---------- Journey helpers (navigation / actions) ----------

    suspend fun screenView(screen: String) =
        info(
            message = "Screen viewed",
            screen = screen,
            action = "SCREEN_VIEW"
        )

    suspend fun journeyStep(
        screen: String,
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
        screen = screen,
        action = "JOURNEY_STEP"
    )

    // ---------- Crash / severe error helper ----------

    suspend fun crash(
        message: String,
        screen: String? = null,
        throwable: Throwable? = null
    ) {
        val combinedMessage = buildString {
            if (message.isNotBlank()) {
                append(message)
            }
            if (throwable != null) {
                if (isNotEmpty()) append("\n\n")
                append("Exception=")
                append(throwable.javaClass.name)
                append("; message=")
                append(throwable.message)
                append("\nStacktrace:\n")
                append(throwable.stackTraceToString())
            }
        }

        log(
            type = "CRASH",
            message = combinedMessage.ifBlank { "Crash reported" },
            screen = screen,
            action = if (throwable == null) "MANUAL_CRASH" else "HANDLED_CRASH"
        )
    }

    // ---------- Structured payment result helper ----------

    suspend fun paymentResult(
        paymentType: String,     // e.g. DTH, CREDIT_CARD
        paymentStatus: String,   // e.g. SUCCESS, FAILED
        paymentAmount: String?,  // e.g. "499"
        title: String,           // human label
        scenario: String,        // e.g. SERVER_ERROR, SUCCESS
        httpCode: Int?,          // 200, 400, 500...
        screen: String = "Payments"
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
        screen = screen,
        action = "PAYMENT_RESULT",
        api = "simulatePayment"
    )
}