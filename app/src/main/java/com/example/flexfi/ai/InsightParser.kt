package com.example.flexfi.ai

/**
 * Parses LLM responses into validated, structured output.
 * Handles edge cases gracefully with fallback to malformed data recovery.
 */
object InsightParser {

    /**
     * Parses LLM response into exactly 3 insights.
     * Each insight must be ≤15 words and factual.
     *
     * @param llmResponse Raw response string from LLM
     * @return List of exactly 3 insights, or empty list if parsing fails
     */
    fun parseInsights(llmResponse: String?): List<String> {
        if (llmResponse.isNullOrBlank()) {
            return emptyList()
        }

        val normalizedResponse = sanitizeOutput(llmResponse)

        val lines = normalizedResponse.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val insights = mutableListOf<String>()

        // Pattern 1: "1. ...", "2. ...", "3. ..."
        for (line in lines) {
            // Extract numbered items (handles "1.", "1)", "1:", etc.)
            val match = Regex("""^[\d]+[\.\):\-]\s*(.+)$""").find(line)
            if (match != null) {
                val text = match.groupValues[1].trim()
                if (text.isNotBlank()) {
                    insights.add(cleanInsight(text))
                }
            }

            // Stop if we have 3 insights
            if (insights.size >= 3) {
                break
            }
        }

        // Fallback: if we couldn't parse any numbered items, try splitting by bullet or newline
        if (insights.isEmpty()) {
            for (line in lines) {
                if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                    val text = line.substring(1).trim()
                    if (text.isNotBlank()) {
                        insights.add(cleanInsight(text))
                    }
                } else if (line.length > 10 && !line.contains("data:", ignoreCase = true)) {
                    insights.add(cleanInsight(line))
                }

                if (insights.size >= 3) {
                    break
                }
            }
        }

        // Must return exactly 3 non-empty insights; otherwise caller should fallback.
        val normalized = insights.take(3).filter { it.isNotBlank() }
        return if (normalized.size == 3 && validateInsights(normalized)) normalized else emptyList()
    }

    /**
     * Parses LLM response for "Explain My Spending" (max 4 lines).
     *
     * @param llmResponse Raw response string from LLM
     * @return Formatted explanation (up to 4 lines)
     */
    fun parseExplanation(llmResponse: String?): String {
        if (llmResponse.isNullOrBlank()) {
            return "Unable to generate spending explanation."
        }

        val lines = sanitizeOutput(llmResponse).lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(4) // Max 4 lines

        return lines.joinToString("\n").ifBlank {
            "Unable to generate spending explanation."
        }
    }

    /**
     * Parses and validates assistant output.
     */
    fun parseAssistantResponse(llmResponse: String?): String {
        if (llmResponse.isNullOrBlank()) {
            return "I could not answer with the available data."
        }

        val lines = sanitizeOutput(llmResponse)
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.removePrefix("-").removePrefix("*").trim() }
            .take(3)

        if (lines.isEmpty()) {
            return "I could not answer with the available data."
        }
        return lines.joinToString("\n")
    }

    /**
     * Cleans an individual insight:
     * - Removes trailing punctuation
     * - Validates word count (max 15)
     * - Truncates if too long
     */
    private fun cleanInsight(text: String): String {
        var cleaned = sanitizeOutput(text).trim()

        // Remove trailing punctuation
        cleaned = cleaned.trimEnd('.', '!', '?', ',', ';', ':')

        // Count words (space-separated)
        val wordCount = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

        // If > 15 words, truncate
        if (wordCount > 15) {
            val words = cleaned.split(Regex("\\s+")).take(15)
            cleaned = words.joinToString(" ")
        }

        return cleaned
    }

    /**
     * Validates that the parsed insights meet requirements.
     * For debugging/logging purposes.
     */
    fun validateInsights(insights: List<String>): Boolean {
        if (insights.size != 3) return false

        return insights.all { insight ->
            val wordCount = insight.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
            insight.isNotBlank() && wordCount <= 15
        }
    }

    /**
     * Generates a deterministic hash for data validation.
     * Used to track which data generated which insights.
     */
    fun generateDataHash(text: String): Long {
        return text.hashCode().toLong()
    }

    private fun sanitizeOutput(text: String): String {
        return text
            .replace("```", "")
            .replace("**", "")
            .replace("__", "")
            .replace("\u0000", "")
            .trim()
    }
}
