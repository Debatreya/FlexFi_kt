package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a budget limit for a specific category (or "OVERALL") for a given month/year.
 * Amounts are stored in base currency (USD).
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val userPhone: String,
    val category: String,            // "Food", "Transport", "OVERALL", etc.
    val limitAmount: Double,         // Budget limit in base currency (USD)
    val month: Int,                  // 1-12
    val year: Int,
    val rollover: Boolean = false,
    val rolledOverAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
