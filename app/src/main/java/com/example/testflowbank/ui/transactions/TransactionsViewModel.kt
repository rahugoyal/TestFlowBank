package com.example.testflowbank.ui.transactions

import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.transactions.TransactionItem
import com.example.testflowbank.data.transactions.TransactionType
import com.example.testflowbank.data.transactions.TransactionCategory
import com.example.testflowbank.data.transactions.TransactionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TransactionsScreenTab {
    LIST,
    DETAIL,
    INSIGHTS
}

data class TransactionsInsights(
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val topCategory: TransactionCategory? = null,
    val topCategoryAmount: Double = 0.0,
    val largestDebit: TransactionItem? = null,
    val largestCredit: TransactionItem? = null
)

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeTab: TransactionsScreenTab = TransactionsScreenTab.LIST,
    val items: List<TransactionItem> = emptyList(),
    val selected: TransactionItem? = null,
    val insights: TransactionsInsights = TransactionsInsights()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repo: TransactionsRepository,
    private val logger: AppLogger
) : ViewModel() {
    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )
    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    init {
        vmScope.launch {
            // Log entry into Transactions module
            logger.screenView()
            logger.journeyStep(
                step = "OPEN_TRANSACTIONS",
                detail = "User opened transactions module"
            )
            loadTransactions()
        }
    }

    fun onTabSelected(tab: TransactionsScreenTab) {
        if (_state.value.activeTab == tab) return
        _state.value = _state.value.copy(activeTab = tab)

        vmScope.launch {
            logger.journeyStep(
                step = "TAB_CHANGE",
                detail = "tab=${tab.name}"
            )
        }
    }

    fun onTransactionClicked(id: Long) {
        val item = _state.value.items.find { it.id == id } ?: return

        _state.value = _state.value.copy(
            selected = item,
            activeTab = TransactionsScreenTab.DETAIL
        )

        vmScope.launch {
            logger.journeyStep(
                step = "VIEW_TRANSACTION_DETAIL",
                detail = "id=$id; amount=${item.amount}; type=${item.type}; category=${item.category}"
            )
        }
    }

    fun onShowInsightsClicked() {
        _state.value = _state.value.copy(
            activeTab = TransactionsScreenTab.INSIGHTS
        )

        vmScope.launch {
            logger.journeyStep(
                step = "OPEN_INSIGHTS",
                detail = "User opened transaction insights"
            )
        }
    }

    private suspend fun loadTransactions() {
        try {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            val items = repo.getAll()

            val insights = computeInsights(items)

            _state.value = _state.value.copy(
                isLoading = false,
                items = items,
                insights = insights
            )

            logger.info(
                message = "Loaded ${items.size} transactions for this session"
            )
        } catch (t: Throwable) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to load transactions: ${t.message}"
            )

            logger.error(
                message = "Failed to load transactions: ${t.message}",
                throwable = t
            )
        }
    }

    private fun computeInsights(items: List<TransactionItem>): TransactionsInsights {
        if (items.isEmpty()) return TransactionsInsights()

        val totalDebit = items
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val totalCredit = items
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

        // Sum by category (for debits)
        val byCategory = items
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val (topCat, topCatAmt) = byCategory.maxByOrNull { it.value }
            ?.let { it.key to it.value }
            ?: (null to 0.0)

        val largestDebit = items
            .filter { it.type == TransactionType.DEBIT }
            .maxByOrNull { it.amount }

        val largestCredit = items
            .filter { it.type == TransactionType.CREDIT }
            .maxByOrNull { it.amount }

        return TransactionsInsights(
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            topCategory = topCat,
            topCategoryAmount = topCatAmt,
            largestDebit = largestDebit,
            largestCredit = largestCredit
        )
    }
}