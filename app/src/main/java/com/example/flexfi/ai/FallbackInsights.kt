package com.example.flexfi.ai

import com.example.flexfi.flexcard.FlexCardLLMResponse

/**
 * Generates rule-based fallback insights when LLM is unavailable.
 * All insights are deterministic: same data hash always produces same insights.
 */
object FallbackInsights {

    private val templates = listOf(
        "Food remains your highest spending category this month",
        "Transport expenses increased compared to previous weeks",
        "Shopping category accounts for over 25% of total transactions",
        "Daily spending has been consistent with less than 10% variance",
        "Top three merchants represent significant portion of expenses",
        "You have tracked expenses for multiple consecutive days",
        "Discretionary spending has remained stable from last period",
        "Category distribution shows balanced spending across multiple areas",
        "Recent transactions show increased weekend spending pattern",
        "Payment methods diversified across multiple platforms and cards",
        "Average transaction size indicates preference for regular purchases",
        "Spending spike detected in current week compared to baseline",
        "Expense frequency has increased from previous month",
        "Top merchants account for majority of monthly spending",
        "Spending behavior shows uptick in specific categories"
    )

    /**
     * Generates 3 deterministic insights based on data hash.
     * Same data always produces same insights (no randomness).
     *
     * @param dataHash Hash of financial data (from FinancialDataSummarizer)
     * @return List of exactly 3 insights
     */
    fun getInsights(dataHash: Long): List<String> {
        val baseIndex = (dataHash % templates.size).toInt().let { if (it < 0) it + templates.size else it }

        return listOf(
            templates[baseIndex],
            templates[(baseIndex + 1) % templates.size],
            templates[(baseIndex + 2) % templates.size]
        )
    }

    /**
     * Generates a fallback explanation for "Explain My Spending".
     * Uses a predefined template since we don't have actual data analysis.
     */
    fun getExplanation(): String {
        return """Your spending shows a balanced distribution across multiple categories.
Regular transactions suggest consistent tracking habits.
Focus areas remain discretionary spending and top merchant visits.
Consider budgeting for high-frequency merchants to optimize expenses."""
    }

    /**
     * Generates a data-driven fallback explanation (2-4 lines) from summary text.
     */
    fun getExplanation(summary: String): String {
        val totalSpend = summary.keyValue("Spend") ?: summary.lineValue("Total Spend")
        val topCategory = summary.keyValue("TopCat")?.substringBefore(":") ?: summary.lineValue("Highest Category")
        val avgDaily = summary.lineValue("Average Daily Spend")
        val topMerchant = summary.keyValue("Top")?.substringBefore(":") ?: summary.sectionFirstItem("Top Merchants")

        val lines = mutableListOf<String>()
        if (totalSpend != null) {
            lines += "You spent $totalSpend this month based on recorded transactions."
        }
        if (topCategory != null) {
            lines += "Your highest spending category was $topCategory."
        }
        if (topMerchant != null) {
            lines += "Your top merchant was $topMerchant."
        }
        if (avgDaily != null) {
            lines += "Your average daily spend was $avgDaily."
        }

        if (lines.isEmpty()) {
            return getExplanation()
        }
        return lines.take(4).joinToString("\n")
    }

    /**
     * Deterministic fallback for assistant mode.
     */
    fun getAssistantResponse(summary: String, question: String): String {
        val q = question.lowercase()
        val spend = summary.keyValue("Spend") ?: "0"
        val prev = summary.keyValue("Prev") ?: "0"
        val topCategory = summary.keyValue("TopCat")?.substringBefore(":") ?: "Unknown"
        val topMerchant = summary.keyValue("Top")?.substringBefore(":") ?: "Unknown"

        return when {
            q.contains("overspend") || q.contains("over spending") ->
                "Highest spend is in $topCategory.\nTop merchant is $topMerchant.\nCurrent spend: $spend."
            q.contains("biggest") || q.contains("highest") ->
                "Biggest expense category is $topCategory.\nTop merchant is $topMerchant.\nCurrent spend: $spend."
            q.contains("compare") || q.contains("last month") || q.contains("previous") ->
                "Current month spend: $spend.\nPrevious month spend: $prev.\nDifference is based on these totals only."
            else ->
                "Current spend is $spend.\nTop category: $topCategory.\nTop merchant: $topMerchant."
        }
    }

    /**
     * Get a specific insight template by index.
     * Useful for testing or debugging fallback behavior.
     */
    fun getTemplate(index: Int): String {
        return templates[index % templates.size]
    }

    /**
     * Log all available fallback templates.
     * Useful for docs/reference.
     */
    fun getAllTemplates(): List<String> {
        return templates.toList()
    }

    fun getFlexCardFallback(dataHash: Long): FlexCardLLMResponse {
        val insights = getInsights(dataHash).map { it.limitWords(12) }.take(3)
        val improvement = "Lower frequent small spends in your top category".limitWords(12)
        val tagline = "You are building stronger money habits one day at a time".limitWords(12)
        return FlexCardLLMResponse(
            highlights = insights,
            improvement = improvement,
            tagline = tagline
        )
    }

    private fun String.lineValue(label: String): String? {
        val prefix = "- $label:"
        return lines()
            .firstOrNull { it.trim().startsWith(prefix) }
            ?.substringAfter(prefix)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun String.sectionFirstItem(sectionTitle: String): String? {
        val lines = lines()
        val sectionIndex = lines.indexOfFirst { it.trim() == "$sectionTitle:" }
        if (sectionIndex == -1) return null
        return lines
            .drop(sectionIndex + 1)
            .firstOrNull { it.trim().startsWith("- ") }
            ?.removePrefix("- ")
            ?.trim()
            ?.substringBefore(":")
            ?.takeIf { it.isNotEmpty() }
    }

    private fun String.keyValue(key: String): String? {
        val prefix = "$key="
        return lines()
            .firstOrNull { it.trim().startsWith(prefix) }
            ?.substringAfter(prefix)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun String.limitWords(maxWords: Int): String {
        val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.take(maxWords).joinToString(" ")
    }
}
