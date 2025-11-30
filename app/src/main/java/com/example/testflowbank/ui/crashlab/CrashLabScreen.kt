package com.example.testflowbank.ui.crashlab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.testflowbank.core.util.CurrentScreenTracker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLabScreen(
    viewModel: CrashLabViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        CurrentScreenTracker.currentScreen = "CrashLabScreen"
    }

    val scenarios = listOf(
        CrashScenario.NPE_CRASH,
        CrashScenario.ILLEGAL_STATE_CRASH,
        CrashScenario.ANR_FREEZE,
        CrashScenario.JSON_PARSE_ERROR
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crash Lab") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (state.lastAction != null || state.lastError != null) {
                StatusBanner(
                    actionText = state.lastAction,
                    errorText = state.lastError
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scenarios, key = { it.name }) { scenario ->
                    CrashScenarioCard(
                        scenario = scenario,
                        onClick = { viewModel.onScenarioClick(scenario) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    actionText: String?,
    errorText: String?
) {
    val bgColor = if (errorText == null) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val icon = if (errorText == null) {
        Icons.Default.Bolt
    } else {
        Icons.Default.ErrorOutline
    }

    val label = if (errorText == null) "Last action" else "Last error"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = errorText ?: actionText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CrashScenarioCard(
    scenario: CrashScenario,
    onClick: () -> Unit
) {
    val (title, description, icon) = when (scenario) {
        CrashScenario.NPE_CRASH -> Triple(
            "NullPointerException crash",
            "Force an uncaught NPE on main thread. Crash handler should log it as CRASH.",
            Icons.Default.BugReport
        )

        CrashScenario.ILLEGAL_STATE_CRASH -> Triple(
            "IllegalStateException crash",
            "Force an uncaught IllegalStateException. Good for checking crash reporting.",
            Icons.Default.ErrorOutline
        )

        CrashScenario.ANR_FREEZE -> Triple(
            "Simulate ANR freeze",
            "Block main thread for ~8 seconds so the ANR watchdog logs an ANR event.",
            Icons.Default.HourglassBottom
        )

        CrashScenario.JSON_PARSE_ERROR -> Triple(
            "Handled JSON parse error",
            "Throw and catch a JSON parsing error. App does not crash; error is logged only.",
            Icons.Default.Bolt
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Run",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}