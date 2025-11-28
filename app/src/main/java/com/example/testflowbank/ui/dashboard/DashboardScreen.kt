package com.example.testflowbank.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGoToTransactions: () -> Unit,
    onGoToPayments: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToLogs: () -> Unit,
    onGoToAssistant: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Dashboard") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (state.isLoading) {
                        Text("Loading summary...")
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Welcome back,", style = MaterialTheme.typography.labelMedium)
                        Text(state.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Email: ${state.email}")
                        Spacer(Modifier.height(4.dp))
                        Text("Total (dummy cart): ${state.totalAmount}")
                        state.error?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Text("Quick actions", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGoToTransactions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Transactions")
                }
                OutlinedButton(
                    onClick = onGoToPayments,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Payments")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGoToProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Profile")
                }
                OutlinedButton(
                    onClick = onGoToLogs,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Logs")
                }
            }

            OutlinedButton(
                onClick = onGoToAssistant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ask AI (later)")
            }
        }
    }
}