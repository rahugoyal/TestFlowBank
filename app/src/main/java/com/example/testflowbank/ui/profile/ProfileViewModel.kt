package com.example.testflowbank.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _state = MutableStateFlow(ProfileUiState(isLoading = true))
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                logger.api("Profile fetch", screen = "Profile", api = "GET /users/1")
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
                    logger.info("Profile loaded", screen = "Profile")
                } else {
                    val msg = "Profile failed code=${res.code()}"
                    _state.value = ProfileUiState(isLoading = false, error = msg)
                    logger.error(msg, screen = "Profile")
                }
            } catch (t: Throwable) {
                val msg = "Profile exception: ${t.message}"
                _state.value = ProfileUiState(isLoading = false, error = msg)
                logger.error(msg, screen = "Profile")
            }
        }
    }
}