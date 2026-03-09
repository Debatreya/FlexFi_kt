package com.example.flexfi.domain.models

/**
 * Domain model for a personal expense record.
 * Mirrors [com.example.flexfi.data.local.entities.PersonalExpenseEntity].
 */
data class PersonalExpense(
    val id: String,
    val userPhone: String,
    val amount: Double,
    val category: String,
    val source: String,
    val sourceGroupId: String?,
    val description: String?,
    val createdAt: Long
)
