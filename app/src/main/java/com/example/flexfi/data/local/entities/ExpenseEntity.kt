package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val title: String,
    val amount: Double,
    val paidByPhone: String,
    val category: String,
    val createdAt: Long
)
