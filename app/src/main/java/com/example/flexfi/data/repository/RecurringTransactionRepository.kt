package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.RecurringTransactionDao
import com.example.flexfi.data.local.entities.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RecurringTransactionRepository(
    private val recurringTransactionDao: RecurringTransactionDao
) {

    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>> =
        recurringTransactionDao.getAllRecurring()

    suspend fun getDueTransactions(now: Long): List<RecurringTransactionEntity> =
        recurringTransactionDao.getDueTransactions(now)

    suspend fun createRecurring(
        title: String,
        amount: Double,
        currency: String,
        category: String,
        type: String,
        interval: String,
        firstOccurrence: Long
    ) {
        recurringTransactionDao.insert(
            RecurringTransactionEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                currency = currency,
                category = category,
                type = type,
                interval = interval,
                nextOccurrence = firstOccurrence,
                lastProcessed = null,
                isActive = true
            )
        )
    }

    suspend fun updateRecurring(transaction: RecurringTransactionEntity) {
        recurringTransactionDao.update(transaction)
    }

    suspend fun deleteRecurring(transaction: RecurringTransactionEntity) {
        recurringTransactionDao.delete(transaction)
    }

    suspend fun setStatus(id: String, active: Boolean) {
        recurringTransactionDao.setStatus(id, active)
    }
}
