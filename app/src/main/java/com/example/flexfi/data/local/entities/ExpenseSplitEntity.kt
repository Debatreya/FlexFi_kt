package com.example.flexfi.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_splits")
data class ExpenseSplitEntity(
    @PrimaryKey
    val id: String,
    val expenseId: String,
    val memberPhone: String,
    val shareAmount: Double
)
