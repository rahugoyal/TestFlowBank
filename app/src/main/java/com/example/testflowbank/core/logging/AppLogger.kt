package com.example.testflowbank.core.logging

import com.example.testflowbank.core.util.CurrentScreenTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppLogger @Inject constructor(
    private val dao: AppLogDao
) {
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
                screen = CurrentScreenTracker.currentScreen,
                action = action,
                api = api,
                type = type,
                message = throwable?.let { it.message ?: it.toString() } ?: message,
                stackTrace = throwable?.stackTraceToString(),
                exception = throwable?.javaClass?.name
            )
        )
    }

    suspend fun info(
        message: String,
        action: String? = null,
        api: String? = null
    ) = log(type = "INFO", message = message, action = "USER_ACTION$action", api = api)

    suspend fun error(
        message: String,
        api: String? = null,
        throwable: Throwable? = null,
        action: String? = null
    ) = log(
        type = "ERROR",
        message = buildString {
            append("ERROR_EVENT; ")

            if (!api.isNullOrBlank()) {
                append("api=")
                append(api)
                append("; ")
            }

            append("result=FAILED; ")

            // main human message
            append("message=")
            append(message)

            if (throwable != null) {
                append("; exception=")
                append(throwable.javaClass.name)
                if (!throwable.message.isNullOrBlank()) {
                    append("; exception_message=")
                    append(throwable.message)
                }
            }
        },
        api = api,
        throwable = throwable,
        action = action
    )

    suspend fun api(
        message: String,
        api: String? = null
    ) = log(type = "API", message = message, api = api, action = "API_RESULT")


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
        paymentType: String,
        paymentStatus: String,
        paymentAmount: String?,
        title: String,
        scenario: String,
        httpCode: Int?

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