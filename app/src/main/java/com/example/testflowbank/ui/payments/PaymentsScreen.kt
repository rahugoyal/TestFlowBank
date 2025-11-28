package com.example.testflowbank.ui.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.testflowbank.core.util.CurrentScreenTracker
import com.example.testflowbank.data.payments.PaymentItem
import com.example.testflowbank.data.payments.PaymentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: PaymentsViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState().value

    LaunchedEffect(Unit) {
        CurrentScreenTracker.currentScreen = "PaymentsScreen"
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Payments (Demo)") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (state.isGlobalLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            state.globalError?.let {
                Text(
                    text = "Global error: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    PaymentCard(
                        item = item,
                        onPayClick = { viewModel.pay(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(
    item: PaymentItem,
    onPayClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(status = item.status)

                Spacer(modifier = Modifier.weight(1f))

                val isPayEnabled = when (item.status) {
                    PaymentStatus.PENDING, PaymentStatus.FAILED -> true
                    PaymentStatus.PROCESSING, PaymentStatus.SUCCESS -> false
                }

                Button(
                    onClick = onPayClick,
                    enabled = isPayEnabled
                ) {
                    Text(
                        when (item.status) {
                            PaymentStatus.PROCESSING -> "Processing..."
                            PaymentStatus.SUCCESS -> "Paid"
                            else -> "Pay"
                        }
                    )
                }
            }

            item.lastMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: PaymentStatus) {
    val (label, color) = when (status) {
        PaymentStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        PaymentStatus.PROCESSING -> "Processing" to MaterialTheme.colorScheme.primary
        PaymentStatus.SUCCESS -> "Success" to MaterialTheme.colorScheme.tertiary
        PaymentStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}