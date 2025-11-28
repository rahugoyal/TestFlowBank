package com.example.testflowbank.data.transactions

import java.time.LocalDateTime

enum class TransactionType {
    DEBIT,      // money out
    CREDIT      // money in
}

enum class TransactionCategory {
    GROCERIES,
    RENT,
    UTILITIES,
    SHOPPING,
    ENTERTAINMENT,
    SALARY,
    OTHER
}

data class TransactionItem(
    val id: Long,
    val dateTime: LocalDateTime,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val description: String,
    val reference: String,        // e.g. UPI ID, card last-4, biller name
    val accountMasked: String     // e.g. "XXXX1234"
)