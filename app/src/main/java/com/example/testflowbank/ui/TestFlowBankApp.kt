package com.example.testflowbank.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testflowbank.ui.assistant.AssistantScreen
import com.example.testflowbank.ui.crashlab.CrashLabScreen
import com.example.testflowbank.ui.dashboard.DashboardScreen
import com.example.testflowbank.ui.logs.LogsScreen
import com.example.testflowbank.ui.login.LoginScreen
import com.example.testflowbank.ui.navigation.NavRoutes
import com.example.testflowbank.ui.payments.PaymentsScreen
import com.example.testflowbank.ui.profile.ProfileScreen
import com.example.testflowbank.ui.scenarios.ScenariosScreen
import com.example.testflowbank.ui.transactions.TransactionsScreen
import kotlin.Unit

@Composable
fun TestFlowBankApp() {
    val navController = rememberNavController()

    MaterialTheme {
        Surface {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.LOGIN
            ) {
                composable(NavRoutes.LOGIN) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(NavRoutes.DASHBOARD) {
                                popUpTo(NavRoutes.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }

                composable(NavRoutes.DASHBOARD) {
                    DashboardScreen(
                        onGoToTransactions = { navController.navigate(NavRoutes.TRANSACTIONS) },
                        onGoToPayments = { navController.navigate(NavRoutes.PAYMENTS) },
                        onGoToProfile = { navController.navigate(NavRoutes.PROFILE) },
                        onGoToLogs = { navController.navigate(NavRoutes.LOGS) },
                        onGoToAssistant = { navController.navigate(NavRoutes.ASSISTANT) },
                        onGoToCrashLab = { navController.navigate(NavRoutes.CRASHLAB)},
                        onGoToScenarios = { navController.navigate(NavRoutes.SCENARIOS) }
                    )
                }

                composable(NavRoutes.TRANSACTIONS) { TransactionsScreen() }
                composable(NavRoutes.SCENARIOS) {
                    ScenariosScreen { type ->
                        navController.navigate("${NavRoutes.PAYMENTS}?scenario=${type.name}")
                    }
                }

                composable(
                    route = "${NavRoutes.PAYMENTS}?scenario={scenario}",
                    arguments = listOf(
                        navArgument("scenario") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    )
                ) {
                    PaymentsScreen()
                }
                composable(NavRoutes.PROFILE) { ProfileScreen() }
                composable(NavRoutes.LOGS) { LogsScreen() }
                composable(NavRoutes.ASSISTANT) { AssistantScreen() }
                composable(NavRoutes.CRASHLAB) { CrashLabScreen() }
            }
        }
    }
}