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
        // Model init only once per app process
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
                    modelError = "Failed to initialize model: ${t.message}",
                    progressMessage = null
                )
            }

            try {
                logger.error(
                    message = "Failed to initialize model: ${t.message}",
                    throwable = t
                )
            } catch (_: Throwable) { }
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

            // Reserve 2 ids for this turn (user + thinking)
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

    // --- Core logic: refresh this-session logs, then call RAG ---

    private suspend fun generateRagAnswer(userQuestion: String): String {
        // 1) Handle greetings without hitting RAG
        if (isGreetingOrSmallTalk(userQuestion)) {
            return "Hi! I’m your test assistant. Ask me things like:\n" +
                    "- \"Summarize this session’s journey\"\n" +
                    "- \"Which payments succeeded or failed?\""
        }

        // 2) Ensure model is ready
        if (!state.value.isModelReady) {
            return "The local model is still initializing. Please wait and try again."
        }

        // 3) Refresh ONLY this session's logs into memory
        try {
            refreshCurrentSessionLogs()
        } catch (t: Throwable) {
            try {
                logger.error(
                    message = "RAG pipeline failure for question: \"$userQuestion\" — ${t.message}",
                    throwable = t
                )
            } catch (_: Throwable) { }

            return "The local AI pipeline failed while answering your question: ${t.message}"
        }

        // 4) Ask the RAG pipeline
        val raw = try {
            ragPipeline.generateResponse(userQuestion)
        } catch (t: Throwable) {
            try {
                logger.error(
                    message = "RAG pipeline failure for question: \"$userQuestion\" — ${t.message}",
                    throwable = t
                )
            } catch (_: Throwable) { }

            return "The local AI pipeline failed while answering your question: ${t.message}"
        }

        return shortenAnswer(raw)
    }

    private suspend fun refreshCurrentSessionLogs() {
        val sessionId = sessionManager.currentSessionId()
        val logs: List<AppLog> = try {
            logRepo.getLatestForSession(sessionId, limit = 500)
        } catch (t: Throwable) {
            logger.error(
                message = "Error loading logs for session $sessionId: ${t.message}",
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
            buildString {
                append("sessionId=")
                append(log.sessionId)
                append("; time=")
                append(log.timestamp)
                append("; type=")
                append(log.type)                 // INFO, API, PAYMENT, ERROR, CRASH, ANR
                if (!log.screen.isNullOrBlank()) {
                    append("; screen=")
                    append(log.screen)
                }
                if (!log.action.isNullOrBlank()) {
                    append("; action=")
                    append(log.action)           // SCREEN_VIEW, JOURNEY_STEP, PAYMENT_RESULT, etc.
                }
                if (!log.api.isNullOrBlank()) {
                    append("; api=")
                    append(log.api)
                }
                append("; message=")
                append(log.message)
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

    private fun shortenAnswer(text: String): String {
        val maxChars = 700
        if (text.length <= maxChars) return text

        val cutIndex = text.lastIndexOf('\n', startIndex = maxChars)
            .takeIf { it > 0 } ?: maxChars

        val trimmed = text.take(cutIndex).trimEnd()
        return "$trimmed\n\n[Answer shortened to keep it concise.]"
    }
}