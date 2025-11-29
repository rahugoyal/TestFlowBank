package com.example.testflowbank.ui.profile

import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val logger: AppLogger
) : ViewModel() {
    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )
    private val _state = MutableStateFlow(ProfileUiState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        vmScope.launch {
            // Log entry into Transactions module
            logger.screenView()
            logger.journeyStep(
                step = "OPEN_Profile",
                detail = "User opened Profile module"
            )
            load()
        }
    }

    fun load() {
        vmScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                logger.api("Profile fetch", api = "GET /users/1")
                val res = repo.getProfile()
                if (res.isSuccessful) {
                    val body = res.body()
                    val name = "${body?.firstName.orEmpty()} ${body?.lastName.orEmpty()}".trim()
                    _state.value = ProfileUiState(
                        isLoading = false,
                        name = name.ifBlank { "Demo User" },
                        email = body?.email ?: "",
                        phone = body?.phone ?: ""
                    )
                    logger.info("Profile loaded")
                } else {
                    val msg = "Profile failed code=${res.code()}"
                    _state.value = ProfileUiState(isLoading = false, error = msg)
                    logger.error(msg)
                }
            } catch (t: Throwable) {
                val msg = "Profile exception: ${t.message}"
                _state.value = ProfileUiState(isLoading = false, error = msg)
                logger.error(
                    action = "EXCEPTION DETECTED",
                    message = msg,
                    throwable = t
                )
            }
        }
    }
}