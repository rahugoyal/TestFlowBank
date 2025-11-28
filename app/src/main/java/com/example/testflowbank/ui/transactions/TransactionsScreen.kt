package com.example.testflowbank.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.testflowbank.core.util.CurrentScreenTracker
import com.example.testflowbank.data.transactions.TransactionItem
import com.example.testflowbank.data.transactions.TransactionType
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        CurrentScreenTracker.currentScreen = "TransactionsScreen"
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transactions") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TransactionsTabRow(
                activeTab = state.activeTab,
                onTabSelected = viewModel::onTabSelected,
                onInsightsTabClick = { viewModel.onShowInsightsClicked() }
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    when (state.activeTab) {
                        TransactionsScreenTab.LIST ->
                            TransactionsListTab(
                                items = state.items,
                                onItemClick = { id -> viewModel.onTransactionClicked(id) }
                            )

                        TransactionsScreenTab.DETAIL ->
                            TransactionsDetailTab(
                                item = state.selected
                            )

                        TransactionsScreenTab.INSIGHTS ->
                            TransactionsInsightsTab(
                                insights = state.insights
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTabRow(
    activeTab: TransactionsScreenTab,
    onTabSelected: (TransactionsScreenTab) -> Unit,
    onInsightsTabClick: () -> Unit
) {
    val tabs = listOf(
        TransactionsScreenTab.LIST to "List",
        TransactionsScreenTab.DETAIL to "Detail",
        TransactionsScreenTab.INSIGHTS to "Insights"
    )

    val icons = mapOf(
        TransactionsScreenTab.LIST to Icons.AutoMirrored.Filled.List,
        TransactionsScreenTab.DETAIL to Icons.AutoMirrored.Default.ReceiptLong,
        TransactionsScreenTab.INSIGHTS to Icons.Default.BarChart
    )

    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)
    ) {
        tabs.forEachIndexed { index, (tab, title) ->
            Tab(
                selected = tab == activeTab,
                onClick = {
                    if (tab == TransactionsScreenTab.INSIGHTS) {
                        onInsightsTabClick()
                    } else {
                        onTabSelected(tab)
                    }
                },
                text = { Text(title) },
                icon = {
                    Icon(
                        imageVector = icons[tab]!!,
                        contentDescription = title
                    )
                }
            )
        }
    }
}

@Composable
private fun TransactionsListTab(
    items: List<TransactionItem>,
    onItemClick: (Long) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transactions found for this session.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            TransactionListItem(
                item = item,
                onClick = { onItemClick(item.id) }
            )
        }
    }
}

@Composable
private fun TransactionListItem(
    item: TransactionItem,
    onClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colored circle by type
            val color = if (item.type == TransactionType.DEBIT)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item.type == TransactionType.DEBIT) "-" else "+",
                    color = color,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.category.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.dateTime.format(formatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatAmount(item.amount, item.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.accountMasked,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionsDetailTab(
    item: TransactionItem?
) {
    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a transaction from the list to see details.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Transaction Detail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = formatAmount(item.amount, item.type),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                DetailRow("Type", item.type.name)
                DetailRow("Category", item.category.name)
                DetailRow("Date & Time", item.dateTime.format(formatter))
                DetailRow("Account", item.accountMasked)
                DetailRow("Reference", item.reference)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TransactionsInsightsTab(
    insights: TransactionsInsights
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Insights (this session)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightsCard(
                title = "Total Debit",
                value = formatPlainAmount(insights.totalDebit),
                isDebit = true,
                modifier = Modifier.weight(1f)
            )
            InsightsCard(
                title = "Total Credit",
                value = formatPlainAmount(insights.totalCredit),
                isDebit = false,
                modifier = Modifier.weight(1f)
            )
        }

        InsightsCard(
            title = "Top Spend Category",
            value = insights.topCategory?.name ?: "None",
            subtitle = if (insights.topCategory != null)
                "≈ ${formatPlainAmount(insights.topCategoryAmount)} spent"
            else
                "No debit transactions",
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InsightsCard(
                title = "Largest Debit",
                value = insights.largestDebit?.let { formatPlainAmount(it.amount) } ?: "N/A",
                subtitle = insights.largestDebit?.category?.name,
                isDebit = true,
                modifier = Modifier.weight(1f)
            )
            InsightsCard(
                title = "Largest Credit",
                value = insights.largestCredit?.let { formatPlainAmount(it.amount) } ?: "N/A",
                subtitle = insights.largestCredit?.description,
                isDebit = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InsightsCard(
    title: String,
    value: String,
    subtitle: String? = null,
    isDebit: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val tint = when (isDebit) {
        true -> MaterialTheme.colorScheme.error
        false -> MaterialTheme.colorScheme.primary
        null -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- small helpers ---

private fun formatAmount(amount: Double, type: TransactionType): String {
    val prefix = if (type == TransactionType.DEBIT) "−₹" else "+₹"
    return prefix + formatPlainAmount(amount)
}

private fun formatPlainAmount(amount: Double): String {
    // simple formatting without locale grouping to avoid complexity
    return if (amount % 1.0 == 0.0) {
        amount.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", amount)
    }
}