package com.example.flexfi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: PersonalExpenseEntity)

    /** All personal expenses for the given user, newest first */
    @Query("SELECT * FROM personal_expenses WHERE userPhone = :phone ORDER BY createdAt DESC")
    fun getExpenses(phone: String): Flow<List<PersonalExpenseEntity>>

    /** Sum of all amounts for the given user */
    @Query("SELECT SUM(amount) FROM personal_expenses WHERE userPhone = :phone")
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
}
