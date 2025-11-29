package com.example.testflowbank.rag

import android.app.Application
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalAndInferenceChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.AsyncProgressListener
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GemmaEmbeddingModel
import com.google.ai.edge.localagents.rag.models.LanguageModelResponse
import com.google.ai.edge.localagents.rag.models.MediaPipeLlmBackend
import com.google.ai.edge.localagents.rag.prompt.PromptBuilder
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig.TaskType
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.jvm.optionals.getOrNull

class RagPipeline(application: Application) {

    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    private val llmOptions: LlmInferenceOptions =
        LlmInferenceOptions.builder()
            .setModelPath(GEMMA_MODEL_PATH)
            .setPreferredBackend(LlmInference.Backend.GPU)
            .setMaxTokens(1024)
            .build()

    private val llmSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions =
        LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(1.0f)
            .setTopP(0.95f)
            .setTopK(64)
            .build()

    private val mediaPipeLanguageModel: MediaPipeLlmBackend =
        MediaPipeLlmBackend(
            application.applicationContext,
            llmOptions,
            llmSessionOptions
        )

    private val embedder: Embedder<String> =
        GemmaEmbeddingModel(
            EMBEDDING_GEMMA_MODEL_PATH,
            TOKENIZER_MODEL_PATH,
            USE_GPU_FOR_EMBEDDINGS,
        )

    private val config = ChainConfig.create(
        mediaPipeLanguageModel,
        PromptBuilder(PROMPT_TEMPLATE),
        DefaultSemanticTextMemory(
            SqliteVectorStore(EMBEDDING_DIMENSION),
            embedder
        )
    )

    private val retrievalAndInferenceChain = RetrievalAndInferenceChain(config)

    private val initFuture: ListenableFuture<Boolean> = mediaPipeLanguageModel.initialize()

    init {
        Futures.addCallback(
            initFuture,
            object : FutureCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    // model ready
                }

                override fun onFailure(t: Throwable) {
                    // model failed to init
                }
            },
            backgroundExecutor
        )
    }

    /**
     * Suspend until the Gemma 3N model is initialized.
     * Does NOT block the main thread.
     */
    suspend fun waitUntilReady(): Boolean = withContext(Dispatchers.Default) {
        initFuture.await()
    }

    suspend fun memorizeLogChunks(logFacts: List<String>) = withContext(Dispatchers.Default) {
        if (logFacts.isEmpty()) return@withContext

        val fullText = logFacts.joinToString(separator = "\n")
        if (fullText.isBlank()) return@withContext

        val chunks = chunkTextSafely(fullText)
        if (chunks.isNotEmpty()) {
            memorize(chunks)
        }
    }

    private fun chunkTextSafely(text: String): List<String> {
        val result = mutableListOf<String>()
        var index = 0
        val len = text.length

        while (index < len) {
            val end = (index + MAX_CHARS_PER_CHUNK).coerceAtMost(len)
            result += text.substring(index, end)
            index = end
        }

        return result
    }

    private suspend fun memorize(facts: List<String>) {
        val future =
            config.semanticMemory.getOrNull()
                ?.recordBatchedMemoryItems(ImmutableList.copyOf(facts))

        future?.await()  // non-blocking
    }

    suspend fun generateResponse(
        prompt: String,
        callback: AsyncProgressListener<LanguageModelResponse>? = null,
    ): String = withContext(Dispatchers.Default) {
        coroutineScope {
            val retrievalRequest =
                RetrievalRequest.create(
                    prompt,
                    RetrievalConfig.create(
                        3,
                        0.1f,
                        TaskType.QUESTION_ANSWERING
                    )
                )

            val response =
                retrievalAndInferenceChain.invoke(retrievalRequest, callback).await()
            response.text
        }
    }

    companion object {
        private const val USE_GPU_FOR_EMBEDDINGS = true

        private const val MAX_CHARS_PER_CHUNK = 400
        private const val EMBEDDING_DIMENSION = 768

        private const val GEMMA_MODEL_PATH = "/data/local/tmp/gemma-3n-E2B-it-int4.task"
        private const val TOKENIZER_MODEL_PATH = "/data/local/tmp/sentencepiece.model"
        private const val EMBEDDING_GEMMA_MODEL_PATH = "/data/local/tmp/embeddinggemma.tflite"

        private const val PROMPT_TEMPLATE: String =
            "You are a concise QA assistant for an Android app.\n" +
                    "\n" +
                    "LOG CONTEXT:\n" +
                    "{0}\n" +
                    "\n" +
                    "USER QUESTION:\n" +
                    "{1}\n" +
                    "\n" +
                    "Each log line may contain: time, type (INFO/API/ERROR/CRASH), screen, action, api, message, stackTrace, exception.\n" +
                    "\n" +
                    "Rules:\n" +
                    "- Use ONLY information found in the logs.\n" +
                    "- First understand what the user is asking (screen, API, payment, crash, etc.).\n" +
                    "- If the question is about what SUCCEEDED, mention only successful operations and ignore failures unless explicitly asked.\n" +
                    "- If the question is about what FAILED or CRASHED or EXCEPTION, focus on EXCEPTION/ERROR/CRASH logs and give the main cause in simple words.\n" +
                    "- If the question is about the journey, describe key screens and actions in order.\n" +
                    "- If the logs do not clearly answer the question, say that briefly.\n" +
                    "- Answer in 1–3 short sentences, no bullet points, no raw stack traces."    }
}