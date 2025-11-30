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

    // AppLogger.kt (only helpers shown, keep your existing log() as-is)

    // 1) Generic info
    suspend fun info(
        message: String,
        action: String? = null,
        api: String? = null
    ) = log(
        type = "INFO",
        message = message,
        api = api,
        action = action
    )

    // 2) Error / failure (for errors + handled crashes, etc.)
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

    // 3) API lifecycle logging
    suspend fun apiStart(
        api: String,
        detail: String? = null
    ) = log(
        type = "API",
        message = buildString {
            append("API_CALL; api=")
            append(api)
            append("; phase=START")
            if (!detail.isNullOrBlank()) {
                append("; detail=")
                append(detail)
            }
        },
        api = api,
        action = "API_START"
    )

    suspend fun apiSuccess(
        api: String,
        httpCode: Int,
        durationMs: Long? = null,
        detail: String? = null
    ) = log(
        type = "API",
        message = buildString {
            append("API_CALL; api=")
            append(api)
            append("; phase=END; result=SUCCESS; http_code=")
            append(httpCode)
            if (durationMs != null) {
                append("; duration_ms=")
                append(durationMs)
            }
            if (!detail.isNullOrBlank()) {
                append("; detail=")
                append(detail)
            }
        },
        api = api,
        action = "API_RESULT"
    )

    suspend fun apiFailure(
        api: String,
        httpCode: Int?,
        throwable: Throwable? = null,
        detail: String? = null
    ) = log(
        type = "API",
        message = buildString {
            append("API_CALL; api=")
            append(api)
            append("; phase=END; result=FAILED")
            if (httpCode != null) {
                append("; http_code=")
                append(httpCode)
            }
            if (!detail.isNullOrBlank()) {
                append("; detail=")
                append(detail)
            }
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
        action = "API_RESULT"
    )

    // 4) Screen navigation / journey
    suspend fun screenView() =
        info(
            message = "USER_JOURNEY; event=SCREEN_VIEW; screen=${CurrentScreenTracker.currentScreen}",
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

    suspend fun userAction(
        actionName: String,
        target: String,
        detail: String? = null
    ) = info(
        message = buildString {
            append("USER_ACTION; action=")
            append(actionName)
            append("; target=")
            append(target)
            if (!detail.isNullOrBlank()) {
                append("; detail=")
                append(detail)
            }
        },
        action = "USER_ACTION"
    )

    // 5) Payments (SUCCESS / FAILED)
    suspend fun paymentResult(
        paymentType: String,     // DTH, CREDIT_CARD, etc.
        status: String,          // SUCCESS or FAILED
        amount: String?,         // normalized "499"
        contextLabel: String,    // "DTH Recharge", etc.
        scenario: String,        // e.g. SERVER_ERROR / SUCCESS
        httpCode: Int?,
        api: String = "simulatePayment"
    ) = log(
        type = "PAYMENT",
        message = buildString {
            append("PAYMENT_RESULT; payment_type=")
            append(paymentType)
            append("; result=")
            append(status)
            if (!amount.isNullOrBlank()) {
                append("; amount=")
                append(amount)
            }
            append("; label=")
            append(contextLabel)
            append("; scenario=")
            append(scenario)
            append("; http_code=")
            append(httpCode ?: -1)
        },
        api = api,
        action = "PAYMENT_RESULT"
    )

    // 6) Assistant question/answer (optional but very useful)
    suspend fun assistantQuestion(
        question: String
    ) = info(
        message = "ASSISTANT_Q; text=$question",
        action = "ASSISTANT_Q"
    )

    suspend fun assistantAnswer(
        answer: String
    ) = info(
        message = "ASSISTANT_A; text=$answer",
        action = "ASSISTANT_A"
    )
}