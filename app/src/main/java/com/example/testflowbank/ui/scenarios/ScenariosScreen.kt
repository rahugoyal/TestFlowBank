package com.example.testflowbank.ui.scenarios

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenariosScreen(
    onNavigateForScenario: (ScenarioType) -> Unit
) {
    val scenarios = listOf(
        TestScenario(
            id = 1,
            title = "Happy Path Payment",
            description = "One clean successful Electricity Bill payment.",
            type = ScenarioType.HAPPY_PAYMENTS
        ),
        TestScenario(
            id = 2,
            title = "Mixed Payments",
            description = "Success + failure + server error + slow success.",
            type = ScenarioType.MIXED_PAYMENTS
        ),
        TestScenario(
            id = 3,
            title = "Crash During Payments",
            description = "Trigger a controlled crash on Payments screen.",
            type = ScenarioType.CRASH_ON_PAYMENTS
        ),
        TestScenario(
            id = 4,
            title = "Slow Network Payment",
            description = "Simulate a slow API (5 seconds).",
            type = ScenarioType.SLOW_NETWORK_PAYMENT
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Scenarios") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(scenarios.size) { i ->
                val item = scenarios[i]
                ScenarioCard(
                    title = item.title,
                    desc = item.description,
                    onClick = { onNavigateForScenario(item.type) }
                )
            }
        }
    }
}

@Composable
fun ScenarioCard(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}