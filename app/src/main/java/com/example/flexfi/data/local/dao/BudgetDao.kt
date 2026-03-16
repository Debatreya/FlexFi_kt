package com.example.flexfi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flexfi.data.local.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)

    /** All budgets for a user in a specific month/year */
    @Query("SELECT * FROM budgets WHERE userPhone = :phone AND month = :month AND year = :year ORDER BY category ASC")
    fun getBudgets(phone: String, month: Int, year: Int): Flow<List<BudgetEntity>>

    /** Get the overall budget for a specific month/year */
    @Query("SELECT * FROM budgets WHERE userPhone = :phone AND category = 'OVERALL' AND month = :month AND year = :year LIMIT 1")
    suspend fun getOverallBudget(phone: String, month: Int, year: Int): BudgetEntity?

    /** Get a specific category budget */
    @Query("SELECT * FROM budgets WHERE userPhone = :phone AND category = :category AND month = :month AND year = :year LIMIT 1")
    suspend fun getCategoryBudget(phone: String, category: String, month: Int, year: Int): BudgetEntity?

    /** Get previous month's budgets for rollover calculation */
    @Query("SELECT * FROM budgets WHERE userPhone = :phone AND month = :month AND year = :year AND rollover = 1")
    suspend fun getRolloverBudgets(phone: String, month: Int, year: Int): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
