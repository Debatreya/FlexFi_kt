package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val type: String, // "INCOME" or "EXPENSE"
    val interval: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextOccurrence: Long,
    val lastProcessed: Long? = null,
    val isActive: Boolean = true
)
