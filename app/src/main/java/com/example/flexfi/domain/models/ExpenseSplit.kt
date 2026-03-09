package com.example.flexfi.domain.models

data class ExpenseSplit(
    val id: String = "",
    val expenseId: String = "",
    val memberPhone: String = "",
    val shareAmount: Double = 0.0
)
