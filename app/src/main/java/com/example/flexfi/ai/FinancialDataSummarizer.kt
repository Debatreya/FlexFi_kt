package com.example.flexfi.ai

import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.local.entities.SettlementRecordEntity
import com.example.flexfi.utils.CurrencyProvider
import kotlin.math.abs

/**
 * Transforms raw financial data into a structured text summary
 * suitable for LLM prompt injection.
 *
 * Converts all amounts from USD base to display currency for readability.
 */
object FinancialDataSummarizer {

    data class CompactMetrics(
        val totalSpend: Double,
        val topCategories: List<Pair<String, Double>>,
        val topMerchants: List<Pair<String, Double>>
    )

    /**
     * Generates a structured monthly financial summary from transaction data.
     *
     * @param personalExpenses List of personal expense entities for the month
     * @param settlements List of settlement records for the month
     * @param streak Current expense tracking streak (days)
     * @return Formatted string ready for LLM ingestion
     */
    fun summarizeMonthlyData(
        personalExpenses: List<PersonalExpenseEntity>,
        settlements: List<SettlementRecordEntity>,
        streak: Int
    ): String {
        val sb = StringBuilder()

        // ─────── HEADER ───────
        sb.append("Monthly Financial Summary:\n")

        // ─────── TOTALS ───────
        val totalSpent = personalExpenses
            .filter { it.type == "EXPENSE" || it.type.isBlank() }
            .sumOf { abs(it.baseAmount) }

        val displayTotal = CurrencyProvider.formatAmount(totalSpent)
        sb.append("- Total Spend: $displayTotal\n")

        val totalIncome = personalExpenses
            .filter { it.type == "INCOME" }
            .sumOf { it.baseAmount }

        if (totalIncome > 0) {
            val displayIncome = CurrencyProvider.formatAmount(totalIncome)
            sb.append("- Total Income: $displayIncome\n")
        }

        // Net settlements (balance from groups)
        val netSettlement = calculateNetSettlement(settlements)
        if (netSettlement != 0.0) {
            val sign = if (netSettlement > 0) "owe" else "owed"
            val displayNetSettlement = CurrencyProvider.formatAmount(abs(netSettlement))
            sb.append("- Net Settlement: You $sign $displayNetSettlement\n")
        }

        sb.append("\n")

        // ─────── CATEGORY BREAKDOWN ───────
        sb.append("Category Breakdown:\n")
        val categorySpend = analyzeByCategory(personalExpenses)

        if (categorySpend.isNotEmpty()) {
            val sortedCategories = categorySpend.entries
                .sortedByDescending { it.value }
                .take(6) // Top 6 categories

            sortedCategories.forEach { (category, amount) ->
                val percentage = if (totalSpent > 0) (amount / totalSpent * 100).toInt() else 0
                val displayAmount = CurrencyProvider.formatAmount(amount)
                sb.append("- ${category.capitalize()}: $displayAmount ($percentage%)\n")
            }
        } else {
            sb.append("- No expenses recorded\n")
        }

        sb.append("\n")

        // ─────── TOP MERCHANTS ───────
        sb.append("Top Merchants:\n")
        val topMerchants = analyzeByMerchant(personalExpenses)

        if (topMerchants.isNotEmpty()) {
            topMerchants.take(5).forEach { (merchant, amount) ->
                val displayAmount = CurrencyProvider.formatAmount(amount)
                sb.append("- $merchant: $displayAmount\n")
            }
        } else {
            sb.append("- No merchant data available\n")
        }

        sb.append("\n")

        // ─────── BEHAVIOR ───────
        sb.append("Spending Behavior:\n")
        sb.append("- Streak: $streak days\n")

        if (personalExpenses.isNotEmpty()) {
            val avgDaily = totalSpent / maxOf(1, streak)
            val displayAvg = CurrencyProvider.formatAmount(avgDaily)
            sb.append("- Average Daily Spend: $displayAvg\n")

            val highestCategory = categorySpend.maxByOrNull { it.value }?.key
            if (highestCategory != null) {
                sb.append("- Highest Category: ${highestCategory.capitalize()}\n")
            }
        }

        return sb.toString()
    }

    /**
     * Compact key-value context for low-latency prompts.
     */
    fun summarizeCompactData(
        currentMonthExpenses: List<PersonalExpenseEntity>,
        previousMonthExpenses: List<PersonalExpenseEntity>,
        settlements: List<SettlementRecordEntity>,
        streak: Int
    ): String {
        val current = calculateCompactMetrics(currentMonthExpenses)
        val previous = calculateCompactMetrics(previousMonthExpenses)

        val topCategory = current.topCategories.joinToString(",") { "${it.first}:${it.second.toInt()}" }
        val topMerchants = current.topMerchants.joinToString(",") { "${it.first}:${it.second.toInt()}" }

        val safeTopCategory = if (topCategory.isBlank()) "None:0" else topCategory
        val safeTopMerchants = if (topMerchants.isBlank()) "None:0" else topMerchants

        // Separate settlements into YouOwe and OwedToYou for clarity
        val (youOwe, owedToYou) = calculateSettlementSplit(settlements)

        return buildString {
            appendLine("Spend=${current.totalSpend.toInt()}")
            appendLine("Prev=${previous.totalSpend.toInt()}")
            appendLine("YouOwe=${youOwe.toInt()}")
            appendLine("OwedToYou=${owedToYou.toInt()}")
            appendLine("TopCat=$safeTopCategory")
            appendLine("Top=$safeTopMerchants")
            append("Streak=$streak")
        }
    }

    /**
     * Analyzes expenses by category.
     * @return Map of category -> total amount
     */
    private fun analyzeByCategory(expenses: List<PersonalExpenseEntity>): Map<String, Double> {
        return expenses
            .filter { it.type == "EXPENSE" || it.type.isBlank() }
            .groupingBy { it.category }
            .fold(0.0) { acc, expense -> acc + abs(expense.baseAmount) }
    }

    /**
     * Analyzes expenses by merchant.
     * @return Map of merchant -> total amount, sorted by amount descending
     */
    private fun analyzeByMerchant(expenses: List<PersonalExpenseEntity>): List<Pair<String, Double>> {
        return expenses
            .filter { it.type == "EXPENSE" || it.type.isBlank() }
            .filter { it.merchant != null && it.merchant.isNotBlank() }
            .groupingBy { it.merchant!! }
            .fold(0.0) { acc, expense -> acc + abs(expense.baseAmount) }
            .toList()
            .sortedByDescending { it.second }
    }

    private fun calculateCompactMetrics(expenses: List<PersonalExpenseEntity>): CompactMetrics {
        val total = expenses
            .filter { it.type == "EXPENSE" || it.type.isBlank() }
            .sumOf { abs(it.baseAmount) }

        val topCategories = analyzeByCategory(expenses)
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        val topMerchants = analyzeByMerchant(expenses)
            .take(2)

        return CompactMetrics(
            totalSpend = total,
            topCategories = topCategories,
            topMerchants = topMerchants
        )
    }

    /**
     * Calculates net settlement balance for the user.
     * Positive = user owes money; Negative = user is owed money.
     */
    private fun calculateNetSettlement(settlements: List<SettlementRecordEntity>): Double {
        // Simplified: sum all amounts (sign indicates direction)
        // This is a placeholder; actual logic depends on settlement direction representation
        return settlements.sumOf { it.amount }
    }

    /**
     * Splits settlements into two separate amounts for clarity.
     * Returns Pair<YouOwe, OwedToYou>
     */
    private fun calculateSettlementSplit(settlements: List<SettlementRecordEntity>): Pair<Double, Double> {
        var youOwe = 0.0
        var owedToYou = 0.0
        
        settlements.forEach { settlement ->
            if (settlement.amount > 0) {
                youOwe += settlement.amount
            } else {
                owedToYou += abs(settlement.amount)
            }
        }
        
        return Pair(youOwe, owedToYou)
    }

    /**
     * Generates a hash of the summary data for cache validation.
     * Same data should always hash to the same value.
     */
    fun generateDataHash(summary: String): Long {
        return summary.hashCode().toLong()
    }

    /**
     * Helper to capitalize strings safely.
     */
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
