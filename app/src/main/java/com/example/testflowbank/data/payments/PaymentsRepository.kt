package com.example.testflowbank.data.payments

import javax.inject.Inject
import javax.inject.Singleton

enum class PaymentScenario {
    SUCCESS, CLIENT_ERROR, SERVER_ERROR, SLOW_SUCCESS
}

// data/payments/PaymentsRepository.kt
@Singleton
class PaymentsRepository @Inject constructor(
    private val api: PaymentsApi
) {
    suspend fun simulatePayment(scenario: PaymentScenario) = api.simulatePayment(
        when (scenario) {
            PaymentScenario.SUCCESS -> "https://httpbin.org/status/200"
            PaymentScenario.CLIENT_ERROR -> "https://httpbin.org/status/400"
            PaymentScenario.SERVER_ERROR -> "https://httpbin.org/status/500"
            // 5-second delayed 200 OK
            PaymentScenario.SLOW_SUCCESS -> "https://httpbin.org/delay/5"
        }
    )
}