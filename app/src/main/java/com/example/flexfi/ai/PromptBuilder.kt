package com.example.flexfi.ai

/**
 * Constructs LLM prompts with exact specifications and constraints.
 * All prompts are deterministic (no randomness) to ensure reproducible outputs.
 */
object PromptBuilder {

    /**
     * Builds a prompt for generating 3 financial insights.
     *
     * @param dataString Structured financial data from FinancialDataSummarizer
     * @return Complete prompt ready for LLM ingestion
     */
    fun buildInsightsPrompt(dataString: String): String {
        return """You are a financial analytics engine.

Analyze the given data and generate EXACTLY 3 insights.

Rules:
- Max 15 words per insight
- No advice
- Only observations
- Output numbered list (1. ... 2. ... 3. ...)
- Keep insights factual and data-driven

Data:
$dataString""".trimIndent()
    }

    /**
     * Builds a prompt for explaining the user's spending pattern.
     *
     * @param dataString Structured financial data from FinancialDataSummarizer
     * @return Complete prompt ready for LLM ingestion
     */
    fun buildExplainPrompt(dataString: String): String {
        return """You are a personal finance assistant.

Explain the user's spending in simple terms.

Rules:
- Max 4 lines
- Clear and helpful
- No generic advice
- Use the user's local currency
- Focus on key patterns or changes

Data:
$dataString""".trimIndent()
    }

    /**
     * Optional: Builds a prompt for merchant categorization (future enhancement).
     * Included for reference but not used in Phase 8.
     */
    fun buildCategorizationPrompt(merchant: String): String {
        return """You are a spending categorizer.

Categorize the given merchant into exactly ONE category.

Rules:
- Choose from: Food, Transport, Shopping, Entertainment, Utilities, Healthcare, Other
- Output only the category name, nothing else

Merchant:
$merchant""".trimIndent()
    }
}
