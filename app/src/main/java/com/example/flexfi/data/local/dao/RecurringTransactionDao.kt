package com.example.flexfi.data.local.dao

import androidx.room.*
import com.example.flexfi.data.local.entities.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextOccurrence <= :now")
    suspend fun getDueTransactions(now: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: RecurringTransactionEntity)

    @Update
    suspend fun update(transaction: RecurringTransactionEntity)

    @Delete
    suspend fun delete(transaction: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET isActive = :active WHERE id = :id")
    suspend fun setStatus(id: String, active: Boolean)
}
