package com.example.testflowbank.ui.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.payments.PaymentItem
import com.example.testflowbank.data.payments.PaymentScenario
import com.example.testflowbank.data.payments.PaymentStatus
import com.example.testflowbank.data.payments.PaymentsRepository
import com.example.testflowbank.ui.scenarios.ScenarioType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private val logger: AppLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )

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

    init {
        vmScope.launch {
            // Log entry into Payments module as journey
            logger.screenView()
            logger.journeyStep(
                step = "OPEN_PAYMENTS",
                detail = "User opened Payments module"
            )

            val scenarioName: String? = savedStateHandle["scenario"]
            scenarioName?.let { name ->
                val type = runCatching { ScenarioType.valueOf(name) }.getOrNull()
                if (type != null) {
                    runScenario(type)
                }
            }
        }
    }

    fun runScenario(type: ScenarioType) {
        when (type) {
            ScenarioType.HAPPY_PAYMENTS -> runHappyPaymentsScenario()
            ScenarioType.MIXED_PAYMENTS -> runMixedPaymentsScenario()
            ScenarioType.CRASH_ON_PAYMENTS -> runCrashOnPaymentsScenario()
            ScenarioType.SLOW_NETWORK_PAYMENT -> runSlowNetworkScenario()
        }
    }

    // 1) Happy path → single successful payment (Electricity Bill, id=1)
    private fun runHappyPaymentsScenario() {
        vmScope.launch {
            logger.journeyStep(
                step = "SCENARIO_HAPPY_PAYMENTS_START",
                detail = "Running happy path payment scenario"
            )

            // just run Electricity Bill once
            pay(itemId = 1)

            logger.journeyStep(
                step = "SCENARIO_HAPPY_PAYMENTS_END",
                detail = "Finished happy path payment scenario"
            )
        }
    }

    // 2) Mixed payments → run all 4 with small gaps
    private fun runMixedPaymentsScenario() {
        vmScope.launch {
            logger.journeyStep(
                step = "SCENARIO_MIXED_PAYMENTS_START",
                detail = "Running mixed payments scenario"
            )

            // 1: Electricity (SUCCESS)
            pay(1)
            delay(1000)

            // 2: Credit Card (CLIENT_ERROR)
            pay(2)
            delay(1000)

            // 3: House Rent (SERVER_ERROR)
            pay(3)
            delay(1000)

            // 4: DTH (SLOW_SUCCESS)
            pay(4)

            logger.journeyStep(
                step = "SCENARIO_MIXED_PAYMENTS_END",
                detail = "Finished mixed payments scenario"
            )
        }
    }

    // 3) Crash on Payments → do one payment, then crash
    private fun runCrashOnPaymentsScenario() {
        vmScope.launch {
            logger.journeyStep(
                step = "SCENARIO_CRASH_ON_PAYMENTS_START",
                detail = "Running crash-on-payments scenario"
            )

            // try one payment (e.g. Credit Card)
            pay(2)
            delay(1000)

            logger.userAction(
                actionName = "CLICK_FORCE_CRASH",
                target = "PaymentsScenario",
                detail = "scenario=CRASH_ON_PAYMENTS"
            )

            logger.journeyStep(
                step = "SCENARIO_CRASH_ON_PAYMENTS_CRASH",
                detail = "Forcing IllegalStateException"
            )

            // This will be caught by your global crash handler
            throw IllegalStateException("Forced crash from PaymentsViewModel scenario")
        }
    }

    // 4) Slow network payment → just run DTH (SLOW_SUCCESS)
    private fun runSlowNetworkScenario() {
        vmScope.launch {
            logger.journeyStep(
                step = "SCENARIO_SLOW_NETWORK_START",
                detail = "Running slow-network DTH payment scenario"
            )

            pay(4)

            logger.journeyStep(
                step = "SCENARIO_SLOW_NETWORK_END",
                detail = "Finished slow-network DTH payment scenario"
            )
        }
    }
    fun pay(itemId: Int) {
        val current = _state.value
        val item = current.items.find { it.id == itemId } ?: return

        vmScope.launch {
            logger.userAction(
                actionName = "CLICK_PAY",
                target = "PaymentsItem",
                detail = "id=${item.id}; title=${item.title}; amount=${canonicalAmount(item.amount)}; scenario=${item.scenario.name}"
            )
            // mark this item as PROCESSING in UI
            _state.value = current.copy(
                isGlobalLoading = true,
                globalError = null,
                items = current.items.map {
                    if (it.id == itemId) it.copy(status = PaymentStatus.PROCESSING)
                    else it
                }
            )

            val apiName = "simulatePayment"

            val apiDesc = when (item.scenario) {
                PaymentScenario.SUCCESS -> "status=200"
                PaymentScenario.CLIENT_ERROR -> "status=400"
                PaymentScenario.SERVER_ERROR -> "status=500"
                PaymentScenario.SLOW_SUCCESS -> "delay/5"
            }

            // Log user intent + API start
            logger.journeyStep(
                step = "PAYMENT_ATTEMPT",
                detail = "title=${item.title}; scenario=${item.scenario.name}"
            )

            logger.apiStart(
                api = apiName,
                detail = "payment_title=${item.title}; payment_amount=${canonicalAmount(item.amount)}; scenario=${item.scenario.name}; api_desc=$apiDesc"
            )

            val startTime = System.currentTimeMillis()

            try {
                val res = repo.simulatePayment(item.scenario)
                val httpCode = res.code()
                val duration = System.currentTimeMillis() - startTime

                val newStatus: PaymentStatus
                val msg: String

                if (res.isSuccessful) {
                    newStatus = PaymentStatus.SUCCESS
                    msg = "Success (code=$httpCode)"

                    // API success log
                    logger.apiSuccess(
                        api = apiName,
                        httpCode = httpCode,
                        durationMs = duration,
                        detail = "payment_title=${item.title}"
                    )

                    // Optional info log
                    logger.info(
                        message = "Payment success for ${item.title}: $msg",
                        action = "PAYMENT_SUCCESS"
                    )

                    // Structured payment result for the assistant
                    logger.paymentResult(
                        paymentType = canonicalPaymentType(item),
                        status = "SUCCESS",
                        amount = canonicalAmount(item.amount),
                        contextLabel = item.title,
                        scenario = item.scenario.name,
                        httpCode = httpCode
                    )
                } else {
                    newStatus = PaymentStatus.FAILED
                    msg = "Failed (code=$httpCode)"

                    logger.apiFailure(
                        api = apiName,
                        httpCode = httpCode,
                        detail = "payment_title=${item.title}; scenario=${item.scenario.name}"
                    )

                    logger.error(
                        message = "Payment failed for ${item.title}: $msg",
                        action = "PAYMENT_FAILED"
                    )

                    logger.paymentResult(
                        paymentType = canonicalPaymentType(item),
                        status = "FAILED",
                        amount = canonicalAmount(item.amount),
                        contextLabel = item.title,
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
                val duration = System.currentTimeMillis() - startTime
                val msg = "Exception: ${t.message ?: t.toString()}"

                logger.apiFailure(
                    api = apiName,
                    httpCode = null,
                    throwable = t,
                    detail = "exception during payment for ${item.title}; duration_ms=$duration"
                )

                logger.error(
                    action = "PAYMENT_EXCEPTION",
                    message = "Payment exception for ${item.title}: $msg",
                    throwable = t
                )

                logger.paymentResult(
                    paymentType = canonicalPaymentType(item),
                    status = "FAILED",
                    amount = canonicalAmount(item.amount),
                    contextLabel = item.title,
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