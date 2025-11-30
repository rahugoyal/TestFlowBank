package com.example.testflowbank.ui.assistant

import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLog
import com.example.testflowbank.core.logging.AppLogger
import com.example.testflowbank.data.logs.LogRepository
import com.example.testflowbank.rag.RagPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val logRepo: LogRepository,
    private val ragPipeline: RagPipeline,
    private val sessionStore: AssistantSessionRepository,
    private val logger: AppLogger
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

            try {
                logger.assistantQuestion(text)
            } catch (_: Throwable) {
                // don't break UI if logging fails
            }

            // Reserve ids for user + assistant “thinking”
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

            var answerText = ""
            val tookMs = try {
                val start = System.currentTimeMillis()
                answerText = generateRagAnswer(text)
                System.currentTimeMillis() - start
            } catch (t: Throwable) {
                answerText = "The assistant failed while answering your question: ${t.message}"
                0L
            }

            // ⭐ Log the answer to AppLogger (shortened text)
            try {
                logger.assistantAnswer(answerText)
            } catch (_: Throwable) {
                // again, don't break UI if logging fails
            }

            sessionStore.updateState {
                it.copy(
                    progressMessage = null,
                    messages = it.messages.map { msg ->
                        if (msg.id == thinkingMsg.id) {
                            msg.copy(
                                text = if (tookMs > 0)
                                    "$answerText\n\n(Answered in ${tookMs}ms)"
                                else
                                    answerText,
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
            return "Hi! I’m your test assistant. Ask me about this session’s payments, crashes, or journeys."
        }

        if (!state.value.isModelReady) {
            return "The local model is still initializing. Please wait and try again."
        }

        // 👇 always sync new logs quickly
        refreshCurrentSessionLogs(userQuestion)

        val raw = try {
            ragPipeline.generateResponse(userQuestion)
        } catch (t: Throwable) {
            logger.error(
                message = "RAG pipeline failure: ${t.message}",
                throwable = t,
                action = "ASSISTANT_RAG_FAIL"
            )
            return "The local AI pipeline failed while answering your question: ${t.message}"
        }

        return raw
    }

    private fun filterLogsForQuestion(
        question: String,
        logs: List<AppLog>
    ): List<AppLog> {
        val q = question.lowercase()

        val crashKeywords = listOf("EXCEPTION","crash", "crashed", "crashes", "force close", "anr", "not responding")
        val isCrashQuestion = crashKeywords.any { it in q }

        if (!isCrashQuestion) {
            // For non-crash questions, keep all logs as-is
            return logs
        }

        // When user asks about crash/ANR → focus on crash-like logs only
        val crashLogs = logs.filter { log ->
            log.type.equals("EXCEPTION", ignoreCase = true) || log.type.equals("ANR", ignoreCase = true)
        }

        // If we have no crash/ANR logs, fall back to all logs
        if (crashLogs.isEmpty()) return logs

        // Simplest version: only crash/ANR logs
        return crashLogs
    }

    private suspend fun refreshCurrentSessionLogs(userQuestion: String) {
        val lastId = sessionStore.lastEmbeddedLogId

        val newLogs = if (lastId == null) {
            logRepo.getLatest(limit = 400)
                .sortedBy { it.timestamp }
        } else {
            // next times: only logs after last embedded id
            logRepo.getNewerForSession(afterId = lastId)
        }

        if (newLogs.isEmpty()) return

        val focusedLogs = filterLogsForQuestion(userQuestion, newLogs)
        val facts = logsToFacts(focusedLogs)
        ragPipeline.memorizeLogChunks(facts)

        // update last embedded id
        val maxId = newLogs.maxOf { it.id }
        sessionStore.lastEmbeddedLogId = maxId
    }

    private fun logsToFacts(logs: List<AppLog>): List<String> =
        logs.map { log ->
            val shortStack = log.stackTrace
                ?.lineSequence()
                ?.take(2)
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
                if (log.message.isNotBlank()) {
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