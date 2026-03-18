package com.example.flexfi.flexcard

import kotlin.math.max

data class FlexScoreInput(
    val totalSpend: Double,
    val totalIncome: Double,
    val budget: Double,
    val previousMonthSpend: Double?,
    val maxCategoryShare: Double,
    val streakDays: Int,
    val smallTransactionRatio: Double,
    val totalTransactions: Int,
    val lastMonthScore: Int?
)

object FlexScoreCalculator {

    fun calculate(input: FlexScoreInput): FlexScore {
        val budgetScore = budgetScore(input.totalSpend, input.budget)
        val savingsScore = savingsScore(input.totalIncome, input.totalSpend)
        val growthScore = growthScore(input.totalSpend, input.previousMonthSpend)
        val balanceScore = balanceScore(input.maxCategoryShare)
        val streakScore = streakScore(input.streakDays)
        val impulseScore = impulseScore(input.smallTransactionRatio)

        val weights = adjustedWeights(input.totalTransactions)

        var weightedScore =
            weights.budget * budgetScore +
                weights.savings * savingsScore +
                weights.growth * growthScore +
                weights.balance * balanceScore +
                weights.streak * streakScore +
                weights.impulse * impulseScore

        if (input.totalSpend <= 0.0) {
            weightedScore = minOf(weightedScore, 85.0)
        }

        val finalScore = clamp(weightedScore).toInt()
        val grade = gradeFor(finalScore)
        val trend = trendFor(finalScore, input.lastMonthScore)

        return FlexScore(
            score = finalScore,
            grade = grade,
            trend = trend
        )
    }

    private fun budgetScore(totalSpend: Double, budget: Double): Double {
        if (budget <= 0.0) return 50.0

        val ratio = totalSpend / budget
        val score = if (ratio <= 1.0) {
            100.0 - (ratio * 30.0)
        } else {
            max(0.0, 70.0 - ((ratio - 1.0) * 100.0))
        }
        return clamp(score)
    }

    private fun savingsScore(totalIncome: Double, totalSpend: Double): Double {
        if (totalIncome <= 0.0) return 50.0

        val savings = totalIncome - totalSpend
        val rate = savings / totalIncome
        return clamp(rate * 100.0)
    }

    private fun growthScore(totalSpend: Double, previousMonthSpend: Double?): Double {
        if (previousMonthSpend == null || previousMonthSpend <= 0.0) {
            return 75.0
        }

        val growth = (totalSpend - previousMonthSpend) / previousMonthSpend
        val score = if (growth <= 0.0) {
            100.0
        } else {
            max(0.0, 100.0 - (growth * 200.0))
        }
        return clamp(score)
    }

    private fun balanceScore(maxCategoryShare: Double): Double {
        return clamp(100.0 - (maxCategoryShare.coerceIn(0.0, 1.0) * 100.0))
    }

    private fun streakScore(streakDays: Int): Double {
        return clamp(streakDays * 10.0)
    }

    private fun impulseScore(smallTransactionRatio: Double): Double {
        return clamp(100.0 - (smallTransactionRatio.coerceIn(0.0, 1.0) * 100.0))
    }

    private fun trendFor(currentScore: Int, lastMonthScore: Int?): String {
        if (lastMonthScore == null) return "Stable"
        val diff = currentScore - lastMonthScore
        return when {
            diff > 5 -> "Improving"
            diff < -5 -> "Declining"
            else -> "Stable"
        }
    }

    private fun gradeFor(score: Int): String {
        return when {
            score >= 90 -> "ELITE"
            score >= 75 -> "GOLD"
            score >= 60 -> "SILVER"
            score >= 40 -> "BRONZE"
            else -> "BEGINNER"
        }
    }

    private data class Weights(
        val budget: Double,
        val savings: Double,
        val growth: Double,
        val balance: Double,
        val streak: Double,
        val impulse: Double
    )

    private fun adjustedWeights(totalTransactions: Int): Weights {
        if (totalTransactions >= 5) {
            return Weights(
                budget = 0.30,
                savings = 0.25,
                growth = 0.15,
                balance = 0.10,
                streak = 0.10,
                impulse = 0.10
            )
        }

        val lowSignalWeight = 0.05
        val reclaimed = (0.10 - lowSignalWeight) * 2

        return Weights(
            budget = 0.30 + reclaimed * 0.45,
            savings = 0.25 + reclaimed * 0.35,
            growth = 0.15 + reclaimed * 0.20,
            balance = lowSignalWeight,
            streak = 0.10,
            impulse = lowSignalWeight
        )
    }

    private fun clamp(value: Double): Double {
        return value.coerceIn(0.0, 100.0)
    }
}
