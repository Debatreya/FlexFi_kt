package com.example.flexfi.data.repository

import com.example.flexfi.data.local.dao.PersonalExpenseDao
import com.example.flexfi.data.local.entities.PersonalExpenseEntity
import com.example.flexfi.data.remote.ExchangeRateApi
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Thin wrapper around [PersonalExpenseDao].
 * All personal expense data lives in Room only — no Firestore.
 */
class PersonalExpenseRepository(
    private val personalExpenseDao: PersonalExpenseDao,
    private val exchangeRateApi: ExchangeRateApi
) {

    /** Reactive stream: all personal expenses for [phone], newest first */
    fun getExpenses(phone: String): Flow<List<PersonalExpenseEntity>> =
        personalExpenseDao.getExpenses(phone)

    /** Reactive stream: running total spent by [phone] */
    fun getTotalSpent(phone: String): Flow<Double?> =
        personalExpenseDao.getTotalSpent(phone)

    /** Expenses in a date range (for budget calculations) */
    fun getExpensesInRange(phone: String, startMillis: Long, endMillis: Long): Flow<List<PersonalExpenseEntity>> =
        personalExpenseDao.getExpensesInRange(phone, startMillis, endMillis)

    /**
     * Inserts a manually entered personal expense.
     * [source] is always "PERSONAL"; not linked to any group expense.
     */
    suspend fun insertManual(
        userPhone: String,
        title: String,
        amount: Double,
        currency: String,
        category: String,
        dateMillis: Long,
        subCategory: String? = null,
        type: String = "EXPENSE",
        paymentMode: String = "Cash",
        merchant: String? = null,
        tags: String? = null,
        attachmentUri: String? = null
    ) {
        val rateToBase = exchangeRateApi.getRate(
            fromCurrency = currency,
            toCurrency = "USD",
            dateMillis = dateMillis
        )
        val baseAmount = amount * rateToBase

        val entity = PersonalExpenseEntity(
            id = UUID.randomUUID().toString(),
            userPhone = userPhone,
            amount = amount,
            currency = currency,
            exchangeRateToBase = rateToBase,
            baseAmount = baseAmount,
            category = category.ifBlank { "Other" },
            subCategory = subCategory,
            type = type,
            paymentMode = paymentMode,
            merchant = merchant,
            tags = tags,
            attachmentUri = attachmentUri,
            source = "PERSONAL",
            sourceExpenseId = null,
            sourceGroupId = null,
            description = title,
            createdAt = dateMillis
        )
        personalExpenseDao.insert(entity)
    }

    suspend fun insertFromRecurring(
        userPhone: String,
        title: String,
        amount: Double,
        currency: String,
        category: String,
        dateMillis: Long
    ) {
        val rateToBase = exchangeRateApi.getRate(
            fromCurrency = currency,
            toCurrency = "USD",
            dateMillis = dateMillis
        )
        val baseAmount = amount * rateToBase

        personalExpenseDao.insert(
            PersonalExpenseEntity(
                id = UUID.randomUUID().toString(),
                userPhone = userPhone,
                amount = amount,
                currency = currency,
                exchangeRateToBase = rateToBase,
                baseAmount = baseAmount,
                category = category.ifBlank { "Other" },
                source = "PERSONAL",
                sourceExpenseId = null,
                sourceGroupId = null,
                description = title,
                createdAt = dateMillis
            )
        )
    }

    /** Updates an existing personal expense record (edit flow) */
    suspend fun update(expense: PersonalExpenseEntity) =
        personalExpenseDao.update(expense)

    /** Deletes a single personal expense by id */
    suspend fun deleteById(id: String) =
        personalExpenseDao.deleteById(id)

    /** Remove records for a specific group (called before re-syncing from Firestore) */
    suspend fun deleteBySourceGroupId(groupId: String) =
        personalExpenseDao.deleteBySourceGroupId(groupId)

    /** Called on logout — clears all personal data for this device user */
    suspend fun deleteAll() =
        personalExpenseDao.deleteAll()
}
