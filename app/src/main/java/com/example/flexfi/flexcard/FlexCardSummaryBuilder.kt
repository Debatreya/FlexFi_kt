package com.example.flexfi.flexcard

object FlexCardSummaryBuilder {

    data class Input(
        val totalSpend: Double,
        val totalIncome: Double,
        val budget: Double,
        val previousSpend: Double,
        val topCategory: String,
        val maxCategoryShare: Double,
        val streakDays: Int,
        val smallTransactionRatio: Double,
        val totalTransactions: Int,
        val monthLabel: String
    )

    fun build(input: Input): String {
        return buildString {
            appendLine("Month=${input.monthLabel}")
            appendLine("TotalSpend=${input.totalSpend.toInt()}")
            appendLine("TotalIncome=${input.totalIncome.toInt()}")
            appendLine("Budget=${input.budget.toInt()}")
            appendLine("PreviousSpend=${input.previousSpend.toInt()}")
            appendLine("TopCategory=${input.topCategory}")
            appendLine("CategoryConcentration=${(input.maxCategoryShare * 100).toInt()}")
            appendLine("Streak=${input.streakDays}")
            appendLine("SmallTxRatio=${(input.smallTransactionRatio * 100).toInt()}")
            append("TotalTx=${input.totalTransactions}")
        }
    }
}
