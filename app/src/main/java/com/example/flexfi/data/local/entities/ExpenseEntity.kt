package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val title: String,
    val amount: Double, // Original amount in original currency
    val currency: String = "INR", // e.g., "INR", "USD"
    val exchangeRateToBase: Double = 1.0, // Rate to convert to USD
    val baseAmount: Double = 0.0, // Amount in USD
    val paidByPhone: String,
    val category: String,
    val createdAt: Long
)
