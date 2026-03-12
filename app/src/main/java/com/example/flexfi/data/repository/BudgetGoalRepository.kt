package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.BudgetGoalDao
import com.example.flexfi.data.local.entities.BudgetGoalEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BudgetGoalRepository(
    private val budgetGoalDao: BudgetGoalDao
) {
    fun getGoalsForUser(userPhone: String): Flow<List<BudgetGoalEntity>> =
        budgetGoalDao.getGoalsForUser(userPhone)

    fun getTotalSaved(userPhone: String): Flow<Double> =
        budgetGoalDao.getTotalSaved(userPhone)

    fun getTotalTarget(userPhone: String): Flow<Double> =
        budgetGoalDao.getTotalTarget(userPhone)

    suspend fun getGoalById(goalId: String): BudgetGoalEntity? =
        budgetGoalDao.getGoalById(goalId)

    suspend fun createGoal(
        userPhone: String,
        title: String,
        description: String,
        targetAmount: Double,
        targetDate: Long?,
        autoSaveAmount: Double,
        autoSaveFrequency: String
    ) {
        val goal = BudgetGoalEntity(
            id = UUID.randomUUID().toString(),
            userPhone = userPhone,
            title = title,
            description = description,
            targetAmount = targetAmount,
            targetDate = targetDate,
            autoSaveAmount = autoSaveAmount,
            autoSaveFrequency = autoSaveFrequency
        )
        budgetGoalDao.insert(goal)
    }

    suspend fun updateGoal(goal: BudgetGoalEntity) {
        budgetGoalDao.update(goal)
    }

    suspend fun addSavings(goalId: String, amount: Double) {
        val goal = budgetGoalDao.getGoalById(goalId) ?: return
        budgetGoalDao.update(goal.copy(savedAmount = goal.savedAmount + amount))
    }

    suspend fun deleteGoal(goalId: String) {
        budgetGoalDao.deleteById(goalId)
    }
}
