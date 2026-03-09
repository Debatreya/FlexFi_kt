package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.PersonalExpenseDao
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper around [PersonalExpenseDao].
 * All personal expense data lives in Room only — no Firestore.
 */
class PersonalExpenseRepository(
    private val personalExpenseDao: PersonalExpenseDao
) {

    /** Reactive stream: all personal expenses for [phone], newest first */
    fun getExpenses(phone: String): Flow<List<PersonalExpenseEntity>> =
        personalExpenseDao.getExpenses(phone)

    /** Reactive stream: running total spent by [phone] */
    fun getTotalSpent(phone: String): Flow<Double?> =
        personalExpenseDao.getTotalSpent(phone)

    /** Remove records for a specific group (called before re-syncing from Firestore) */
    suspend fun deleteBySourceGroupId(groupId: String) =
        personalExpenseDao.deleteBySourceGroupId(groupId)

    /** Called on logout — clears all personal data for this device user */
    suspend fun deleteAll() =
        personalExpenseDao.deleteAll()
}
