package com.example.testflowbank.ui.assistant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long,
    val isPending: Boolean = false
)

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isModelReady: Boolean = false,
    val modelError: String? = null,
    val progressMessage: String? = null
)

@Singleton
class AssistantSessionRepository @Inject constructor() {

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state

    var hasInitializedModel: Boolean = false

    // 👇 track up to which log id we've embedded
    var lastEmbeddedLogId: Long? = null

    private var nextMessageId: Long = 1L

    fun updateState(reducer: (AssistantUiState) -> AssistantUiState) {
        _state.value = reducer(_state.value)
    }

    fun reserveMessageIds(count: Int = 1): Long {
        val start = nextMessageId
        nextMessageId += count
        return start
    }
}