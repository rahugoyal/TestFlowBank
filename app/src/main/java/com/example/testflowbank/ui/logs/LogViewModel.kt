package com.example.testflowbank.ui.logs

import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.data.logs.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repo: LogRepository
) : ViewModel() {
    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )
    private val _logs = MutableStateFlow<List<AppLog>>(emptyList())
    val logs = _logs.asStateFlow()

    init {
        vmScope.launch {
            _logs.value = repo.getLatest(400)
        }
    }
}