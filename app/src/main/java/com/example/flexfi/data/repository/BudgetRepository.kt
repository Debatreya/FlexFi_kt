package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.BudgetDao
import com.example.flexfi.data.local.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository layer for budget management.
 * Handles CRUD operations and rollover logic.
 */
class BudgetRepository(
    private val budgetDao: BudgetDao
) {

    fun getBudgets(phone: String, month: Int, year: Int): Flow<List<BudgetEntity>> =
        budgetDao.getBudgets(phone, month, year)

    suspend fun getOverallBudget(phone: String, month: Int, year: Int): BudgetEntity? =
        budgetDao.getOverallBudget(phone, month, year)

    suspend fun getCategoryBudget(phone: String, category: String, month: Int, year: Int): BudgetEntity? =
        budgetDao.getCategoryBudget(phone, category, month, year)

    suspend fun upsertBudget(
        userPhone: String,
        category: String,
        limitAmount: Double,
        month: Int,
        year: Int,
        rollover: Boolean = false
    ) {
        val existing = budgetDao.getCategoryBudget(userPhone, category, month, year)
        if (existing != null) {
            budgetDao.update(existing.copy(
                limitAmount = limitAmount,
                rollover = rollover
            ))
        } else {
            budgetDao.insert(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    userPhone = userPhone,
                    category = category,
                    limitAmount = limitAmount,
                    month = month,
                    year = year,
                    rollover = rollover
                )
            )
        }
    }

    suspend fun deleteBudget(id: String) = budgetDao.deleteById(id)

    /**
     * Calculate rolled-over amount from previous month.
     * If a budget had rollover enabled and was underspent,
     * the remaining amount carries forward.
     */
    suspend fun calculateRollover(
        phone: String,
        prevMonth: Int,
        prevYear: Int,
        spentByCategory: Map<String, Double>
    ): Map<String, Double> {
        val rolloverBudgets = budgetDao.getRolloverBudgets(phone, prevMonth, prevYear)
        val result = mutableMapOf<String, Double>()
        for (budget in rolloverBudgets) {
            val spent = spentByCategory[budget.category] ?: 0.0
            val remaining = (budget.limitAmount + budget.rolledOverAmount) - spent
            if (remaining > 0) {
                result[budget.category] = remaining
            }
        }
        return result
    }

    suspend fun deleteAll() = budgetDao.deleteAll()
}
