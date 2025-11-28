package com.example.testflowbank.ui.payments

import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.payments.PaymentItem
import com.example.testflowbank.data.payments.PaymentScenario
import com.example.testflowbank.data.payments.PaymentStatus
import com.example.testflowbank.data.payments.PaymentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val isGlobalLoading: Boolean = false,
    val items: List<PaymentItem> = emptyList(),
    val globalError: String? = null
)

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val repo: PaymentsRepository,
    private val logger: AppLogger
) : ViewModel() {

    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )
    init {
        vmScope.launch {
            // Log entry into Transactions module
            logger.screenView()
            logger.journeyStep(
                step = "OPEN_Payments",
                detail = "User opened Payments module"
            )
        }
    }

    private val _state = MutableStateFlow(
        PaymentsUiState(
            items = listOf(
                PaymentItem(
                    id = 1,
                    title = "Electricity Bill",
                    description = "BESCOM - Home",
                    amount = "₹1,200",
                    scenario = PaymentScenario.SUCCESS
                ),
                PaymentItem(
                    id = 2,
                    title = "Credit Card Payment",
                    description = "Visa **** 4321",
                    amount = "₹8,500",
                    scenario = PaymentScenario.CLIENT_ERROR  // will likely fail (400)
                ),
                PaymentItem(
                    id = 3,
                    title = "House Rent",
                    description = "Monthly rent",
                    amount = "₹18,000",
                    scenario = PaymentScenario.SERVER_ERROR  // will likely fail (500)
                ),
                PaymentItem(
                    id = 4,
                    title = "DTH Recharge",
                    description = "Set-top box",
                    amount = "₹499",
                    scenario = PaymentScenario.SLOW_SUCCESS  // slow 200
                )
            )
        )
    )
    val state = _state.asStateFlow()

    fun pay(itemId: Int) {
        val current = _state.value
        val item = current.items.find { it.id == itemId } ?: return

        vmScope.launch {
            // mark this item as PROCESSING
            _state.value = current.copy(
                isGlobalLoading = true,
                globalError = null,
                items = current.items.map {
                    if (it.id == itemId) it.copy(status = PaymentStatus.PROCESSING)
                    else it
                }
            )

            val apiDesc = when (item.scenario) {
                PaymentScenario.SUCCESS -> "status=200"
                PaymentScenario.CLIENT_ERROR -> "status=400"
                PaymentScenario.SERVER_ERROR -> "status=500"
                PaymentScenario.SLOW_SUCCESS -> "delay/5"
            }

            // Attempt log
            logger.api(
                message = "PAYMENT_ATTEMPT; " +
                        "payment_title=${item.title}; " +
                        "payment_amount=${canonicalAmount(item.amount)}; " +
                        "scenario=${item.scenario.name}; " +
                        "api_desc=$apiDesc",
                api = "simulatePayment"
            )

            try {
                val res = repo.simulatePayment(item.scenario)
                val httpCode = res.code()
                val newStatus: PaymentStatus
                val msg: String

                if (res.isSuccessful) {
                    newStatus = PaymentStatus.SUCCESS
                    msg = "Success (code=$httpCode)"

                    logger.info(
                        message = "Payment success for ${item.title}: $msg"
                    )

                    // ✅ Structured PAYMENT_RESULT log
                    logger.paymentResult(
                        paymentType = canonicalPaymentType(item),
                        paymentStatus = "SUCCESS",
                        paymentAmount = canonicalAmount(item.amount),
                        title = item.title,
                        scenario = item.scenario.name,
                        httpCode = httpCode
                    )
                } else {
                    newStatus = PaymentStatus.FAILED
                    msg = "Failed (code=$httpCode)"

                    logger.error(
                        message = "Payment failed for ${item.title}: $msg"
                    )

                    // ✅ Structured PAYMENT_RESULT log
                    logger.paymentResult(
                        paymentType = canonicalPaymentType(item),
                        paymentStatus = "FAILED",
                        paymentAmount = canonicalAmount(item.amount),
                        title = item.title,
                        scenario = item.scenario.name,
                        httpCode = httpCode
                    )
                }

                val updatedItems = _state.value.items.map {
                    if (it.id == itemId) it.copy(
                        status = newStatus,
                        lastMessage = msg
                    ) else it
                }

                _state.value = _state.value.copy(
                    isGlobalLoading = false,
                    items = updatedItems,
                    globalError = null
                )
            } catch (t: Throwable) {
                val msg = "Exception: ${t.message}"

                logger.error(
                    message = "Payment exception for ${item.title}: $msg",
                    throwable = t
                )

                // ✅ Structured PAYMENT_RESULT log for exception case
                logger.paymentResult(
                    paymentType = canonicalPaymentType(item),
                    paymentStatus = "FAILED",
                    paymentAmount = canonicalAmount(item.amount),
                    title = item.title,
                    scenario = item.scenario.name,
                    httpCode = null
                )

                val updatedItems = _state.value.items.map {
                    if (it.id == itemId) it.copy(
                        status = PaymentStatus.FAILED,
                        lastMessage = msg
                    ) else it
                }

                _state.value = _state.value.copy(
                    isGlobalLoading = false,
                    items = updatedItems,
                    globalError = msg
                )
            }
        }
    }

    /**
     * Map PaymentItem to a canonical payment_type string for the logs,
     * so the model can talk about "DTH", "CREDIT_CARD", etc.
     */
    private fun canonicalPaymentType(item: PaymentItem): String =
        when (item.id) {
            1 -> "ELECTRICITY"
            2 -> "CREDIT_CARD"
            3 -> "RENT"
            4 -> "DTH"
            else -> item.title.uppercase().replace(" ", "_")
        }

    /**
     * Normalize the amount text like "₹1,200" → "1200"
     * so logs are easier to parse for the model.
     */
    private fun canonicalAmount(amountText: String): String =
        amountText
            .replace("₹", "")
            .replace(",", "")
            .trim()
}