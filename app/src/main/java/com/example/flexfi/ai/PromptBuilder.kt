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
        return """You are a finance analytics engine.
Return EXACTLY 3 factual observations from data.
Rules:
- Max 15 words each
- No advice
- Use only given data
- Output numbered list: 1. 2. 3.
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
Explain spending patterns from data only.
Rules:
- Max 5 lines
- Use only given data
- Cite specific numbers from data
- No generic advice
- No hallucination
Data:
$dataString""".trimIndent()
    }

    /**
     * Strict stateless prompt for assistant mode.
     */
    fun buildAssistantPrompt(dataString: String, question: String): String {
        return """You are a personal finance assistant.

Answer the user's question using the given data.

Rules:
- Be concise (max 3 lines)
- Use only given data
- No generic advice
- No hallucination

Data:
$dataString

Question:
$question""".trimIndent()
    }

    /**
     * Builds a strict JSON-only prompt for Flex Card content generation.
     */
    fun buildFlexCardPrompt(dataString: String): String {
        return """You are a financial insights generator.

Return JSON ONLY with:
- 3 highlights
- 1 improvement
- 1 tagline

Rules:
- No extra text
- No explanation
- Max 12 words per line
- No hallucinated numbers

Expected JSON shape:
{
  "highlights": [],
  "improvement": "",
  "tagline": ""
}

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
