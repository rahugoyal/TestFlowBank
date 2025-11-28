package com.example.testflowbank.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val logger: AppLogger
) : ViewModel() {
    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )
    var email by mutableStateOf("eve.holt@reqres.in")
    var password by mutableStateOf("cityslicka")
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    init {
        vmScope.launch {
            // Log entry into Transactions module
            logger.screenView()
            logger.journeyStep(
                step = "OPEN_Login",
                detail = "User opened Login module"
            )
        }
    }
    fun onLoginClick(onSuccess: () -> Unit) {
        vmScope.launch {
            isLoading = true
            error = null
            logger.api("Login started", api = "POST /api/login")

            try {
                val response = authRepository.login(email, password)
                if (response.token != null) {
                    logger.info("Login success", api = "POST /api/login")
                    isLoading = false
                    onSuccess()
                } else {
                    val msg = "Login failed: token missing"
                    logger.error(msg, api = "POST /api/login")
                    error = msg
                    isLoading = false
                }
            } catch (t: Throwable) {
                val msg = "Login exception: ${t.message}"
                logger.error(msg, api = "POST /api/login")
                error = msg
                isLoading = false
            }
        }
    }
}