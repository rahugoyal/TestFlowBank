package com.example.testflowbank.data.payments

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}

data class PaymentItem(
    val id: Int,
    val title: String,
    val description: String,
    val amount: String,
    val scenario: PaymentScenario,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val lastMessage: String? = null
)