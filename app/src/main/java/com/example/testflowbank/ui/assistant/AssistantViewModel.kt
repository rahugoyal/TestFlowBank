package com.example.testflowbank.ui.assistant

import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.core.session.SessionManager
import com.example.testflowbank.data.logs.LogRepository
import com.example.testflowbank.rag.RagPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val logRepo: LogRepository,
    private val ragPipeline: RagPipeline,
    private val sessionStore: AssistantSessionRepository,
    private val logger: AppLogger,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )
    val state: StateFlow<AssistantUiState> = sessionStore.state

    init {
        if (!sessionStore.hasInitializedModel) {
            vmScope.launch {
                initModelOnce()
            }
        }
    }

    private suspend fun initModelOnce() {
        try {
            sessionStore.updateState {
                it.copy(
                    progressMessage = "Initializing local AI model…",
                    modelError = null
                )
            }

            ragPipeline.waitUntilReady()

            sessionStore.updateState {
                it.copy(
                    isModelReady = true,
                    progressMessage = null
                )
            }

            sessionStore.hasInitializedModel = true
        } catch (t: Throwable) {
            sessionStore.updateState {
                it.copy(
                    isModelReady = false,
                    modelError = "Failed to initialize model: $t",
                    progressMessage = null
                )
            }

            try {
                logger.error(
                    action = "EXCEPTION DETECTED",
                    message = "Failed to initialize model: $t",
                    throwable = t
                )
            } catch (_: Throwable) {
            }
        }
    }

    fun onInputChange(new: String) {
        sessionStore.updateState { it.copy(inputText = new) }
    }

    fun onSendClicked() {
        val current = state.value
        val text = current.inputText.trim()
        if (text.isEmpty() || current.isSending) return

        vmScope.launch {
            val isSmallTalk = isGreetingOrSmallTalk(text)

            val baseId = sessionStore.reserveMessageIds(count = 2)

            val userMsg = ChatMessage(
                id = baseId,
                isUser = true,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            val thinkingMsg = ChatMessage(
                id = baseId + 1,
                isUser = false,
                text = "Thinking…",
                timestamp = System.currentTimeMillis(),
                isPending = true
            )

            sessionStore.updateState {
                it.copy(
                    messages = it.messages + userMsg + thinkingMsg,
                    inputText = "",
                    isSending = true,
                    progressMessage = if (isSmallTalk) null
                    else "Refreshing this session logs and querying the model…"
                )
            }

            val answerText: String
            val tookMs = measureTimeMillis {
                answerText = generateRagAnswer(text)
            }

            sessionStore.updateState {
                it.copy(
                    progressMessage = null,
                    messages = it.messages.map { msg ->
                        if (msg.id == thinkingMsg.id) {
                            msg.copy(
                                text = "$answerText\n\n(Answered in ${tookMs}ms)",
                                isPending = false
                            )
                        } else msg
                    },
                    isSending = false
                )
            }
        }
    }

    private suspend fun generateRagAnswer(userQuestion: String): String {
        if (isGreetingOrSmallTalk(userQuestion)) {
            return "Hi! I’m your test assistant. Ask me things like:\n" +
                    "- \"Summarize this session’s journey\"\n" +
                    "- \"Which payments succeeded or failed?\""
        }

        if (!state.value.isModelReady) {
            return "The local model is still initializing. Please wait and try again."
        }

        try {
            refreshCurrentSessionLogs()
        } catch (t: Throwable) {
            try {
                logger.error(
                    action = "EXCEPTION DETECTED",
                    message = "RAG pipeline failure for question: \"$userQuestion\" — $t",
                    throwable = t
                )
            } catch (_: Throwable) {
            }

            return "The local AI pipeline failed while answering your question: $t"
        }

        val raw = try {
            ragPipeline.generateResponse(userQuestion)
        } catch (t: Throwable) {
            try {
                logger.error(
                    action = "EXCEPTION DETECTED",
                    message = "RAG pipeline failure for question: \"$userQuestion\" — $t",
                    throwable = t
                )
            } catch (_: Throwable) {
            }

            return "The local AI pipeline failed while answering your question: ${t.message}"
        }

        return raw
    }

    private suspend fun refreshCurrentSessionLogs() {
        val logs: List<AppLog> = try {
            logRepo.getLatest(limit = 400)
        } catch (t: Throwable) {
            logger.error(
                action = "EXCEPTION DETECTED",
                message = "Error loading logs : ${t.message}",
                throwable = t
            )
            emptyList()
        }

        if (logs.isEmpty()) return

        val facts = logsToFacts(logs)
        ragPipeline.memorizeLogChunks(facts)
    }

    private fun logsToFacts(logs: List<AppLog>): List<String> =
        logs.map { log ->
            val shortStack = log.stackTrace
                ?.lineSequence()
                ?.take(3)
                ?.joinToString(" | ")
                ?: ""

            buildString {
                append("time=")
                append(log.timestamp)
                append("; type=")
                append(log.type)
                if (!log.screen.isNullOrBlank()) {
                    append("; screen=")
                    append(log.screen)
                }
                if (!log.action.isNullOrBlank()) {
                    append("; action=")
                    append(log.action)
                }
                if (!log.api.isNullOrBlank()) {
                    append("; api=")
                    append(log.api)
                }
                if (!log.message.isNullOrBlank()) {
                    append("; message=")
                    append(log.message)
                }
                if (!log.exception.isNullOrBlank()) {
                    append("; exception=")
                    append(log.exception)
                }
                if (shortStack.isNotBlank()) {
                    append("; stack=")
                    append(shortStack)
                }
            }
        }

    private fun isGreetingOrSmallTalk(q: String): Boolean {
        val text = q.trim().lowercase()
        if (text.isEmpty()) return true
        val patterns = listOf("hi", "hello", "hey", "thanks", "thank you", "ok", "okay")
        return patterns.any { pattern ->
            text == pattern || text.startsWith("$pattern ")
        }
    }
}