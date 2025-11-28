package com.example.testflowbank.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.data.logs.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repo: LogRepository
) : ViewModel() {

    private val _logs = MutableStateFlow<List<AppLog>>(emptyList())
    val logs = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            _logs.value = repo.getLatest(200)
        }
    }
}