package com.example.testflowbank.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.testflowbank.core.util.CurrentScreenTracker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    val logs = viewModel.logs.collectAsState().value
    val df = rememberDateFormatter()

    LaunchedEffect(Unit) {
        CurrentScreenTracker.currentScreen = "LogsScreen"
    }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Logs") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(logs) { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${df.format(Date(log.timestamp))} — ${log.type}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text("Screen: ${log.screen ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    log.api?.let {
                        Text("API: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(log.message, style = MaterialTheme.typography.bodyMedium)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())