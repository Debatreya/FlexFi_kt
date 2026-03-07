package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val groupId: String?,
    val amount: Double,
    val paidBy: String,
    val category: String,
    val timestamp: Long,
    val createdBy: String,
    val isSynced: Boolean
)
