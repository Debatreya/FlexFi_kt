package com.example.flexfi.domain.models

data class ExpenseSplit(
    val userId: String = "",
    val amount: Double = 0.0,
    val isPaid: Boolean = false
)
