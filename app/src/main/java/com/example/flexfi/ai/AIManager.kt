package com.example.flexfi.ai

import android.util.Log
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
    }

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
        try {
            Log.d(TAG, "Starting insight generation...")

            // Get current month boundaries
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis - 1L

            Log.d(TAG, "Querying expenses from ${Calendar.getInstance().apply { timeInMillis = monthStart }} to ${Calendar.getInstance().apply { timeInMillis = monthEnd }}")

            // Query personal expenses for the month
            val personalExpenses = withContext(Dispatchers.IO) {
                try {
                    personalExpenseRepository.getExpensesInRange(userPhone ?: "", monthStart, monthEnd)
                        .let { it.firstOrNull() ?: emptyList() } // Convert Flow to list
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query personal expenses", e)
                    emptyList()
                }
            }

            // Query settlements for the month
            val settlements = withContext(Dispatchers.IO) {
                try {
                    expenseRepository.getSettlementsForUserOnce(userPhone ?: "")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query settlements", e)
                    emptyList()
                }
            }

            // Get current streak
            val streak = withContext(Dispatchers.IO) {
                try {
                    streakRepository.getStreakSync(userPhone ?: "")?.currentStreak ?: 0
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query streak", e)
                    0
                }
            }

            Log.d(TAG, "Data retrieved: ${personalExpenses.size} expenses, ${settlements.size} settlements, streak=$streak")

            // Summarize financial data
            val summary = FinancialDataSummarizer.summarizeMonthlyData(
                personalExpenses,
                settlements,
                streak
            )
            val dataHash = summary.hashCode().toLong()

            Log.d(TAG, "Summary generated with hash: $dataHash")
            Log.d(TAG, "Summary preview: ${summary.take(100)}...")

            // Check cache
            val cached = aiInsightRepository.getInsightIfValid(dataHash)
            if (cached != null) {
                Log.d(TAG, "Cache hit! Emitting cached insights")
                emit(cached)
                return@flow
            }

            Log.d(TAG, "Cache miss. Proceeding with LLM inference...")

            // Build prompt
            val prompt = PromptBuilder.buildInsightsPrompt(summary)
            Log.d(TAG, "Prompt built (length: ${prompt.length})")

            // Invoke LLM or fallback
            val insights = withContext(Dispatchers.Default) {
                try {
                    if (modelManager.isModelReady()) {
                        Log.d(TAG, "Model ready, calling LLM...")
                        val response = modelManager.generateText(prompt) 
                            ?: throw Exception("LLM returned null")

                        Log.d(TAG, "LLM response received (length: ${response.length})")
                        val parsed = InsightParser.parseInsights(response)
                        if (parsed.size == 3) parsed else FallbackInsights.getInsights(dataHash)
                    } else {
                        Log.w(TAG, "Model not ready, using fallback")
                        FallbackInsights.getInsights(dataHash)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LLM inference failed, using fallback", e)
                    FallbackInsights.getInsights(dataHash)
                }
            }

            Log.d(TAG, "Insights generated: ${insights.size} insights")
            insights.forEachIndexed { index, insight ->
                Log.d(TAG, "Insight $index: $insight")
            }

            // Cache results
            withContext(Dispatchers.IO) {
                try {
                    aiInsightRepository.cacheInsights(insights, dataHash)
                    Log.d(TAG, "Insights cached successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cache insights", e)
                }
            }

            // Emit insights
            emit(insights)

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in generateInsights, using ultimate fallback", e)
            emit(FallbackInsights.getInsights(0L))
        }
    }

    /**
     * Generates a human-readable spending explanation (2-4 lines).
     * Uses LLM if available, falls back to rule-based summary.
     *
     * @return Flow<String> emitting explanation text
     */
    fun explainSpending(userPhone: String? = null): Flow<String> = flow {
        try {
            Log.d(TAG, "Starting spending explanation...")

            // Get current month boundaries
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis - 1L

            // Query data
            val personalExpenses = withContext(Dispatchers.IO) {
                try {
                    personalExpenseRepository.getExpensesInRange(userPhone ?: "", monthStart, monthEnd)
                        .let { it.firstOrNull() ?: emptyList() }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query personal expenses for explanation", e)
                    emptyList()
                }
            }

            val settlements = withContext(Dispatchers.IO) {
                try {
                    expenseRepository.getSettlementsForUserOnce(userPhone ?: "")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query settlements for explanation", e)
                    emptyList()
                }
            }

            val streak = withContext(Dispatchers.IO) {
                try {
                    streakRepository.getStreakSync(userPhone ?: "")?.currentStreak ?: 0
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to query streak for explanation", e)
                    0
                }
            }

            // Summarize
            val summary = FinancialDataSummarizer.summarizeMonthlyData(
                personalExpenses,
                settlements,
                streak
            )

            Log.d(TAG, "Explanation data prepared")

            // Build prompt
            val prompt = PromptBuilder.buildExplainPrompt(summary)

            // Invoke LLM or fallback
            val explanation = withContext(Dispatchers.Default) {
                try {
                    if (modelManager.isModelReady()) {
                        Log.d(TAG, "Model ready, calling LLM for explanation...")
                        val response = modelManager.generateText(prompt)
                            ?: throw Exception("LLM returned null")

                        Log.d(TAG, "LLM explanation response received")
                        InsightParser.parseExplanation(response)
                    } else {
                        Log.w(TAG, "Model not ready, using fallback explanation")
                        FallbackInsights.getExplanation(summary)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LLM explanation failed, using fallback", e)
                    FallbackInsights.getExplanation(summary)
                }
            }

            Log.d(TAG, "Explanation generated: ${explanation.take(50)}...")

            // Emit explanation
            emit(explanation)

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in explainSpending", e)
            emit(FallbackInsights.getExplanation())
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
}
