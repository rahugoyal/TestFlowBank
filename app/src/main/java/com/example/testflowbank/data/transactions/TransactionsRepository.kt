package com.example.testflowbank.data.transactions

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionsRepository @Inject constructor() {

    private val items: List<TransactionItem> = buildFakeTransactions()

    suspend fun getAll(): List<TransactionItem> = items

    suspend fun getRecent(limit: Int): List<TransactionItem> =
        items.sortedByDescending { it.dateTime }.take(limit)

    private fun buildFakeTransactions(): List<TransactionItem> {
        val now = LocalDateTime.now()

        return listOf(
            TransactionItem(
                id = 1,
                dateTime = now.minusDays(1),
                amount = 499.0,
                type = TransactionType.DEBIT,
                category = TransactionCategory.ENTERTAINMENT,
                description = "DTH recharge",
                reference = "TataSky#12345",
                accountMasked = "XXXX 1234"
            ),
            TransactionItem(
                id = 2,
                dateTime = now.minusDays(2),
                amount = 8500.0,
                type = TransactionType.DEBIT,
                category = TransactionCategory.RENT,
                description = "Flat rent - Nov",
                reference = "Rent#A-204",
                accountMasked = "XXXX 5678"
            ),
            TransactionItem(
                id = 3,
                dateTime = now.minusDays(3),
                amount = 1200.0,
                type = TransactionType.DEBIT,
                category = TransactionCategory.UTILITIES,
                description = "BESCOM electricity bill",
                reference = "BESCOM#9988",
                accountMasked = "XXXX 1234"
            ),
            TransactionItem(
                id = 4,
                dateTime = now.minusDays(4),
                amount = 52000.0,
                type = TransactionType.CREDIT,
                category = TransactionCategory.SALARY,
                description = "Salary credit",
                reference = "Company Pvt Ltd",
                accountMasked = "XXXX 1234"
            ),
            TransactionItem(
                id = 5,
                dateTime = now.minusDays(5),
                amount = 1499.0,
                type = TransactionType.DEBIT,
                category = TransactionCategory.SHOPPING,
                description = "Online shopping",
                reference = "Amazon#8899",
                accountMasked = "XXXX 5678"
            )
        )
    }
}