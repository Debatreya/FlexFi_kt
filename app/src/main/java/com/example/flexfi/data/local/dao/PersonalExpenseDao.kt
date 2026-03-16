package com.example.flexfi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: PersonalExpenseEntity)

    /** Replaces the full row matched by primary key — used for editing manual expenses */
    @Update
    suspend fun update(expense: PersonalExpenseEntity)

    /** Deletes a single manual personal expense by its id */
    @Query("DELETE FROM personal_expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    /** All personal expenses for the given user, newest first */
    @Query("SELECT * FROM personal_expenses WHERE userPhone = :phone ORDER BY createdAt DESC")
    fun getExpenses(phone: String): Flow<List<PersonalExpenseEntity>>

    /** Sum of all base amounts (USD) for the given user */
    @Query("SELECT SUM(baseAmount) FROM personal_expenses WHERE userPhone = :phone")
    fun getTotalSpent(phone: String): Flow<Double?>

    /** Called when a group's expenses are re-synced — wipe stale personal records */
    @Query("DELETE FROM personal_expenses WHERE sourceGroupId = :groupId")
    suspend fun deleteBySourceGroupId(groupId: String)

    /** Called when a single expense is deleted — removes only its personal mirrors */
    @Query("DELETE FROM personal_expenses WHERE sourceExpenseId = :expenseId")
    suspend fun deleteBySourceExpenseId(expenseId: String)

    /** Called on user logout */
    @Query("DELETE FROM personal_expenses")
    suspend fun deleteAll()

    /** Filter expenses by type (INCOME, EXPENSE, TRANSFER) */
    @Query("SELECT * FROM personal_expenses WHERE userPhone = :phone AND type = :type ORDER BY createdAt DESC")
    fun getExpensesByType(phone: String, type: String): Flow<List<PersonalExpenseEntity>>

    /** Filter expenses by payment mode */
    @Query("SELECT * FROM personal_expenses WHERE userPhone = :phone AND paymentMode = :mode ORDER BY createdAt DESC")
    fun getExpensesByPaymentMode(phone: String, mode: String): Flow<List<PersonalExpenseEntity>>

    /** Search expenses by tag (comma-separated field) */
    @Query("SELECT * FROM personal_expenses WHERE userPhone = :phone AND tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun getExpensesByTag(phone: String, tag: String): Flow<List<PersonalExpenseEntity>>

    /** Get expenses in a date range (for budget calculations) */
    @Query("SELECT * FROM personal_expenses WHERE userPhone = :phone AND createdAt >= :startMillis AND createdAt <= :endMillis ORDER BY createdAt DESC")
    fun getExpensesInRange(phone: String, startMillis: Long, endMillis: Long): Flow<List<PersonalExpenseEntity>>

    /** Sum of base amounts for a specific category in a date range (for budget vs actual) */
    @Query("SELECT SUM(baseAmount) FROM personal_expenses WHERE userPhone = :phone AND category = :category AND type = 'EXPENSE' AND createdAt >= :startMillis AND createdAt <= :endMillis")
    fun getCategorySpentInRange(phone: String, category: String, startMillis: Long, endMillis: Long): Flow<Double?>

    /** Total spent (expenses only) in a date range */
    @Query("SELECT SUM(baseAmount) FROM personal_expenses WHERE userPhone = :phone AND type = 'EXPENSE' AND createdAt >= :startMillis AND createdAt <= :endMillis")
    fun getTotalSpentInRange(phone: String, startMillis: Long, endMillis: Long): Flow<Double?>
}
