package com.example.flexfi.ai

import android.util.Log
import android.os.SystemClock
import com.example.flexfi.data.repository.AIInsightRepository
import com.example.flexfi.data.repository.ExpenseRepository
import com.example.flexfi.data.repository.PersonalExpenseRepository
import com.example.flexfi.data.repository.StreakRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Central orchestrator for AI-powered financial insights.
 * Coordinates data querying, summarization, LLM inference, parsing, and caching.
 *
 * Non-blocking: all inference runs on Dispatchers.Default.
 * Graceful degradation: falls back to rule-based insights if LLM unavailable.
 */
class AIManager(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val expenseRepository: ExpenseRepository,
    private val streakRepository: StreakRepository,
    private val aiInsightRepository: AIInsightRepository,
    private val modelManager: ModelManager
) {

    companion object {
        private const val TAG = "AIManager"
        private const val PERF_TAG = "AI_PERF"
        private const val INSIGHTS_MAX_TOKENS = 128
        private const val EXPLAIN_MAX_TOKENS = 384
        private const val ASSISTANT_MAX_TOKENS = 256
        private const val INSIGHTS_TEMPERATURE = 0.2f
        private const val EXPLAIN_TEMPERATURE = 0.2f
        private const val ASSISTANT_TEMPERATURE = 0.3f
        private const val ASSISTANT_MAX_QUESTION_CHARS = 220
    }

    private data class FinancialContext(
        val compactSummary: String,
        val dataHash: Long
    )

    @Volatile
    private var firstInferenceCompleted = false

    val modelDownloadState: StateFlow<ModelDownloadState>
        get() = modelManager.downloadState

    /**
     * Generates 3 financial insights for the current month.
     * Flow emits as data becomes available.
     *
     * Flow:
     * 1. Query monthly expenses & settlements
     * 2. Summarize into text format
     * 3. Check cache (valid if data hash matches)
     * 4. If cache hit: emit cached insights
     * 5. If cache miss: build prompt → invoke LLM → parse output
     * 6. On error: emit fallback rule-based insights
     * 7. Cache result for 24h
     *
     * @return Flow<List<String>> emitting exactly 3 insights
     */
    fun generateInsights(userPhone: String? = null): Flow<List<String>> = flow {
        val requestStart = SystemClock.elapsedRealtime()
        try {
            Log.d(TAG, "Starting insight generation...")

            val context = fetchFinancialContext(userPhone)

            // Check cache
            val cacheLookupStart = SystemClock.elapsedRealtime()
            val cached = aiInsightRepository.getInsightIfValid(context.dataHash)
            if (cached != null) {
                val cacheLookupMs = SystemClock.elapsedRealtime() - cacheLookupStart
                val totalMs = SystemClock.elapsedRealtime() - requestStart
                Log.i(PERF_TAG, "mode=insights event=cache_hit cacheLookupMs=$cacheLookupMs totalMs=$totalMs hash=${context.dataHash}")
                Log.d(TAG, "Cache hit! Emitting cached insights")
                emit(cached)
                return@flow
            }

            Log.d(TAG, "Cache miss. Proceeding with LLM inference...")

            // Build prompt
            val prompt = PromptBuilder.buildInsightsPrompt(context.compactSummary)
            Log.d(TAG, "Prompt built (length: ${prompt.length})")

            // Invoke LLM or fallback
            val insights = withContext(Dispatchers.Default) {
                try {
                    if (modelManager.isModelReady()) {
                        val isColdStart = !firstInferenceCompleted
                        val inferenceStart = SystemClock.elapsedRealtime()
                        Log.d(TAG, "Model ready, calling LLM...")
                        val response = modelManager.generateText(
                            prompt = prompt,
                            maxTokens = INSIGHTS_MAX_TOKENS,
                            temperature = INSIGHTS_TEMPERATURE
                        )
                            ?: throw Exception("LLM returned null")
                        val inferenceMs = SystemClock.elapsedRealtime() - inferenceStart
                        val event = if (isColdStart) "cold_inference" else "warm_inference"
                        Log.i(PERF_TAG, "mode=insights event=$event inferenceMs=$inferenceMs")
                        firstInferenceCompleted = true

                        Log.d(TAG, "LLM response received (length: ${response.length})")
                        val parsed = InsightParser.parseInsights(response)
                        if (InsightParser.validateInsights(parsed)) {
                            parsed
                        } else {
                            FallbackInsights.getInsights(context.dataHash)
                        }
                    } else {
                        Log.w(TAG, "Model not ready, using fallback")
                        FallbackInsights.getInsights(context.dataHash)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LLM inference failed, using fallback", e)
                    FallbackInsights.getInsights(context.dataHash)
                }
            }

            Log.d(TAG, "Insights generated: ${insights.size} insights")
            insights.forEachIndexed { index, insight ->
                Log.d(TAG, "Insight $index: $insight")
            }

            // Cache results
            withContext(Dispatchers.IO) {
                try {
                    aiInsightRepository.cacheInsights(insights, context.dataHash)
                    Log.d(TAG, "Insights cached successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cache insights", e)
                }
            }

            // Emit insights
            emit(insights)
            val totalMs = SystemClock.elapsedRealtime() - requestStart
            Log.i(PERF_TAG, "mode=insights event=complete totalMs=$totalMs")

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in generateInsights, using ultimate fallback", e)
            emit(FallbackInsights.getInsights(0L))
            val totalMs = SystemClock.elapsedRealtime() - requestStart
            Log.i(PERF_TAG, "mode=insights event=fallback_error totalMs=$totalMs")
        }
    }

    /**
     * Generates a human-readable spending explanation (2-4 lines).
     * Uses LLM if available, falls back to rule-based summary.
     *
     * @return Flow<String> emitting explanation text
     */
    fun explainSpending(userPhone: String? = null): Flow<String> = flow {
        val requestStart = SystemClock.elapsedRealtime()
        try {
            Log.d(TAG, "Starting spending explanation...")

            val context = fetchFinancialContext(userPhone)

            val cacheLookupStart = SystemClock.elapsedRealtime()
            val cached = aiInsightRepository.getExplanationIfValid(context.dataHash)
            if (!cached.isNullOrBlank()) {
                val cacheLookupMs = SystemClock.elapsedRealtime() - cacheLookupStart
                val totalMs = SystemClock.elapsedRealtime() - requestStart
                Log.i(PERF_TAG, "mode=explain event=cache_hit cacheLookupMs=$cacheLookupMs totalMs=$totalMs hash=${context.dataHash}")
                emit(cached)
                return@flow
            }

            // Build prompt
            val prompt = PromptBuilder.buildExplainPrompt(context.compactSummary)

            // Invoke LLM or fallback
            val explanation = withContext(Dispatchers.Default) {
                try {
                    if (modelManager.isModelReady()) {
                        val isColdStart = !firstInferenceCompleted
                        val inferenceStart = SystemClock.elapsedRealtime()
                        Log.d(TAG, "Model ready, calling LLM for explanation...")
                        val response = modelManager.generateText(
                            prompt = prompt,
                            maxTokens = EXPLAIN_MAX_TOKENS,
                            temperature = EXPLAIN_TEMPERATURE
                        )
                            ?: throw Exception("LLM returned null")
                        val inferenceMs = SystemClock.elapsedRealtime() - inferenceStart
                        val event = if (isColdStart) "cold_inference" else "warm_inference"
                        Log.i(PERF_TAG, "mode=explain event=$event inferenceMs=$inferenceMs")
                        firstInferenceCompleted = true

                        Log.d(TAG, "LLM explanation response received")
                        InsightParser.parseExplanation(response)
                    } else {
                        Log.w(TAG, "Model not ready, using fallback explanation")
                        FallbackInsights.getExplanation(context.compactSummary)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LLM explanation failed, using fallback", e)
                    FallbackInsights.getExplanation(context.compactSummary)
                }
            }

            Log.d(TAG, "Explanation generated: ${explanation.take(50)}...")

            withContext(Dispatchers.IO) {
                runCatching { aiInsightRepository.cacheExplanation(explanation, context.dataHash) }
                    .onFailure { Log.e(TAG, "Failed to cache explanation", it) }
            }

            // Emit explanation
            emit(explanation)
            val totalMs = SystemClock.elapsedRealtime() - requestStart
            Log.i(PERF_TAG, "mode=explain event=complete totalMs=$totalMs")

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in explainSpending", e)
            emit(FallbackInsights.getExplanation())
            val totalMs = SystemClock.elapsedRealtime() - requestStart
            Log.i(PERF_TAG, "mode=explain event=fallback_error totalMs=$totalMs")
        }
    }

    /**
     * Initializes the AIManager (downloads model if needed).
     * Call once on app startup.
     */
    suspend fun initialize() {
        Log.d(TAG, "Initializing AIManager...")
        try {
            modelManager.initialize()
            Log.d(TAG, "AIManager initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "AIManager initialization failed", e)
        }
    }

    /**
     * Checks if AI features are available and ready.
     */
    fun isReady(): Boolean {
        return modelManager.isModelReady()
    }

    /**
     * Manually clear cached insights and model.
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                aiInsightRepository.clearAllCache()
                Log.d(TAG, "Insights cache cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing insights cache", e)
            }
        }
    }

    /**
     * Gets diagnostic info for debugging.
     */
    fun getDiagnostics(): String {
        return """
AIManager Diagnostics:
- Model Ready: ${modelManager.isModelReady()}
- Model Size: ${modelManager.getModelSizeFormatted()}
            """.trimIndent()
    }

    /**
     * Stateless assistant call using question + compact financial data only.
     */
    suspend fun askAssistant(question: String, userPhone: String? = null): String {
        val requestStart = SystemClock.elapsedRealtime()
        val normalizedQuestion = normalizeAssistantQuestion(question)
        if (!isUsableAssistantQuestion(normalizedQuestion)) {
            Log.i(
                PERF_TAG,
                "mode=assistant event=question_rejected rawLen=${question.length} normalizedLen=${normalizedQuestion.length}"
            )
            return "Please enter a question."
        }

        Log.i(
            PERF_TAG,
            "mode=assistant event=question_normalized rawLen=${question.length} normalizedLen=${normalizedQuestion.length}"
        )

        return try {
            val context = fetchFinancialContext(userPhone)
            val prompt = PromptBuilder.buildAssistantPrompt(context.compactSummary, normalizedQuestion)

            val response = withContext(Dispatchers.Default) {
                if (!modelManager.isModelReady()) {
                    return@withContext null
                }

                val isColdStart = !firstInferenceCompleted
                val inferenceStart = SystemClock.elapsedRealtime()
                modelManager.generateText(
                    prompt = prompt,
                    maxTokens = ASSISTANT_MAX_TOKENS,
                    temperature = ASSISTANT_TEMPERATURE
                ).also {
                    val inferenceMs = SystemClock.elapsedRealtime() - inferenceStart
                    val event = if (isColdStart) "cold_inference" else "warm_inference"
                    Log.i(PERF_TAG, "mode=assistant event=$event inferenceMs=$inferenceMs")
                    firstInferenceCompleted = true
                }
            }

            if (response.isNullOrBlank()) {
                val totalMs = SystemClock.elapsedRealtime() - requestStart
                Log.i(PERF_TAG, "mode=assistant event=fallback_model_unavailable totalMs=$totalMs")
                return FallbackInsights.getAssistantResponse(context.compactSummary, normalizedQuestion)
            }

            val parsed = InsightParser.parseAssistantResponse(response)
            val totalMs = SystemClock.elapsedRealtime() - requestStart
            Log.i(PERF_TAG, "mode=assistant event=complete totalMs=$totalMs")
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Assistant inference failed", e)
            val fallbackSummary = runCatching { fetchFinancialContext(userPhone).compactSummary }
                .getOrElse { "Spend=0\nPrev=0\nTopCat=None:0\nTop=None:0\nStreak=0" }
            val totalMs = SystemClock.elapsedRealtime() - requestStart
            Log.i(PERF_TAG, "mode=assistant event=fallback_error totalMs=$totalMs")
            FallbackInsights.getAssistantResponse(fallbackSummary, normalizedQuestion)
        }
    }

    private suspend fun fetchFinancialContext(userPhone: String?): FinancialContext {
        val safePhone = userPhone ?: ""
        val currentMonth = getMonthRange(0)
        val previousMonth = getMonthRange(-1)

        val currentExpenses = queryExpenses(safePhone, currentMonth.first, currentMonth.second)
        val previousExpenses = queryExpenses(safePhone, previousMonth.first, previousMonth.second)
        val settlements = querySettlements(safePhone)
        val streak = queryStreak(safePhone)

        val compactSummary = FinancialDataSummarizer.summarizeCompactData(
            currentMonthExpenses = currentExpenses,
            previousMonthExpenses = previousExpenses,
            settlements = settlements,
            streak = streak
        )

        return FinancialContext(
            compactSummary = compactSummary,
            dataHash = InsightParser.generateDataHash(compactSummary)
        )
    }

    private fun getMonthRange(monthOffset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, monthOffset)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis - 1L
        return start to end
    }

    private suspend fun queryExpenses(userPhone: String, start: Long, end: Long) =
        withContext(Dispatchers.IO) {
            runCatching {
                personalExpenseRepository.getExpensesInRange(userPhone, start, end)
                    .firstOrNull()
                    ?: emptyList()
            }.getOrElse {
                Log.e(TAG, "Failed to query expenses", it)
                emptyList()
            }
        }

    private suspend fun querySettlements(userPhone: String) = withContext(Dispatchers.IO) {
        runCatching { expenseRepository.getSettlementsForUserOnce(userPhone) }
            .getOrElse {
                Log.e(TAG, "Failed to query settlements", it)
                emptyList()
            }
    }

    private suspend fun queryStreak(userPhone: String) = withContext(Dispatchers.IO) {
        runCatching { streakRepository.getStreakSync(userPhone)?.currentStreak ?: 0 }
            .getOrElse {
                Log.e(TAG, "Failed to query streak", it)
                0
            }
    }

    private fun normalizeAssistantQuestion(rawQuestion: String): String {
        val blockedPhrases = listOf(
            "ignore previous instructions",
            "reveal system prompt",
            "show hidden prompt",
            "developer mode"
        )

        var normalized = rawQuestion
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("[\\u0000-\\u001F]"), " ")
            .replace(Regex("`{2,}"), " ")
            .replace(Regex("[{}<>]"), " ")
            .replace(Regex("(.)\\1{6,}"), "$1$1$1")

        blockedPhrases.forEach { phrase ->
            normalized = normalized.replace(Regex(Regex.escape(phrase), RegexOption.IGNORE_CASE), " ")
        }

        normalized = normalized
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.length > ASSISTANT_MAX_QUESTION_CHARS) {
            normalized = normalized.take(ASSISTANT_MAX_QUESTION_CHARS).trimEnd()
        }

        return normalized
    }

    private fun isUsableAssistantQuestion(question: String): Boolean {
        return question.isNotBlank() && question.any { it.isLetterOrDigit() }
    }
}
