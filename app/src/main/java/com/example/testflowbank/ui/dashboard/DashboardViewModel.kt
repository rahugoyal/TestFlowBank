package com.example.testflowbank.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.dashboard.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val email: String = "",
    val totalAmount: String = "",
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val logger: AppLogger
) : ViewModel() {
    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )
    var uiState by mutableStateOf(DashboardUiState())
        private set

    init {
        vmScope.launch {
            logger.screenView()
            logger.journeyStep(
                step = "OPEN_DASHBOARD",
                detail = "User opened Dashboard module"
            )
            loadData()
        }
    }

    fun loadData() {
        vmScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                logger.api("Dashboard user fetch", api = "GET /users/1")
                val userRes = repo.getUser()
                val cartRes = repo.getCart()

                if (userRes.isSuccessful && cartRes.isSuccessful) {
                    val user = userRes.body()
                    val cart = cartRes.body()

                    val fullName = "${user?.firstName.orEmpty()} ${user?.lastName.orEmpty()}".trim()
                    val total = cart?.total ?: 0.0

                    uiState = uiState.copy(
                        isLoading = false,
                        name = fullName.ifBlank { "Demo User" },
                        email = user?.email ?: "",
                        totalAmount = "₹$total",
                        error = null
                    )
                    logger.info("Dashboard loaded ok")
                } else {
                    val msg = "Dashboard API failed user=${userRes.code()} cart=${cartRes.code()}"
                    uiState = uiState.copy(isLoading = false, error = msg)
                    logger.error(msg)
                }
            } catch (t: Throwable) {
                val msg = "Dashboard exception: ${t.message}"
                uiState = uiState.copy(isLoading = false, error = msg)
                logger.error(msg, throwable = t)
            }
        }
    }
}