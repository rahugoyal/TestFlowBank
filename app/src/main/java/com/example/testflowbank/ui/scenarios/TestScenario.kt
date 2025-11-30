package com.example.testflowbank.ui.scenarios

data class TestScenario(
    val id: Int,
    val title: String,
    val description: String,
    val type: ScenarioType
)

class TestScenariosRepository {
    fun getScenarios(): List<TestScenario> = listOf(
        TestScenario(
            id = 1,
            title = "Happy path payment",
            description = "Login → Dashboard → Payments → successful Electricity Bill payment.",
            type = ScenarioType.HAPPY_PAYMENTS
        ),
        TestScenario(
            id = 2,
            title = "Mixed payments (success + failures)",
            description = "Run all 4 payments: some succeed, some fail, one slow success.",
            type = ScenarioType.MIXED_PAYMENTS
        ),
        TestScenario(
            id = 3,
            title = "Crash on Payments",
            description = "Do some actions on Payments, then force a crash on that screen.",
            type = ScenarioType.CRASH_ON_PAYMENTS
        ),
        TestScenario(
            id = 4,
            title = "Slow network payment",
            description = "Simulate a slow but successful DTH payment.",
            type = ScenarioType.SLOW_NETWORK_PAYMENT
        )
    )
}