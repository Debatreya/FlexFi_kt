package com.example.flexfi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.flexfi.data.local.entities.BudgetGoalEntity
import com.example.flexfi.data.local.entities.GoalContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: BudgetGoalEntity)

    @Update
    suspend fun update(goal: BudgetGoalEntity)

    @Delete
    suspend fun delete(goal: BudgetGoalEntity)

    @Query("SELECT * FROM budget_goals WHERE userPhone = :userPhone ORDER BY createdAt DESC")
    fun getGoalsForUser(userPhone: String): Flow<List<BudgetGoalEntity>>

    @Query("SELECT * FROM budget_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: String): BudgetGoalEntity?

    @Query("SELECT COALESCE(SUM(savedAmount), 0.0) FROM budget_goals WHERE userPhone = :userPhone")
    fun getTotalSaved(userPhone: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(targetAmount), 0.0) FROM budget_goals WHERE userPhone = :userPhone")
    fun getTotalTarget(userPhone: String): Flow<Double>

    @Query("DELETE FROM budget_goals WHERE id = :goalId")
    suspend fun deleteById(goalId: String)

    // --- Contributions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: GoalContributionEntity)

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    fun getContributionsForGoal(goalId: String): Flow<List<GoalContributionEntity>>
}
